package com.example.cow_data.drive;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cow_data.ex.DownloadEvents;
import com.example.cow_data.ex.Logs;
import com.example.cow_data.ex.PreferenceHelper;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
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
        Result mResult = null;
        String failureMessage = "";
        String downloadMessage = "";
        boolean isFileOk = true;

        count = 0;

        String filePath = getInputData().getString("path");
        String fileName = getInputData().getString("name");
        String fileType = getInputData().getString("type");
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

        if (!authState.isAuthorized()) {
            failureMessage = "Could not download to Google Drive. Not Authorized.";
            //EventBus.getDefault().post(new UploadEvents.GoogleDrive().failed("Could not upload to Google Drive. Not Authorized."));
        }

        final AtomicBoolean taskDone = new AtomicBoolean(false);
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

            if (DriveUtils.isNullOrEmpty(googleDriveAccessToken)) {
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

            String mFolderId = latestFolderId;

            if (DriveUtils.isNullOrEmpty(mFolderId)) {
                failureMessage = "Could not create folder";
                success = false;
            }
            else {
                if(isImg) {
                    String imgFolderName = PreferenceHelper.getInstance().getGoogleDriveImgPath();
                    String imgFolderId = DriveUtils.getFileIdFromFileName(googleDriveAccessToken, imgFolderName, mFolderId, "application/vnd.google-apps.folder");
                    if (!DriveUtils.isNullOrEmpty(imgFolderId)) {
                        LOG.debug("Folder " + imgFolderName + " found, folder ID is " + mFolderId);
                    } else {
                        LOG.debug("Folder " + imgFolderName + " not found, creating.");
                        imgFolderId = DriveUtils.createEmptyFile(googleDriveAccessToken, imgFolderName,
                                "application/vnd.google-apps.folder", mFolderId);
                    }

                    if (DriveUtils.isNullOrEmpty(imgFolderId)) {
                        failureMessage = "Could not create folder";
                        success = false;
                    }
                    else {

                        List<String[]> mList = DriveUtils.getDriveIdAndNameList(googleDriveAccessToken, imgFolderId);
                        count = mList.size();
                        for (String[] dataFile : mList){
                            String fId = dataFile[0];
                            String fName = dataFile[1];

                            if (DriveUtils.isNullOrEmpty(fId)) {
                                isFileOk = false;
                                failureMessage = "Error no se encontraron DATOS.";
                                return Result.failure(new Data.Builder().putString(KEY_RESULT_MESSAGE, failureMessage)
                                        .putBoolean(KEY_IS_PRELOADER, isPreloader)
                                        .putBoolean(KEY_IS_FILE_OK, isFileOk)
                                        .build());
                            }
                            // The above empty file creation needs to happen first - this shouldn't be an 'else' to the above if.
                            if (!DriveUtils.isNullOrEmpty(fId)) {
                                File currFile = new File(filePath+"/"+fName);

                                LOG.debug("Downloading file contents");
                                //File destinationFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/cueblo.db");
                                failureMessage = "" + downloadFileContents(googleDriveAccessToken, imgFolderId, fId, currFile, fileType, false);
                                //Basic.msg("Fail: "+failureMessage);
                            }
                        }
                    }
                }
                else {
                    // Now search for the file
                    String driveFileId = isId? fileId : DriveUtils.getFileIdFromFileName2(googleDriveAccessToken, fileName, mFolderId);

                    //Basic.msg(fileName + " : "+driveFileId);
                    if (DriveUtils.isNullOrEmpty(driveFileId)) {
                        isFileOk = false;
                        failureMessage = "Error no se encontraron DATOS.";
                        return Result.failure(new Data.Builder().putString(KEY_RESULT_MESSAGE, failureMessage)
                                .putBoolean(KEY_IS_PRELOADER, isPreloader)
                                .putBoolean(KEY_IS_FILE_OK, isFileOk)
                                .build());
                    }

                    // The above empty file creation needs to happen first - this shouldn't be an 'else' to the above if.
                    if (!DriveUtils.isNullOrEmpty(driveFileId)) {
                        LOG.debug("Uploading file contents");
                        //File destinationFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/cueblo.db");
                        failureMessage = "" + downloadFileContents(googleDriveAccessToken, mFolderId, driveFileId, fileToDownload, fileType, isId);
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
                EventBus.getDefault().post(new DownloadEvents.GoogleDrive().succeeded(" Archivos Descargados: ", count));
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
                        .putBoolean(KEY_IS_ID, isId)
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

    protected int getRetryLimit() {
        return 3;
    }

    // Método para descargar un archivo desde Google Drive
    public int downloadFileContents(String accessToken, String folderId, String mFileId, File mFile, String mType, boolean isId) throws Exception {


        if(!isId) {
            // 1. Obtener metadatos del archivo en Drive (incluye modifiedTime)
            DriveFileMeta driveFile = DriveUtils.getFileMetaFromDrive(
                    accessToken,
                    mFile.getName(),   // Usamos el nombre del archivo
                    folderId
            );
            if (driveFile == null) {
                LOG.warn("No se pudieron obtener metadatos del archivo en Drive. Se procederá a descargar.");
            } else {
                LOG.debug("Fecha en Drive: {}", driveFile.modifiedTime);

                //DriveUtils.copyToClipboard(mContext, mFile.getName()+driveFile.md5Checksum+" ?"+ driveFile.md5Checksum +" "+DriveUtils.getLocalFileMd5(mFile), mFile.getName());
                //copyToClipboard(mContext, localFile.getName()+" "+ driveFile.md5Checksum +" "+GoogleDriveFileHelper.getLocalFileMd5(localFile), localFile.getName());

                // 2. Comparar fechas si el archivo local existe
                if (mFile.exists() && driveFile.hasModifiedTime()) {
                    long localLastModified = mFile.lastModified();           // milisegundos
                    long driveLastModified = DriveUtils.parseGoogleDriveTime(driveFile.modifiedTime); // milisegundos

                    LOG.debug("Fecha local : {}", new java.util.Date(localLastModified));

                    if (driveLastModified <= localLastModified) {
                        LOG.info("✅ Archivo local está actualizado. No se descargará.");
                        count--;
                        return 0;   // No necesita descargar
                    } else {
                        if (driveFile.md5Checksum.equals(DriveUtils.getLocalFileMd5(mFile))) {
                            count--;
                            return 0;   // No necesita descargar
                        }

                        LOG.info("🔄 Archivo en Drive es más reciente. Procediendo a descargar...");
                    }
                }
            }
        }

        return DriveUtils.downloadFileFromDrive(accessToken, mFileId, mFile, mType);
    }
}