package com.example.cow_data.db;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.cow_data.Basic;
import com.example.cow_data.StartVar;
import com.example.cow_data.activitys.MainActivity;
import com.google.gson.Gson;
import java.util.LinkedList;
import java.util.List;

public class UsuarioQueue {
    private LifecycleOwner lifecycle;
    private final LinkedList<Usuario> queue;
    private final QueueItemDao queueItemDao;
    private final Context context;
    private final Gson gson;

    public UsuarioQueue(LifecycleOwner lifecycle, Context context) {
        this.lifecycle = lifecycle;
        this.context = context.getApplicationContext();
        this.queue = new LinkedList<>();
        this.queueItemDao = StartVar.appDatabase.queueItemDao();
        this.gson = new Gson();
        // Cargar la cola desde Room al iniciar
        loadQueueFromDatabase();
    }

    // Encolar un usuario individual
    public void enqueue(Usuario usuario) {
        // Agregar a la cola en memoria
        queue.add(usuario);

        // Persistir en Room
        String usuarioJson = gson.toJson(usuario);
        long order = queue.size(); // Usar el tamaño actual como orden
        QueueItem queueItem = new QueueItem(usuarioJson, order);
        queueItemDao.insert(queueItem);

//        // Iniciar el procesamiento si la cola estaba vacía
//        if (queue.size() == 1) {
//            processNext();
//        }
    }

    // Procesar el siguiente elemento de la cola
    private void processNext() {
        if (queue.isEmpty()) {
            return;
        }

        // Obtener el primer usuario
        Usuario usuario = queue.peek();
        if (usuario == null) {
            return;
        }

        // Encolar un trabajo en WorkManager
        Data inputData = new Data.Builder()
                .putString("usuarioJson", gson.toJson(usuario))
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(UsuarioWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(workRequest.getId())
                .observe(lifecycle, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            // Eliminar el elemento procesado
                            queue.poll();
                            QueueItem queueItem = queueItemDao.getFirstQueueItem();




                            if (queueItem != null) {

                                Basic.msg("Aqui hay!! :) : "+gson.fromJson(queueItem.usuarioJson, Usuario.class).nombre);
                                queueItemDao.delete(queueItem);
                            }
                            // Procesar el siguiente
                            processNext();
                        } else {
                            //Log.e("UsuarioQueue", "Error procesando usuario: " + workInfo.getState());
                        }
                    }
                });

        WorkManager.getInstance(context).enqueue(workRequest);
    }

    // Cargar la cola desde la base de datos
    private void loadQueueFromDatabase() {
        List<QueueItem> queueItems = queueItemDao.getAllQueueItems();
        for (QueueItem item : queueItems) {
            Usuario usuario = gson.fromJson(item.usuarioJson, Usuario.class);
            queue.add(usuario);
        }
        // Iniciar el procesamiento si hay elementos
        if (!queue.isEmpty()) {
            processNext();
        }
    }

    // Obtener el tamaño de la cola
    public int size() {
        return queue.size();
    }

    // Limpiar la cola (opcional)
    public void clear() {
        queue.clear();
        queueItemDao.deleteAll();
    }
}