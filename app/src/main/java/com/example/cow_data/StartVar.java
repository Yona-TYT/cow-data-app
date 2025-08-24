package com.example.cow_data;

import android.content.Context;

import androidx.room.Room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StartVar {
    //Mapa de arrays
    public static HashMap<String, ArrayList<String>> arrayMap;
    public static final String mId1 = "id1";
    public static final String mId2 = "id2";

    //Nombre de data Base
    public static String nameDB = "Registro2";

    // Var redundants
    public static List<Usuario> listuser;
    public static boolean mPermiss;
    public static int mDayA = 276;
    public static int mDayB = 283;
    public static String mDateFormEN = "yyyy-MM-dd";
    public static String mDateFormES = "dd-MM-yyyy";

    // DB
    public static AppDatabase appDatabase;
    public static ArrayList<String> textList;
    public static ArrayList<String> dirList;
    public static ArrayList<String> typeList;
    public static ArrayList<String> morlist = new ArrayList<>();
    public static ArrayList<String> boxlist1 = new ArrayList<>();
    public static ArrayList<String> boxlist2 = new ArrayList<>();

    public static int currSel2 = 4;

    public static Context mContex;
    public StartVar(Context mContex){
        this.mContex = mContex;
    }

    public void setUserListDB(){
        //Instancia de la base de datos
        StartVar.appDatabase = Room.databaseBuilder( mContex, AppDatabase.class, nameDB).allowMainThreadQueries().build();
        StartVar.listuser =  appDatabase.daoUser().getUsers();
    }

    public static void getUserListDB(){
        //Instancia de la base de datos
        StartVar.listuser =  StartVar.appDatabase.daoUser().getUsers();
    }

    public void setmPermiss(boolean permiss){
        mPermiss = permiss;
    }

    public void setArrayList(ArrayList<String> listA, ArrayList<String> listB, ArrayList<String> listC){
        StartVar.textList = listA;
        StartVar.dirList = listB;
        StartVar.typeList = listC;
    }

    public void setCurrSel2(int value){
        StartVar.currSel2 = value;
    }

    public void setMorlist(ArrayList<String> list){
        StartVar.morlist.clear();
        StartVar.morlist = list;
    }

    public void setBoxlist1(ArrayList<String> list){
        StartVar.boxlist1 = list;
    }

    public void clearBoxlist1(){
        StartVar.boxlist1.clear();
    }

    public void setBoxlist2(ArrayList<String> list){
        StartVar.boxlist2 = list;
    }

    public void clearBoxlist2(){
        StartVar.boxlist2.clear();
    }

    public static void setArrayMap(HashMap<String, ArrayList<String>> mMap){
        StartVar.arrayMap = mMap;
    }
}
