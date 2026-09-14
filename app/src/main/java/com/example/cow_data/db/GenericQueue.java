package com.example.cow_data.db;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;


import com.example.cow_data.DBListCreator;
import com.example.cow_data.db.dao.QueueItemDao;
import com.example.cow_data.drive.DriveManager;
import com.example.cow_data.drive.SetWorkResult;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.StartVar;
import com.example.cow_data.utls.CalendUtls;
import com.example.cow_data.utls.Msg;
import com.google.gson.Gson;

import net.openid.appauth.AuthState;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class GenericQueue {
    private final LinkedList<Object> queue;
    private QueueItemDao queueItemDao;
    private final Context context;
    private final Gson gson;
    private boolean isProcessing = false;

    private static final String TAG = "GenericQueue";


    public GenericQueue(Context context) {
        this.context = context;
        this.queue = new LinkedList<>();
        this.gson = new Gson();
    }

    public interface OnSyncCompleteListener {
        void onSyncComplete();
    }

    // 2. Crea una variable global para almacenar el listener temporal
    private OnSyncCompleteListener syncCompleteListener;

    public void setOnSyncCompleteListener(OnSyncCompleteListener listener) {
        this.syncCompleteListener = listener;
    }

    public void notifySyncComplete() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (syncCompleteListener != null) {
                syncCompleteListener.onSyncComplete();
                syncCompleteListener = null; // Limpieza anti-fugas de memoria
                Log.d(TAG, "Callback de éxito notificado externamente.");
            }
        });
    }

    // 3. Agrega este método para asegurar que el DAO se obtenga solo cuando se necesite
    private QueueItemDao getQueueItemDao() {
        if (queueItemDao == null) {
            // Si la app despertó en segundo plano y StartVar no se ha inicializado, lo forzamos
            if (StartVar.appDBall == null) {
                Log.w(TAG, "La BD en StartVar es null. Inicializando contenedores...");
                //StartVar.setAllListDB();
            }
            queueItemDao = StartVar.appDBall.daoQueue();
        }
        return queueItemDao;
    }

    // Encolar con mSend = 2 por defecto
    public void enqueue(Object objeto) {
        enqueue(objeto, 2);
    }

    // Encolar con mSend personalizado
    public void enqueue(Object objeto, int mSend) {
        if (objeto == null) return;
        if (objeto instanceof List) {
            Log.e(TAG, "enqueue(List) no permitido; usa enqueueList");
            List<?> raw = (List<?>) objeto;
            enqueueList(new ArrayList<>(raw), mSend);
            return;
        }

        String json = gson.toJson(objeto);
        String tipoClase = objeto.getClass().getName();
        long order = System.currentTimeMillis();
        QueueItem item = new QueueItem(json, tipoClase, order);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                getQueueItemDao().insert(item);
                Log.d(TAG, "INSERT ok tipo=" + tipoClase
                        + " roomSize=" + getQueueItemDao().getAllQueueItems().size());

                AuthState authState = DriveManager.getAuthState();
                if (authState != null && authState.isAuthorized()) {
                    synchronizeCheck(); // SetDb → startUsuarioQueue
                } else {
                    // Sin red: procesar en local si quieres
                    new Handler(Looper.getMainLooper()).post(() -> {
                        queue.add(objeto);
                        startUsuarioQueue(mSend);
                    });
                    return;
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    queue.add(objeto);
                    Log.d(TAG, "Objeto añadido a la cola en memoria");
                    // NO startUsuarioQueue aquí
                });
            } catch (Exception e) {
                Log.e(TAG, "Error al procesar item en cola", e);
            }
        });
    }

    public void enqueueList(List<Object> objList, int mSend) {
        if (objList == null || objList.isEmpty()) {
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {

                long baseOrder = System.currentTimeMillis();

                // Convertir y guardar secuencialmente en la base de datos
                for (int i = 0; i < objList.size(); i++) {
                    Object objeto = objList.get(i);
                    String json = gson.toJson(objeto);
                    String tipoClase = objeto.getClass().getName();

                    // Sumamos el índice para mantener el orden exacto de llegada
                    QueueItem item = new QueueItem(json, tipoClase, baseOrder + i);
                    getQueueItemDao().insert(item);
                }

                // Sincronización con Drive
                AuthState authState = DriveManager.getAuthState();
                if (authState != null && authState.isAuthorized()) {
                    synchronizeCheck();
                }

                // Pasar al hilo principal para actualizar memoria e iniciar Workers
                new Handler(Looper.getMainLooper()).post(() -> {
                    queue.addAll(objList);
                    Log.d(TAG, objList.size() + " objetos añadidos a la memoria.");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error al procesar lista en cola", e);
            }
        });
    }
    public void loadQueueFromDatabase(int send) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<QueueItem> items = getQueueItemDao().getAllQueueItems();

            queue.clear();

            for (QueueItem item : items) {
                try {
                    Class<?> clazz = Class.forName(item.tipo);
                    Object objeto = gson.fromJson(item.json, clazz);
                    queue.add(objeto);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (!queue.isEmpty()) {
                    processNext(send);
                } //else {
                ///Basic.msg("No hay elementos en cola");
                //}
            });
        });
    }

    public void startUsuarioQueue(int send) {
        loadQueueFromDatabase(send);
    }

    private void synchronizeCheck() {
        // LiveData.observe DEBE ejecutarse en el hilo principal
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                DriveManager manager = new DriveManager(PreferenceHelper.getInstance());
                ExecutorService executorService = Executors.newSingleThreadExecutor();

                StartVar.mWorkResult = new SetWorkResult(
                        ProcessLifecycleOwner.get(),
                        executorService,
                        manager
                );

                StartVar.mWorkResult.observeWorkResult();
                manager.dataSynchronizeCheck();
            } catch (Exception e) {
                Log.e(TAG, "Error en synchronizeCheck", e);
            }
        });
    }
    private void processNext(int sendOpt) {
        if (isProcessing) {
            Log.d(TAG, "Ya hay un lote en curso");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            List<QueueItem> items = getQueueItemDao().getAllQueueItems();
            if (items == null || items.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    isProcessing = false;
                    if (sendOpt == 1 || sendOpt == 2 || sendOpt == 3) {
                        performFinalUpload(sendOpt);
                    }
                });
                return;
            }

            isProcessing = true;

            Data inputData = new Data.Builder()
                    .putInt("send", sendOpt)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GenericWorker.class)
                    .setInputData(inputData)
                    .build();

            // Observar en main una sola vez (simplificado)
            new Handler(Looper.getMainLooper()).post(() -> {
                WorkManager.getInstance(context)
                        .getWorkInfoByIdLiveData(request.getId())
                        .observe(ProcessLifecycleOwner.get(), workInfo -> {
                            if (workInfo == null || !workInfo.getState().isFinished()) return;

                            isProcessing = false;

                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                // Worker ya borró lo procesado en Room
                                processNext(sendOpt); // verá si quedan más en Room
                            } else {
                                Log.e(TAG, "Lote falló: " + workInfo.getState());
                            }
                        });
                WorkManager.getInstance(context).enqueue(request);
            });
        });
    }

