package com.example.cow_data.db;

import android.util.Log;

import com.example.cow_data.StartVar;
import com.google.gson.Gson;

public class QueueProcessor {
    private static final String TAG = "QueueProcessor";
    private final Gson gson;

    public QueueProcessor() {
        this.gson = new Gson();
    }

    public boolean applyQueueObject(String json, String tipo) {
        try {
            Class<?> clazz = Class.forName(tipo);
            Object obj = gson.fromJson(json, clazz);

//            if (obj instanceof Article) {
//                return processArt((Article) obj);
//            } else if (obj instanceof Cliente) {
//                return processCliente((Cliente) obj);
//            } else if (obj instanceof Sale) {
//                return processSale((Sale) obj);
//            } else if (obj instanceof Fecha) {
//                return processFecha((Fecha) obj);
//            } else if (obj instanceof Deuda) {
//                return processDeuda((Deuda) obj);
//            } else if (obj instanceof Conf) {
//                return processConf((Conf) obj);
//            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error aplicando objeto de cola", e);
            return false;
        }
    }

//    private boolean processDeuda(Deuda mUser) {
//        DaoDeb mDao = StartVar.appDBall.daoDeb();
//        if (mUser == null) return false;
//
//        if ("@null".equals(mUser.deuda)) {
//            mDao.removerUser(mUser.uid); // CORRECCIÓN: Borrado físico real por UID
//        } else {
//            mDao.update(mUser);
//        }
//        return true;
//    }
//
//    private boolean processCliente(Cliente mUser) {
//        DaoClt mDao = StartVar.appDBall.daoClt();
//        if (mUser == null) return false;
//
//        if ("@null".equals(mUser.cliente)) {
//            mDao.removerUser(mUser.uid); // CORRECCIÓN: Borrado físico real por UID
//        } else {
//            mDao.update(mUser);
//        }
//        return true;
//    }
//
//    private boolean processArt(Article mUser) {
//        DaoArt mDao = StartVar.appDBall.daoAtr();
//        if (mUser == null) return false;
//
//        if ("@null".equals(mUser.article)) {
//            mDao.removerUser(mUser.uid); // CORRECCIÓN: Borrado físico real por UID
//        } else {
//            mDao.update(mUser);
//        }
//        return true;
//    }
//
//    private boolean processSale(Sale mUser) {
//        DaoSal mDao = StartVar.appDBall.daoSal();
//        if (mUser == null) return false;
//
//        if ("@null".equals(mUser.sale)) {
//            mDao.removerUser(mUser.uid); // CORRECCIÓN: Borrado físico real por UID
//        } else {
//            mDao.update(mUser);
//        }
//        return true;
//    }
//
//    private boolean processFecha(Fecha mUser) {
//        DaoDat mDao = StartVar.appDBall.daoDat();
//        if (mUser == null) return false;
//
//        if ("@null".equals(mUser.fecha)) {
//            mDao.removerUser(mUser.uid); // CORRECCIÓN: Borrado físico real por UID
//        } else {
//            mDao.update(mUser);
//        }
//        return true;
//    }
//
//    private boolean processConf(Conf mUser) {
//        DaoCfg mDao = StartVar.appDBall.daoCfg();
//        if (mUser == null) return false;
//
//        if ("@null".equals(mUser.config)) {
//            mDao.removerUser(mUser.uid); // CORRECCIÓN: Borrado físico real por UID
//        } else {
//            mDao.update(mUser);
//        }
//        return true;
//    }
}
