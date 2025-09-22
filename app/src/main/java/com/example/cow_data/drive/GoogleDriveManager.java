package com.example.cow_data.drive;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cow_data.Basic;
import com.example.cow_data.FilesManager;
import com.example.cow_data.StartVar;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;


import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.json.JSONException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class GoogleDriveManager  {
    private static GoogleDriveManager instance;
    private static final Logger LOG = Logs.of(GoogleDriveManager.class);
    private final PreferenceHelper preferenceHelper;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    private java.io.File file;

    public static synchronized GoogleDriveManager getInstance() {
        if (instance == null) {
            instance = new GoogleDriveManager(PreferenceHelper.getInstance());
        }
        return instance;
    }

    public GoogleDriveManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
        GoogleDriveManager.mContext = StartVar.mContex;
    }

    public static String getGoogleDriveApplicationClientID() {
        //OAuth Client for F-Droid release key
        return "889382808911-scco623dhspjbf5guflmg68f61jl1na3.apps.googleusercontent.com";
        // The Client ID doesn't matter too much, it needs to exist, but for verification what Android
        // does is match by SHA1 signing key + package name.
    }

    public static String getGoogleDriveApplicationOauth2Redirect() {
        //Needs to match in androidmanifest.xml
        return "com.mendhak.gpslogger:/oauth2googledrive";
    }

    public static String[] getGoogleDriveApplicationScopes() {
        return new String[]{"https://www.googleapis.com/auth/drive.file"};
    }

    public static AuthorizationService getAuthorizationService(Context context) {
        return new AuthorizationService(context, new AppAuthConfiguration.Builder().build());
    }

    public static AuthorizationServiceConfiguration getAuthorizationServiceConfiguration() {
        return new AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://www.googleapis.com/oauth2/v4/token"),
                null,
                Uri.parse("https://accounts.google.com/o/oauth2/revoke?token=")
        );
    }

    public static AuthState getAuthState() {
        AuthState authState = new AuthState();

        //Esto guarda la autentificacion ==========================================================
        String google_drive_auth_state = PreferenceHelper.getInstance().getGoogleDriveAuthState();

        //copyToClipboard(mContext, google_drive_auth_state, "tago");

        if (!isNullOrEmpty(google_drive_auth_state)) {
            try {
                authState = AuthState.jsonDeserialize(google_drive_auth_state);

            } catch (JSONException e) {
                LOG.debug(e.getMessage(), e);
            }
        }
        //==============================================================================================

        return authState;
    }

    public void ImportDataToDrive(List<File> files, boolean img) {
        InternalImportDataToDrive(files, img);
    }

    public void ImportDataToDrive(File fileToUpload) {
        InternalImportDataToDrive(fileToUpload, false);
    }

    public void ImportImgToDrive(File fileToUpload) {
        InternalImportDataToDrive(fileToUpload, true);
    }

    public void InternalImportDataToDrive(List<File> files, boolean img) {
        String tag = String.valueOf(Objects.hashCode(files));


        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("filePaths", files.stream().map(File::getAbsolutePath).toArray(String[]::new));

        dataMap.put("filePath", "");

        dataMap.put("img", img);

        dataMap.put("list", true);

        SetWorkResult.startWorkManagerRequest(GoogleDriveUploadWorker.class, dataMap, tag);
    }

    public void InternalImportDataToDrive(File fileToUpload, boolean img) {
        String tag = String.valueOf(Objects.hashCode(fileToUpload));
        HashMap<String, Object> dataMap = new HashMap<>();

        dataMap.put("filePaths", new String[0]);

        dataMap.put("filePath", fileToUpload.getAbsolutePath());

        dataMap.put("img", img);

        dataMap.put("list", false);

        SetWorkResult.startWorkManagerRequest(GoogleDriveUploadWorker.class, dataMap, tag);
    }

    // Metodo para sincronizar desde el preloder
    public void dataSynchronizeStarting(){
        internalDataSynchronize(false,true, false, false);
    }

    // Metodo para sincronizar y enviar objetos
    public void dataSynchronizeObj(){
        internalDataSynchronize(false,false, false, false);
    }

    // Metodo para sincronizar y enviar imagenes
    public void dataSynchronizeImg(){
        internalDataSynchronize(true,false, false, false);
    }

    // Metodo para chequear estado sincronizacio
    public void dataSynchronizeCheck(){
        internalDataSynchronize(false, false, false, true);
    }

    // Metodo para sincronizar
    public void dataSynchronize(){
        internalDataSynchronize( false,false, false, false);
    }

    public void internalDataSynchronize(boolean img, boolean preLoader, boolean newObj, boolean check){
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/DataSave.csv");
        // Crear un tag único para la tarea de descarga
        String tag = StartVar.WORK_TAG_DOWNLOAD;

        // Preparar datos de entrada
        HashMap<String, Object> dataMap = new HashMap<>();

        if(img){
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/");
            dataMap.put("img", true);
            dataMap.put("type", "?alt=media");

        }
        else {
            dataMap.put("img", false);
            dataMap.put("type", "/export?mimeType=text/csv");
        }

        if (path != null) {
            dataMap.put("path", path.getAbsolutePath());
        }
        dataMap.put("name", "DataSave.csv");
        dataMap.put("preloader", preLoader);
        dataMap.put("newobj", newObj);
        dataMap.put("check", check);

        // Encolar el GoogleDriveDownloadWorker
        SetWorkResult.startWorkManagerRequest(GoogleDriveDownloadWorker.class, dataMap, tag);
    }

    public void uploadDataBase() {
        //Dialogs.progress((FragmentActivity) getActivity(), "getString(R.string.please_wait)");
        //Basic.msg("StartVar.csvList: "+StartVar.csvList.get(1)[1]);

        try {
            // Ejecutar ImportDataToDrive en el hilo principal
            new Handler(Looper.getMainLooper()).post(() -> {
                FilesManager fMang = new FilesManager();
                String name = "DataSave.csv";
                try {
                    file = fMang.csvExport(StartVar.csvList, name);
                } catch (IOException e) {
                    Basic.msg("Error Archivo no creado: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                if (file != null) {
                    ImportDataToDrive(file);

                    // Ahora se envía también un respaldo
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        LocalDate currDate = LocalDate.now();
                        File newFile = null;
                        try {
                            newFile = FilesManager.getNewFile(file.getAbsolutePath(), currDate.toString().replaceAll("\\D", "-") + ".csv", StartVar.mContex);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (newFile != null) {
                            // Ejecutar ImportDataToDrive en el hilo principal
                            ImportDataToDrive(newFile);
                        }
                    }
                }
            });

        } catch (Exception e) {
            Basic.msg("Error Archivo no creado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void uploadDataImg() {
        //Dialogs.progress((FragmentActivity) getActivity(), "getString(R.string.please_wait)");
        //Basic.msg("StartVar.csvList: "+StartVar.csvList.get(1)[1]);

        try {
            // Ejecutar ImportDataToDrive en el hilo principal
            new Handler(Looper.getMainLooper()).post(() -> {

                List<File> mFileList = new ArrayList<>();
                for (Usuario mUser : StartVar.listuser){
                   if(mUser != null && !mUser.imagen.isEmpty()){
                       File mFile = new File(mUser.imagen);
                       if(mFile.exists()){
                           mFileList.add(mFile);
                       }
                   }
                }
                ImportDataToDrive(mFileList, true);
            });

        } catch (Exception e) {
            Basic.msg("Error Archivo no creado: " + e.getMessage());
            e.printStackTrace();
        }
    }




    public boolean isAvailable() {
        return getAuthState().isAuthorized();
    }

    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isGoogleDriveAutoSendEnabled();
    }
//
//    public String getName() {
//        return SenderNames.GOOGLEDRIVE;
//    }

    public boolean accept(File file, String s) {
        return true;
    }


    /**
     * Checks if a string is null or empty
     *
     * @param text
     * @return
     */
    public static boolean isNullOrEmpty(String text) {
        return text == null ||  text.trim().length() == 0;
    }



    /**
     * Copia un texto al portapapeles del dispositivo.
     *
     * @param context Contexto de la aplicación.
     * @param text    Texto a copiar al portapapeles.
     * @param label   Etiqueta opcional para describir el contenido (puede ser null).
     * @return true si se copió exitosamente, false si ocurrió un error.
     */
    public static boolean copyToClipboard(@NonNull Context context, @NonNull String text, @Nullable String label) {
        try {
            // Obtener el servicio del portapapeles
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

            // Crear un ClipData con el texto
            ClipData clip = ClipData.newPlainText(label != null ? label : "Texto copiado", text);

            // Copiar al portapapeles
            clipboard.setPrimaryClip(clip);

            return true;
        } catch (Exception e) {
            // Registrar el error (puedes usar un logger como Logcat o el de tu preferencia)
            android.util.Log.e("ClipboardUtils", "Error al copiar al portapapeles: " + e.getMessage(), e);
            return false;
        }
    }
}
