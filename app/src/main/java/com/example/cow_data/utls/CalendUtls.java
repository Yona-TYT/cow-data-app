package com.example.cow_data.utls;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.cow_data.AppContextProvider;
import com.example.cow_data.GlobalData;
import com.example.cow_data.StartVar;
import com.example.cow_data.db.Fecha;
import com.example.cow_data.db.dao.DaoDat;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import android.icu.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendUtls {
    private GlobalData glData = GlobalData.getInstance(AppContextProvider.getContext());

    public CalendUtls() {
    }

    public static String dataConverted(String text, int selec) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Convierte Sting  a forrmato de fecha
            LocalDate date = LocalDate.parse(text);
            //Inicia la fecha actual
            LocalDate currdate = LocalDate.now();

            long vlresult = 0;
            //Para años
            if (selec == 0) {
                vlresult = ChronoUnit.YEARS.between(date, currdate);
            }
            //Para meses
            else if (selec == 1) {
                vlresult = ChronoUnit.MONTHS.between(date, currdate);
            }
            //Para Dias
            else if (selec == 2) {
                vlresult = ChronoUnit.DAYS.between(date, currdate);
            }
            //Para Formato de fecha
            else if (selec == 3) {
                Period result = date.until(currdate);
                return result.getDays() + "-" + result.getMonths() + "-" + result.getYears();
            }
            return "" + (vlresult < 0 ? 1 : vlresult);
        }
        return "1";
    }

    public static String[] dataValidate(String text) {
        Pattern patt = Pattern.compile("(^(\\d{1,2})(/)(\\d{1,2})(/)(\\d{1,3})$)|(^(\\d{1,2})(-)(\\d{1,2})(-)(\\d{1,3})$)|(^(\\d{1,2})(\\.)(\\d{1,2})(\\.)(\\d{1,3})$)");
        Matcher matcher = patt.matcher(text);
        if (matcher.find()) {
            if (text.contains("-")) {
                return text.split("-");
            } else if (text.contains("/")) {
                return text.split("/");
            } else if (text.contains(".")) {
                return text.split("\\.");
            } else {
                return null;
            }
        }
        return null;
    }

    public static String getDate(Long value) {
        // 1. Convertimos el long a LocalDate usando la zona horaria del dispositivo
        LocalDate date = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            date = Instant.ofEpochMilli(value)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // 2. Definimos el formato: dd (día), MM (mes numérico), yyyy (año completo)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // 3. Retornamos el texto formateado
            return date.format(formatter);
        }
        return "NA";
    }

    public static String getTime(Long value) {
        // 1. Convertimos el long a LocalTime usando la zona horaria del dispositivo
        LocalTime time = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            time = Instant.ofEpochMilli(value)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime();

            // 2. Definimos el formato de 24 horas (HH mayúscula fuerza las 24h)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            // 3. Retornamos el texto formateado
            return time.format(formatter);
        }
        return "NA";
    }

    public static String getTime(String value) {
        String text = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalTime mTime = LocalTime.parse(value);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return formatter.format(mTime);
        }
        return text;
    }

    // Constantes para identificar el tipo de comparación de forma clara
    public static final int COMPARE_BY_DAY = 0;
    public static final int COMPARE_BY_MONTH = 1;
    public static final int COMPARE_BY_YEAR = 2;

    /**
     * Inserta la fecha actual en la base de datos si no existe previa coincidencia
     * según el tipo de comparación solicitado.
     *
     * @param mContext Contexto de la aplicación.
     * @param compareMode Modo de comparación: DAY_MONTH_YEAR, MONTH_YEAR o BY_YEAR.
     */
