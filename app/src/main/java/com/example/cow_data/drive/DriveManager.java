package com.example.cow_data.drive;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.cow_data.AppContextProvider;
import com.example.cow_data.DBListCreator;
import com.example.cow_data.GlobalData;
import com.example.cow_data.db.Conf;
import com.example.cow_data.db.GenericQueue;
import com.example.cow_data.utls.FilesManager;
import com.example.cow_data.StartVar;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.utls.Msg;


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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class DriveManager {
    private static DriveManager instance;
    private static final Logger LOG = Logs.of(DriveManager.class);
    private static final String TAG = "DriveManager";

    private final PreferenceHelper preferenceHelper;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    private java.io.File file;

    private GlobalData glData = GlobalData.getInstance(AppContextProvider.getContext());

    public static synchronized DriveManager getInstance() {
        if (instance == null) {
            instance = new DriveManager(PreferenceHelper.getInstance());
        }
        return instance;
    }

    public DriveManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
        DriveManager.mContext = AppContextProvider.getContext();
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

        if (!DriveUtils.isNullOrEmpty(google_drive_auth_state)) {
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

    public void ImportDataToDrive(File file, boolean img) {
        if (file == null) {
            android.util.Log.e(TAG, "El archivo es null");
            return;
        }
        List<File> files = Collections.singletonList(file);   // Forma más corta y eficiente
        InternalImportDataToDrive(files, img);
    }

    /** Directorio de datos de la app en Documents */
    public static File getAppDataDir() {
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS + "/" + StartVar.dirAppName + "/");
        if (!path.exists()) {
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();
        }
        return path;
    }

    public static File getLocalUploadFile() {
        return new File(getAppDataDir(), StartVar.LOCAL_UPLOAD);
    }

    public static File getLocalDownloadFile() {
        return new File(getAppDataDir(), StartVar.LOCAL_DOWNLOAD);
    }

    /** Nombre remoto en Drive para un file local de subida */
    public static String remoteNameForUpload(File local) {
        if (local == null) return StartVar.EXPORT_NAME;
        String n = local.getName();
        if (StartVar.LOCAL_UPLOAD.equals(n) || StartVar.EXPORT_NAME.equals(n)) {
            return StartVar.EXPORT_NAME; // siempre DataSave.bin en Drive
        }
        return n; // respaldos diarios 2026-08-21.bin, etc.
    }


    public void InternalImportDataToDrive(List<File> files, boolean img) {
        String tag = img ? StartVar.WORK_TAG_UPLOAD_IMG : StartVar.WORK_TAG_UPLOAD;

        String[] paths = files.stream().map(File::getAbsolutePath).toArray(String[]::new);
        String[] remoteNames = files.stream().map(DriveManager::remoteNameForUpload).toArray(String[]::new);

        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("filePaths", paths);
        dataMap.put("remoteNames", remoteNames); // paralelo a filePaths
        dataMap.put("filePath", "");
        dataMap.put("img", img);
        dataMap.put("list", true);

        SetWorkResult.startWorkManagerRequest(DriveUpWorker.class, dataMap, tag);
    }

    // Metodo para sincronizar desde el preloder
    public void dataSynchronizeStarting(){
        internalDataSynchronize(false,true, false, false, null);
    }

    // Metodo para sincronizar y enviar objetosnull
    public void dataSynchronizeObj(){
        internalDataSynchronize(false,false, false, false, null);
    }

    // Metodo para sincronizar con un Id especifico
    public void dataSynchronizeSelect(String id){
        internalDataSynchronize(false,false, false, false, id);
    }

    // Metodo para sincronizar y enviar imagenes
    public void dataSynchronizeImg(){
        internalDataSynchronize(true,false, false, false, null);
    }

    // Metodo para chequear estado sincronizacio
    public void dataSynchronizeCheck(){
        internalDataSynchronize(false, false, false, true, null);
    }

    // Metodo para sincronizar
    public void dataSynchronize(){
        GenericQueue q = GlobalData.getInstance(mContext).getGenericQueue();
        if (q.hasPendingQueueItems()) {
            Log.w(TAG, "Cola pendiente: se omite dataSynchronize()");
            Msg.m("Sincronizando...");
            return;
        }
        internalDataSynchronize(false, false, false, false, null);
    }

    public void internalDataSynchronize(boolean img, boolean preLoader, boolean newObj,
                                        boolean check, String selectId) {
        String tag = img ? StartVar.WORK_TAG_DOWNLOAD_IMG : StartVar.WORK_TAG_DOWNLOAD;

        HashMap<String, Object> dataMap = new HashMap<>();
        File path;

        if (img) {
            path = getAppDataDir();
            dataMap.put("img", true);
            dataMap.put("path", path.getAbsolutePath());
            dataMap.put("name", ""); // imágenes: según tu worker
        } else {
            // Baja a archivo SEPARADO: no pisa el de subida
            File downloadTarget = getLocalDownloadFile();
            dataMap.put("img", false);
            dataMap.put("path", downloadTarget.getAbsolutePath()); // path completo del file
            dataMap.put("name", StartVar.EXPORT_NAME);           // nombre en Drive a buscar
            dataMap.put("localName", StartVar.LOCAL_DOWNLOAD);
        }

        dataMap.put("type", "?alt=media");
        dataMap.put("fileId", selectId);
        dataMap.put("preloader", preLoader);
        dataMap.put("newobj", newObj);
        dataMap.put("check", check);

        SetWorkResult.startWorkManagerRequest(DriveDowWorker.class, dataMap, tag);
    }


    public void uploadDataBase() {
        final String tag = TAG;

        try {
            if (StartVar.csvList != null) {
                StartVar.csvList.clear();
            }
            DBListCreator.createDbLists();

            if (StartVar.csvList == null || StartVar.csvList.isEmpty()) {
                Log.e(tag, "csvList vacía tras createDbLists → cancelo subida");
                Msg.m("Error: no hay datos para exportar");
                return;
            }

            Conf c = StartVar.appDBall.daoCfg().getUsers(StartVar.mConfID);
            if (c == null) {
                Log.e(tag, "Conf null en Room");
                return;
            }

            String[] confRow = StartVar.csvList.get(1);
            Log.d(tag, "Room date=" + c.date + " time=" + c.time
                    + " | csvList date=" + confRow[6] + " dbg=" + confRow[13]);

            if (!String.valueOf(c.date).equals(String.valueOf(confRow[6]))) {
                Log.w(tag, "Desfase Room vs csvList → regenerando");
                StartVar.csvList.clear();
                DBListCreator.createDbLists();
                confRow = StartVar.csvList.get(1);
                Log.d(tag, "Reintento csvList date=" + confRow[6]);
            }

            FilesManager fMang = new FilesManager();

            // Export SOLO a DataSave.upload.bin (no al path que usa el download)
            File uploadFile = getLocalUploadFile();
            if (uploadFile.exists() && !uploadFile.delete()) {
                Log.w(tag, "No se pudo borrar upload previo");
            }

            File file = fMang.csvExport(StartVar.csvList, StartVar.LOCAL_UPLOAD);
            if (file == null || !file.exists()) {
                Log.e(tag, "csvExport falló para LOCAL_UPLOAD");
                Msg.m("Error: archivo no creado");
                return;
            }

            file.setLastModified(System.currentTimeMillis());
            Log.d(tag, "Upload local path=" + file.getAbsolutePath()
                    + " size=" + file.length()
                    + " md5=" + DriveUtils.getLocalFileMd5(file)
                    + " → remoto=" + StartVar.EXPORT_NAME);

            if (!FilesManager.isCsvSafeToUpload(file)) {
                Log.e(tag, "CSV inválido");
                Msg.m("Error: archivo inválido");
                return;
            }

            List<File> mFileList = new ArrayList<>();
            mFileList.add(file); // Worker debe subir como DataSave.bin

            // Respaldo diario (mismo contenido; en Drive conserva su nombre)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    String backupName = LocalDate.now().toString().replaceAll("\\D", "-") + ".bin";
                    File backup = FilesManager.getNewFile(file.getAbsolutePath(), backupName);
                    if (backup != null && backup.exists()) {
                        Log.d(tag, "Respaldo=" + backup.getName()
                                + " md5=" + DriveUtils.getLocalFileMd5(backup));
                        mFileList.add(backup);
                    }
                } catch (IOException e) {
                    Log.e(tag, "Error respaldo: " + e.getMessage());
                }
            }

            ImportDataToDrive(mFileList, false);
            Log.d(tag, "Subida encolada: " + mFileList.size() + " archivo(s)");

        } catch (Exception e) {
            Log.e(tag, "Error en uploadDataBase", e);
            Msg.m("Error al subir: " + e.getMessage());
        }
    }

    public void uploadDataImg() {
        // 1. Obtener una referencia segura al contexto (evita fugas de memoria)
        final Context appContext =  AppContextProvider.getContext();

        // 2. Ejecutar la búsqueda de archivos en un hilo de fondo
        // NUNCA procesar listas de archivos en el MainLooper/Handler
        new Thread(() -> {
            try {
                List<File> mFileList = new ArrayList<>();

                // Procesamiento de la lista (Operación pesada de I/O)
                for (String s : StartVar.getImgList()) {
                    File mFile = new File(s);
                    if (mFile.exists()) {
                        mFileList.add(mFile);
                    }
                }
                // 3. Encolar el trabajo solo si hay archivos
                if (!mFileList.isEmpty()) {
                    // Basic.msg("Siz img: "+mFileList.size());
                    // Llamamos a ImportDataToDrive directamente desde este hilo
                    ImportDataToDrive( mFileList, true);
                    android.util.Log.i(TAG, "✅ Lista preparada: " + mFileList.size() + " imágenes.");
                }
                else {
                    Msg.m("Descargando imagenes...");
                    //Si la lista esta vacia se procede a descargar las imagenes
                    dataSynchronizeImg();
                    android.util.Log.w(TAG, "⚠️ No se encontraron imágenes para subir.");
                }

            } catch (Exception e) {
                android.util.Log.e(TAG, "❌ Error en el hilo de búsqueda de imágenes", e);
                // Si necesitas mostrar un mensaje al usuario, usa el MainLooper solo para el Toast
                new Handler(Looper.getMainLooper()).post(() ->
                        Msg.m("Error al procesar imágenes: " + e.getMessage())
                );
            }
        }).start();
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
}
