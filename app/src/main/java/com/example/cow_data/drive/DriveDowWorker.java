package com.example.cow_data.drive;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cow_data.StartVar;
import com.example.cow_data.ex.DownloadEvents;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.ex.UploadEvents;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;

public class DriveDowWorker extends Worker {
    private static final Logger LOG = Logs.of(DriveDowWorker.class);

    private String googleDriveAccessToken;
    private final Context mContext;

    private static final String KEY_RESULT_MESSAGE = "result_message";
    private static final String KEY_FILES_DOWNLOADED = "files_downloaded";
    private static final String KEY_IS_PRELOADER = "preloader";
    private static final String KEY_IS_NEW_OBJ = "newobj";
    private static final String KEY_IS_FILE_OK = "file";
    private static final String KEY_IS_CHECK = "check";
    private static final String KEY_IS_IMG = "img";
    private static final String KEY_IS_ID = "isId";

    private int count = 0;


    public DriveDowWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        String failureMessage = "";
        boolean isFileOk = true;
        count = 0;

        String filePath = getInputData().getString("path");
        String fileName = getInputData().getString("name");
        String fileType = getInputData().getString("type"); // ej. "?alt=media"
        String fileId = getInputData().getString("fileId");

        boolean isPreloader = getInputData().getBoolean("preloader", false);
        boolean isNewObj = getInputData().getBoolean("newobj", false);
        boolean isCheck = getInputData().getBoolean("check", false);
        boolean isImg = getInputData().getBoolean("img", false);
        boolean isId = fileId != null && !fileId.isEmpty();

        File fileToDownload = new File(filePath);
        boolean success = true;
        Throwable failureThrowable = null;

        AuthState authState = DriveManager.getAuthState();
        if (authState == null || !authState.isAuthorized()) {
            failureMessage = "Could not download. Not Authorized.";
            return Result.failure(new Data.Builder()
                    .putString(KEY_RESULT_MESSAGE, failureMessage)
                    .putBoolean(KEY_IS_PRELOADER, isPreloader)
                    .putBoolean(KEY_IS_FILE_OK, false)
                    .build());
        }