//    public static Fecha getAndCreateDate(Context mContext, int compareMode) {
//        DaoDat daoFecha = StartVar.appDBall.daoDat();
//        List<Fecha> listFecha = daoFecha.getUsers();
//        boolean exists = false;
//
//        // Forzamos la zona horaria a UTC para garantizar consistencia global
//        TimeZone localZone = TimeZone.getDefault();
//
//        // 1. Instancia de la fecha actual en UTC y extracción de sus partes
//        Calendar calCurr = Calendar.getInstance(localZone);
//        int currDay = calCurr.get(Calendar.DAY_OF_MONTH);
//        int currMonth = calCurr.get(Calendar.MONTH);
//        int currYear = calCurr.get(Calendar.YEAR);
//
//        // 2. Instancia reutilizable en UTC para el bucle
//        Calendar calItem = Calendar.getInstance(localZone);
//
//        Fecha mF = null;
//        for (Fecha d : listFecha) {
//            mF = d;
//            calItem.setTimeInMillis(d.date);
//
//            int itemDay = calItem.get(Calendar.DAY_OF_MONTH);
//            int itemMonth = calItem.get(Calendar.MONTH);
//            int itemYear = calItem.get(Calendar.YEAR);
//
//            // Evaluación en cascada basada en el parámetro compareMode
//            if (compareMode == COMPARE_BY_DAY) {
//                // Compara DÍA, MES y AÑO
//
//                //Basic.msg(""+currYear +"=="+ itemYear +" :: "+ currMonth +"=="+ itemMonth +" :: "+ currDay +"=="+ itemDay,true);
//
//                if (currYear == itemYear && currMonth == itemMonth && currDay == itemDay) {
//                    exists = true;
//                    break;
//                }
//            } else if (compareMode == COMPARE_BY_MONTH) {
//                // Compara MES y AÑO (Tu lógica original)
//                if (currYear == itemYear && currMonth == itemMonth) {
//                    exists = true;
//                    break;
//                }
//            } else if (compareMode == COMPARE_BY_YEAR) {
//                // Compara SOLO AÑO
//                if (currYear == itemYear) {
//                    exists = true;
//                    break;
//                }
//            }
//        }
//
//        // Si ya existe registro bajo ese criterio, detenemos la inserción
//        if (exists) {
//            return mF;
//        }
//
//        // Registro de la nueva fecha si no hubo coincidencias
//        long currDate = System.currentTimeMillis();
//        long currTime = System.currentTimeMillis();
//
//        Fecha obj = new Fecha("dateID" + listFecha.size(), getShortDateYear(currDate), currDate, currTime);
//        daoFecha.insertUser(obj);
//
//        // Recarga la lista de la DB
//        //StartVar.getFecListDB();
//
//        return obj;
//    }

//    public static boolean isSameMonth(long date1, long date2) {
//        // Si son idénticos en milisegundos, obviamente son del mismo mes/año (Ahorra cálculos)
//        if (date1 == date2) {
//            return true;
//        }
//
//        // Usamos la zona horaria UTC para una consistencia absoluta
//        TimeZone localZone = TimeZone.getDefault();
//
//        // Instancia para procesar el primer long
//        Calendar cal1 = Calendar.getInstance(localZone);
//        cal1.setTimeInMillis(date1);
//        int month1 = cal1.get(Calendar.MONTH);
//        int year1 = cal1.get(Calendar.YEAR);
//
//        // Instancia para procesar el segundo long
//        Calendar cal2 = Calendar.getInstance(localZone);
//        cal2.setTimeInMillis(date2);
//
//        // Comparación en cascada: si el año no coincide, se descarta de inmediato sin evaluar el mes
//        return year1 == cal2.get(Calendar.YEAR) && month1 == cal2.get(Calendar.MONTH);
//    }

