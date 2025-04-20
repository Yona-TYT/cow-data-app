package com.example.cow_data;

import android.icu.util.Calendar;
import android.os.Build;
import android.util.Log;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
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
      //  Basic.msg("1 a: "+text +" sel: "+selec);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Convierte Sting  a forrmato de fecha
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(StartVar.mDateFormEN);
            LocalDate date;


            try {
                date = LocalDate.parse(text, formatter);
            }
            catch (Exception e) {
                date = null;
            }


            if(date != null) {
                //Inicia la fecha actual
                LocalDate currdate = LocalDate.now();

                long vlresult = 0;

                //Para fecha de Nacimiento
                if (selec == 0) {
                    //Log.d("Calendar", "Calen3 -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+text);
                    return getFormatDateES(text);
                }
                //Para años
                else if (selec == 1) {
                    vlresult = ChronoUnit.YEARS.between(date, currdate);
                }
                //Para meses
                else if (selec == 2) {
                    vlresult = ChronoUnit.MONTHS.between(date, currdate);
                }
                //Para Dias
                else if (selec == 3) {
                    vlresult = ChronoUnit.DAYS.between(date, currdate);
                }
                //Para Formato de fecha
                else if (selec == 4) {
                    Period result = date.until(currdate);
                    return result.getYears() + "-" + result.getMonths() + "-" + result.getDays();
                }
                return "" + (vlresult < 0 ? 1 : vlresult);
            }
            return text;
        }
        return "1";
    }

    public static String dataConvertedTo(String text, int selA, int selB) {

        String dateResult = "";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            //Inicia la fecha actual
            LocalDate currdate = LocalDate.now();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(StartVar.mDateFormEN);

            String myParse = text.replaceAll("\\D", "");
            long vlresult = Long.parseLong(myParse.isEmpty()?"0" : myParse);


            //Para fecha de Nacimiento
            if (selA == 0) {
                //Log.d("Calendar", "Calen3 -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+text);
                dateResult = getFormatDateEN(isDateFormat(text));
            }
            //Para años
            else if (selA == 1) {
                LocalDate from = currdate.minusYears(vlresult);
                dateResult = from.format(formatter);
            }
            //Para meses
            else if (selA == 2) {
                LocalDate from = currdate.minusMonths(vlresult);
                dateResult = from.format(formatter);
            }
            //Para Dias
            else if (selA == 3) {
                LocalDate from = currdate.minusDays(vlresult);
                dateResult = from.format(formatter);
            }
            //Para Formato de fecha
            else if (selA == 4) {
                String[] dateList = dataValidate(text);

                if (dateList != null && dateList.length > 1) {
                    LocalDate from = currdate.minusYears(Long.parseLong(dateList[0]));
                    from = from.minusMonths(Long.parseLong(dateList[1]));
                    from = from.minusDays(Long.parseLong(dateList[2]));
                    //Log.d("PhotoPicker", "1-->>>>>>>>>>>>>>>>>>>>>>>>>>>> Experimento: "+ from.toString());
                    dateResult = from.format(formatter);
                }
            }

            //Basic.msg("?a: "+2 +" b: "+dateResult+" - "+ selA);

        }
        return dateResult.isEmpty()? "" : dataConverted(dateResult, selB);
    }

    public static String[] dataValidate(String text){

        Pattern patt = Pattern.compile("^(\\d+)([/:-])(\\d+)([/:-])(\\d+)$");
        Matcher matcher = patt.matcher(text);
        if(matcher.find()) {
            text = text.replaceAll("[/:]","-");
            return text.split("-");
        }
        return new String[0];
    }

    public static String isDateFormat(String rawTx){

        Pattern patt = Pattern.compile("^(\\d{1,2})([/:-])(\\d{1,2})([/:-])(\\d{4})$");
        Matcher m = patt.matcher(rawTx);
        if (m.find()) {
            rawTx = rawTx.replaceAll("[/:]", "-");
            String[] mArray = rawTx.split("-");
            if(mArray.length>2){
                NumberFormat formatter = new DecimalFormat("00");
                String dd = formatter.format(Integer.parseInt(mArray[0]));
                String mm = formatter.format(Integer.parseInt(mArray[1]));
                String yyyy = mArray[2];
                rawTx = dd+"-"+mm+"-"+yyyy;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //rawTx = "14/08/2024";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(StartVar.mDateFormES);
                try {
                    LocalDate date = LocalDate.parse(rawTx, formatter);
                    //Inicia la fecha actual
                    LocalDate currdate = LocalDate.now();
//                    //Para descartar fechas futuras
//                    if (date.until(currdate).getDays() > 0) {
//                        rawTx = date.toString();
//                    }
                    rawTx = date.format(formatter);
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

        return getFormatDate(rawTx,StartVar.mDateFormEN,StartVar.mDateFormES);
    }

    public static String getFormatDateEN(String rawTx){
        //Log.d("Calendar", "Calen EN -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+rawTx);

        return getFormatDate(rawTx,StartVar.mDateFormES, StartVar.mDateFormEN);
    }

    public static String getFormatDate(String rawTx, String txA, String txB){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter formatA = DateTimeFormatter.ofPattern(txA);
            DateTimeFormatter formatB = DateTimeFormatter.ofPattern(txB);
            try {
                rawTx = LocalDate.parse(rawTx, formatA).format(formatB);
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
                mCale.set(Calendar.MONTH, myDate.getMonthValue() - 1);
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
                LocalDate mDate = LocalDate.parse(rawTx).plusDays(StartVar.mDayA);
                //Inicia la fecha actual
                LocalDate currdate = LocalDate.now();
                long mLong = ChronoUnit.DAYS.between(currdate, mDate );
                if(mLong < 0){
                     if (mLong < (-7)) {
                         return "N/A";
                     }
                     else {
                         return "0";
                     }
                }
                return Long.toString(mLong);
            }
            catch (Exception e) {
                return "";
            }
        }
        return "";
    }
    public static String getBrithDateText(String text){
        //Log.d("Calendar", "View -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+text);

        if(text.isEmpty()){
            return "";
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Convierte Sting  a forrmato de fecha
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(StartVar.mDateFormEN);
            LocalDate date;
            try{
                date = LocalDate.parse(text, formatter);

            }
            catch (Exception e) {
                date = null;
            }

            if(date != null) {
                //Inicia la fecha actual
                LocalDate currdate = LocalDate.now();

                String mText = "";

                //Para años
                int mYears = date.until(currdate).getYears();
                if (mYears >= 1) {
                    mText += mYears + " años ";
                }
                //Para meses
                int mMonth = date.until(currdate).getMonths();
                if (mMonth >= 1) {
                    mText += mMonth + " m ";
                }
                //Para Dias
                int mDays = date.until(currdate).getDays();
                if (mDays >= 0) {
                    mText += mDays + " d";
                }
                return mText.isEmpty() ? "Fecha no Valida" : mText;
            }
        }
        return "Fecha no Valida";
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