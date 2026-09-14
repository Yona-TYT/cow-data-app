package com.example.cow_data;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.room.Room;

import com.example.cow_data.db.AllDao;
import com.example.cow_data.db.AppDatabase;
import com.example.cow_data.db.Conf;
import com.example.cow_data.db.ConfigDatabase;
import com.example.cow_data.db.GenericQueue;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.drive.SetWorkResult;
import com.example.cow_data.utls.CalendUtls;

import java.nio.ByteBuffer;
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
    private static final String nameDB = "Date-COW";
    public static String nameDBconf = "Config-COW";

    //Worker tags
    public static final String WORK_TAG_DOWNLOAD = "google_drive_download";
    public static final String WORK_TAG_DOWNLOAD_IMG = "google_drive_download_img";
    public static final String WORK_TAG_UPLOAD = "google_drive_upload";
    public static final String WORK_TAG_UPLOAD_IMG = "google_drive_upload_img";

    public static List<String[]> csvList = new ArrayList<>();

    // Var redundants
    public static boolean mPermiss;
    public static int mDayA = 276;
    public static int mDayB = 283;
    public static String mDateFormEN = "yyyy-MM-dd";
    public static String mDateFormES = "dd-MM-yyyy";

    public static int accSelect = 0;      // Cuenta seleccionada
    public static int accCierre = 0;      // Cuenta seleccionada
    public static int mCurrency = 0;        //Moneda seleccionada
    public static int mCurrMes = 0;        //Mes seleccionado

    public static Double mDollar = 0d;       //Precio del dolar
    public static String mShortDate = "";

    // DB Cow
    public static ArrayList<Object> textList = new ArrayList<>();
    public static ArrayList<Object> dirList = new ArrayList<>();
    public static ArrayList<Object> typeList = new ArrayList<>();
    public static ArrayList<Object> retirList = new ArrayList<>();

    public static ArrayList<String> morlist = new ArrayList<>();

    // DB
    public static AllDao appDBall;

    // DB Config
    public static Conf mConfigDB;
    public static String mConfID = "confID0";
    public static String mDateVersion = "1";

    public static final String dirAppName = ".cowdata";
    public static final String csvAppName = "DataSave.csv";
    public static final String fileName = "DataSave";

    public static final String EXPORT_NAME = "DataSave.bin";           // lógico / Drive
    public static final String LOCAL_UPLOAD = "DataSave.upload.bin";   // solo subida
    public static final String LOCAL_DOWNLOAD = "DataSave.download.bin"; // solo bajada

    public static int currSel2 = 4;

    public static Context mContex ;
    public static Activity mActivity;
    public static GenericQueue genericQueue;
    public static int sendDate = 0;

    public static SetWorkResult mWorkResult = null;

    public static LifecycleOwner mLifecycle = null;

    //Preloder
    public static boolean mainStart = false;
    //Hacer upload cuando los datos esten disponibles.
    public static boolean makeUpdate = false;


    public StartVar(Context mContex){
        StartVar.mContex = AppContextProvider.getContext();
    }


    public static void setAllListDB(){
        //Instancia de la base de datos
        StartVar.appDBall = Room.databaseBuilder( AppContextProvider.getContext(), AllDao.class, StartVar.nameDB).allowMainThreadQueries().build();

//        StartVar.listacc = StartVar.appDBall.daoAtr().getUsers();
//        StartVar.listclt = StartVar.appDBall.daoClt().getUsers();
//        StartVar.listdeb = StartVar.appDBall.daoDeb().getUsers();
//        StartVar.listfec = StartVar.appDBall.daoDat().getUsers();
//        StartVar.listpay = StartVar.appDBall.daoSal().getUsers();

        //Instancia de la base de datos para Config
        StartVar.mConfigDB = StartVar.appDBall.daoCfg().getUsers(StartVar.mConfID);

        if(StartVar.mConfigDB == null){
            long currDate = 0;
            long currTime = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                currDate = java.time.Instant.now().toEpochMilli();
                currTime = System.currentTimeMillis();
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

            String strDbg = "Frits Config: "+ CalendUtls.getShortDate(currDate)+" "+CalendUtls.getTime(currTime);
            Conf obj = new Conf(StartVar.mConfID, mDateVersion, textID, "",0d, 0d,
                    currDate, currTime, 0, 0, 0, 0, "", strDbg);
            StartVar.appDBall.daoCfg().insertUser(obj);
        }
    }

//    public static void getUserListDB(){
//        //Instancia de la base de datos
//        StartVar.listuser.clear();
//        StartVar.listuser =  StartVar.appDatabase.daoUser().getUsers();
//    }

    public static void getConfigDB(){
        //Instancia de la base de datos
        StartVar.mConfigDB =  StartVar.appDBall.daoCfg().getUsers(StartVar.mConfID);
    }

    public void setmPermiss(boolean permiss){
        mPermiss = permiss;
    }

    public static void setmMainStart(boolean mStart){mainStart = mStart;}


    public void setArrayList(ArrayList<Object> listA, ArrayList<Object> listB, ArrayList<Object> listC, ArrayList<Object> listD){
        StartVar.textList.clear();
        StartVar.dirList.clear();
        StartVar.typeList.clear();
        StartVar.retirList.clear();

        StartVar.textList = listA;
        StartVar.dirList = listB;
        StartVar.typeList = listC;
        StartVar.retirList = listD;
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

}
