package com.example.cow_data;

import android.icu.util.Calendar;
import android.os.Build;
import android.util.Log;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalcCalendar {
    public CalcCalendar(){
    }
    public static String dataConverted(String text, int selec){
        if(text.isEmpty()){
            return "";
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Convierte Sting  a forrmato de fecha
            LocalDate date = LocalDate.parse(text);
            //Inicia la fecha actual
            LocalDate currdate = LocalDate.now();

            long vlresult = 0;

            //Para fecha de Nacimiento
            if(selec == 0){
                //Log.d("Calendar", "Calen3 -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+text);
                return getFormatDateES(text);
            }
            //Para años
            else if(selec == 1){
                vlresult = ChronoUnit.YEARS.between(date, currdate);
            }
            //Para meses
            else if(selec == 2){
                vlresult = ChronoUnit.MONTHS.between(date, currdate );
            }
            //Para Dias
            else if(selec == 3){
                vlresult = ChronoUnit.DAYS.between(date, currdate );
            }
            //Para Formato de fecha
            else if(selec == 4){
                Period result = date.until(currdate);
                return result.getYears()+"-"+result.getMonths()+"-"+result.getDays();
            }
            return ""+(vlresult < 0? 1 : vlresult);
        }
        return "1";
    }
    public static String[] dataValidate(String text){
        Pattern patt = Pattern.compile("(^(\\d{1,2})(/)(\\d{1,2})(/)(\\d{1,3})$)|(^(\\d{1,2})(-)(\\d{1,2})(-)(\\d{1,3})$)|(^(\\d{1,2})(\\.)(\\d{1,2})(\\.)(\\d{1,3})$)");
        Matcher matcher = patt.matcher(text);
        if(matcher.find()) {
            if (text.contains("-")) {
                return text.split("-");
            }
            else if (text.contains("/")) {
                return text.split("/");
            }
            else if (text.contains(".")) {
                return text.split("\\.");
            }
            else {
                return null;
            }
        }
        return null;
    }

    public static String isDateFormat(String rawTx){

        Pattern patt = Pattern.compile("^(\\d{1,2})([/:-])(\\d{1,2})([/:-])(\\d{4})$");
        Matcher m = patt.matcher(rawTx);
        if (m.find()) {
            rawTx = rawTx.replaceAll("[-:]", "/");
            String[] mArray = rawTx.split("/");
            if(mArray.length>2){
                NumberFormat formatter = new DecimalFormat("00");
                String dd = formatter.format(Integer.parseInt(mArray[0]));
                String mm = formatter.format(Integer.parseInt(mArray[1]));
                String yyyy = mArray[2];
                rawTx = dd+"/"+mm+"/"+yyyy;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //rawTx = "14/08/2024";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                try {
                    LocalDate date = LocalDate.parse(rawTx, formatter);
                    //Inicia la fecha actual
                    LocalDate currdate = LocalDate.now();
//                    //Para descartar fechas futuras
//                    if (date.until(currdate).getDays() > 0) {
//                        rawTx = date.toString();
//                    }
                    rawTx = date.toString();
                }
                catch (Exception e) {
                    return "";
                }
            }
           // Log.d("Calendar", "Calen RESULT -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+rawTx);
            return rawTx;
        }
        return "";
    }

    public static String getFormatDateES(String rawTx){
        //Log.d("Calendar", "Calen ES -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+rawTx);

        return getFormatDate(rawTx,"","dd/MM/yyyy");
    }

    public static String getFormatDateEN(String rawTx){
        //Log.d("Calendar", "Calen EN -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+rawTx);

        return getFormatDate(rawTx,"dd/MM/yyyy", "yyyy/MM/dd");
    }

    public static String getFormatDate(String rawTx, String txA, String txB){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter formatA = DateTimeFormatter.ofPattern(txA);
            DateTimeFormatter formatB = DateTimeFormatter.ofPattern(txB);
            try {
                if (txA.isEmpty()) {
                    rawTx = LocalDate.parse(rawTx).format(formatB);
                }
                else{
                    rawTx = LocalDate.parse(rawTx, formatA).format(formatB);
                }
            }
            catch (Exception e) {
                //Log.d("Calendar", "Calen2 -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+e.getMessage());

                return "";
            }
        }
        //Log.d("Calendar", "Calen4 -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+rawTx);

        return rawTx;
    }

    public static long getDateFromDays(Calendar mCale, String rawTx, long days){
        long myLong = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                LocalDate myDate = LocalDate.parse(rawTx).plusDays(days);
                mCale.set(Calendar.YEAR, myDate.getYear());
                mCale.set(Calendar.MONTH, myDate.getMonthValue()-1);
                mCale.set(Calendar.DAY_OF_MONTH, myDate.getDayOfMonth());
                myLong = mCale.getTimeInMillis();
            }
            catch (Exception e) {

                //Log.d("Calendar", "-->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+e.getMessage());

                return 0;
            }
        }
        return myLong;
    }

    public static String dateDaysCount(String rawTx){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                LocalDate mDate = LocalDate.parse(rawTx).plusDays(SatrtVar.mDayA);
                //Inicia la fecha actual
                LocalDate currdate = LocalDate.now();

                return Long.toString(ChronoUnit.DAYS.between(currdate, mDate ));
            }
            catch (Exception e) {
                return "";
            }
        }
        return "";
    }
}


//    @RequiresApi(api = Build.VERSION_CODES.O)
//    private LocalDate validateDate(int year, int moth, int day){
//        Log.d("PhotoPicker", "1-->>>>>>>>>>>>>>>>>>>>>>>>>>>> year: " + year + " mes: "+ moth);
//
//        //Esto saca un aproximado de los meses restantes pero no es perfecto
//        if(moth > 12) {
//            float myFloat =  ((float)(moth-1) / (float)12);
//            year = ((int)myFloat)+1;
//            moth = getFloatPart(myFloat)+1;
//
//            Log.d("PhotoPicker", "-->>>>>>>>>>>>>>>>>>>>>>>>>>>> year: " + year + " mes: "+ moth);
//        }
//        boolean result = true;
//        try{
//            LocalDate.of(year, moth, day);
//        }
//        catch(DateTimeException e) {
//            result = false;
//        }
//        if(result){
//            return LocalDate.of(year, moth, day);
//        }
//        else {
//            return LocalDate.of(1, 1, 1);
//        }
//    }

//    private int getFloatPart(float numero) {
//
//        Log.d("", String.format("El número originalmente es: %f\n", numero));
//
//        int parteEntera = (int)numero; // Le quitamos la parte decimal pasando a int
//
//        float parteDecimal = (numero - (float)parteEntera); // restamos la parte entera
//
//        String text =  Float.toString(parteDecimal); //Convertimos los decimales a string
//
//        text = text.replace('.', '0');
//        text = ""+(text.length() > 2? text.charAt(2): 0);
//        Log.d("", String.format("Parte entera: %d. Parte decimal: %s\n", parteEntera, text));
//
//        return Integer.parseInt(text);
//
//    }