package com.example.cow_data;

import android.content.Context;

import com.example.cow_data.db.GenericQueue;

import java.util.Arrays;
import java.util.List;

public class GlobalData {

    private static GlobalData instance;
    private final Context context;
    private GenericQueue genericQueue;

    // Variables globales
    public static String glName = "";
    public static String glTelef = "";
    public static String glPhone= "";

    public static String glCedula = "";
    public static String glCodeBank = "";
    public static String glNameBank = "";

    public static  boolean shouldReload = true;

    public static final double glUdsAlert = 3.0;
    public static final double glMtrAlert = 0.5;

    public static final float pointPay = 0.05f;
    public static final float pointNoPay = 0.01f;
    public static final float pointLost = 0.0001f;

//    public Article currArt = null;
    public String currSalId = "";

    public List<String> unitList = Arrays.asList("uds.", "kg", "L", "m", "cm", "?", "?", "?");

    public List<String> categ = Arrays.asList("Unidad", "Paquete", "Caja", "No Empacables");

    public List<String> saleType = Arrays.asList("Venta", "Sin Pagar", "Perdida");

    public int optTasa = 0;
    public double tasaDolar = 0.0;
    public double sendValue = 0.0;
    public boolean isEsFormat = true;

    public Double[][] listCalc = {
            new Double[] {0.0, 0.0, 0.0},
            new Double[] {0.0, 0.0, 0.0},
            new Double[] {0.0, 0.0, 0.0}
    };

    public static String[] dataList = {"","","","","","",""};

    public static long[] longList = new long[0];

    public static double[] doubList = new double[0];

    public static String[] dataDbg = {""};

    public int optCalc = 0;
    public boolean isEdit = false;

    private GlobalData(Context context) {
        this.context = AppContextProvider.getContext(); // Garantizamos ApplicationContext
        this.genericQueue = new GenericQueue(this.context);
    }

    public GenericQueue getGenericQueue() {
        if (this.genericQueue == null) {
            this.genericQueue = new GenericQueue(this.context != null ? this.context : AppContextProvider.getContext());
        }
        return this.genericQueue;
    }

    /**
     * Método seguro para obtener la instancia
     */
    public static GlobalData getInstance(Context context) {
        if (instance == null) {
            synchronized (GlobalData.class) {
                if (instance == null) {
                    if (context == null) {
                        throw new IllegalArgumentException("Context cannot be null when initializing AppData");
                    }
                    instance = new GlobalData(context);
                }
            }
        }
        return instance;
    }

    /**
     * Método recomendado para usar desde Application
     */
    public static void initialize(Context context) {
        if (instance == null) {
            instance = new GlobalData(context);
        }
    }

//    public void setCurrArt(Article obj){
//        this.currArt = obj;
//    }
//    public Article getCurrArt(){
//        return this.currArt;
//    }

    public void setCurrSalId(String obj){
        this.currSalId = obj;
    }
    public String getCurrSalId(){
        return this.currSalId;
    }

    public void setTasaDolar(double tasa) {
        this.tasaDolar = tasa;
    }
    public double getTasaDolar() {
        return this.tasaDolar;
    }

    public void setSendValue(double value){
        this.sendValue = value;
    }
    public double getSendValue() {
        return this.sendValue;
    }

    public boolean getIsEsFormat(){ return this.isEsFormat; }

    public void setIsEsFormat(boolean b){
        this.isEsFormat = b;
    }

    public void setOptTasa(int opt) {
        this.optTasa = opt;
    }

    public int getOptTasa() {
        return this.optTasa;
    }


    public List<String> getUnitList(){
        return this.unitList;
    }

    public void setOptCalc(int opt) {
        this.optCalc = opt;
    }

    public int getOptCalc() {
        return this.optCalc;
    }

    public void setIsEdit(boolean b) {
        this.isEdit = b;
    }

    public boolean getIsEdit() {
        return this.isEdit;
    }

    public void setListCalc(Double[] mList, int opt) {
        if (opt < this.listCalc.length){
            this.listCalc[opt] = mList;
        }
    }

    public Double[] getListCalc(int opt) {
        if (opt < this.listCalc.length){
            return this.listCalc[opt];
        }
        return new Double[] {(double)0, (double)0, (double)0};
    }
    public void cleanListCalc() {
        this.listCalc = new Double[][]{
                new Double[]{(double) 0, (double) 0, (double) 0},
                new Double[]{(double) 0, (double) 0, (double) 0},
                new Double[]{(double) 0, (double) 0, (double) 0}
        };
    }

    public void setDateList(int opt, String mDate) {
        if (opt < this.dataList.length){
            this.dataList[opt] = mDate;
        }
    }

    public void setLongList(long[] mDate) {
        this.longList = mDate;
    }

    public String getDate(int opt) {
        if (opt < this.dataList.length){
            return this.dataList[opt];
        }
        return "";
    }

    public long[] getLongList() {
        return this.longList;
    }

    public double[] getDoubList() {
        return this.doubList;
    }
}
