package com.example.cow_data.db;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.cow_data.Basic;
import com.example.cow_data.StartVar;
import com.example.cow_data.ex.GoogleDriveManager;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.ex.SetWorkResult;
import com.google.gson.Gson;

import net.openid.appauth.AuthState;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    }

    // Cargar la cola desde Room
    public void startUsuarioQueue(int send) {
        loadQueueFromDatabase(send);
    }

    // Encolar un usuario individual
    public void enqueue(Usuario usuario) {

        StartVar.sendDate = 0;

        // Agregar a la cola en memoria
        queue.add(usuario);

        // Persistir en Room
        String usuarioJson = gson.toJson(usuario);
        long order = queue.size(); // Usar el tamaño actual como orden
        QueueItem queueItem = new QueueItem(usuarioJson, order);
        queueItemDao.insert(queueItem);

        //Sincroniza para asegurar que no hay cambios en los datos en drive -----------------------------------------
        synchronizeCheck();
        //--------------------------------------------------------------------------------------------------------------

//        // Iniciar el procesamiento si la cola estaba vacía
//        if (queue.size() == 1) {
//            processNext();
//        }
    }

    private void synchronizeCheck(){
        GoogleDriveManager manager = new GoogleDriveManager(PreferenceHelper.getInstance());
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        StartVar.mWorkResult = new SetWorkResult( lifecycle, executorService, manager);

        AuthState authState = new AuthState();
        authState = GoogleDriveManager.getAuthState();
        if(authState.isAuthorized()){
            manager.dataSynchronizeCheck();
        }
    }

    // Procesar el siguiente elemento de la cola
    private void processNext(int sendOpt) {

        if (queue.isEmpty()) {
            return;
        }

        // Obtener el primer usuario
        Usuario usuario = queue.peek();

        if (usuario == null) {
            queue.poll();
            return;
        }

        // Encolar un trabajo en WorkManager
        Data inputData = new Data.Builder()
                .putString("usuarioJson", gson.toJson(usuario))
                .putInt("send", sendOpt)
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
                                queueItemDao.delete(queueItem);
                            }
                            // Procesar el siguiente
                            processNext(sendOpt);

                        } else {
                            Basic.msg("Aqui fallloooo: "+StartVar.sendDate);
                            //Log.e("UsuarioQueue", "Error procesando usuario: " + workInfo.getState());
                        }
                    }
                });

        WorkManager.getInstance(context).enqueue(workRequest);
    }

    // Cargar la cola desde la base de datos
    public void loadQueueFromDatabase(int send) {
        List<QueueItem> queueItems = queueItemDao.getAllQueueItems();
        for (QueueItem item : queueItems) {
            Usuario usuario = gson.fromJson(item.usuarioJson, Usuario.class);
            queue.add(usuario);
        }
        // Iniciar el procesamiento si hay elementos
        if (!queue.isEmpty()) {
            processNext(send);
        }
        else{
            Basic.msg("No hay usuario");
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

    public void poll() {
        queue.poll();
    }
}