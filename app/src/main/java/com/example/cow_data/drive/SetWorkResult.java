package com.example.cow_data.drive;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
import com.example.cow_data.DBListCreator;
import com.example.cow_data.GlobalData;
import com.example.cow_data.activitys.MainActivity;
import com.example.cow_data.db.GenericQueue;
import com.example.cow_data.db.QueueItem;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.db.dao.QueueItemDao;
import com.example.cow_data.db.QueueProcessor;
import com.example.cow_data.StartVar;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.utls.Msg;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SetWorkResult {
    private static final String TAG = "SetWorkResult";
    private LifecycleOwner lifecycle;
    private ExecutorService executorService;
    private DriveManager manager;
    private final Set<UUID> processedWorkIds = new HashSet<>();
    private final Set<UUID> processedUploadIds = new HashSet<>();


    private static final String KEY_RESULT_MESSAGE = "result_message";
    private static final String KEY_FILES_DOWNLOADED = "files_downloaded";
    private static final String KEY_IS_PRELOADER = "preloader";
    private static final String KEY_IS_NEW_OBJ = "newobj";
    private static final String KEY_IS_FILE_OK = "file";
    private static final String KEY_IS_CHECK = "check";
    private static final String KEY_IS_IMG = "img";
    private static final String KEY_IS_ID = "isId";

    private Observer<WorkInfo> workObserver; // Referencia al Observer

    public SetWorkResult(LifecycleOwner lifecycle, ExecutorService executorService, DriveManager manager) {
        this.lifecycle = lifecycle;
        this.executorService = executorService;
        this.manager = manager;
    }

    // Observar los resultados del Worker
    public void observeWorkResult() {
        Context context = AppContextProvider.getContext();
        if (context == null) return;

        observeDownloadTag(context, StartVar.WORK_TAG_DOWNLOAD);
        observeDownloadTag(context, StartVar.WORK_TAG_DOWNLOAD_IMG);

        WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(StartVar.WORK_TAG_UPLOAD)
                .observe(lifecycle, this::handleUploadWorkInfos);
    }

    private void observeDownloadTag(Context context, String uniqueName) {
        WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(uniqueName)
                .observe(lifecycle, workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) return;
                    handleDownloadWorkInfos(workInfos);
                });
    }
    private void handleUploadWorkInfos(List<WorkInfo> workInfos) {
        if (workInfos == null || workInfos.isEmpty()) return;
        Context context = AppContextProvider.getContext();
        if (context == null) return;

        for (WorkInfo workInfo : workInfos) {
            if (!workInfo.getState().isFinished()) continue;
            if (!processedUploadIds.add(workInfo.getId())) continue;

            Data out = workInfo.getOutputData();
            int uploaded = out.getInt("uploaded", 0);
            int skipped = out.getInt("skipped", 0);
            int missing = out.getInt("missing", 0);
            boolean mainUploaded = out.getBoolean("main_uploaded", false);
            String message = out.getString(KEY_RESULT_MESSAGE);

            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                Log.d(TAG, "Upload OK uploaded=" + uploaded
                        + " main=" + mainUploaded
                        + " skipped=" + skipped
                        + " missing=" + missing
                        + " msg=" + message);

                // Confirmar recepción real en Drive → limpiar cola
                if (uploaded > 0 || mainUploaded) {

                    // 1. Abrimos un hilo de fondo rápido para aplicar los borrados físicos en Room antes de vaciar la cola
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            Log.d(TAG, "Iniciando QueueProcessor desde SetWorkResult...");

                            // Obtenemos los elementos que están actualmente en tránsito en la cola de Room
                            QueueItemDao queueItemDao = StartVar.appDBall.daoQueue();
                            List<QueueItem> itemsEnCola = queueItemDao.getAllQueueItems();

                            if (itemsEnCola != null && !itemsEnCola.isEmpty()) {
                                QueueProcessor processor = new QueueProcessor();

                                // Procesamos cada JSON para aplicar mDao.removerUser(mUser.uid) de forma real por ID
                                for (QueueItem item : itemsEnCola) {
                                    processor.applyQueueObject(item.json, item.tipo);
                                }
                                Log.d(TAG, "Purga física de objetos '@null' completada con éxito.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error ejecutando la purga física de base de datos", e);
                        } finally {
                            // 2. Una vez purgado Room, regresamos al hilo principal para vaciar la cola de tránsito
                            // y desbloquear la pantalla del usuario de forma instantánea
                            new Handler(Looper.getMainLooper()).post(() -> {
                                // Tu lógica original de limpieza
                                GlobalData.getInstance(context).getGenericQueue().clear();
                                Log.d(TAG, "Cola limpiada tras confirmación Drive e impacto físico.");

                                // NOTIFICACIÓN MAESTRA: Le avisamos a la Activity de Ventas que ya puede cerrarse solo
                                GenericQueue queue = GlobalData.getInstance(AppContextProvider.getContext()).getGenericQueue();
                                queue.notifySyncComplete();
                            });
                        }
                    });

                } else {
                    Log.w(TAG, "Upload SUCCESS pero nada subido → cola intacta");

                    // RESPALDO: Si no subió nada porque las marcas ya eran iguales, liberamos la interfaz igualmente
                    GenericQueue queue = GlobalData.getInstance(AppContextProvider.getContext()).getGenericQueue();
                    queue.notifySyncComplete();
                }
            } else {
                Log.e(TAG, "Upload falló: " + workInfo.getState() + " " + message);
                // Si el Worker falla de forma definitiva, liberamos la pantalla para no congelar al usuario
                GenericQueue queue = GlobalData.getInstance(AppContextProvider.getContext()).getGenericQueue();
                queue.notifySyncComplete();
            }
        }
    }

    private void handleDownloadWorkInfos(List<WorkInfo> workInfos) {
        if (workInfos == null || workInfos.isEmpty()) return;
        Context context = AppContextProvider.getContext();
        if (context == null) return;

        for (WorkInfo workInfo : workInfos) {
            if (!workInfo.getState().isFinished()) continue;

            // Evitar reprocesar el mismo WorkInfo (p.ej. el del preloader)
            if (!processedWorkIds.add(workInfo.getId())) {
                continue;
            }
            //StartVar.setmMainStart(true);

            Data outputData = workInfo.getOutputData();
            boolean preloader = outputData.getBoolean(KEY_IS_PRELOADER, false);
            boolean isFileOk = outputData.getBoolean(KEY_IS_FILE_OK, false);
            boolean isImg = outputData.getBoolean(KEY_IS_IMG, false);
            String message = outputData.getString(KEY_RESULT_MESSAGE);
            String[] filesDownloaded = outputData.getStringArray(KEY_FILES_DOWNLOADED);

            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                if (isImg) {
                    // EventBus / mensaje de imágenes OK, si lo necesitas
                    android.util.Log.d(TAG, "Download IMG OK: " + message);
                    continue;
                }

                // 1) Preferir path que devolvió el Worker
                File mFile = null;
                if (filesDownloaded != null && filesDownloaded.length > 0
                        && filesDownloaded[0] != null && !filesDownloaded[0].isEmpty()) {
                    mFile = new File(filesDownloaded[0]);
                }

                // 2) Si no viene, usar siempre LOCAL_DOWNLOAD (DataSave.download.bin)
                if (mFile == null || !mFile.exists()) {
                    mFile = DriveManager.getLocalDownloadFile();
                }

                if (mFile.exists()) {
                    Uri uri = Uri.fromFile(mFile);
                    try {
                        new SetDb().set(context, outputData, uri, manager);
                    } catch (IOException e) {
                        android.util.Log.e(TAG, "Error en SetDb", e);
                    }
                } else {
                    Msg.m("CSV no existe: " + message);
                    SetWorkResult.resetPreloader(preloader);
                }

            } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                if (isImg) {
                    android.util.Log.e(TAG, "Download IMG failed: " + message);
                    continue;
                }
                if (!isFileOk) {
                    // No hay archivo en Drive (o no se pudo obtener)
                    List<Usuario> users = StartVar.appDBall.daoUser().getUsers();
                    boolean hasLocal = users != null && !users.isEmpty();

                    if (hasLocal) {
                        Msg.m("Subiendo Datos...");
                        try {
                            DBListCreator.createDbLists();
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error createDbLists", e);
                        }
                        manager.uploadDataBase();

                        if (preloader) {
                            StartVar.makeUpdate = true;
                            resetPreloader(true);
                        }
                    } else {
                        // Sin datos locales ni en Drive
                        android.util.Log.w(TAG, "Sin archivo en Drive y sin datos locales");
                        resetPreloader(preloader);
                    }
                } else {
                    // Fallo de red/token/etc. pero el flag de archivo no indica "no encontrado"
                    android.util.Log.e(TAG, "Download failed: " + message);
                    resetPreloader(preloader);
                }
            }
        }
    }

    public static void startWorkManagerRequest(Class<? extends ListenableWorker> workerClass, HashMap<String, Object> dataMap, String tag) {
        // 1. Usar el contexto proporcionado o el global como respaldo
        Context appContext = AppContextProvider.getContext();

        if (appContext == null) {
            android.util.Log.e(TAG, "❌ Sin contexto disponible.");
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
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(tag)
                .build();

        // 5. Verificar conexión (usando la versión segura)
        if (!isNetworkAvailable(appContext)) {
            android.util.Log.w(TAG, "Sin conexión a internet. Se encolará cuando vuelva la conexión.");
            // Solo forzamos el preloader si es el flujo inicial
            if (!StartVar.mainStart) {
                StartVar.setmMainStart(true);
                resetPreloader(true);
            }
            // Puedes decidir si quieres encolar igual o no. WorkManager lo manejará con las constraints.
        }

        // 6. Encolar con política conservadora
        try {
            // Misma política, tags distintas = no se pisan entre sí
            ExistingWorkPolicy policy = ExistingWorkPolicy.REPLACE;

            WorkManager.getInstance(appContext)
                    .enqueueUniqueWork(tag, policy, workRequest);

            android.util.Log.i(TAG, "✅ WorkManager encolado: " + tag + " policy=" + policy);
        } catch (Exception e) {
            android.util.Log.e(TAG, "❌ Error Binder/WorkManager", e);
        }
    }
    public static boolean isNetworkAvailable(Context context) {

        if (context == null) {
            android.util.Log.e(TAG, "Context pasado es null");
            return false;
        }

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

    public static void resetPreloader(boolean preloader) {
        android.util.Log.d(TAG, "resetPreloader called | preloader=" + preloader
                + " mainStart=" + StartVar.mainStart
                + " activity=" + (StartVar.mActivity != null
                ? StartVar.mActivity.getClass().getSimpleName()
                : "null"));

        if (!preloader) {
            return;
        }

        if (StartVar.mActivity == null) {
            android.util.Log.e(TAG, "mActivity es null");
            StartVar.setmMainStart(true);
            return;
        }

        String current = StartVar.mActivity.getClass().getSimpleName();
        if (!"Preloader".equals(current)) {
            // Ya salimos del preloader
            StartVar.setmMainStart(true);
            return;
        }

        // Seguimos en Preloader → cerrar de verdad
        StartVar.setmMainStart(true);
        Intent i = new Intent(AppContextProvider.getContext(), MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        StartVar.mActivity.startActivity(i);
        StartVar.mActivity.finish();
    }
}
