package com.example.cow_data.drive;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cow_data.utls.Basic;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.utls.Msg;

import net.openid.appauth.AuthorizationService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DriveUtils {
    private static final Logger LOG = Logs.of(DriveUpWorker.class);

    private static final String TAG = "GoogleDriveFileHelper";

    public static final String MIME_FOLDER = "application/vnd.google-apps.folder";

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    /**
     * Obtiene un accessToken fresco (renovado si es necesario).
     * Bloquea hasta que tenga el token o falle.
     */
    public static String getFreshAccessToken(AuthState authState, AuthorizationService authorizationService) throws Exception {
        if (authState == null || !authState.isAuthorized()) {
            throw new Exception("No hay autorización válida en Google Drive");
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> tokenRef = new AtomicReference<>();
        final AtomicReference<AuthorizationException> errorRef = new AtomicReference<>();

        authState.performActionWithFreshTokens(authorizationService, (accessToken, idToken, ex) -> {
            if (ex != null) {
                errorRef.set(ex);
            } else {
                tokenRef.set(accessToken);
            }
            latch.countDown();   // Liberamos el latch
        });

        // Esperamos a que el callback termine (máximo 15 segundos)
        boolean finished = latch.await(15, TimeUnit.SECONDS);

        if (!finished) {
            throw new Exception("Timeout al obtener token fresco de Google Drive");
        }

        AuthorizationException error = errorRef.get();
        if (error != null) {
            throw new Exception("Error al refrescar token: " + error.toJsonString(), error);
        }

        String token = tokenRef.get();
        if (token == null || token.isEmpty()) {
            throw new Exception("No se recibió accessToken");
        }

        return token;
    }


    public static DriveFileMeta getFileMetaFromDrive(String accessToken, String fileName, String folderId) {
        try {
            String escaped = fileName.replace("\\", "\\\\").replace("'", "\\'");
            String query = "name = '" + escaped + "' and trashed = false";
            if (!isNullOrEmpty(folderId)) {
                query += " and '" + folderId + "' in parents";
            }

            String url = "https://www.googleapis.com/drive/v3/files"
                    + "?q=" + URLEncoder.encode(query, "UTF-8")
                    + "&fields=files(id,name,md5Checksum,modifiedTime)"
                    + "&pageSize=1";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Accept", "application/json")
                    .build();

            try (Response response = HTTP.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Error HTTP " + response.code() + " buscando: " + fileName);
                    return null;
                }
                String body = response.body() != null ? response.body().string() : "";
                JSONObject json = new JSONObject(body);
                JSONArray files = json.optJSONArray("files");
                if (files == null || files.length() == 0) return null;

                JSONObject f = files.getJSONObject(0);
                String id = f.optString("id", null);
                String name = f.optString("name", "Unknown");
                String md5 = f.optString("md5Checksum", "");
                String modified = f.optString("modifiedTime", "");

                if (id == null || id.isEmpty()) return null;

                Log.d(TAG, "Archivo encontrado: " + name + " | MD5: " + md5 + " | Modified: " + modified);
                return new DriveFileMeta(id, name, md5, modified);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en getFileMetaFromDrive para " + fileName, e);
            return null;
        }
    }

    // ====================== MÉTODOS DE AYUDA ======================

    /**
     * Obtiene solo el MD5 del archivo
     */
    public static String getMd5Checksum(String accessToken, String fileName, String folderId) {
        DriveFileMeta meta = getFileMetaFromDrive(accessToken, fileName, folderId);
        return (meta != null) ? meta.md5Checksum : null;
    }

    /**
     * Obtiene solo la fecha de modificación
     */
    public static String getModifiedTime(String accessToken, String fileName, String folderId) {
        DriveFileMeta meta = getFileMetaFromDrive(accessToken, fileName, folderId);
        return (meta != null) ? meta.modifiedTime : null;
    }

    /**
     * Verifica si el archivo existe en la carpeta
     */
    public static boolean fileExists(String accessToken, String fileName, String folderId) {
        return getFileMetaFromDrive(accessToken, fileName, folderId) != null;
    }

    public static long parseGoogleDriveTime(String modifiedTime) {
        if (modifiedTime == null || modifiedTime.isEmpty()) {
            return 0;
        }
        try {
            // Google usa formato ISO 8601
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(modifiedTime);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            Log.e("No se pudo parsear la fecha de Drive: {}", modifiedTime);
            return 0;
        }
    }

    public static String getLocalFileMd5(File file) {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5sum) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Busca carpeta por nombre exacto en el padre.
     * Si hay varias, devuelve la más antigua (primera creada) para estabilizar.
     */
    public static String findFolderId(String accessToken, String folderName, String parentId) throws Exception {
        if (isNullOrEmpty(folderName)) return "";

        String escaped = folderName.replace("\\", "\\\\").replace("'", "\\'");
        String parent = isNullOrEmpty(parentId) ? "root" : parentId;

        String query = "name = '" + escaped + "'"
                + " and mimeType = '" + MIME_FOLDER + "'"
                + " and trashed = false"
                + " and '" + parent + "' in parents";

        String url = "https://www.googleapis.com/drive/v3/files"
                + "?q=" + URLEncoder.encode(query, "UTF-8")
                + "&spaces=drive"
                + "&fields=files(id,name,createdTime)"
                + "&orderBy=createdTime"
                + "&pageSize=10";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                LOG.warn("findFolderId HTTP {} - {}", response.code(), err);
                // Importante: NO devolver "" como si "no existiera" si fue error de red
                throw new Exception("Error buscando carpeta '" + folderName + "': " + response.code());
            }

            String body = response.body() != null ? response.body().string() : "{}";
            JSONArray files = new JSONObject(body).optJSONArray("files");
            if (files == null || files.length() == 0) {
                return "";
            }

            if (files.length() > 1) {
                LOG.warn("Hay {} carpetas llamadas '{}'. Se usará la más antigua.", files.length(), folderName);
            }
            return files.getJSONObject(0).getString("id");
        }
    }

    /**
     * Obtiene la carpeta o la crea. Evita duplicados en el caso normal.
     */
    public static synchronized String getOrCreateFolder(
            String accessToken, String folderName, String parentId) throws Exception {

        String parent = isNullOrEmpty(parentId) ? "root" : parentId;

        // 1. Buscar
        String id = findFolderId(accessToken, folderName, parent);
        if (!isNullOrEmpty(id)) {
            return id;
        }

        // 2. Crear
        LOG.info("Creando carpeta '{}' bajo {}", folderName, parent);
        id = createEmptyFile(accessToken, folderName, MIME_FOLDER, parent);

        // 3. Por si otro hilo creó otra a la vez: volver a listar y quedarte con la antigua
        String stable = findFolderId(accessToken, folderName, parent);
        if (!isNullOrEmpty(stable)) {
            if (!stable.equals(id)) {
                LOG.warn("Se detectó carpeta duplicada al crear '{}'. Usando ID estable {}", folderName, stable);
            }
            return stable;
        }
        return id;
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
                LOG.warn("API error: " + response.code() + " - " + errorBody);
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
        if (DriveUtils.isNullOrEmpty(folderId)) {
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
            if (!DriveUtils.isNullOrEmpty(nextPageToken)) {
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
                    Msg.m("Error API: " + code + " - " + errorMsg + ". Body: " + errorBody);

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
                        if (!DriveUtils.isNullOrEmpty(id) && !DriveUtils.isNullOrEmpty(name)) {
                            fileList.add(new String[]{id, name});
                            LOG.debug("Archivo encontrado: " + name + " (ID: " + id + ")");
                        }
                    }
                }
                nextPageToken = fileMetadataJson.optString("nextPageToken", null);
            }
        } while (!DriveUtils.isNullOrEmpty(nextPageToken));

        LOG.info("Total archivos en carpeta " + folderId + ": " + fileList.size());
        return fileList;
    }

    /**
     * Descarga un archivo desde Google Drive (soporta archivos normales y Google Sheets)
     *
     * @param accessToken     Token de acceso válido
     * @param mFileId          ID del archivo en Google Drive
     * @param mFile Archivo local donde se guardará
     * @param mType       Sufijo de la URL: "?alt=media" o "/export?mimeType=..."
     * @return 0 = éxito | cualquier otro valor = error
     * @throws Exception si ocurre algún error
     */
    public static int downloadFileFromDrive(String accessToken, String mFileId, File mFile, String mType) throws Exception {

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
            try (FileOutputStream fos = new FileOutputStream(mFile)) {
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

            LOG.debug("Archivo descargado exitosamente: {} en {}", mFileId, mFile.getAbsolutePath());
            return 0 ; // Retornar el ID del archivo descargado
        } catch (Exception e) {
            LOG.error("Error al descargar archivo: {}", e.getMessage(), e);
            throw e;
        }
    }

    public static String createEmptyFile(String accessToken, String fileName, String mimeType, String parentFolderId) throws Exception {

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

    /**
     * Lista archivos CSV, Google Sheets y archivos Binarios (.bin) dentro de una carpeta
     */
    public static List<DriveFileMeta> listFilesFromDrive(String accessToken, String folderId) throws Exception {
        List<DriveFileMeta> fileList = new ArrayList<>();

        if (DriveUtils.isNullOrEmpty(folderId)) {
            throw new IllegalArgumentException("folderId es obligatorio");
        }

        // Query extendida:
        // 1. CSV nativo (text/csv)
        // 2. Google Sheets (application/vnd.google-apps.spreadsheet)
        // 3. Binarios genéricos (application/octet-stream)
        // 4. Archivos que terminen en .csv o .bin en el nombre
        String query = "'" + folderId + "' in parents " +
                "and trashed = false " +
                "and (" +
                "mimeType = 'text/csv' " +
                "or mimeType = 'application/vnd.google-apps.spreadsheet' " +
                "or mimeType = 'application/octet-stream' " +
                "or name contains '.csv' " +
                "or name contains '.bin'" +
                ")";

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

        // Es importante incluir 'mimeType' en fields para que luego puedas saber qué tipo de archivo es
        String baseUrl = "https://www.googleapis.com/drive/v3/files" +
                "?q=" + encodedQuery +
                "&fields=files(id,name,modifiedTime,mimeType)&orderBy=modifiedTime desc";

        OkHttpClient client = new OkHttpClient();
        String nextPageToken = null;

        do {
            String url = baseUrl;
            if (!DriveUtils.isNullOrEmpty(nextPageToken)) {
                url += "&pageToken=" + URLEncoder.encode(nextPageToken, StandardCharsets.UTF_8.toString());
            }

            LOG.debug("Consultando archivos en Drive: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    LOG.error("Error API: {} - {}", response.code(), errorBody);
                    throw new Exception("Error al listar archivos: " + response.code());
                }

                String jsonResponse = response.body().string();
                JSONObject json = new JSONObject(jsonResponse);

                JSONArray files = json.optJSONArray("files");
                if (files != null) {
                    for (int i = 0; i < files.length(); i++) {
                        JSONObject f = files.getJSONObject(i);
                        String id = f.optString("id");
                        String name = f.optString("name");
                        String modifiedTime = f.optString("modifiedTime");
                        String mimeType = f.optString("mimeType"); // Obtenemos el mimeType

                        if (!DriveUtils.isNullOrEmpty(id) && !DriveUtils.isNullOrEmpty(name)) {
                            // Pasamos el mimeType al constructor si tu DriveFileMeta lo soporta
                            fileList.add(new DriveFileMeta(id, name, mimeType, modifiedTime));
                        }
                    }
                }

                nextPageToken = json.optString("nextPageToken", null);
            }
        } while (!DriveUtils.isNullOrEmpty(nextPageToken));

        LOG.info("Total de archivos encontrados en carpeta " + folderId + ": " + fileList.size());
        return fileList;
    }

    /**
     * Gets the MIME type to use for a given filename/extension
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

    /**
     * Checks if a string is null or empty
     *
     * @param text
     * @return
     */
    public static boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
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
