package com.example.cow_data.drive;



import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.cow_data.Basic;
import com.example.cow_data.CalendUtls;
import com.example.cow_data.DBListCreator;
import com.example.cow_data.StartVar;
import com.example.cow_data.activitys.MainActivity;
import com.example.cow_data.db.Configdb;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.ex.PreferenceHelper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class SetWorkResult {
    private static final Log log = LogFactory.getLog(SetWorkResult.class);
    private LifecycleOwner lifecycle;
    private ExecutorService executorService;
    private GoogleDriveManager manager;
    private Observer<WorkInfo> workObserver; // Referencia al Observer

    public SetWorkResult(LifecycleOwner lifecycle, ExecutorService executorService, GoogleDriveManager manager) {
        this.lifecycle = lifecycle;
        this.executorService = executorService;
        this.manager = manager;
    }

    //Debug
//    public void observeWorkResult() {
//        android.util.Log.d("QueueManager", "Iniciando observador para WORK_TAG_CONFDB: " + StartVar.WORK_TAG_CONFDB);
//        WorkManager.getInstance(StartVar.mContex)
//                .getWorkInfosForUniqueWorkLiveData(StartVar.WORK_TAG_CONFDB)
//                .observe(lifecycle, workInfos -> {
//                    android.util.Log.d("WorkerStatus", "Recibidos " + workInfos.size() + " WorkInfos");
//                    for (WorkInfo workInfo : workInfos) {
//                        android.util.Log.d("WorkerStatus", "Estado: " + workInfo.getState() + ", ID: " + workInfo.getId());
//                        if (workInfo.getState().isFinished()) {
//                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
//                                String result = workInfo.getOutputData().getString("result");
//                                android.util.Log.d("WorkerResult", "Éxito: " + result);
//                            } else if (workInfo.getState() == WorkInfo.State.FAILED) {
//                                android.util.Log.d("WorkerResult", "Fallo en Worker");
//                            } else if (workInfo.getState() == WorkInfo.State.CANCELLED) {
//                                android.util.Log.d("WorkerResult", "Worker cancelado");
//                            }
//                        } else {
//                            android.util.Log.d("WorkerStatus", "Worker en curso: " + workInfo.getState());
//                        }
//                    }
//                });
//    }
//
    // Observar los resultados del Worker
    public void observeWorkResult() {
        WorkManager.getInstance(StartVar.mContex)
                .getWorkInfosForUniqueWorkLiveData(StartVar.WORK_TAG_DOWNLOAD)
                .observe(lifecycle, workInfos -> {
                    for (WorkInfo workInfo : workInfos) {
                        if (workInfo.getState().isFinished()) {
                            StartVar.setmMainStart(true);

                            Data outputData = workInfo.getOutputData();
                            String message = outputData.getString("result_message");
                            boolean preloader = outputData.getBoolean("preloader", false);
                            boolean newObj = outputData.getBoolean("newobj", false);
                            boolean isFileOk = outputData.getBoolean("file", false);
                            boolean isCheck = outputData.getBoolean("check", false);
                            boolean isImg = outputData.getBoolean("img", false);


                            //Basic.msg("!!!!---0 !: "+ isCheck);


                            String[] filesDownloaded = outputData.getStringArray("files_downloaded");

                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String displayMessage = message != null ? message : "Descarga completada";
                                if (filesDownloaded != null && filesDownloaded.length > 0) {
                                    displayMessage += ": " + String.join(", ", filesDownloaded);
                                }

                                if(isImg){
                                    return;
                                }

                                File mFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/DataSave.csv");
                                if(mFile.exists()){

                                    Uri uri = Uri.fromFile(mFile);

                                    // call this to persist permission across decice reboots
                                    StringBuilder stringBuilder = new StringBuilder();
                                    try {
                                        InputStream inputStream = StartVar.mContex.getContentResolver().openInputStream(uri);
                                        BufferedReader reader = new BufferedReader( new InputStreamReader(Objects.requireNonNull(inputStream)));

                                        String line;

                                        String hexID = "";
                                        String date = "";
                                        String time = "";

                                        while ((line = reader.readLine()) != null) {
                                            line = line.replaceAll("\"", "");
                                            String[] spl = line.split(",");

                                            if (spl[0].equals("confID0")){
                                                //spl[0]; //Obj id
                                                //spl[1]; //Version
                                                hexID = spl[2]; //Hexa ID
                                                date = spl[3]; //Date
                                                time = spl[4]; //Time
                                                //spl[5]; //Save1
                                                //spl[6]; //Save2
                                                //spl[7]; //Save3
                                            }
                                            stringBuilder.append(line);
                                            break;
                                        }
                                       Configdb mConf = StartVar.configDatabase.daoConf().getUsers(StartVar.mConfID);

                                        List<Usuario> mUserList = StartVar.appDatabase.daoUser().getUsers();
                                        if(!mConf.hexid.equals(hexID)){
                                            if(mUserList.isEmpty()){
                                                String mMsg = "Los datos locales están vacios";
                                                DBListCreator.cvsToDB(StartVar.mActivity, uri, 1, mMsg);
                                                return;
                                            }
                                            else {
                                                Basic.msg("Error: Los IDs de las DB no coinciden:");

                                                //Si es desde el preloder se reinicia la actividad
                                                resetPreloader(preloader);
                                                return;
                                            }
                                        }
                                        else if(mUserList.isEmpty()){
                                            String mMsg = "Los datos locales están vacios";
                                            DBListCreator.cvsToDB(StartVar.mActivity, uri, 1, mMsg);
                                            return;
                                        }
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            // Validar datos de entrada
                                            if (mConf.date == null || date.isEmpty() || mConf.time == null || time.isEmpty()) {
                                                Basic.msg("Error: Datos de fecha/hora incompletos");
                                                return;
                                            }
//                                            if (true) {
//                                                LocalDateTime test = LocalDateTime.now();
//                                                Basic.msg(test.toString()+" "+mConf.date + "T" + mConf.time+" "+date + "T" + time, true);
//                                                return;
//                                            }

                                            // Combinar fecha y hora en LocalDateTime
                                            LocalDateTime dateTimeA = CalendUtls.DTformat(mConf.date + "T" + mConf.time);
                                            LocalDateTime dateTimeB = CalendUtls.DTformat(date + "T" + time);

                                            // Comparar fechas y horas
                                            int result = dateTimeA.compareTo(dateTimeB);
                                            if (result > 0) {
                                                //uploadDataBase();
                                                if (newObj) {
                                                    //Basic.msg("Enviando Actualizacion...");
                                                    manager.uploadDataBase();

                                                }
                                                else{
                                                    //Basic.msg("Los datos locales están más actualizados (" + dateTimeA + " > " + dateTimeB + ")");

                                                    if(isCheck) {
                                                        StartVar.usuarioQueue.startUsuarioQueue(1);
                                                    }
                                                }
                                            }
                                            else if (result < 0) {

                                                String mMsg = "Los datos en línea están más actualizados (" + dateTimeA + " < " + dateTimeB + ")";

                                                if (newObj){
                                                    mMsg = "Error los cambios no se sincronizaron";
                                                }
                                                if(isCheck) {
                                                    DBListCreator.cvsToDbNotFinish(StartVar.mActivity, uri, 1, "");
                                                    StartVar.usuarioQueue.startUsuarioQueue(2);
                                                }
                                                else {
                                                    DBListCreator.cvsToDB(StartVar.mActivity, uri, 1, "");

                                                }
                                                return;
                                            }
                                            else {
                                                if (newObj){
                                                    //Basic.msg("Enviando Actualizacion...");
                                                    String currDate = LocalDate.now().toString();
                                                    String currTime = LocalTime.now().toString();
                                                    StartVar.configDatabase.daoConf().updateDateTime(StartVar.mConfID, currDate, currTime);
                                                    StartVar.getConfigDB();
                                                    manager.uploadDataBase();
                                                }
                                                else {
                                                    if(!isCheck) {
                                                        Basic.msg("La base de datos está actualizada (" + dateTimeA + ")");
                                                    }
                                                }
                                                if(isCheck) {
                                                    StartVar.usuarioQueue.startUsuarioQueue(1);
                                                }                                            }

                                            //Si es desde el preloder se reinicia la actividad
                                            resetPreloader(preloader);
                                        }
                                    }
                                    catch (FileNotFoundException e) {
                                        throw new RuntimeException(e);
                                    }
                                    catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                else {
                                    Basic.msg("CVS no Existe 1 !: "+displayMessage);
                                }
                            }
                            else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                String displayMessage = message != null ? message : "Error en la descarga";
                                Basic.msg("CVS no Existe 2 !: "+displayMessage);

                                if (!isFileOk) {
                                    if(preloader){
                                        resetPreloader(true);
                                        StartVar.makeUpdate = true;
                                    }
                                    else {
                                        Basic.msg("Subiendo Datos...");
                                        manager.uploadDataBase();
                                    }
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Starts a OneTimeWorkRequest with the given worker class and data map and tag. The constraints are set to
     * UNMETERED network type if the user has set the app to only send on wifi. Otherwise it is set to
     * CONNECTED. The initial delay is set to 1 second to avoid the work being enqueued immediately.
     * The backoff criteria is set to exponential with a 30 second initial delay. The tag is used to
     * uniquely identify the work request, and it replaces any existing work with the same tag.
     * @param workerClass
     * @param dataMap
     * @return
     */
    public static void startWorkManagerRequest(Class workerClass, HashMap<String, Object> dataMap, String tag) {
        androidx.work.Data data = new Data.Builder().putAll(dataMap).build();

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        if(PreferenceHelper.getInstance().shouldAutoSendOnWifiOnly()){
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        }
        NetworkRequest networkRequest = builder.build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkRequest(networkRequest, PreferenceHelper.getInstance().shouldAutoSendOnWifiOnly() ? NetworkType.UNMETERED: NetworkType.CONNECTED)
                .setRequiredNetworkType(PreferenceHelper.getInstance().shouldAutoSendOnWifiOnly() ? NetworkType.UNMETERED: NetworkType.CONNECTED)
                .build();

        //En caso de error de conexion se forza para cerrar el preloader
        if(!isNetworkAvailable(StartVar.mContex) && !StartVar.mainStart){
            StartVar.setmMainStart(true);
            resetPreloader(true);
        }

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest
                .Builder(workerClass)
                .setConstraints(constraints)
                .setInitialDelay(1, java.util.concurrent.TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                .setInputData(data)
                .build();

        WorkManager.getInstance(StartVar.mContex)
                .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, workRequest);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private static void resetPreloader(boolean preloader){
        if(preloader){
            if(StartVar.mActivity != null){
                Intent mIntent = new Intent(StartVar.mContex,  MainActivity.class);
                StartVar.mActivity.startActivity(mIntent);
                StartVar.mActivity.finish();
            }
        }
    }
}
