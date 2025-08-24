package com.example.cow_data;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.Collections;

import com.google.api.client.http.FileContent;

public class GoogleDriveManager {
    private static final String TAG = "GoogleDriveManager";
    private Drive mDriveService;
    private final Context mContext;
    private boolean isInitialized = false;

    public GoogleDriveManager(Context context, String accountName) {
        mContext = context;
        try {
            Log.d(TAG, "Inicializando GoogleDriveManager con accountName: " + accountName);
            if (accountName == null || accountName.isEmpty()) {
                String errorMsg = "Error: accountName es nulo o vacío.";
                Log.e(TAG, errorMsg);
                copyToClipboard(errorMsg);
                showToast(errorMsg);
                mDriveService = null;
                return;
            }

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccountName(accountName);
            Log.d(TAG, "Credenciales configuradas para: " + accountName);

            mDriveService = new Drive.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName("TuAppName")
                    .build();
            isInitialized = true;
            Log.d(TAG, "Servicio Drive inicializado correctamente.");
        } catch (Exception e) {
            String errorMsg = "Error al inicializar GoogleDriveManager: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            copyToClipboard(errorMsg);
            showToast(errorMsg);
            mDriveService = null;
        }
    }

    /**
     * Sube un archivo a Google Drive en la carpeta raíz.
     * @param fileToUpload El archivo local a subir.
     * @return true si la subida es exitosa, false si falla.
     */
    public boolean uploadFile(java.io.File fileToUpload) {
        if (!isInitialized || mDriveService == null) {
            String errorMsg = "Error: Servicio Drive no inicializado.";
            Log.e(TAG, errorMsg);
            copyToClipboard(errorMsg);
            showToast(errorMsg);
            return false;
        }

        try {
            // Validar archivo
            Log.d(TAG, "Validando archivo: " + fileToUpload.getAbsolutePath());
            if (!fileToUpload.exists()) {
                String errorMsg = "Error: El archivo " + fileToUpload.getAbsolutePath() + " no existe.";
                Log.e(TAG, errorMsg);
                copyToClipboard(errorMsg);
                showToast(errorMsg);
                return false;
            }
            if (!fileToUpload.canRead()) {
                String errorMsg = "Error: No se puede leer el archivo " + fileToUpload.getAbsolutePath();
                Log.e(TAG, errorMsg);
                copyToClipboard(errorMsg);
                showToast(errorMsg);
                return false;
            }

            Log.d(TAG, "Iniciando subida del archivo: " + fileToUpload.getName());

            // Metadata del archivo
            File metadata = new File()
                    .setName(fileToUpload.getName())
                    .setParents(Collections.singletonList("root"));

            // Contenido del archivo
            String mimeType = "application/octet-stream";
            FileContent mediaContent = new FileContent(mimeType, fileToUpload);

            // Subida
            Log.d(TAG, "Subiendo archivo a Google Drive...");
            File uploadedFile = mDriveService.files().create(metadata, mediaContent)
                    .setFields("id")
                    .execute();

            String successMsg = "Archivo subido con éxito. ID: " + uploadedFile.getId();
            Log.d(TAG, successMsg);
            showToast(successMsg);
            return true;

        } catch (UserRecoverableAuthIOException e) {
            String errorMsg = "Error de autenticación: " + e.getMessage() + ". Necesita acción del usuario.";
            Log.e(TAG, errorMsg, e);
            copyToClipboard(errorMsg);
            showToast(errorMsg);
            // Opcional: Lanza la actividad para re-autenticar
            // Intent intent = e.getIntent();
            // mContext.startActivityForResult(intent, REQUEST_CODE_AUTH);
            return false;
        } catch (IOException e) {
            String errorMsg = "Error de IO al subir archivo: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            copyToClipboard(errorMsg);
            showToast(errorMsg);
            return false;
        } catch (Exception e) {
            String errorMsg = "Error inesperado al subir archivo: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            copyToClipboard(errorMsg);
            showToast(errorMsg);
            return false;
        }
    }

    /**
     * Copia un mensaje al portapapeles.
     * @param text Texto a copiar.
     */
    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Google Drive Error", text);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Mensaje copiado al portapapeles: " + text);
        } catch (Exception e) {
            Log.e(TAG, "Error al copiar al portapapeles: " + e.getMessage(), e);
        }
    }

    /**
     * Muestra un Toast en la UI.
     * @param message Mensaje a mostrar.
     */
    private void showToast(String message) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mContext.getMainExecutor().execute(() -> {
                    Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Toast mostrado: " + message);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar Toast: " + e.getMessage(), e);
        }
    }
}