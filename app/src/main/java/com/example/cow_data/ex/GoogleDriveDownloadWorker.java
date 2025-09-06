package com.example.cow_data.ex;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cow_data.Basic;
import com.example.cow_data.StartVar;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONObject;
import org.slf4j.Logger;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class GoogleDriveDownloadWorker extends Worker {
    private static final Logger LOG = Logs.of(GoogleDriveDownloadWorker.class);

    private String googleDriveAccessToken;
    private final Context mContext;

    private static final String KEY_RESULT_MESSAGE = "result_message";
    private static final String KEY_FILES_DOWNLOADED = "files_downloaded";
    private static final String KEY_IS_PRELOADER = "preloader";
    private static final String KEY_IS_NEW_OBJ = "newobj";

    public GoogleDriveDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
    }


    @NonNull
    @Override
    public Result doWork() {
        Result mResult = null;
        String failureMessage = "";
        String downloadMessage = "";
        String filePath = getInputData().getString("path");
        String fileName = getInputData().getString("name");
        String fileType = getInputData().getString("type");
        String isPreloader = getInputData().getString("preloader");
        String isNewObj = getInputData().getString("newobj");

        File fileToDownload = new File(filePath);

        boolean success = true;

        Throwable failureThrowable = null;


        AuthState authState = GoogleDriveManager.getAuthState();

        if (!authState.isAuthorized()) {
            failureMessage = "Could not download to Google Drive. Not Authorized.";
            //EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not upload to Google Drive. Not Authorized."));
        }

        final AtomicBoolean taskDone = new AtomicBoolean(false);
        //PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

        try {
            AuthorizationService authorizationService = GoogleDriveManager.getAuthorizationService(mContext);

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

            if (isNullOrEmpty(googleDriveAccessToken)) {
                LOG.error("Failed to fetch Access Token for Google Drive. Stopping this job.");
                failureMessage = "Failed to fetch Access Token for Google Drive. Stopping this job.";
                return Result.failure(new Data.Builder().putString(KEY_RESULT_MESSAGE, failureMessage).build());
            }

            // Figure out the Folder ID to upload to, from the path; recursively create if it doesn't exist.
            String folderPath = PreferenceHelper.getInstance().getGoogleDriveFolderPath();
            String[] pathParts = folderPath.split("/");
            String parentFolderId = null;//Se obtiene el id desde google driver
            String latestFolderId = null;
            for (String part : pathParts) {
                latestFolderId = getFileIdFromFileName(googleDriveAccessToken, part, parentFolderId);
                if (!isNullOrEmpty(latestFolderId)) {
                    LOG.debug("Folder " + part + " found, folder ID is " + latestFolderId);
                } else {
                    LOG.debug("Folder " + part + " not found, creating.");
                    latestFolderId = createEmptyFile(googleDriveAccessToken, part,
                            "application/vnd.google-apps.folder", isNullOrEmpty(parentFolderId) ? "root" : parentFolderId);
                }
                parentFolderId = latestFolderId;
            }

            //copyToClipboard(mContext, folderPath+" id: "+parentFolderId, "tago");

            String gpsLoggerFolderId = latestFolderId;

            if (isNullOrEmpty(gpsLoggerFolderId)) {
                failureMessage = "Could not create folder";
                success = false;
            }
            else {
                // Now search for the file
                String gpxFileId = getFileIdFromFileName(googleDriveAccessToken, fileName, gpsLoggerFolderId);

                //Basic.msg(fileName + " : "+gpxFileId);
                if (isNullOrEmpty(gpxFileId)) {
                    failureMessage = "Error no se encontraron DATOS.";
                    return Result.failure(new Data.Builder().putString(KEY_RESULT_MESSAGE, failureMessage).build());
                }

                // The above empty file creation needs to happen first - this shouldn't be an 'else' to the above if.
                if (!isNullOrEmpty(gpxFileId)) {
                    LOG.debug("Uploading file contents");
                    //File destinationFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/cueblo.db");
                    failureMessage = ""+ downloadFileContents(googleDriveAccessToken, gpxFileId, fileToDownload, fileType);
                    //Basic.msg("Fail: "+failureMessage);
                }
            }
        }
        catch (Exception e) {
            LOG.error(e.getMessage(), e);
            success = false;
            failureMessage = e.getMessage();
            failureThrowable = e;
        }

        if(success){
            // Notify internal listeners
            //EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded());
            // Notify external listeners
            //Systems.sendFileUploadedBroadcast(getApplicationContext(), new String[]{fileToUpload.getAbsolutePath()}, "googledrive");
            failureMessage = "";
            return Result.success(new Data.Builder()
                        .putString(KEY_RESULT_MESSAGE, failureMessage)
                        .putString(KEY_IS_PRELOADER, isPreloader)
                        .putString(KEY_IS_NEW_OBJ, isNewObj)

                        .putStringArray(KEY_FILES_DOWNLOADED, new String[]{fileToDownload.getAbsolutePath()})
                        .build());
        }

        if(getRunAttemptCount() < getRetryLimit()){
            LOG.warn(String.format("Google Drive - attempt %d of %d failed, will retry", getRunAttemptCount(), getRetryLimit()));
            return Result.retry();
        }

        if(failureThrowable == null) {
            failureThrowable = new Exception(failureMessage);
        }

        //EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed(failureMessage, failureThrowable));
        return Result.failure(new Data.Builder().putString(KEY_RESULT_MESSAGE, failureMessage +" : "+ failureThrowable).build());
    }

    private String createEmptyFile(String accessToken, String fileName, String mimeType, String parentFolderId) throws Exception {

        String fileId = null;
        String createFileUrl = "https://www.googleapis.com/drive/v3/files";

        String createFilePayload = "   {\n" +
                "             \"name\": \"" + fileName + "\",\n" +
                "             \"mimeType\": \"" + mimeType + "\",\n" +
                "             \"parents\": [\"" + parentFolderId + "\"]\n" +
                "            }";


        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(createFileUrl);

        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), createFilePayload);
        requestBuilder = requestBuilder.method("POST", body);


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

    // Método para descargar un archivo desde Google Drive
    public int downloadFileContents(String accessToken, String gpxFileId, File destinationFile, String mType) throws Exception {
        String failureMessage = "";
        // Cambiar a endpoint de exportación para archivos de Google Sheets
        String fileDownloadUrl = "https://www.googleapis.com/drive/v3/files/" + gpxFileId + mType;

        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(fileDownloadUrl);

        // Añadir el token de acceso al encabezado de autorización
        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);

        // Crear la solicitud GET
        Request request = requestBuilder.get().build();

        // Ejecutar la solicitud
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                LOG.error("Error al descargar archivo: Código {} - {}", response.code(), errorBody);
                failureMessage = "Error al descargar archivo: Código " + response.code() + " - " + errorBody;
                throw new Exception(failureMessage);
            }

            // Obtener el flujo de datos del archivo
            assert response.body() != null;
            InputStream inputStream = response.body().byteStream();

            // Guardar el contenido en el archivo de destino
            String bytesCopy = "";
            try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    LOG.warn("f", "getStringFromInputStream - could not close stream");
                    bytesCopy = "Error de Conexion.";
                }
            }
            copyToClipboard(mContext, bytesCopy, "tago");


            LOG.debug("Archivo descargado exitosamente: {} en {}", gpxFileId, destinationFile.getAbsolutePath());
            return 0 ; // Retornar el ID del archivo descargado
        } catch (Exception e) {
            LOG.error("Error al descargar archivo: {}", e.getMessage(), e);
            throw e;
        }
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
     * Gets the GPSLogger-specific MIME type to use for a given filename/extension
     *
     * @param fileName
     * @return
     */
    public static String getMimeTypeFromFileName(String fileName) {
        if (fileName.endsWith("kml")) {
            return "application/vnd.google-earth.kml+xml";
        }

        if (fileName.endsWith("gpx")) {
            return "application/gpx+xml";
        }

        if (fileName.endsWith("zip")) {
            return "application/zip";
        }

        if (fileName.endsWith("xml")) {
            return "application/xml";
        }

        if (fileName.endsWith("nmea") || fileName.endsWith("txt")) {
            return "text/plain";
        }

        if (fileName.endsWith("geojson")) {
            return "application/vnd.geo+json";
        }

        if (fileName.endsWith("csv")){
            return "application/vnd.google-apps.spreadsheet";
        }

        return "application/octet-stream";

    }

    public static String getFileIdFromFileName(String accessToken, String fileName, String inFolderId) throws Exception {
        String fileId = "";
        fileName = URLEncoder.encode(fileName, "UTF-8");

        String inFolderParam = "";
        if (!isNullOrEmpty(inFolderId)) {
            inFolderParam = "+and+'" + inFolderId + "'+in+parents";
        }
        String searchUrl = "https://www.googleapis.com/drive/v3/files?q=name%20%3D%20%27" + fileName + "%27%20and%20trashed%20%3D%20false" + inFolderParam;
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(searchUrl);

        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);

        Request request = requestBuilder.build();
        Response response = client.newCall(request).execute();
        String fileMetadata = response.body().string();
        LOG.debug(fileMetadata);
        response.body().close();
        JSONObject fileMetadataJson = new JSONObject(fileMetadata);
        if (fileMetadataJson.getJSONArray("files") != null && fileMetadataJson.getJSONArray("files").length() > 0) {
            fileId = fileMetadataJson.getJSONArray("files").getJSONObject(0).get("id").toString();
            LOG.debug("Found file with ID " + fileId);
        }
        return fileId;
    }
}