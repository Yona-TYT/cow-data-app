package com.example.cow_data.ex;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

//import com.mendhak.gpslogger.common.AppSettings;    //Necesaria para el context
//import com.mendhak.gpslogger.common.PreferenceHelper;   //Necesaria
//import com.mendhak.gpslogger.common.Strings;
//import com.mendhak.gpslogger.common.Systems;
//import com.mendhak.gpslogger.common.slf4j.Logs;     //solo debug
//import com.mendhak.gpslogger.senders.FileSender;

//import com.mendhak.gpslogger.common.AppSettings;

import com.example.cow_data.Basic;
import com.example.cow_data.FilesManager;
import com.example.cow_data.StartVar;


import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.json.JSONException;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;


public class GoogleDriveManager  {
    private static GoogleDriveManager instance;
    private static final Logger LOG = Logs.of(GoogleDriveManager.class);
    private final PreferenceHelper preferenceHelper;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

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

    public void ImportDataToDrive(List<File> files) {
        for (File f : files) {
            LOG.debug(f.getName());
            ImportDataToDrive(f);
        }
    }

    public void ImportDataToDrive(File fileToUpload) {
        String tag = StartVar.WORK_TAG_UPLOAD;

        HashMap<String, Object> dataMap = new HashMap<String, Object>() {{
           put("filePath", fileToUpload.getAbsolutePath());
        }};
        SetWorkResult.startWorkManagerRequest(GoogleDriveUploadWorker.class, dataMap, tag);
    }

    // Metodo para sincronizar desde el preloder
    public void dataSynchronizeStarting(){
        internalDataSynchronize(true, false);
    }

    // Metodo para sincronizar nuevo objeto
    public void dataSynchronizeObj(){internalDataSynchronize(false, true);}

    // Metodo para sincronizar
    public void dataSynchronize(){
        internalDataSynchronize(false, false);
    }

    public void internalDataSynchronize(boolean preLoader, boolean newObj){
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/DataSave.csv");
        // Crear un tag único para la tarea de descarga
        String tag = StartVar.WORK_TAG_DOWNLOAD;

        // Preparar datos de entrada
        HashMap<String, Object> dataMap = new HashMap<>();
        if (path != null) {
            dataMap.put("path", path.getAbsolutePath());
        }
        dataMap.put("name", "DataSave.csv");
        dataMap.put("type", "/export?mimeType=text/csv");
        dataMap.put("preloader", (preLoader?"1":"0"));
        dataMap.put("newobj", (newObj?"1":"0"));

        // Encolar el GoogleDriveDownloadWorker
        SetWorkResult.startWorkManagerRequest(GoogleDriveDownloadWorker.class, dataMap, tag);
    }

    public void uploadDataBase() {
        //Dialogs.progress((FragmentActivity) getActivity(), "getString(R.string.please_wait)");

        try {
            FilesManager fMang = new FilesManager();
            String name = "DataSave.csv";
            java.io.File file = fMang.csvExport(StartVar.csvList, name);
            if(file != null) {
                ImportDataToDrive(file);
            }
        }
        catch (Exception e) {
            Basic.msg("Error Archivo no creado: "+ e.getMessage());
            e.printStackTrace();
        }

//        try {
//            File dbFileA = new File(StartVar.mContex.getDatabasePath(StartVar.nameDBconf).getPath());
//            manager.uploadFile(dbFileA);
//
//            Configdb mConf = StartVar.configDatabase.daoConf().getUsers(StartVar.mConfID);
//            String fileName = (mConf.date+mConf.time).replaceAll("\\D","")+"_"+mConf.hexid+".db";
//            manager.uploadFile(FilesManager.getNewFile(StartVar.mContex.getDatabasePath(StartVar.nameDBcow).getPath(), fileName, StartVar.mContex));
//        }
//        catch (Exception ex) {
//            LOG.error("Could not create local test file", ex);
//            //EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not create local test file", ex));
//        }
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
