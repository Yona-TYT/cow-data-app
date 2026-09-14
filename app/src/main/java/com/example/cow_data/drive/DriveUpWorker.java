package com.example.cow_data.drive;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cow_data.StartVar;
import com.example.cow_data.ex.UploadEvents;
import com.example.cow_data.utls.FilesManager;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
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

    private static final String KEY_RESULT_MESSAGE = "result_message";
    private static final String KEY_IS_IMG = "img";

    public DriveUpWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        //String filePath = getInputData().getString("filePath");
        boolean isList = getInputData().getBoolean("list", false);
        boolean isImg = getInputData().getBoolean(KEY_IS_IMG, false);


        String[] filePaths = getInputData().getStringArray("filePaths");

        count = filePaths.length;

        //File fileToUpload = new File(filePath);
        boolean success = true;
        String failureMessage = "";
        Throwable failureThrowable = null;
        int uploaded = 0;
        int skipped = 0;
        int missing = 0;
        boolean mainUploaded = false;


        AuthState authState = DriveManager.getAuthState();
        if (!authState.isAuthorized()) {
            LOG.error("Google Drive - No autorizado para subir archivos.");
            // Devolvemos un fallo inmediato con el mensaje incrustado en el mapa de datos
            return Result.failure(new Data.Builder()
                    .putString(KEY_RESULT_MESSAGE, "Could not upload to Google Drive. Not Authorized.")
                    .putInt("uploaded", 0)
                    .putBoolean("main_uploaded", false)
                    .build());
        }

        final AtomicBoolean taskDone = new AtomicBoolean(false);
        //PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

        // DriveUtils.copyToClipboard(mContext, filePaths.length+" ?", "tag");


        try {
            AuthorizationService authorizationService = DriveManager.getAuthorizationService(mContext);
            googleDriveAccessToken = DriveUtils.getFreshAccessToken(authState, authorizationService);

            if (DriveUtils.isNullOrEmpty(googleDriveAccessToken)) {
                LOG.error("Failed to fetch Access Token for Google Drive. Stopping this job.");
                return Result.failure();
            }
            // Figure out the Folder ID to upload to, from the path; recursively create if it doesn't exist.
            String parent = PreferenceHelper.getInstance().getGoogleDriveFolderId();
            if (DriveUtils.isNullOrEmpty(parent)) {
                parent = "root";
            }

            String salesName = PreferenceHelper.getInstance().getGoogleDriveFolderPath(); // "Sales-Save"
            String imgName = PreferenceHelper.getInstance().getGoogleDriveImgPath();     // "Img"

            // Sales-Save (siempre carpeta)
            String salesId = DriveUtils.getFileIdFromFileName(
                    googleDriveAccessToken, salesName, parent, "application/vnd.google-apps.folder");
            if (DriveUtils.isNullOrEmpty(salesId)) {
                salesId = DriveUtils.createEmptyFile(
                        googleDriveAccessToken, salesName,
                        "application/vnd.google-apps.folder", parent);
            }

            if (DriveUtils.isNullOrEmpty(salesId)) {
                failureMessage = "Could not create folder Sales-Save";
                success = false;
            } else if (isList) {
                String targetFolderId = salesId;

                // Img solo si es subida de imágenes
                if (isImg) {
                    String imgId = DriveUtils.getFileIdFromFileName(
                            googleDriveAccessToken, imgName, salesId, "application/vnd.google-apps.folder");
                    if (DriveUtils.isNullOrEmpty(imgId)) {
                        imgId = DriveUtils.createEmptyFile(
                                googleDriveAccessToken, imgName,
                                "application/vnd.google-apps.folder", salesId);
                    }
                    if (DriveUtils.isNullOrEmpty(imgId)) {
                        failureMessage = "Could not create folder Img";
                        success = false;
                    } else {
                        targetFolderId = imgId;
                    }
                }

                if (success) {
                    String[] remoteNames = getInputData().getStringArray("remoteNames");

                    for (int i = 0; i < filePaths.length; i++) {
                        String path = filePaths[i];
                        File mFile = new File(path);

                        if (!mFile.exists()) {
                            LOG.error("No existe: " + path);
                            missing++;
                            continue;
                        }

                        // Nombre en Drive: remoto explícito o mapeo LOCAL_UPLOAD → DataSave.bin
                        String remoteName;
                        if (remoteNames != null && i < remoteNames.length && remoteNames[i] != null) {
                            remoteName = remoteNames[i];
                        } else {
                            remoteName = DriveManager.remoteNameForUpload(mFile);
                        }

                        if (filesSet(mFile, targetFolderId, remoteName)) {
                            uploaded++;
                            if (StartVar.EXPORT_NAME.equals(remoteName)) {
                                mainUploaded = true;
                            }
                        } else {
                            skipped++;
                        }
                    }

                    LOG.info("Upload resumen: uploaded=" + uploaded
                            + " skipped=" + skipped
                            + " missing=" + missing
                            + " mainUploaded=" + mainUploaded);

                    if (uploaded == 0 && missing > 0) {
                        success = false;
                        failureMessage = "Ningún archivo local encontrado";
                    } else if (uploaded == 0 && skipped == 0) {
                        success = false;
                        failureMessage = "filePaths vacío o sin archivos válidos";
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            success = false;
            failureMessage = e.getMessage();
            failureThrowable = e;
        }
        if (success) {
            // Generamos un mensaje informativo nativo según sea una lista o un archivo único
            String msgExito = isList
                    ? "Subidos=" + uploaded + " skip=" + skipped + " total=" + count
                    : "Sincronización de archivo único exitosa";

            LOG.info("Google Drive - " + msgExito);

            // Restaurar EventBus (lo que usa tu Fragment)
            if (isImg) {
                EventBus.getDefault().post(
                        new UploadEvents.GoogleDrive().succeeded("Imágenes subidas: " + uploaded)
                );
            } else {
                EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded());
            }

            return Result.success(new Data.Builder()
                    .putString(KEY_RESULT_MESSAGE, msgExito)
                    .putInt("uploaded", uploaded)
                    .putInt("skipped", skipped)
                    .putInt("missing", missing)
                    .putBoolean("main_uploaded", mainUploaded)
                    .putBoolean("img", isImg)
                    .build());
        }

        if(getRunAttemptCount() < getRetryLimit()){
            LOG.warn(String.format("Google Drive - attempt %d of %d failed, will retry", getRunAttemptCount(), getRetryLimit()));
            return Result.retry();
        }

        if(failureThrowable == null) {
            failureThrowable = new Exception(failureMessage);
        }

        EventBus.getDefault().post(
                new UploadEvents.GoogleDrive().failed(failureMessage, failureThrowable)
        );

        return Result.failure(new Data.Builder()
                .putString(KEY_RESULT_MESSAGE, failureMessage+ "  "+failureThrowable)
                .build());

    }

    private boolean filesSet(File localFile, String folderId, String remoteName) throws Exception {
        if (remoteName == null || remoteName.isEmpty()) {
            remoteName = DriveManager.remoteNameForUpload(localFile);
        }

        LOG.info("=== filesSet local=" + localFile.getName() + " → remoto=" + remoteName);

        // 1) Metadatos en Drive por NOMBRE REMOTO (DataSave.bin, no .upload.bin)
        DriveFileMeta driveFile = DriveUtils.getFileMetaFromDrive(
                googleDriveAccessToken, remoteName, folderId);

        String driveFileId;
        boolean isNew = false;

        if (driveFile == null || DriveUtils.isNullOrEmpty(driveFile.id)) {
            LOG.info("   → No existe en Drive → createEmptyFile(" + remoteName + ")");
            driveFileId = DriveUtils.createEmptyFile(
                    googleDriveAccessToken,
                    remoteName,
                    DriveUtils.getMimeTypeFromFileName(remoteName),
                    folderId
            );
            isNew = true;
        } else {
            driveFileId = driveFile.id;
            LOG.info("   → ID=" + driveFileId + " | MD5 remoto=" + driveFile.md5Checksum);
        }

        if (DriveUtils.isNullOrEmpty(driveFileId)) {
            LOG.error("   ❌ No se pudo obtener/crear en Drive: " + remoteName);
            count--;
            return false;
        }

        // 2) ¿Hay que subir contenido?
        boolean isMainDb = StartVar.EXPORT_NAME.equals(remoteName);
        boolean mustUpload = isNew;

        if (!isNew) {
            if (isMainDb) {
                // Siempre actualizar DataSave.bin en sync
                mustUpload = true;
            } else {
                String localMd5 = DriveUtils.getLocalFileMd5(localFile);
                String remoteMd5 = driveFile.md5Checksum != null ? driveFile.md5Checksum : "";
                mustUpload = remoteMd5.isEmpty()
                        || localMd5 == null
                        || !remoteMd5.equalsIgnoreCase(localMd5);
            }
        }

        if (!mustUpload) {
            LOG.info("   → MD5 igual, no se sube: " + remoteName);
            count--;
            return false;
        }

        // 3) Subir bytes del file LOCAL; en Drive queda como remoteName
        LOG.info("   → Subiendo contenido de " + localFile.getName() + " como " + remoteName);
        uploadFileContents(googleDriveAccessToken, driveFileId, localFile);
        LOG.info("=== filesSet OK: " + remoteName);
        return true;
    }

    private String uploadFileContents(String accessToken, String driveFileId, File fileToUpload) throws Exception {
        if (fileToUpload == null || !fileToUpload.exists()) {
            throw new IllegalArgumentException("El archivo a subir no existe");
        }

        String contentType = DriveUtils.getMimeTypeFromFileName(fileToUpload.getName());
        String updateUrl = "https://www.googleapis.com/upload/drive/v3/files/"
                + driveFileId + "?uploadType=media";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        RequestBody body = RequestBody.create(fileToUpload, MediaType.parse(contentType));

        Request.Builder builder = new Request.Builder()
                .url(updateUrl)
                .addHeader("Authorization", "Bearer " + accessToken);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            builder.addHeader("X-HTTP-Method-Override", "PATCH");
            builder.method("POST", body);
        } else {
            builder.method("PATCH", body);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new Exception("Error al subir: HTTP " + response.code() + " - " + errorBody);
            }
            String responseBody = response.body() != null ? response.body().string() : "{}";
            return new JSONObject(responseBody).optString("id", driveFileId);
        }
    }

    protected int getRetryLimit() {
        return 3;
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
