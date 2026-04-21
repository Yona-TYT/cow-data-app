package com.example.cow_data.drive;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;


import com.example.cow_data.AppContextProvider;
import com.example.cow_data.Basic;
import com.example.cow_data.R;
import com.example.cow_data.StartVar;
import com.example.cow_data.ex.Dialogs;
import com.example.cow_data.ex.DownloadEvents;
import com.example.cow_data.ex.EventBusHook;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.ex.PreferenceNames;
import com.example.cow_data.ex.UploadEvents;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.greenrobot.event.EventBus;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;

import com.example.cow_data.activitys.MainActivity;


public class SettingsFragment extends PreferenceFragmentCompat implements
        SimpleDialog.OnDialogResultListener,
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(SettingsFragment.class);

    DriveManager manager;

    private AuthState authState = new AuthState();
    private AuthorizationService authorizationService;
    private SetWorkResult mWorkResult;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = new DriveManager(PreferenceHelper.getInstance());


        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            findPreference(PreferenceNames.AUTOSEND_GOOGLE_DRIVE_ENABLED).setEnabled(false);
            ((SwitchPreferenceCompat)findPreference(PreferenceNames.AUTOSEND_GOOGLE_DRIVE_ENABLED)).setChecked(false);
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setEnabled(false);
            //https://github.com/openid/AppAuth-Android/issues/299
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setTitle("This feature does not work on devices lower than Android 5 (Lollipop)");
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setSummary("This is due to a limitation in the OAuth2 workflow.");
            findPreference(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH).setEnabled(false);
            findPreference("google_drive_test").setEnabled(false);
            return;
        }

        findPreference(PreferenceNames.AUTOSEND_GOOGLE_DRIVE_ENABLED).setVisible(false); // Ocultar la preferencia

        findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setOnPreferenceClickListener(this);
        findPreference(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH).setOnPreferenceClickListener(this);
        findPreference("google_drive_test").setOnPreferenceClickListener(this);
        findPreference("google_drive_sync").setOnPreferenceClickListener(this);
        findPreference("google_drive_sync_img").setOnPreferenceClickListener(this);

        DropDownPreference backupSpinner = findPreference("google_drive_backups");
        if (backupSpinner != null) {
            loadDriveCsvBackups(backupSpinner);
            backupSpinner.setOnPreferenceChangeListener(this);
        }

        registerEventBus();

        setPreferencesState();

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        mWorkResult = new SetWorkResult(this, executorService, manager);
    }

    @Override
    public void onDestroy() {
        unregisterEventBus();
        super.onDestroy();
    }

    private void unregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    private void loadDriveCsvBackups(DropDownPreference spinner) {
        spinner.setEnabled(false);
        spinner.setSummary("Buscando respaldos en Drive...");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AuthorizationService authorizationService = DriveManager.getAuthorizationService(getContext());
                String googleDriveAccessToken = DriveUtils.getFreshAccessToken(authState, authorizationService);

                // Obtener folderId (tu código actual)
                String folderPath = PreferenceHelper.getInstance().getGoogleDriveFolderPath();
                String[] pathParts = folderPath.split("/");
                String parentFolderId = null;
                String latestFolderId = null;

                for (String part : pathParts) {
                    latestFolderId = DriveUtils.getFileIdFromFileName(googleDriveAccessToken, part, parentFolderId);
                    if (DriveUtils.isNullOrEmpty(latestFolderId)) {
                        latestFolderId = DriveUtils.createEmptyFile(googleDriveAccessToken, part,
                                "application/vnd.google-apps.folder",
                                DriveUtils.isNullOrEmpty(parentFolderId) ? "root" : parentFolderId);
                    }
                    parentFolderId = latestFolderId;
                }

                List<DriveFileMeta> csvFiles = DriveUtils.listCsvFilesFromDrive(googleDriveAccessToken, latestFolderId);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (csvFiles.isEmpty()) {
                        spinner.setSummary("No hay respaldos disponibles");
                        spinner.setEnabled(false);
                        return;
                    }

                    // ==================== ELEMENTO FALSO AL INICIO ====================
                    String[] entries = new String[csvFiles.size() + 1];
                    String[] entryValues = new String[csvFiles.size() + 1];

                    // Primer elemento falso (no hace nada)
                    entries[0] = "— Selecciona un respaldo —";
                    entryValues[0] = "";   // valor vacío = no hacer nada

                    // Agregar los respaldos reales a partir del índice 1
                    for (int i = 0; i < csvFiles.size(); i++) {
                        DriveFileMeta file = csvFiles.get(i);
                        entries[i + 1] = file.name + " (" + file.modifiedTime + ")";
                        entryValues[i + 1] = file.id;
                    }

                    spinner.setEntries(entries);
                    spinner.setEntryValues(entryValues);
                    spinner.setEnabled(true);
                    spinner.setSummary("Selecciona un respaldo para restaurar");

                    // Seleccionamos el elemento falso por defecto
                    spinner.setValue("");
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    spinner.setSummary("Error al cargar respaldos");
                    Log.e("SettingsFragment", "Error cargando backups", e);
                });
            }
        });
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void setPreferencesState() {

        authState = DriveManager.getAuthState();
        if (authState.isAuthorized()) {
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setTitle("Cerrar Seccion"/*R.string.osm_resetauth*/);
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setSummary("Al cerrar la seccion se desactiva la sincronizacion con Drive."/*R.string.google_drive_clearauthorization_summary*/);
        } else {
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setTitle("Iniciar Sesion"/*R.string.osm_lbl_authorize*/);
            findPreference(PreferenceNames.GOOGLE_DRIVE_RESETAUTH).setSummary("Utiliza tu cuenta Google para autentificarte.");
        }
        //findPreference("google_drive_test").setVisible(false); // Ocultar la preferencia

        findPreference("google_drive_test").setEnabled(authState.isAuthorized());
        findPreference("google_drive_sync").setEnabled(authState.isAuthorized());
        findPreference("google_drive_sync_img").setEnabled(authState.isAuthorized());
        findPreference("google_drive_backups").setEnabled(authState.isAuthorized());

        findPreference(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH).setVisible(false); // Ocultar la preferencia
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals("google_drive_backups")) {

            String selectedFileId = (String) newValue;

            // Ignorar si es el elemento falso
            if (DriveUtils.isNullOrEmpty(selectedFileId)) {

                LOG.debug("Elemento 'Selecciona un respaldo' seleccionado → ignorado");
                return true;
            }

            if (!DriveUtils.isNullOrEmpty(selectedFileId)) {
                LOG.debug("Usuario seleccionó respaldo con ID: " + selectedFileId);

                // Confirmación antes de restaurar
                new AlertDialog.Builder(requireContext())
                        .setTitle("Restaurar respaldo")
                        .setMessage("¿Estás seguro de restaurar este respaldo?\n\nEsta acción puede sobrescribir datos existentes.")
                        .setPositiveButton("Restaurar", (dialog, which) -> {

                            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/"+StartVar.dirAppName+"/"+StartVar.csvAppName);
                            String mType = "?alt=media";
                            //DriveUtils.downloadFileFromDrive(accessToken,  selectedFileId, path, mType);
                            manager.dataSynchronizeSelect(selectedFileId);

                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH)) {

            SimpleFormDialog.build()
                    .title("R.string.google_drive_folder_path")
                    .neg("R.string.cancel")
                    .pos("R.string.ok")
                    .msgHtml("getString(R.string.google_drive_folder_path_summary_1)"
                            + "<br /><br />"
                            + "getString(R.string.google_drive_folder_path_summary_2)")
                    .fields(
                            Input.plain(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH)
                                    .text(PreferenceHelper.getInstance().getGoogleDriveFolderPath())
                                    .min(1)
                    )
                    .show(this, PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH);
            return true;
        }

        if (preference.getKey().equals(PreferenceNames.GOOGLE_DRIVE_RESETAUTH)) {
            if (authState.isAuthorized()) {
                authState = new AuthState();
                saveGoogleDriveAuthState();
                setPreferencesState();
                return true;
            }
            authorizationService = DriveManager.getAuthorizationService(getActivity());

            SecureRandom sr = new SecureRandom();
            byte[] ba = new byte[64];
            sr.nextBytes(ba);
            String codeVerifier = android.util.Base64.encodeToString(ba, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes());
                String codeChallenge = android.util.Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

                AuthorizationRequest.Builder requestBuilder = new AuthorizationRequest.Builder(
                        DriveManager.getAuthorizationServiceConfiguration(),
                        DriveManager.getGoogleDriveApplicationClientID(),
                        ResponseTypeValues.CODE,
                        Uri.parse(DriveManager.getGoogleDriveApplicationOauth2Redirect())
                ).setCodeVerifier(codeVerifier, codeChallenge, "S256");

                requestBuilder.setScopes(DriveManager.getGoogleDriveApplicationScopes());
                AuthorizationRequest authRequest = requestBuilder.build();
                Intent authIntent = authorizationService.getAuthorizationRequestIntent(authRequest);
                googleDriveAuthenticationWorkflow.launch(new IntentSenderRequest.Builder(
                        PendingIntent.getActivity(getActivity(), 0, authIntent, PendingIntent.FLAG_IMMUTABLE))
                        .setFillInIntent(authIntent)
                        .build());

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
            return true;
        }

        if (preference.getKey().equals("google_drive_test")) {
//            mWorkResult.observeWorkResult();
//            manager.uploadDataBase();

            manager.dataSynchronizeObj();  //Envia una sincronizacion de datos
            mWorkResult.observeWorkResult(); //Observa los resultados

            return true;
        }

        if (preference.getKey().equals("google_drive_sync")) {
            manager.dataSynchronize();
            mWorkResult.observeWorkResult();
            return true;
        }

        if (preference.getKey().equals("google_drive_sync_img")) {
            Dialogs.progress((FragmentActivity) getActivity(), "Sincronizado Imagenes...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                manager.uploadDataImg();
            }, 500);
            return true;
        }

        return false;
    }

    ActivityResultLauncher<IntentSenderRequest> googleDriveAuthenticationWorkflow = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        LOG.debug(String.valueOf(result.getData()));
                        AuthorizationResponse authResponse = AuthorizationResponse.fromIntent(result.getData());
                        AuthorizationException authException = AuthorizationException.fromIntent(result.getData());
                        authState = new AuthState(authResponse, authException);
                        if (authException != null) {
                            LOG.error(authException.toJsonString(), authException);
                        }
                        if (authResponse != null) {
                            TokenRequest tokenRequest = authResponse.createTokenExchangeRequest();
                            authorizationService.performTokenRequest(tokenRequest, new AuthorizationService.TokenResponseCallback() {
                                @Override
                                public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
                                    if (ex != null) {
                                        authState = new AuthState();
                                        LOG.error(ex.toJsonString(), ex);
                                    } else {
                                        if (response != null) {
                                            authState.update(response, ex);

                                        }
                                    }
                                    saveGoogleDriveAuthState();
                                    setPreferencesState();

                                    //Inicia la sincronizacion
                                    Basic.msg("Sincronizando Datos...");
                                    manager.dataSynchronize();
                                    mWorkResult.observeWorkResult();
                                }
                            });
                        }

                    }

                }
            });

    void saveGoogleDriveAuthState() {
        PreferenceHelper.getInstance().setGoogleDriveAuthState(authState.jsonSerializeString());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.googledrivesettings, rootKey);
    }


    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which != BUTTON_POSITIVE) {
            return true;
        }

        if (dialogTag.equalsIgnoreCase(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH)) {
            PreferenceHelper.getInstance().setGoogleDriveFolderPath(extras.getString(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH));
            findPreference(PreferenceNames.GOOGLE_DRIVE_FOLDER_PATH).setSummary(PreferenceHelper.getInstance().getGoogleDriveFolderPath());
            return true;
        }

        return false;
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.GoogleDrive event) {
        LOG.debug("Evento Google Drive recibido, éxito: " + event.success);
        Dialogs.hideProgress();  // Oculta loading

        if (!event.success) {
            Dialogs.showError(getContext(),
                    "Error",  // Título
                    "No se pudo Sincronizar desde Google Drive",  // Mensaje amigable
                    event.message,
                    event.throwable);
        } else {
            // Detalles opcionales
            @SuppressLint("DefaultLocale") String detailMsg = String.format("✅ %s %d",
                    event.message,
                    event.count);

            DialogInterface.OnClickListener successListener = (dialog, which) -> {
                LOG.debug("Botón [Aceptar] en éxito pulsado");
                dialog.dismiss();  // Opcional

                Intent mIntent = new Intent(AppContextProvider.getContext(), MainActivity.class);
                StartVar.mActivity.startActivity(mIntent);
                StartVar.mActivity.finish();
            };

            if(event.count > 0) {
                Dialogs.progress((FragmentActivity) getActivity(), "Subidos " + event.count +" Archivos...");
            }
            else{
                Dialogs.progress((FragmentActivity) getActivity(), "Sincronizado Imagenes...");
            }

            manager.dataSynchronizeImg();

        }
    }

    @EventBusHook
    public void onEventMainThread(DownloadEvents.GoogleDrive event) {
        LOG.debug("Evento Google Drive recibido, éxito: " + event.success);
        Dialogs.hideProgress();  // Oculta loading

        if (!event.success) {
            Dialogs.showError(getContext(),
                    "Error",  // Título
                    "No se pudo Sincronizar desde Google Drive",  // Mensaje amigable
                    event.message,
                    event.throwable);
        } else {
            // Detalles opcionales
            @SuppressLint("DefaultLocale") String detailMsg = String.format("✅ %s %d",
                    event.message,
                    event.count);

            DialogInterface.OnClickListener successListener = (dialog, which) -> {
                LOG.debug("Botón [Aceptar] en éxito pulsado");
                dialog.dismiss();  // Opcional

                Intent mIntent = new Intent(AppContextProvider.getContext(), MainActivity.class);
                StartVar.mActivity.startActivity(mIntent);
                StartVar.mActivity.finish();
            };
            Dialogs.alert(getContext(),
                    "Completado",
                    detailMsg,
                    successListener);
        }
    }

    public static File createTestFile() throws IOException {
        File gpxFolder = new File(PreferenceHelper.getInstance().getGpsLoggerFolder());
        if (!gpxFolder.exists()) {
            gpxFolder.mkdirs();
        }

        String timeName = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            timeName = LocalTime.now().toString().replaceAll("\\D", "-");
        }
        File testFile = new File(gpxFolder.getPath(), StartVar.nameDBconf);
        if (!testFile.exists()) {
            testFile.createNewFile();

            FileOutputStream initialWriter = new FileOutputStream(testFile, true);
            BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

            initialOutput.write("<x>This is a test file</x>".getBytes());
            initialOutput.flush();
            initialOutput.close();
        }

        return testFile;
    }
}