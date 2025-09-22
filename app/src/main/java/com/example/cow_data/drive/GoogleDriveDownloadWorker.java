package com.example.cow_data.drive;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cow_data.Basic;
import com.example.cow_data.StartVar;
import com.example.cow_data.ex.DownloadEvents;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleDriveDownloadWorker extends Worker {
    private static final Logger LOG = Logs.of(GoogleDriveDownloadWorker.class);

    private String googleDriveAccessToken;
    private final Context mContext;

    private static final String KEY_RESULT_MESSAGE = "result_message";
    private static final String KEY_FILES_DOWNLOADED = "files_downloaded";
    private static final String KEY_IS_PRELOADER = "preloader";
    private static final String KEY_IS_NEW_OBJ = "newobj";
    private static final String KEY_IS_FILE_OK = "file";
    private static final String KEY_IS_CHECK = "check";
    private static final String KEY_IS_IMG = "img";


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
        boolean isFileOk = true;

        int count = 0;

        String filePath = getInputData().getString("path");
        String fileName = getInputData().getString("name");
        String fileType = getInputData().getString("type");
        boolean isPreloader = getInputData().getBoolean("preloader", false);
        boolean isNewObj = getInputData().getBoolean("newobj", false);
        boolean isCheck = getInputData().getBoolean("check", false);
        boolean isImg = getInputData().getBoolean("img", false);

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
                        EventBus.getDefault().post(new DownloadEvents.GoogleDrive().failed(ex.toJsonString(), ex));
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

            String mFolderId = latestFolderId;

            if (isNullOrEmpty(mFolderId)) {
                failureMessage = "Could not create folder";
                success = false;
            }
            else {
                if(isImg) {
                    String imgFolderName = PreferenceHelper.getInstance().getGoogleDriveImgPath();
                    String imgFolderId = getFileIdFromFileName(googleDriveAccessToken, imgFolderName, mFolderId, "application/vnd.google-apps.folder");
                    if (!isNullOrEmpty(imgFolderId)) {
                        LOG.debug("Folder " + imgFolderName + " found, folder ID is " + mFolderId);
                    } else {
                        LOG.debug("Folder " + imgFolderName + " not found, creating.");
                        imgFolderId = createEmptyFile(googleDriveAccessToken, imgFolderName,
                                "application/vnd.google-apps.folder", mFolderId);
                    }

                    if (isNullOrEmpty(imgFolderId)) {
                        failureMessage = "Could not create folder";
                        success = false;
                    }
                    else {

                        List<String[]> mList = getDriveIdAndNameList(googleDriveAccessToken, imgFolderId);
                        count = mList.size();
                        for (String[] dataFile : mList){
                            String fId = dataFile[0];
                            String fName = dataFile[1];

                            if (isNullOrEmpty(fId)) {
                                isFileOk = false;
                                failureMessage = "Error no se encontraron DATOS.";
                                return Result.failure(new Data.Builder().putString(KEY_RESULT_MESSAGE, failureMessage)
                                        .putBoolean(KEY_IS_PRELOADER, isPreloader)
                                        .putBoolean(KEY_IS_FILE_OK, isFileOk)
                                        .build());
                            }
                            // The above empty file creation needs to happen first - this shouldn't be an 'else' to the above if.
                            if (!isNullOrEmpty(fId)) {
                                File currFile = new File(filePath+"/"+fName);

                                LOG.debug("Uploading file contents");
                                //File destinationFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/cueblo.db");
                                failureMessage = "" + downloadFileContents(googleDriveAccessToken, fId, currFile, fileType);
                                //Basic.msg("Fail: "+failureMessage);
                            }
                        }
                    }
                }
                else {
                    // Now search for the file
                    String gpxFileId = getFileIdFromFileName(googleDriveAccessToken, fileName, mFolderId);

                    //Basic.msg(fileName + " : "+gpxFileId);
                    if (isNullOrEmpty(gpxFileId)) {
                        isFileOk = false;
                        failureMessage = "Error no se encontraron DATOS.";
                        return Result.failure(new Data.Builder().putString(KEY_RESULT_MESSAGE, failureMessage)
                                .putBoolean(KEY_IS_PRELOADER, isPreloader)
                                .putBoolean(KEY_IS_FILE_OK, isFileOk)
                                .build());
                    }

                    // The above empty file creation needs to happen first - this shouldn't be an 'else' to the above if.
                    if (!isNullOrEmpty(gpxFileId)) {
                        LOG.debug("Uploading file contents");
                        //File destinationFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/cueblo.db");
                        failureMessage = "" + downloadFileContents(googleDriveAccessToken, gpxFileId, fileToDownload, fileType);
                        //Basic.msg("Fail: "+failureMessage);
                    }
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
            if(isImg) {
                // Notify internal listeners
                EventBus.getDefault().post(new DownloadEvents.GoogleDrive().succeeded("Archivos Descargados: ", count));
                // Notify external listeners
                //Basic.sendFileUploadedBroadcast(getApplicationContext(), new String[]{fileToUpload.getAbsolutePath()}, "googledrive");
            }
            failureMessage = "";
            return Result.success(new Data.Builder()
                        .putString(KEY_RESULT_MESSAGE, failureMessage)
                        .putBoolean(KEY_IS_PRELOADER, isPreloader)
                        .putBoolean(KEY_IS_NEW_OBJ, isNewObj)
                        .putBoolean(KEY_IS_CHECK, isCheck)
                        .putBoolean(KEY_IS_IMG, isImg)
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
    public int downloadFileContents(String accessToken, String mFileId, File destinationFile, String mType) throws Exception {
        String failureMessage = "";
        // Cambiar a endpoint de exportación para archivos de Google Sheets
        String fileDownloadUrl = "https://www.googleapis.com/drive/v3/files/" + mFileId + mType;

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
            //copyToClipboard(mContext, bytesCopy, "tago");

            LOG.debug("Archivo descargado exitosamente: {} en {}", mFileId, destinationFile.getAbsolutePath());
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
        return text == null || text.trim().isEmpty();
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

    public static String getFileIdFromFileName(String accessToken, String fileName, String inFolderId, String mimeType) throws Exception {
        if (isNullOrEmpty(fileName)) {
            return "";
        }

        // Build plain query string (escape specials like ' with \ if in fileName)
        String escapedFileName = fileName.replace("\\", "\\\\").replace("'", "\\'");  // Escape for query syntax
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("name = '").append(escapedFileName).append("'");
        queryBuilder.append(" and trashed = false");

        if (!isNullOrEmpty(inFolderId)) {
            queryBuilder.append(" and '").append(inFolderId).append("' in parents");
        }

        if (!isNullOrEmpty(mimeType)) {
            queryBuilder.append(" and mimeType = '").append(mimeType).append("'");
        }

        String fullQuery = queryBuilder.toString();
        String encodedQuery = URLEncoder.encode(fullQuery, StandardCharsets.UTF_8.toString());

        String searchUrl = "https://www.googleapis.com/drive/v3/files?q=" + encodedQuery;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                LOG.error("API error: " + response.code() + " - " + errorBody);
                throw new Exception("Search failed: " + errorBody);  // e.g., "Invalid query"
            }

            String fileMetadata = response.body().string();
            LOG.debug(fileMetadata);

            JSONObject fileMetadataJson = new JSONObject(fileMetadata);
            JSONArray filesArray = fileMetadataJson.optJSONArray("files");
            if (filesArray != null && filesArray.length() > 0) {
                if (filesArray.length() > 1) {
                    LOG.warn("Multiple matches for '" + fileName + "'. Returning first.");
                }
                return filesArray.getJSONObject(0).getString("id");
            }
        }

        return "";  // Not found
    }
    public static List<String[]> getDriveIdAndNameList(String accessToken, String folderId) throws Exception {
        List<String[]> fileList = new ArrayList<>();
        if (isNullOrEmpty(folderId)) {
            throw new IllegalArgumentException("folderId requerido");
        }

        // Consulta: todos los archivos en la carpeta, no en papelera
        String query = "'" + folderId + "' in parents and trashed = false";
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

        // FIX: Separa files y nextPageToken con coma (nivel superior)
        String baseUrl = "https://www.googleapis.com/drive/v3/files?q=" + encodedQuery + "&fields=files(id,name),nextPageToken";

        OkHttpClient client = new OkHttpClient();
        String url = baseUrl;
        String nextPageToken = null;

        do {
            if (!isNullOrEmpty(nextPageToken)) {
                url = baseUrl + "&pageToken=" + URLEncoder.encode(nextPageToken, StandardCharsets.UTF_8.toString());
            }

            LOG.debug("Consultando URL: " + url);

            Request.Builder requestBuilder = new Request.Builder().url(url);
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
            Request request = requestBuilder.build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    int code = response.code();
                    String errorMsg = response.message();
                    String errorBody = "";
                    try {
                        if (response.body() != null) {
                            errorBody = response.body().string();
                        }
                    } catch (Exception bodyEx) {
                        LOG.error("Error al leer body: " + bodyEx.getMessage());
                    }

                    LOG.error("Error API: Código " + code + " - " + errorMsg + ". Body: " + errorBody);
                    Basic.msg("Error API: " + code + " - " + errorMsg + ". Body: " + errorBody);

                    throw new Exception("Error API " + code + ": " + errorMsg + ". Detalles: " + errorBody);
                }

                String fileMetadata = response.body().string();
                LOG.debug("Respuesta: " + fileMetadata);  // Log para debug

                JSONObject fileMetadataJson = new JSONObject(fileMetadata);
                JSONArray filesArray = fileMetadataJson.optJSONArray("files");

                if (filesArray != null && filesArray.length() > 0) {
                    for (int i = 0; i < filesArray.length(); i++) {
                        JSONObject fileObj = filesArray.getJSONObject(i);
                        String id = fileObj.optString("id", "");
                        String name = fileObj.optString("name", "");
                        if (!isNullOrEmpty(id) && !isNullOrEmpty(name)) {
                            fileList.add(new String[]{id, name});
                            LOG.debug("Archivo encontrado: " + name + " (ID: " + id + ")");
                        }
                    }
                }

                nextPageToken = fileMetadataJson.optString("nextPageToken", null);

            }
        } while (!isNullOrEmpty(nextPageToken));

        LOG.info("Total archivos en carpeta " + folderId + ": " + fileList.size());
        return fileList;
    }
}