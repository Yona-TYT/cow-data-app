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
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.cow_data.AppContextProvider;
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
import java.util.concurrent.TimeUnit;

public class SetWorkResult {
    private static final Log log = LogFactory.getLog(SetWorkResult.class);
    private LifecycleOwner lifecycle;
    private ExecutorService executorService;
    private DriveManager manager;
    private Observer<WorkInfo> workObserver; // Referencia al Observer

    public SetWorkResult(LifecycleOwner lifecycle, ExecutorService executorService, DriveManager manager) {
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
                            boolean isId = outputData.getBoolean("isId", false);

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

                                    // Se ha seleccionado un respaldo y remplazara todos los datos locales
                                    if (isId){
                                        String mMsg = "Restaurando respaldo...";
                                        DBListCreator.cvsToDB(StartVar.mActivity, uri, 1, mMsg);
                                        return;
                                    }

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
    public static void startWorkManagerRequest(Class<? extends ListenableWorker> workerClass, HashMap<String, Object> dataMap, String tag) {
        // 1. Usar el contexto proporcionado o el global como respaldo
        Context appContext = AppContextProvider.getContext();

        if (appContext == null) {
            android.util.Log.e("DriveSync", "❌ Sin contexto disponible.");
            return;
        }

        // 2. Datos
        Data data = new Data.Builder().putAll(dataMap).build();

        // 3. Constraints simplificadas (Evita DeadObject en MIUI)
        boolean soloWifi = PreferenceHelper.getInstance().shouldAutoSendOnWifiOnly();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(soloWifi ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                .build();

        // 4. Request
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(workerClass)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tag)
                .build();

        // 5. Verificar conexión (usando la versión segura)
        if (!isNetworkAvailable(appContext)) {

            //Basic.msg("Aqui hay! "+isNetworkAvailable(appContext),true);
            android.util.Log.w("DriveSync", "Sin conexión a internet. Se encolará cuando vuelva la conexión.");

            // Solo forzamos el preloader si es el flujo inicial
            if (!StartVar.mainStart) {
                StartVar.setmMainStart(true);
                resetPreloader(true);
            }
            // Puedes decidir si quieres encolar igual o no. WorkManager lo manejará con las constraints.
        }

        // 6. Encolar con política conservadora
        try {
            WorkManager.getInstance(appContext)
                    .enqueueUniqueWork(tag, ExistingWorkPolicy.KEEP, workRequest);
            android.util.Log.i("DriveSync", "✅ WorkManager encolado: " + tag);
        } catch (Exception e) {
            android.util.Log.e("DriveSync", "❌ Error Binder/WorkManager", e);
        }
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
