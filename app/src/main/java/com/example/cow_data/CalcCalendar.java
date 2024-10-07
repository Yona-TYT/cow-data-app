package com.example.cow_data;

import android.content.Context;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalcCalendar {
    public CalcCalendar(){
    }
    public static String dataConverted(String text, int selec){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Convierte Sting  a forrmato de fecha
            LocalDate date = LocalDate.parse(text);
            //Inicia la fecha actual
            LocalDate currdate = LocalDate.now();

            long vlresult = 0;
            //Para años
            if(selec == 0){
                vlresult = ChronoUnit.YEARS.between(date, currdate);
            }
            //Para meses
            else if(selec == 1){
                vlresult = ChronoUnit.MONTHS.between(date, currdate );
            }
            //Para Dias
            else if(selec == 2){
                vlresult = ChronoUnit.DAYS.between(date, currdate );
            }
            //Para Formato de fecha
            else if(selec == 3){
                Period result = date.until(currdate);
                return result.getDays()+"-"+result.getMonths()+"-"+result.getYears();
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