//    private void performFinalUpload(int sendOpt) {
//
//        Log.d(TAG, "performFinalUpload → upload (clear al confirmar Drive)");
//        Executors.newSingleThreadExecutor().execute(() -> {
//            try {
//                updateConfigTimestamp();
//                StartVar.getConfigDB();
//                // createDbLists lo puede hacer solo uploadDataBase()
//                new DriveManager(PreferenceHelper.getInstance()).uploadDataBase();
//                // SIN clear() aquí
//
//                if (sendOpt == 2) {
//                    new Handler(Looper.getMainLooper()).post(() -> {
//                        if (StartVar.mActivity != null) {
//                            Intent i = new Intent(context, StartVar.mActivity.getClass());
//                            StartVar.mActivity.startActivity(i);
//                            StartVar.mActivity.finish();
//                        }
//                    });
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error en subida final", e);
//            }
//        });
//
//    }

    private void performFinalUpload(int sendOpt) {
        Log.d(TAG, "performFinalUpload → upload (clear al confirmar Drive)");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                updateConfigTimestamp();
                StartVar.getConfigDB();

                // 1. Exporta el CSV fresco con los datos fusionados y encola la subida final
                new DriveManager(PreferenceHelper.getInstance()).uploadDataBase();

                if (sendOpt == 2) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (StartVar.mActivity != null) {
                            Intent i = new Intent(context, StartVar.mActivity.getClass());
                            StartVar.mActivity.startActivity(i);
                            StartVar.mActivity.finish();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en subida final", e);
            } finally {
                // 2. ÉXITO DE INTERFAZ: Como el lote local ya se vació y se mandó a fusionar/subir,
                // notificamos a la Activity para que cierre la pantalla de Ventas.
                // El Worker de Drive ("google_drive_upload") seguirá haciendo la subida física en background de forma segura.
                notifySyncComplete();
            }
        });
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
        getQueueItemDao().deleteAll();
    }

    public void poll() {
        queue.poll();
    }

    private void updateConfigTimestamp() {
        long currDate = 0L;
        long currTime = System.currentTimeMillis();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            currDate = java.time.Instant.now().toEpochMilli();
        } else {
            currDate = currTime; // fallback para versiones antiguas
        }

        String strDbg = "GenericWorker: " +
                CalendUtls.getShortDate(currDate) + " " +
                CalendUtls.getTime(currTime);

        StartVar.appDBall.daoCfg().updateDateTime(
                StartVar.mConfID,
                currDate,
                currTime,
                strDbg
        );

        StartVar.getConfigDB(); // refresca la configuración en memoria
    }

    public boolean hasPendingQueueItems() {
        try {
            List<QueueItem> items = getQueueItemDao().getAllQueueItems();
            return items != null && !items.isEmpty();
        } catch (Exception e) {
            return !queue.isEmpty();
        }
    }
}