package com.example.cow_data;

import android.content.Context;

import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

public class SatrtVar {

    //Nombre de data Base
    private static String nameDB = "Registro2";

    // Var redundants
    public static List<Usuario> listuser;
    public static boolean mPermiss;

    // DB
    public static AppDatabase appDatabase;
    public static ArrayList<String> textList;
    public static ArrayList<String> dirList;
    public static ArrayList<String> typeList;

    public static int currSel2 = 4;

    private  Context mContex;

    public  SatrtVar(Context mContex){
        this.mContex = mContex;
    }

    public void setUserListDB(){
        //Instancia de la base de datos
        SatrtVar.appDatabase = Room.databaseBuilder( mContex, AppDatabase.class, nameDB).allowMainThreadQueries().build();
        SatrtVar.listuser =  appDatabase.daoUser().getUsers();
    }

    public void getUserListDB(){
        //Instancia de la base de datos
        SatrtVar.listuser =  SatrtVar.appDatabase.daoUser().getUsers();
    }

    public void setmPermiss(boolean permiss){
        mPermiss = permiss;
    }

    public void setArrayList(ArrayList<String> listA, ArrayList<String> listB, ArrayList<String> listC){
        SatrtVar.textList = listA;
        SatrtVar.dirList = listB;
        SatrtVar.typeList = listC;
    }

    public void setCurrSel2(int value){
        SatrtVar.currSel2 = value;
    }
}
