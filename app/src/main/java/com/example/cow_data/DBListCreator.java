package com.example.cow_data;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cow_data.activitys.MainActivity;
import com.example.cow_data.db.AppDatabase;
import com.example.cow_data.db.Configdb;
import com.example.cow_data.db.DaoConf;
import com.example.cow_data.db.DaoUser;
import com.example.cow_data.db.Usuario;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DBListCreator extends AppCompatActivity {

    // Classs para la gestion de archivos
    private FilesManager fmang = new FilesManager();
    public DBListCreator(){}

    public static HashMap<String, ArrayList<Object>> createList(){

        //Define y Inizializa el Array Map
        HashMap<String, ArrayList<Object>> arrayMap;
        arrayMap = new HashMap<>();
        arrayMap.put("name",  new ArrayList<>());
        arrayMap.put("lts",  new ArrayList<>());
        arrayMap.put("datePre",  new ArrayList<>());
        arrayMap.put("dateBrith",  new ArrayList<>());
        arrayMap.put("swPre", new ArrayList<>());
        arrayMap.put("type", new ArrayList<>());
        arrayMap.put("select", new ArrayList<>());
        arrayMap.put("img",  new ArrayList<>());

        List<String[]> mList = new ArrayList<>();

        //Instancia de la base de datos
        List<Usuario> listuser =  StartVar.appDatabase.daoUser().getUsers();

        //Se agrega una fila con las configuraciones de la db y versoion
        StartVar.getConfigDB();
        Configdb mConf = StartVar.mConfigDB;
        mList.add(new String[]{mConf.config, mConf.version, mConf.hexid, mConf.date, mConf.time, mConf.save1, mConf.save2, mConf.save3});


        ArrayList<Object> nameL = arrayMap.get("name");
        ArrayList<Object> ltsL = arrayMap.get("lts");
        ArrayList<Object> dPreL = arrayMap.get("datePre");
        ArrayList<Object> dBrithL = arrayMap.get("dateBrith");
        ArrayList<Object> swPreL = arrayMap.get("swPre");
        ArrayList<Object> typeL = arrayMap.get("type");
        ArrayList<Object> selL = arrayMap.get("select");
        ArrayList<Object> imgL = arrayMap.get("img");

        if(nameL != null && ltsL != null && dPreL != null && dBrithL!= null && swPreL != null && typeL != null && selL != null && imgL != null) {
            for (Usuario myUser : listuser) {
                String tximg = myUser.imagen;
                String txname = myUser.nombre;
                String txsel2 = myUser.sel2;
                String txsel3 = myUser.sel3;
                String txpre = myUser.pre;
                String txlitros = myUser.litros;
                String txedad = myUser.edad;

                //------------------------------------------------------
                // Se crea la lista para esportar a csv  ---------------
                String[] txList = new String[14];

                txList[0] = myUser.usuario;
                txList[1] = txname;
                txList[2] = myUser.color;
                txList[3] = myUser.litros;
                txList[4] = txedad;
                txList[5] = txpre;
                txList[6] = tximg;
                txList[7] = myUser.sel1;
                txList[8] = txsel2;
                txList[9] = txsel3;
                txList[10] = (myUser.more1).isEmpty()?"@null":myUser.more1;
                txList[11] = (myUser.more2).isEmpty()?"@null":myUser.more2;
                txList[12] = (myUser.more3).isEmpty()?"@null":myUser.more3;
                txList[13] = (myUser.more4).isEmpty()?"@null":myUser.more4;

                mList.add(txList);

                //--------------------------------------------------------
                // Se obtine la direccion de la image,  el nombre, la listSelec etc.
                nameL.add(txname);
                ltsL.add(txlitros);
                dPreL.add(txpre);
                dBrithL.add(txedad);
                swPreL.add(txsel3);
                typeL.add(txsel2);
                selL.add(Integer.parseInt(txsel2));

                if (FilesManager.isBlockedPath(StartVar.mContex, tximg)) {
                    imgL.add(tximg);
                } else {
                    imgL.add("null");
                }
                //------------------------------------------
            }
            StartVar.setCsvList(mList);
        }
        return arrayMap;

//        ArrayList<Object> mArray = arrayMap.get("name");
//        if (mArray != null) {
//            String mText = "";
//            for (Object s : mArray) {
//                mText = ((String) s+",");
//            }
//            Basic.msg(mText);
//        }
//        else {
//            Basic.msg("Aqui no hay!");
//        }

    }
    public static void cvsToDB(Activity myThis, Uri uri, int importType, String mMsg) {
        cvsToDBInternal(myThis, uri, importType, mMsg, true);
    }

    public static void cvsToDbNotFinish(Activity myThis, Uri uri, int importType, String mMsg) {
        cvsToDBInternal(myThis, uri, importType, mMsg,false);
    }

    public static void cvsToDBInternal(Activity myThis, Uri uri, int importType, String mMsg, boolean finish){
        StartVar mStartVar = new StartVar(StartVar.mContex);
        mStartVar.setUserListDB();

        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = StartVar.mContex.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader( new InputStreamReader(Objects.requireNonNull(inputStream)));

            String line;
            String version = "0";

            DaoUser mDao = StartVar.appDatabase.daoUser();
            DaoConf mDaoConf = StartVar.configDatabase.daoConf();
            for (Usuario mUser : mDao.getUsers()){
                mDao.removerUser(mUser.usuario);
            }

            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\"", "");
                String[] spl = line.split(",");
                //Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: "+ line);
                int f = spl.length;
                //Si la version es vieja
                if(f<2){
                    version = spl[0];
                    continue;
                }
                //Si no se agrega la configuracion aqui
                else if (spl[0].equals("confID0")){
                    mDaoConf.updateUser("confID0", spl[1], spl[2], spl[3], spl[4], spl[5], spl[6] ,spl[7]);

                    version = spl[1];
                    continue;
                }
                if(Objects.equals(version, "0")) {
                    Usuario obj = new Usuario(
                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], "0", (f > 7 ? spl[7] : ""),
                            "0", (f > 8 ? spl[8] : ""), (f > 9 ? spl[9] : ""), (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : "")
                    );
                    mDao.insetUser(obj);
                }
                else if(Objects.equals(version, "1")) {
                    Usuario obj = new Usuario(
                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], ""/*spl[5]*/, spl[5], spl[6], spl[7], "0",
                            (f > 8 ? spl[8] : ""), (f > 9 ? spl[9] : ""), (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : "")                                    );
                    mDao.insetUser(obj);

                }

                else if(Objects.equals(version, "2")) {
                    Usuario obj = new Usuario(
                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], spl[7], spl[8], spl[9],
                            (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : ""), (f > 12 ? spl[12] : ""), (f > 13 ? spl[13] : "")
                    );
                    mDao.insetUser(obj);
                }
                else if(Objects.equals(version, "3")) {
                    Usuario obj = new Usuario(
                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], spl[7], spl[8], spl[9],
                            (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : ""), (f > 12 ? spl[12] : ""), (f > 13 ? spl[13] : "")
                    );
                    mDao.insetUser(obj);
                }

                stringBuilder.append(line);
            }
            mStartVar.setUserListDB();

        }
        catch (FileNotFoundException e) {
            Basic.msg("ErrorA: "+ e.getMessage());
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            Basic.msg("ErrorB: "+ e.getMessage());
            throw new RuntimeException(e);
        }

        if(finish) {
            Intent mIntent = new Intent(StartVar.mContex, myThis.getClass());
            myThis.startActivity(mIntent);
            Basic.msg(mMsg);
            myThis.finish();
        }
    }

    private static String getUserId(DaoUser mDao){
        //Configura el nuevo index-------------------------------------------------------------------
        int mSiz = mDao.getUsers().size();
        String mIdx = "userID0";
        if(mSiz > 0) {
            mIdx = "userID" + mSiz;
        }
        for(int i = 0; i < mSiz; i++){
            Usuario mUser = mDao.getUsers("userID"+i);
            if(mUser == null){
                mIdx =  "userID"+i;
                break;
            }
        }
        return mIdx;
        //-------------------------------------------------------------------------------------------
    }
}
