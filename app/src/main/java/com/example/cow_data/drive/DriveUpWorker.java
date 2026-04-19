package com.example.cow_data.drive;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.ex.UploadEvents;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DriveUpWorker extends Worker {
    private static final Logger LOG = Logs.of(DriveUpWorker.class);

    private String googleDriveAccessToken;

    private final Context mContext;
    private int count = 0;

    public DriveUpWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        //String filePath = getInputData().getString("filePath");
        boolean isList = getInputData().getBoolean("list", false);
        boolean isImg = getInputData().getBoolean("img", false);


        String[] filePaths = getInputData().getStringArray("filePaths");

        count = filePaths.length;

        //File fileToUpload = new File(filePath);
        boolean success = true;
        String failureMessage = "";
        Throwable failureThrowable = null;


        AuthState authState = DriveManager.getAuthState();
        if (!authState.isAuthorized()) {
            EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not upload to Google Drive. Not Authorized."));
        }

        final AtomicBoolean taskDone = new AtomicBoolean(false);
        //PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

        try {
            AuthorizationService authorizationService = DriveManager.getAuthorizationService(mContext);

            // The performActionWithFreshTokens seems to happen on a UI thread! (Why??)
            // So I can't do network calls on this thread.
            // Instead, updating a class level variable, and waiting for it afterwards.
            // https://github.com/openid/AppAuth-Android/issues/123
            authState.performActionWithFreshTokens(authorizationService, new AuthState.AuthStateAction() {
                @Override
                public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                    if (ex != null) {
                        EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(ex.toJsonString(), ex));
                        taskDone.set(true);
                        LOG.error(ex.toJsonString(), ex);
                        return;
                    }
                    googleDriveAccessToken = accessToken;
                    taskDone.set(true);
                }
            });

            // Wait for the performActionWithFreshTokens.execute callback
            // (which happens on the UI thread for some reason) to complete.
            while (!taskDone.get()) {
                Thread.sleep(500);
            }

            if (DriveUtils.isNullOrEmpty(googleDriveAccessToken)) {
                LOG.error("Failed to fetch Access Token for Google Drive. Stopping this job.");
                return Result.failure();
            }
            // Figure out the Folder ID to upload to, from the path; recursively create if it doesn't exist.
            String folderPath = PreferenceHelper.getInstance().getGoogleDriveFolderPath();
            String[] pathParts = folderPath.split("/");
            String parentFolderId = PreferenceHelper.getInstance().getGoogleDriveFolderId();
            String latestFolderId = null;

            for (String part : pathParts) {

                latestFolderId = DriveUtils.getFileIdFromFileName(googleDriveAccessToken, part, parentFolderId);

                if (!DriveUtils.isNullOrEmpty(latestFolderId)) {

                    LOG.debug("Folder " + part + " found, folder ID is " + latestFolderId);
                } else {
                    LOG.debug("Folder " + part + " not found, creating.");
                    latestFolderId = DriveUtils.createEmptyFile(googleDriveAccessToken, part,
                            "application/vnd.google-apps.folder", DriveUtils.isNullOrEmpty(parentFolderId) ? "root" : parentFolderId);
                }
                parentFolderId = latestFolderId;
            }

            //copyToClipboard(mContext, folderPath+" id: "+parentFolderId, "tago");

            String diverFolderId = latestFolderId;

            if (DriveUtils.isNullOrEmpty(diverFolderId)) {
                failureMessage = "Could not create folder";
                success = false;
            }
            else{
                if (isList){
                    String imgFolderName = PreferenceHelper.getInstance().getGoogleDriveImgPath();
                    String imgFolderId = DriveUtils.getFileIdFromFileName(googleDriveAccessToken, imgFolderName, diverFolderId, "application/vnd.google-apps.folder");
                    if (!DriveUtils.isNullOrEmpty(imgFolderId)) {
                        LOG.debug("Folder " + imgFolderName + " found, folder ID is " + diverFolderId);
                    } else {
                        LOG.debug("Folder " + imgFolderName + " not found, creating.");
                        imgFolderId = DriveUtils.createEmptyFile(googleDriveAccessToken, imgFolderName,
                                "application/vnd.google-apps.folder", diverFolderId);
                    }
                    if (DriveUtils.isNullOrEmpty(imgFolderId)) {
                        failureMessage = "Could not create folder";
                        success = false;
                    }
                    else {
                        String folderId = (isImg ? imgFolderId : diverFolderId );
                        for(String path : filePaths){
                            File mFile = new File(path);
                            if(mFile.exists()){
                                filesSet(mFile, folderId);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            success = false;
            failureMessage = e.getMessage();
            failureThrowable = e;
        }

        if(success){
            // Notify internal listeners
            if (isList) {
                EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded("Archivos Subidos: "+filePaths.length+" ; ", count));
            }
            else {
                EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded());
            }
            // Notify external listeners
            //Basic.sendFileUploadedBroadcast(getApplicationContext(), new String[]{fileToUpload.getAbsolutePath()}, "googledrive");
            return Result.success();
        }

        if(getRunAttemptCount() < getRetryLimit()){
            LOG.warn(String.format("Google Drive - attempt %d of %d failed, will retry", getRunAttemptCount(), getRetryLimit()));
            return Result.retry();
        }

        if(failureThrowable == null) {
            failureThrowable = new Exception(failureMessage);
        }

        EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(failureMessage, failureThrowable));
        return Result.failure();

    }

    private boolean filesSet(File localFile, String folderId) throws Exception {
        LOG.info("=== INICIANDO filesSet() - Archivo: " + localFile.getName());

        String fileName = localFile.getName();

        // 1. Buscar ID existente
        String driveFileId = DriveUtils.getFileIdFromFileName(googleDriveAccessToken, fileName, folderId);
        LOG.info("   → getFileIdFromFileName() → " + (driveFileId != null ? driveFileId : "NULL"));


        if (DriveUtils.isNullOrEmpty(driveFileId)) {
            LOG.info("   → Archivo no existe → Creando archivo vacío...");
            driveFileId = DriveUtils.createEmptyFile(googleDriveAccessToken,
                    fileName,
                    DriveUtils.getMimeTypeFromFileName(fileName),
                    folderId);
            LOG.info("   → createEmptyFile() devolvió ID: " + (driveFileId != null ? driveFileId : "NULL"));
        }

        if (DriveUtils.isNullOrEmpty(driveFileId)) {
            LOG.error("   ❌ ERROR: No se pudo obtener ni crear el archivo en Drive");
            count--;
            return false;
        }

        LOG.info("   → ID listo para usar: " + driveFileId);

        // 2. Obtener metadatos + MD5 + modification date
        DriveFileMeta driveFile = DriveUtils.getFileMetaFromDrive(googleDriveAccessToken, localFile.getName(), folderId);

        if (driveFile != null) {
            LOG.info("✅ MD5 obtenido: " + (driveFile.md5Checksum != null ? driveFile.md5Checksum : "NULL"));
            LOG.info("   → DriveFileMeta completo: " + driveFile.toString());
        } else {
            LOG.warn("⚠️ getFileMetaFromDrive devolvió NULL (posible archivo recién creado)");
        }

        // 3. Subir contenido
        LOG.info("   → Iniciando updateFileContents()...");
        //copyToClipboard(mContext, localFile.getName()+" "  +getLocalFileMd5(localFile), localFile.getName());

        if(driveFile == null) {
            //copyToClipboard(mContext, localFile.getName()+" "+ driveFile.md5Checksum +" "+getLocalFileMd5(localFile), localFile.getName());
            updateFileContents(googleDriveAccessToken, driveFileId, localFile);
            LOG.info("   → updateFileContents() finalizado correctamente");
            LOG.info("=== FIN DE filesSet() para " + fileName);
            return true;
        }

        else {
            if(fileName.endsWith("csv")){
                updateFileContents(googleDriveAccessToken, driveFileId, localFile);

                LOG.info("   → updateFileContents() finalizado correctamente");
                LOG.info("=== FIN DE filesSet() para " + fileName);
                return true;
            }
            else if(!driveFile.md5Checksum.equals(DriveUtils.getLocalFileMd5(localFile))){
                copyToClipboard(mContext, driveFile.modifiedTime+driveFile.md5Checksum.isEmpty()+" --"+ driveFile.md5Checksum +" --"+ DriveUtils.getLocalFileMd5(localFile), fileName);

                updateFileContents(googleDriveAccessToken, driveFileId, localFile);

                LOG.info("   → updateFileContents() finalizado correctamente");
                LOG.info("=== FIN DE filesSet() para " + fileName);
                return true;
            }
        }
        count--;
        return false;
    }

    private String updateFileContents(String accessToken, String driveFileId, File fileToUpload) throws Exception {
        FileInputStream fis = new FileInputStream(fileToUpload);
        String fileId = null;

        String fileUpdateUrl = "https://www.googleapis.com/upload/drive/v3/files/" + driveFileId + "?uploadType=media";

        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(fileUpdateUrl);

        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        RequestBody body = RequestBody.create(MediaType.parse(DriveUtils.getMimeTypeFromFileName(fileToUpload.getName())), getByteArrayFromInputStream(fis));
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            requestBuilder.addHeader("X-HTTP-Method-Override", "PATCH");
        }
        requestBuilder = requestBuilder.method("PATCH", body);

        Request request = requestBuilder.build();
        Response response = client.newCall(request).execute();
        String fileMetadata = response.body().string();
        LOG.debug(fileMetadata);
        response.body().close();

        JSONObject fileMetadataJson = new JSONObject(fileMetadata);
        fileId = fileMetadataJson.getString("id");

        return fileId;
    }

    protected int getRetryLimit() {
        return 3;
    }

    /**
     * Copia un texto al portapapeles del dispositivo.
     *
     * @param context Contexto de la aplicación.
     * @param text    Texto a copiar al portapapeles.
     * @param label   Etiqueta opcional para describir el contenido (puede ser null).
     * @return true si se copió exitosamente, false si ocurrió un error.
     */
    private static boolean copyToClipboard(@NonNull Context context, @NonNull String text, @Nullable String label) {
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

    public static byte[] getByteArrayFromInputStream(InputStream is) {

        try {
            int length;
            int size = 1024;
            byte[] buffer;

            if (is instanceof ByteArrayInputStream) {
                size = is.available();
                buffer = new byte[size];
                is.read(buffer, 0, size);
            } else {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                buffer = new byte[size];
                while ((length = is.read(buffer, 0, size)) != -1) {
                    outputStream.write(buffer, 0, length);
                }

                buffer = outputStream.toByteArray();
            }
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                LOG.warn("f", "getStringFromInputStream - could not close stream");
            }
        }

        return null;
    }
}

