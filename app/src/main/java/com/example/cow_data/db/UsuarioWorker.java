package com.example.cow_data.db;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;

import com.example.cow_data.Basic;
import com.example.cow_data.DBListCreator;
import com.example.cow_data.StartVar;
import com.example.cow_data.ex.GoogleDriveManager;
import com.example.cow_data.ex.PreferenceHelper;
import com.google.gson.Gson;

import java.time.LocalDate;
import java.time.LocalTime;

public class UsuarioWorker extends Worker {
    public UsuarioWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String usuarioJson = getInputData().getString("usuarioJson");
            int mSend = getInputData().getInt("send",0);

            Gson gson = new Gson();
            Usuario mUser = gson.fromJson(usuarioJson, Usuario.class);

            if(mSend == 1){
                DBListCreator.createList(); //Actualiza la lista para exportar csv
                String currDate = "";
                String currTime = "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    currDate = LocalDate.now().toString();
                    currTime = LocalTime.now().toString();
                }
                StartVar.configDatabase.daoConf().updateDateTime(StartVar.mConfID, currDate, currTime);
                StartVar.getConfigDB();

                GoogleDriveManager manager = new GoogleDriveManager(PreferenceHelper.getInstance());
                manager.uploadDataBase();
                //Basic.msg("Aqui hay!! :) : "+gson.fromJson(queueItem.usuarioJson, Usuario.class).nombre);
                StartVar.usuarioQueue.clear();
            }

            else if(mSend == 2){
                DaoUser mDao = StartVar.appDatabase.daoUser();
                if (mUser.usuario.equals("@null")) {
                    mDao.removerUser(mUser.nombre);
                    mDao.removerUser(mUser.uid);
                } else {
                    mDao.updateUser(mUser.usuario, mUser.nombre, mUser.color,mUser.litros,
                            mUser.edad, mUser.pre, mUser.imagen,mUser.sel1,mUser.sel2,mUser.sel3);

                    mDao.updateMore(mUser.usuario, mUser.more1, mUser.more2, mUser.more3, mUser.more4);
                }

                String currDate = "";
                String currTime = "";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    currDate = LocalDate.now().toString();
                    currTime = LocalTime.now().toString();
                }
                StartVar.configDatabase.daoConf().updateDateTime(StartVar.mConfID, currDate, currTime);
                StartVar.getConfigDB();

                DBListCreator.createList(); //Actualiza la lista para exportar csv

                GoogleDriveManager manager = new GoogleDriveManager(PreferenceHelper.getInstance());
                manager.uploadDataBase();
                //Basic.msg("Aqui hay!! :) : "+gson.fromJson(queueItem.usuarioJson, Usuario.class).nombre);
                StartVar.usuarioQueue.clear();

                Intent mIntent = new Intent(StartVar.mContex, StartVar.mActivity.getClass());
                StartVar.mActivity.startActivity(mIntent);
                StartVar.mActivity.finish();
            }
            return Result.success();
        } catch (Exception e) {
            Basic.msg("Aqui no hay :(  "+ StartVar.sendDate);

            //LOG.error("Error procesando usuario: {}", e.getMessage(), e);
            return Result.failure();
        }
    }
}
