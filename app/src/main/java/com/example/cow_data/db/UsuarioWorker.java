package com.example.cow_data.db;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;

import com.example.cow_data.Basic;
import com.google.gson.Gson;

public class UsuarioWorker extends Worker {
    public UsuarioWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String usuarioJson = getInputData().getString("usuarioJson");
            Gson gson = new Gson();
            Usuario usuario = gson.fromJson(usuarioJson, Usuario.class);


            // Aquí implementas la lógica de procesamiento del usuario
            // Por ejemplo, enviar a un servidor, guardar en otra tabla, etc.
//            LOG.debug("Procesando usuario: nombre={}, edad={}, color={}, fecha={}",
//                    usuario.getNombre(), usuario.getEdad(), usuario.getColor(), usuario.getFecha());

            // Simulación de procesamiento (puedes reemplazar con tu lógica real)
            // Por ejemplo, enviar a un servidor usando una API
            // HttpClient.post("https://api.example.com/usuarios", usuario);

            // Devolver resultado exitoso
            return Result.success();
        } catch (Exception e) {
            //LOG.error("Error procesando usuario: {}", e.getMessage(), e);
            return Result.failure();
        }
    }
}
