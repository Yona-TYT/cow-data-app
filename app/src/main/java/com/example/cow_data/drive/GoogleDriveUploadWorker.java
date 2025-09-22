package com.example.cow_data.drive;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cow_data.Basic;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.ex.UploadEvents;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleDriveUploadWorker extends Worker {
    private static final Logger LOG = Logs.of(GoogleDriveUploadWorker.class);

    private String googleDriveAccessToken;

    private final Context mContext;


    public GoogleDriveUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        String filePath = getInputData().getString("filePath");
        boolean isList = getInputData().getBoolean("list", false);

        String[] filePaths = getInputData().getStringArray("filePaths");



        File fileToUpload = new File(filePath);
        boolean success = true;
        String failureMessage = "";
        Throwable failureThrowable = null;


        AuthState authState = GoogleDriveManager.getAuthState();
        if (!authState.isAuthorized()) {
            EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not upload to Google Drive. Not Authorized."));
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
                return Result.failure();
            }
            // Figure out the Folder ID to upload to, from the path; recursively create if it doesn't exist.
            String folderPath = PreferenceHelper.getInstance().getGoogleDriveFolderPath();
            String[] pathParts = folderPath.split("/");
            String parentFolderId = PreferenceHelper.getInstance().getGoogleDriveFolderId();
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
            else{
                if (isList){
                    String imgFolderName = PreferenceHelper.getInstance().getGoogleDriveImgPath();
                    String imgFolderId = getFileIdFromFileName(googleDriveAccessToken, imgFolderName, gpsLoggerFolderId, "application/vnd.google-apps.folder");
                    if (!isNullOrEmpty(imgFolderId)) {
                        LOG.debug("Folder " + imgFolderName + " found, folder ID is " + gpsLoggerFolderId);
                    } else {
                        LOG.debug("Folder " + imgFolderName + " not found, creating.");
                        imgFolderId = createEmptyFile(googleDriveAccessToken, imgFolderName,
                                "application/vnd.google-apps.folder", gpsLoggerFolderId);
                    }
                    if (isNullOrEmpty(imgFolderId)) {
                        failureMessage = "Could not create folder";
                        success = false;
                    }
                    else {
                        for(String path : filePaths){
                            File mFile = new File(path);
                            if(mFile.exists()){
                                fileToUpload = mFile;
                                if(!filesSet(mFile, imgFolderId)){
                                    failureMessage = "Could not create file";
                                    break;
                                }
                            }
                        }
                    }
                }
                else {
                    if(!filesSet(fileToUpload, gpsLoggerFolderId)){
                        failureMessage = "Could not create file";
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
                EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded("Archivos Subidos: ", filePaths.length));
            }
            else {
                EventBus.getDefault().post(new UploadEvents.GoogleDrive().succeeded());
            }
            // Notify external listeners
            Basic.sendFileUploadedBroadcast(getApplicationContext(), new String[]{fileToUpload.getAbsolutePath()}, "googledrive");
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

    private boolean filesSet(File fileToUpload, String folderId) throws Exception {
        // Now search for the file
        String gpxFileId = getFileIdFromFileName(googleDriveAccessToken, fileToUpload.getName(), folderId);

        if (isNullOrEmpty(gpxFileId)) {
            LOG.debug("Creating an empty file first.");
            gpxFileId = createEmptyFile(googleDriveAccessToken, fileToUpload.getName(), getMimeTypeFromFileName(fileToUpload.getName()), folderId);

            if (isNullOrEmpty(gpxFileId)) {
                return false;
            }
        }

        // The above empty file creation needs to happen first - this shouldn't be an 'else' to the above if.
        if (!isNullOrEmpty(gpxFileId)) {
            LOG.debug("Uploading file contents");
            updateFileContents(googleDriveAccessToken, gpxFileId, fileToUpload);
        }

        return true;
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

//    private String createEmptyFile(String accessToken, String fileName, String mimeType, String parentFolderId) throws Exception {
//        if (isNullOrEmpty(fileName)) {
//            return "";
//        }
//
//        // Construir JSON de forma segura con JSONObject
//        JSONObject fileMetadataJson = new JSONObject();
//        fileMetadataJson.put("name", fileName);
//        fileMetadataJson.put("mimeType", mimeType);
//
//        // Manejar parents: OMITIR si es root (nulo o vacío) para visibilidad en UI
//        // Solo agregar si es un ID real de carpeta padre
//        if (!isNullOrEmpty(parentFolderId)) {
//            JSONArray parentsArray = new JSONArray();
//            parentsArray.put(parentFolderId);  // Single parent
//            fileMetadataJson.put("parents", parentsArray);
//        }
//        // Si parentFolderId es nulo/vacío, NO agregar "parents" -> crea en root de My Drive
//
//        String createFileUrl = "https://www.googleapis.com/drive/v3/files";
//        String payload = fileMetadataJson.toString();  // JSON válido (minificado)
//
//        OkHttpClient client = new OkHttpClient();
//        // Coincide con el viejo: sin charset
//        MediaType mediaType = MediaType.parse("application/json");
//        RequestBody body = RequestBody.create(payload, mediaType);
//        Request.Builder requestBuilder = new Request.Builder()
//                .url(createFileUrl)
//                .addHeader("Authorization", "Bearer " + accessToken)
//                .post(body);
//
//        Request request = requestBuilder.build();
//
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                String errorBody = response.body() != null ? response.body().string() : "No body";
//                LOG.error("Error al crear carpeta: Código " + response.code() + " - " + response.message() + ". Body: " + errorBody);
//                throw new Exception("Fallo al crear carpeta '" + fileName + "': " + response.code() + " - " + errorBody);
//            }
//
//            String fileMetadata = response.body().string();
//            LOG.debug("Respuesta de creación: " + fileMetadata);
//
//            JSONObject responseJson = new JSONObject(fileMetadata);
//            String fileId = responseJson.optString("id", null);
//
//            if (isNullOrEmpty(fileId)) {
//                throw new Exception("No se devolvió ID en la respuesta: " + fileMetadata);
//            }
//
//            LOG.info("Carpeta creada exitosamente: " + fileName + " con ID: " + fileId);
//            return fileId;
//        }
//    }


    private String updateFileContents(String accessToken, String gpxFileId, File fileToUpload) throws Exception {
        FileInputStream fis = new FileInputStream(fileToUpload);
        String fileId = null;

        String fileUpdateUrl = "https://www.googleapis.com/upload/drive/v3/files/" + gpxFileId + "?uploadType=media";

        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(fileUpdateUrl);

        requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        RequestBody body = RequestBody.create(MediaType.parse(getMimeTypeFromFileName(fileToUpload.getName())), getByteArrayFromInputStream(fis));
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

    /**
     * Checks if a string is null or empty
     *
     * @param text
     * @return
     */
    public static boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
    public static boolean isNullOrEmpty2(String text) {
        Basic.msg(text);
        return text == null || text.trim().isEmpty();
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
        if (isNullOrEmpty(fileName)) {
            return fileId;
        }
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