        try {
            AuthorizationService authorizationService = DriveManager.getAuthorizationService(mContext);
            googleDriveAccessToken = DriveUtils.getFreshAccessToken(authState, authorizationService);

            if (DriveUtils.isNullOrEmpty(googleDriveAccessToken)) {
                failureMessage = "Failed to fetch Access Token.";
                return Result.failure(new Data.Builder()
                        .putString(KEY_RESULT_MESSAGE, failureMessage)
                        .putBoolean(KEY_IS_PRELOADER, isPreloader)
                        .putBoolean(KEY_IS_FILE_OK, false)
                        .build());
            }

            // ===== Carpetas del path (Sales-Save, etc.) =====
            String folderPath = PreferenceHelper.getInstance().getGoogleDriveFolderPath();
            String[] pathParts = folderPath.split("/");
            String parentFolderId = "root";

            for (String part : pathParts) {
                if (part == null || part.trim().isEmpty()) continue;
                // getOrCreateFolder ya usa mimeType = application/vnd.google-apps.folder
                parentFolderId = DriveUtils.getOrCreateFolder(
                        googleDriveAccessToken,
                        part.trim(),
                        parentFolderId
                );
            }

            String mFolderId = parentFolderId;

            if (DriveUtils.isNullOrEmpty(mFolderId)) {
                failureMessage = "Could not resolve folder";
                success = false;
            }
            else if (isImg) {
                String imgFolderName = PreferenceHelper.getInstance().getGoogleDriveImgPath();
                String imgFolderId = DriveUtils.getOrCreateFolder(
                        googleDriveAccessToken,
                        imgFolderName,
                        mFolderId
                );

                if (DriveUtils.isNullOrEmpty(imgFolderId)) {
                    failureMessage = "Could not create img folder";
                    success = false;
                } else {
                    File dir = new File(filePath);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    List<String[]> mList = DriveUtils.getDriveIdAndNameList(
                            googleDriveAccessToken, imgFolderId);
                    count = mList.size();

                    if (mList.isEmpty()) {
                        failureMessage = "No hay imágenes en Drive";
                        isFileOk = false;
                        success = false;
                    } else {
                        for (String[] dataFile : mList) {
                            String fId = dataFile[0];
                            String fName = dataFile[1];
                            if (DriveUtils.isNullOrEmpty(fId) || DriveUtils.isNullOrEmpty(fName)) {
                                continue;
                            }

                            File currFile = new File(dir, fName);

                            // forceDownload = true → bajar siempre en sync de imágenes
                            downloadFileContents(
                                    googleDriveAccessToken,
                                    imgFolderId,
                                    fId,
                                    currFile,
                                    fileType,
                                    true,
                                    fName
                            );
                        }
                    }
                }
            }
            else {
                // ===== Archivo DB =====
                String driveFileId;
                if (isId) {
                    driveFileId = fileId;
                } else {
                    String fileMime = DriveUtils.getMimeTypeFromFileName(fileName);
                    driveFileId = DriveUtils.getFileIdFromFileName(
                            googleDriveAccessToken, fileName, mFolderId, fileMime);

                    if (DriveUtils.isNullOrEmpty(driveFileId)) {
                        driveFileId = DriveUtils.getFileIdFromFileName(
                                googleDriveAccessToken, fileName, mFolderId, null);
                    }
                }

                if (DriveUtils.isNullOrEmpty(driveFileId)) {
                    isFileOk = false;
                    failureMessage = "Error no se encontraron DATOS.";
                    return Result.failure(new Data.Builder()
                            .putString(KEY_RESULT_MESSAGE, failureMessage)
                            .putBoolean(KEY_IS_PRELOADER, isPreloader)
                            .putBoolean(KEY_IS_FILE_OK, isFileOk)
                            .putBoolean(KEY_IS_CHECK, isCheck)
                            .putBoolean(KEY_IS_NEW_OBJ, isNewObj)
                            .build());
                }

                // fileToDownload = path local (DataSave.download.bin)
                // fileName       = nombre en Drive (DataSave.bin)
                downloadFileContents(
                        googleDriveAccessToken,
                        mFolderId,
                        driveFileId,
                        fileToDownload,
                        fileType,
                        isId || isCheck || isPreloader,
                        fileName
                );
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            success = false;
            failureMessage = e.getMessage() != null ? e.getMessage() : e.toString();
            failureThrowable = e;
        }

        if (success) {
            if (isImg) {
                EventBus.getDefault().post(
                        new UploadEvents.GoogleDrive().succeeded("Imágenes sincronizadas: ", count)
                );
                LOG.info("Google Drive - Imágenes procesadas: " + count);
                return Result.success(new Data.Builder()
                        .putString(KEY_RESULT_MESSAGE, "Imágenes descargadas: " + count)
                        .putInt("downloaded_count", count)
                        .putBoolean(KEY_IS_IMG, true)      // "img" = true
                        .putBoolean(KEY_IS_FILE_OK, true)
                        .putBoolean(KEY_IS_PRELOADER, isPreloader)
                        .build());
            }
            return Result.success(new Data.Builder()
                    .putString(KEY_RESULT_MESSAGE, "DriveDowWorker")
                    .putBoolean(KEY_IS_PRELOADER, isPreloader)
                    .putBoolean(KEY_IS_NEW_OBJ, isNewObj)
                    .putBoolean(KEY_IS_CHECK, isCheck)
                    .putBoolean(KEY_IS_IMG, isImg)
                    .putBoolean(KEY_IS_ID, isId)
                    .putBoolean(KEY_IS_FILE_OK, true)
                    .putStringArray(KEY_FILES_DOWNLOADED, new String[]{fileToDownload.getAbsolutePath()})
                    .build());
        }

        if (getRunAttemptCount() < getRetryLimit()) {
            return Result.retry();
        }

        EventBus.getDefault().post(
                new UploadEvents.GoogleDrive().failed(failureMessage, failureThrowable)
        );

        return Result.failure(new Data.Builder()
                .putString(KEY_RESULT_MESSAGE, failureMessage)
                .putBoolean(KEY_IS_PRELOADER, isPreloader)
                .putBoolean(KEY_IS_FILE_OK, isFileOk)
                .putBoolean(KEY_IS_CHECK, isCheck)
                .build());
    }

    protected int getRetryLimit() {
        return 3;
    }

    // Método para descargar un archivo desde Google Drive
    public int downloadFileContents(
            String accessToken,
            String folderId,
            String mFileId,
            File mFile,
            String mType,
            boolean forceDownload,
            String remoteName
    ) throws Exception {

        // Nombre en Drive (DataSave.bin). NUNCA uses mFile.getName() si es .download.bin
        if (remoteName == null || remoteName.isEmpty()) {
            remoteName = mFile.getName(); // no forzar EXPORT_NAME
        }

        if (!forceDownload) {
            DriveFileMeta driveFile = DriveUtils.getFileMetaFromDrive(
                    accessToken,
                    remoteName,
                    folderId
            );

            if (driveFile == null) {
                LOG.warn("Sin metadatos en Drive para " + remoteName + ". Se descarga igual.");
            } else {
                LOG.debug("Fecha en Drive: {}", driveFile.modifiedTime);

                if (mFile.exists() && driveFile.hasModifiedTime()) {
                    long localLastModified = mFile.lastModified();
                    long driveLastModified = DriveUtils.parseGoogleDriveTime(driveFile.modifiedTime);

                    if (driveLastModified <= localLastModified) {
                        LOG.info("Local actualizado. No se descarga: " + mFile.getName());
                        count--;
                        return 0;
                    }

                    String localMd5 = DriveUtils.getLocalFileMd5(mFile);
                    if (driveFile.md5Checksum != null
                            && localMd5 != null
                            && driveFile.md5Checksum.equalsIgnoreCase(localMd5)) {
                        LOG.info("MD5 igual. No se descarga: " + mFile.getName());
                        count--;
                        return 0;
                    }

                    LOG.info("Drive más reciente. Descargando → " + mFile.getAbsolutePath());
                }
            }
        }

        // Escribe en mFile = DataSave.download.bin (path que vino en input "path")
        return DriveUtils.downloadFileFromDrive(accessToken, mFileId, mFile, mType);
    }
}
