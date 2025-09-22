package com.example.cow_data;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.room.Room;

import com.example.cow_data.db.AppDatabase;
import com.example.cow_data.db.ConfigDatabase;
import com.example.cow_data.db.Configdb;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.db.UsuarioQueue;
import com.example.cow_data.drive.SetWorkResult;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Base64;

public class StartVar {
    //Mapa de arrays
    public static HashMap<String, ArrayList<String>> arrayMap = new HashMap<>();
    public static final String mId1 = "id1";
    public static final String mId2 = "id2";

    //Nombre de data Base
    public static String nameDBcow = "Registro2";
    public static String nameDBconf = "Config-COW";

    //Worker tags
    public static final String WORK_TAG_DOWNLOAD = "DownloadWorkConfigDb"; // Define WORK_TAG para configdb
    //public static final String WORK_TAG_UPLOAD = "UploadWorkCowData"; // Define WORK_TAG para cowdatadb

    public static List<String[]> csvList = new ArrayList<>();

    // Var redundants
    public static List<Usuario> listuser;
    public static boolean mPermiss;
    public static int mDayA = 276;
    public static int mDayB = 283;
    public static String mDateFormEN = "yyyy-MM-dd";
    public static String mDateFormES = "dd-MM-yyyy";

    // DB Cow
    public static AppDatabase appDatabase;
    public static ArrayList<Object> textList = new ArrayList<>();
    public static ArrayList<Object> dirList = new ArrayList<>();
    public static ArrayList<Object> typeList = new ArrayList<>();
    public static ArrayList<String> morlist = new ArrayList<>();

    // DB Config
    public static ConfigDatabase configDatabase;
    public static Configdb mConfigDB;
    public static String mConfID = "confID0";

    // DB Config Temp
    public static ConfigDatabase configDatabaseTemp = null;


    public static int currSel2 = 4;

    public static Context mContex ;
    public static Activity mActivity;
    public static UsuarioQueue usuarioQueue;
    public static int sendDate = 0;

    public static SetWorkResult mWorkResult = null;

    public static LifecycleOwner mLifecycle = null;

    //Preloder
    public static boolean mainStart = false;
    //Hacer upload cuando los datos esten disponibles.
    public static boolean makeUpdate = false;


    public StartVar(Context mContex){
        StartVar.mContex = AppContextProvider.getAppContext();
    }


    public void setUserListDB(){
        //Instancia de la base de datos
        StartVar.appDatabase = Room.databaseBuilder( mContex, AppDatabase.class, nameDBcow).allowMainThreadQueries().build();
        StartVar.listuser =  appDatabase.daoUser().getUsers();

        //Instancia de la base de datos para Config
        StartVar.configDatabase = Room.databaseBuilder( mContex, ConfigDatabase.class, nameDBconf).allowMainThreadQueries().build();
        mConfigDB = configDatabase.daoConf().getUsers(mConfID);

        if(mConfigDB == null){
            String date = "";
            String time= "";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                date = LocalDate.now().toString();
                time = LocalTime.now().toString();
            }

            // Generar UUID
            UUID uuid = UUID.randomUUID();
            // Convertir UUID a bytes (16 bytes)
            ByteBuffer byteBuffer = ByteBuffer.allocate(16);
            byteBuffer.putLong(uuid.getMostSignificantBits());
            byteBuffer.putLong(uuid.getLeastSignificantBits());

            // Codificar en Base64 (sin padding para ahorrar espacio)
            String textID = "";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                textID = Base64.getUrlEncoder().withoutPadding().encodeToString(byteBuffer.array());
            }

            //configDatabase.daoConf().insertUser();
            Configdb obj = new Configdb(mConfID, "3", textID, date, time, "0", "0", "0");
            configDatabase.daoConf().insetUser(obj);
        }
    }

    public static void getUserListDB(){
        //Instancia de la base de datos
        StartVar.listuser.clear();
        StartVar.listuser =  StartVar.appDatabase.daoUser().getUsers();
    }

    public static void getConfigDB(){
        //Instancia de la base de datos
        StartVar.mConfigDB =  StartVar.configDatabase.daoConf().getUsers(mConfID);
    }

    public void setmPermiss(boolean permiss){
        mPermiss = permiss;
    }

    public static void setmMainStart(boolean mStart){mainStart = mStart;}


    public void setArrayList(ArrayList<Object> listA, ArrayList<Object> listB, ArrayList<Object> listC){
        StartVar.textList.clear();
        StartVar.dirList.clear();
        StartVar.typeList.clear();

        StartVar.textList = listA;
        StartVar.dirList = listB;
        StartVar.typeList = listC;
    }

    public void setCurrSel2(int value){
        StartVar.currSel2 = value;
    }

    public void setmActivity(Activity activity){
        StartVar.mActivity = activity;
    }


    public void setMorlist(ArrayList<String> list){
        StartVar.morlist.clear();
        StartVar.morlist = list;
    }

    public static void setCsvList(List<String[]> mList){
        StartVar.csvList.clear();
        StartVar.csvList = mList;
    }


    public static void setArrayMap(HashMap<String, ArrayList<String>> mMap){
        StartVar.arrayMap.clear();
        StartVar.arrayMap = mMap;
    }

    public static void setTempDB(ConfigDatabase mTempDB){
        StartVar.configDatabaseTemp = mTempDB;
    }


}
