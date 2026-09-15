package com.example.cow_data;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cow_data.db.Conf;
import com.example.cow_data.db.dao.DaoUser;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.db.dao.DaoCfg;
import com.example.cow_data.utls.FilesManager;
import com.example.cow_data.utls.Msg;

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
    private GlobalData glData ;

    private final Context context;

    // Constructor para forzar el uso del contexto correcto
    public DBListCreator(Context context) {
        this.context = context.getApplicationContext();
        this.glData = GlobalData.getInstance(this.context);
    }

    public static HashMap<String, ArrayList<Object>> createDbLists(){

        DaoCfg daoConf = StartVar.appDBall.daoCfg();

        //Define y Inizializa el Array Map
        HashMap<String, ArrayList<Object>> arrayMap;
        arrayMap = new HashMap<>();
        arrayMap.put("name",  new ArrayList<>());
        arrayMap.put("lts",  new ArrayList<>());
        arrayMap.put("datePre",  new ArrayList<>());
        arrayMap.put("dateBrith",  new ArrayList<>());
        arrayMap.put("img",  new ArrayList<>());
        arrayMap.put("format", new ArrayList<>());
        arrayMap.put("select", new ArrayList<>());
        arrayMap.put("swPre", new ArrayList<>());
        arrayMap.put("retire", new ArrayList<>());

        List<String[]> mList = new ArrayList<>();

        //Instancia de la base de datos
        List<Usuario> listuser =  StartVar.appDBall.daoUser().getUsers();

        //=================================== Config DB Lista =====================================================
        mList.add(new String[]{"<0>"});// Etiqueta para config
        //Instancia de la base de datos
        Conf mConf =  daoConf.getUsers(StartVar.mConfID);

        mList.add(new String[]{mConf.config, mConf.version, mConf.hexid, mConf.datetasa,
                String.valueOf(mConf.dolar), String.valueOf(mConf.margen), String.valueOf(mConf.date),
                String.valueOf(mConf.time), mConf.curr.toString(), mConf.moneda.toString(),
                mConf.mes.toString(), mConf.show.toString(), mConf.datos, mConf.dbg
        });

        ArrayList<Object> nameL = arrayMap.get("name");
        ArrayList<Object> ltsL = arrayMap.get("lts");
        ArrayList<Object> dPreL = arrayMap.get("datePre");
        ArrayList<Object> dBrithL = arrayMap.get("dateBrith");
        ArrayList<Object> imgL = arrayMap.get("img");
        ArrayList<Object> formatL = arrayMap.get("format");
        ArrayList<Object> selL = arrayMap.get("select");
        ArrayList<Object> swPreL = arrayMap.get("swPre");
        ArrayList<Object> retL = arrayMap.get("retire");

        if(nameL != null && ltsL != null && dPreL != null && dBrithL!= null && swPreL != null && formatL != null && selL != null && retL != null && imgL != null) {
            for (Usuario myUser : listuser) {
                String tximg = myUser.imagen;
                String txname = myUser.nombre;
                String txlitros = myUser.litros;
                String txedad = myUser.edad;
                String txpre = myUser.pre;
                Integer txsel1 = myUser.sel1;
                Integer txsel2 = myUser.sel2;
                Integer txsel3 = myUser.sel3;
                Integer txsel4 = myUser.sel4;

                //------------------------------------------------------
                // Se crea la lista para esportar a csv  ---------------
                String[] txList = new String[15];

                txList[0] = myUser.usuario;
                txList[1] = txname;
                txList[2] = myUser.color;
                txList[3] = txlitros;
                txList[4] = txedad;
                txList[5] = txpre;
                txList[6] = tximg;
                txList[7] = txsel1.toString();
                txList[8] = txsel2.toString();
                txList[9] = txsel3.toString();
                txList[10] = txsel4.toString();
                txList[11] = (myUser.more1).isEmpty()?"@null":myUser.more1;
                txList[12] = (myUser.more2).isEmpty()?"@null":myUser.more2;
                txList[13] = (myUser.more3).isEmpty()?"@null":myUser.more3;
                txList[14] = (myUser.more4).isEmpty()?"@null":myUser.more4;

                mList.add(txList);

                //--------------------------------------------------------
                // Se obtine la direccion de la image,  el nombre, la listSelec etc.
                nameL.add(txname);
                ltsL.add(txlitros);
                dPreL.add(txpre);
                dBrithL.add(txedad);
                formatL.add(txsel1);
                selL.add(txsel2);
                swPreL.add(txsel3);
                retL.add(txsel4);

                imgL.add(tximg);

                //------------------------------------------
            }

            mList.add(new String[]{"<end>"});  // marca de cierre

            StartVar.setCsvList(mList);
        }
        return arrayMap;

//        ArrayList<Object> mArray = arrayMap.get("name");
//        if (mArray != null) {
//            String mText = "";
//            for (Object s : mArray) {
//                mText = ((String) s+",");
//            }
//            Msg.m(mText);
//        }
//        else {
//            Msg.m("Aqui no hay!");
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
        StartVar.setAllListDB();

        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = StartVar.mContex.getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader( new InputStreamReader(Objects.requireNonNull(inputStream)));

            String line;
            String version = "0";

            DaoUser mDao = StartVar.appDBall.daoUser();
            DaoCfg daoConf = StartVar.appDBall.daoCfg();
            for (Usuario mUser : mDao.getUsers()){
                mDao.removerUser(mUser.usuario);
            }

            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\"", "");
                String[] spl = line.split(",");
                //Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: "+ line);
                int f = spl.length;

                if (spl[0].equals("<0>") || spl[0].equals("<end>")) {
                    continue;
                }

                //Si no se agrega la configuracion aqui
                if (spl[0].equals("confID0")){
                    version = spl[1];
                    if(Objects.equals(version, "4")) {
                        daoConf.updateUser("confID0", StartVar.mDateVersion, spl[2], "", 0d,
                                0d, 0L, 0L,
                                0, 0, 0,
                                0, "", "Old Version");

                    }
                    else {
                        daoConf.updateUser("confID0", StartVar.mDateVersion, spl[2], spl[3], Double.parseDouble(spl[4]),
                                Double.parseDouble(spl[5]), Long.parseLong(spl[6]), Long.parseLong(spl[7]),
                                Integer.parseInt(spl[8]), Integer.parseInt(spl[9]), Integer.parseInt(spl[10]),
                                Integer.parseInt(spl[11]), spl[12], spl[13]);
                    }
                    continue;
                }

                if (Objects.equals(version, "1")) {
                    Usuario obj = new Usuario(
                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], Integer.parseInt(spl[7]), Integer.parseInt(spl[8]), Integer.parseInt(spl[9]), Integer.parseInt(spl[10]),
                            (f > 11 ? spl[11] : "@null"), (f > 12 ? spl[12] : "@null"), (f > 13 ? spl[13] : "@null"), (f > 14 ? spl[14] : "@null")
                    );
                    mDao.insetUser(obj);
                }

                else if (Objects.equals(version, "4")) {
                    Usuario obj = new Usuario(
                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], Integer.parseInt(spl[7]), Integer.parseInt(spl[8]), Integer.parseInt(spl[9]), Integer.parseInt(spl[10]),
                            (f > 11 ? spl[11] : "@null"), (f > 12 ? spl[12] : "@null"), (f > 13 ? spl[13] : "@null"), (f > 14 ? spl[14] : "@null")
                    );
                    mDao.insetUser(obj);
                }

                stringBuilder.append(line);
            }
            StartVar.setAllListDB();

        }
        catch (FileNotFoundException e) {
            Msg.m("ErrorA: "+ e.getMessage());
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            Msg.m("ErrorB: "+ e.getMessage());
            throw new RuntimeException(e);
        }

        if(finish) {
            Intent mIntent = new Intent(StartVar.mContex, myThis.getClass());
            myThis.startActivity(mIntent);
            Msg.m(mMsg);
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
