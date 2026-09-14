package com.example.cow_data.drive;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.work.Data;

import com.example.cow_data.GlobalData;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.utls.Basic;
import com.example.cow_data.utls.CalendUtls;
import com.example.cow_data.DBListCreator;
import com.example.cow_data.StartVar;
import com.example.cow_data.db.Conf;
import com.example.cow_data.utls.Msg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

public class SetDb {
    private static final String TAG = "SetDb";

    private static final String KEY_RESULT_MESSAGE = "result_message";
    private static final String KEY_FILES_DOWNLOADED = "files_downloaded";
    private static final String KEY_IS_PRELOADER = "preloader";
    private static final String KEY_IS_NEW_OBJ = "newobj";
    private static final String KEY_IS_FILE_OK = "file";
    private static final String KEY_IS_CHECK = "check";
    private static final String KEY_IS_IMG = "img";
    private static final String KEY_IS_ID = "isId";

    public void set(Context context, Data outputData, Uri uri, DriveManager manager) throws IOException {

        boolean preloader = outputData.getBoolean(KEY_IS_PRELOADER, false);
        boolean newObj    = outputData.getBoolean(KEY_IS_NEW_OBJ, false);
        boolean isCheck   = outputData.getBoolean(KEY_IS_CHECK, false);
        boolean isId      = outputData.getBoolean(KEY_IS_ID, false);

        boolean hasQueue = GlobalData.getInstance(context)
                .getGenericQueue().hasPendingQueueItems();

        Log.d("SetDb", "hasQueue=" + hasQueue );

        DBListCreator mListCreator = new DBListCreator(context);

        //Basic.msg("Bug "+preloader,true);

        // Respaldo completo → reemplaza todo
        if (isId) {
            mListCreator.cvsToDB(StartVar.mActivity, uri, 1, "Restaurando respaldo...");
            SetWorkResult.resetPreloader(preloader);
            return;
        }

        // ----- Leer timestamps del CSV -----
        String hexID = "";
        long remoteDate = 0L;
        long remoteTime = 0L;
        boolean confLineFound = false;


        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\"", "");
                String[] spl = line.split(",");

                if (spl.length >= 7 && "confID0".equals(spl[0])) {
                    hexID = spl[2];
                    try {
                        remoteDate = Long.parseLong(spl[6].trim());
                        remoteTime = Long.parseLong(spl[7].trim());
                        confLineFound = true;
                    } catch (NumberFormatException e) {
                        Msg.m("Error: El CSV no contiene timestamps válidos");
                        Log.e(TAG, "CSV date/time inválidos: " + spl[5] + " / " + spl[6], e);
                        SetWorkResult.resetPreloader(preloader);
                        return;
                    }
                    break;
                }
            }
        }

        if (!confLineFound) {
            Msg.m("Error: No se encontró configuración en el CSV");
            Log.e(TAG, "Línea confID0 no encontrada");
            SetWorkResult.resetPreloader(preloader);
            return;
        }

        Conf mConf = StartVar.appDBall.daoCfg().getUsers(StartVar.mConfID);
        List<Usuario> users = StartVar.appDBall.daoUser().getUsers();

        if (mConf == null) {
            Msg.m("Error: No hay configuración local");
            SetWorkResult.resetPreloader(preloader);
            return;
        }

        // ----- IDs no coinciden -----
        if (!mConf.hexid.equals(hexID)) {
            if (users == null || users.isEmpty()) {
                mListCreator.cvsToDB(StartVar.mActivity, uri, 1, "Los datos locales están vacios");
            } else {
                Msg.m("Error: Los IDs de las DB no coinciden: " + hexID + " , " + mConf.hexid);
            }
            SetWorkResult.resetPreloader(preloader);
            return;
        }

        // ----- Base local vacía -----
        if (users == null || users.isEmpty()) {
            mListCreator.cvsToDB(StartVar.mActivity, uri, 1, "Los datos locales están vacios");
            SetWorkResult.resetPreloader(preloader);
            return;
        }

        // ----- Timestamps locales -----
        if (mConf.date == null || mConf.time == null) {
            Msg.m("Error: Datos de fecha/hora locales incompletos");
            SetWorkResult.resetPreloader(preloader);
            return;
        }

        //Msg.m(outputData.getString(KEY_RESULT_MESSAGE)+" " +hasQueue+" "+mConf.date+" "+mConf.time, true);

        // Comparación: primero date, si empatan usa time
        int comparison;
        if (mConf.date.equals(remoteDate)) {
            comparison = Long.compare(mConf.time, remoteTime);
        } else {
            comparison = Long.compare(mConf.date, remoteDate);
        }

        Log.d(TAG, "Comparación → local(" + mConf.date + "," + mConf.time
                + ") vs remote(" + remoteDate + "," + remoteTime
                + ") = " + comparison
                + " | newObj=" + newObj + " isCheck=" + isCheck);

        if (comparison > 0) {
            // =====================================================
            // A) LOCAL más nuevo
            // =====================================================

            if (isCheck || hasQueue) {
                GlobalData.getInstance(context).getGenericQueue().startUsuarioQueue(1);
            } else if (newObj) {
                manager.uploadDataBase();
            }
        } else if (comparison < 0) {
            // =====================================================
            // B) REMOTO más nuevo
            // =====================================================

            if (newObj) {
                Msg.m("Error: Existen cambios más recientes en la red. Sincronizando...");
            }

            if (isCheck || hasQueue) {
                mListCreator.cvsToDbNotFinish(StartVar.mActivity, uri, 1, "");
                GlobalData.getInstance(context).getGenericQueue().startUsuarioQueue(2);
            } else {
                mListCreator.cvsToDB(StartVar.mActivity, uri, 1, "");
            }

        } else {
            // =====================================================
            // C) IGUALES
            // =====================================================
            if (isCheck || hasQueue) {
                GlobalData.getInstance(context).getGenericQueue().startUsuarioQueue(1);
            } else if (newObj) {
                long now = System.currentTimeMillis();
                String strDbg = TAG + ": " + CalendUtls.getShortDate(now) + " " + CalendUtls.getTime(now);
                StartVar.appDBall.daoCfg().updateDateTime(StartVar.mConfID, now, now, strDbg);
                StartVar.getConfigDB();
                manager.uploadDataBase();
            }
        }

        // Solo el flujo real de preloader debe poder cerrar
        if (preloader && !StartVar.mainStart) {
            SetWorkResult.resetPreloader(true);
        } else if (preloader) {
            Log.d(TAG, "Ignorando resetPreloader: main ya iniciado");
        }
    }
}