package com.example.cow_data.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cow_data.db.dao.QueueItemDao;
import com.example.cow_data.StartVar;
import java.util.List;

public class GenericWorker extends Worker {
    private static final String TAG = "GenericWorker";
    private Context context;
    public GenericWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            int mSend = getInputData().getInt("send", 0);
            if (mSend != 1 && mSend != 2 && mSend != 3) {
                return Result.success();
            }

            if (StartVar.appDBall == null) {
                StartVar.setAllListDB();
            }

            QueueItemDao dao = StartVar.appDBall.daoQueue();
            List<QueueItem> items = dao.getAllQueueItems();
            if (items == null || items.isEmpty()) {
                return Result.success();
            }

            Log.d(TAG, "Procesando lote desde Room: " + items.size());
            QueueProcessor processor = new QueueProcessor();

            for (QueueItem item : items) {
                if (item.json == null || item.tipo == null) {
                    dao.delete(item);
                    continue;
                }
                boolean ok = processor.applyQueueObject(item.json, item.tipo);
                if (!ok) {
                    Log.e(TAG, "Fallo aplicando tipo=" + item.tipo);
                    // Opción segura: no retry eterno del lote entero
                    return Result.failure(new Data.Builder()
                            .putString("error", "apply failed: " + item.tipo)
                            .build());
                }
                dao.delete(item); // solo si ok
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error en GenericWorker", e);
            return Result.failure(new Data.Builder()
                    .putString("error", e.getMessage())
                    .build());
        }
    }
}