//    public static boolean isSameDay(long date1, long date2) {
//        // Si son idénticos en milisegundos, obviamente son de la misma fecha (Ahorra cálculos)
//        if (date1 == date2) {
//            return true;
//        }
//
//        // Usamos la zona horaria UTC para una consistencia absoluta
//        TimeZone localZone = TimeZone.getDefault();
//
//        // Instancia para procesar el primer long
//        Calendar cal1 = Calendar.getInstance(localZone);
//        cal1.setTimeInMillis(date1);
//        int day1 = cal1.get(Calendar.DAY_OF_MONTH);
//        int month1 = cal1.get(Calendar.MONTH);
//        int year1 = cal1.get(Calendar.YEAR);
//
//        // Instancia para procesar el segundo long
//        Calendar cal2 = Calendar.getInstance(localZone);
//        cal2.setTimeInMillis(date2);
//
//        // Comparación en cascada: el procesador descarta la operación de golpe si el año
//        // o el mes no coinciden, evaluando el día sólo si es estrictamente necesario.
//        return year1 == cal2.get(Calendar.YEAR) &&
//                month1 == cal2.get(Calendar.MONTH) &&
//                day1 == cal2.get(Calendar.DAY_OF_MONTH);
//    }

    public static String getShortDateYear(long timestamp) {
        return getShortDateInternal(timestamp, true);
    }

    public static String getShortDate(long timestamp) {
        return getShortDateInternal(timestamp, false);
    }

    public static String getShortDateInternal(long timestamp, boolean year) {
        // 1. Convertimos el long a LocalDate usando la zona horaria del dispositivo
        LocalDate date = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            date = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // 2. Construimos un formato personalizado para forzar la primera letra en Mayúscula sin puntos
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("d ") // 'd' sin repetir muestra el día sin ceros a la izquierda (ej: 2 en vez de 02)
                    .appendText(java.time.temporal.ChronoField.MONTH_OF_YEAR, java.time.format.TextStyle.SHORT)
                    .toFormatter(Locale.getDefault()); // Usa el idioma del teléfono del usuario

            // 3. Retornamos el texto y limpiamos posibles puntos finales que añade Android en algunos idiomas (como "jul.")
            return date.format(formatter).replace(".", "")+ (year ? " "+date.getYear():"");
        }
        return "NA";
    }

    public static int getRangeMultiple(String txDate, int selec) {
        long num = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!txDate.isEmpty()) {
                // Convierte String a formato de fecha
                LocalDate date = LocalDate.parse(txDate);
                // Fecha actual
                LocalDate currdate = LocalDate.now();

                if (selec == 1) {
                    // Para días: diferencia directa (incluye día actual)
                    num = ChronoUnit.DAYS.between(date, currdate) + 1;
                } else if (selec == 2) {
                    // Para meses: desde 1 del mes de txDate hasta 1 del mes actual +1 si día actual >1
                    LocalDate mDate = LocalDate.of(date.getYear(), date.getMonth(), 1);
                    LocalDate currMonthStart = currdate.withDayOfMonth(1);
                    num = ChronoUnit.MONTHS.between(mDate, currMonthStart);
                    if (currdate.getDayOfMonth() > 1) {
                        num += 1;
                    }
                } else if (selec == 3) {
                    // Para años: desde 1/1 del año de txDate hasta 1/1 del año actual +1 si día del año >1
                    LocalDate yDate = LocalDate.of(date.getYear(), 1, 1);
                    LocalDate currYearStart = currdate.withDayOfYear(1);
                    num = ChronoUnit.YEARS.between(yDate, currYearStart);
                    if (currdate.getDayOfYear() > 1) {
                        num += 1;
                    }
                }
            }
        }
        return (int) num;
    }


    public static Object[] dateToMoney(String startDate, int select, Double rent, Double paid) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (rent <= 0) {
                return null;
            }
            if (!startDate.isEmpty()) {
                LocalDate originalDate = LocalDate.parse(startDate);
                LocalDate currdate = LocalDate.now();  // O usa LocalDate.of(2025, 10, 26) para testing fijo

                int result = currdate.compareTo(originalDate);
                if (result < 0) {
                    return null;
                }

                // Ajustar originalDate según select
                LocalDate adjustedOriginal = originalDate;
                if (select == 2) {
                    adjustedOriginal = LocalDate.of(originalDate.getYear(), originalDate.getMonth(), 1);
                } else if (select == 3) {
                    adjustedOriginal = LocalDate.of(originalDate.getYear(), 1, 1);
                }

                // Calcular numOwed (períodos completos)
                long numOwed = 0;
                int daysPassed = 0;
                if (select == 1) {
                    numOwed = ChronoUnit.DAYS.between(adjustedOriginal, currdate)+1;
//                    if (numOwed == 0) {
//                        numOwed = 1;
//                    }
                } else if (select == 2) {
                    LocalDate currMonthStart = currdate.withDayOfMonth(1);
                    numOwed = ChronoUnit.MONTHS.between(adjustedOriginal, currMonthStart);
                    daysPassed = currdate.getDayOfMonth() - 1;
                    if (daysPassed > 0) {
                        numOwed += 1;
                    }
                } else if (select == 3) {
                    LocalDate currYearStart = currdate.withDayOfYear(1);
                    numOwed = ChronoUnit.YEARS.between(adjustedOriginal, currYearStart);
                    daysPassed = currdate.getDayOfYear() - 1;
                    if (daysPassed > 0) {
                        numOwed += 1;
                    }
                } else {
                    return new Object[]{0f, 0f, "", 1};
                }
                if (numOwed < 1) {
                    numOwed = 0;
                }

                double debt = 0.0;
                double currentPaid = 0.0;
                LocalDate date = adjustedOriginal;
                long covered = 0;

                // Calcular covered
                if (paid >= 0) {
                    covered = (long) Math.floor(paid / rent);
                } else {
                    covered = (long) Math.ceil(paid / rent);
                }

                long unpaidFull = numOwed - covered;
                double remainder = paid - (double) covered * rent;
                if (unpaidFull > 0) {
                    debt = (double) unpaidFull * rent;
                    currentPaid = remainder;
                } else {
                    debt = 0.0;
                    currentPaid = paid - (double) numOwed * rent;
                }

                // Calcular fecha: clamp superior para overpay positivo, permite negativo
                long periodsToAdd;
                if (covered > numOwed) {
                    periodsToAdd = numOwed;
                } else {
                    periodsToAdd = covered;
                }
                if (select == 1) {
                    date = adjustedOriginal.plusDays(periodsToAdd);
                } else if (select == 2) {
                    date = adjustedOriginal.plusMonths(periodsToAdd);
                } else {  // select == 3
                    date = adjustedOriginal.plusYears(periodsToAdd);
                }
                // Para overpay, opcional: si periodsToAdd == numOwed && currentPaid > 0, mantener en el siguiente inicio

                return new Object[]{debt, currentPaid, date.toString(), 0};
            }
        }
        return null;
    }

    public static String getDatePlus(String txDate, int sum, int selec) {
        String newDate = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!txDate.isEmpty()) {
                //Convierte Sting  a forrmato de fecha
                LocalDate date = LocalDate.parse(txDate);
                //Para Dias
                if (selec == 1) {
                    newDate = date.plusDays(sum).toString();
                }
                //Para meses
                else if (selec == 2) {
                    newDate = date.plusMonths(sum).toString();
                }
                //Para años
                else if (selec == 3) {
                    newDate = date.plusYears(sum).toString();
                }
            }
        }
        return newDate;
    }

    public static String getCorrectDate(String txDate, int selec) {
        String newDate = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!txDate.isEmpty()) {
                //Convierte Sting  a forrmato de fecha
                LocalDate date = LocalDate.parse(txDate);
                //Basic.msg("selec"+selec+" txDate "+txDate);
                //Para Dias
                if (selec == 1) {
                    return txDate;
                }
                //Para meses
                else if (selec == 2) {
                    newDate = LocalDate.of(date.getYear(), date.getMonth(), 1).toString();

                }
                //Para años
                else if (selec == 3) {
                    newDate = LocalDate.of(date.getYear(), 1, 1).toString();
                }
            }
        }
        return newDate;
    }

    public static LocalDateTime DTformat(String dt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (dt == null || dt.trim().isEmpty()) {
                Log.e("DTformat", "Fecha recibida es null o vacía");
                return LocalDateTime.now();
            }

            // Intentamos varios formatos comunes de Google Drive
            String[] patterns = {
                    "yyyy-MM-dd'T'HH:mm:ss",           // formato normal
                    "yyyy-MM-dd'T'H:mm:ss",            // hora con 1 dígito
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",       // con milisegundos
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",    // con microsegundos
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"  // con nanosegundos
            };

            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault());
                    LocalDateTime result = LocalDateTime.parse(dt, formatter);

                    Log.d("DTformat", "✅ Parseado con éxito usando: " + pattern + " → " + result);
                    return result;

                } catch (Exception ignored) {
                    // Probamos el siguiente patrón
                }
            }

            // Si ninguno funcionó
            Log.e("DTformat", "❌ No se pudo parsear la fecha: " + dt);
            return LocalDateTime.now(); // fallback seguro
        }
        return null;
    }


    // Old------------------------------------------------------------------------------

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
