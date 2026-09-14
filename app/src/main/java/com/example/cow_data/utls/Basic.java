package com.example.cow_data.utls;

import static android.widget.GridLayout.spec;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.cow_data.R;
import com.example.cow_data.StartVar;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.IllformedLocaleException;
import java.util.Locale;


public class Basic {
    private static Context mContex;
    public static boolean isDow = true;
    public static boolean isUp = false;

    private static final String ACTION_APP_EVENT = "com.example.cow_data.EVENT";
    private static final String EXTRA_EVENT_TYPE = "cow_data_event";
    private static final String EXTRA_FILE_PATHS = "file_paths";
    private static final String EXTRA_SENDER_TYPE = "sender_type";
    private static final String EVENT_FILE_UPLOADED = "file_uploaded";

    private static String oldMsg = "";
    private static long lastShowTime = 0;

    public Basic(Context mContex) {
        this.mContex = mContex;
    }

    public int getPixelSiz(int id) {
        return mContex.getResources().getDimensionPixelSize(id);
    }

    public float getFloatSiz(int id) {
        DisplayMetrics metrics = new DisplayMetrics();
        float scaledDensity = mContex.getResources().getDisplayMetrics().scaledDensity;
        return getPixelSiz(id) / scaledDensity;
    }

    public static Float parseFloat(String value){
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat("0.##");
        format.setDecimalFormatSymbols(symbols);

        try {
            return format.parse(value).floatValue();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return (float)0.00;
    }

    public static String setFormatter(String value){
        value = value.replaceAll("([^\\d.,-])","");
        if (value.isEmpty()){
            value = "0";
        }
        return setFormatter(Double.parseDouble(value));
    }
    public static String setFormatter(Double value){
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("ES"));
        DecimalFormat formatter = (DecimalFormat) nf;
        formatter.applyPattern("###,###.##");
        return formatter.format(value);
    }

    @SuppressLint("DefaultLocale")
    public static String setValue(String value) {
        value = value.replaceAll("([^\\d.,])","");
        if (value.isEmpty()){
            value = "0";
        }
        Double precDoll = StartVar.mDollar;
        Double number = Double.parseDouble(value);
        if (StartVar.mCurrency == 1) {    //Selector en Bs
            number = number / precDoll;
        }
        return String.valueOf(number);
    }

    public static Double setValue(double value) {
        Double precDoll = StartVar.mDollar;
        if (StartVar.mCurrency == 1) {    //Selector en Bs
            value = value / precDoll;
        }
        return value;
    }

    @SuppressLint("DefaultLocale")
    public static String getConverteValue(String value) {
        value = value.replaceAll("([^\\d.,])","");
        value = value.replaceAll(",",".");

        if (value.isEmpty()){
            value = "0";
        }
        Double number = Double.parseDouble(value);
        return String.valueOf(getConverteValue(number));
    }

    public static Double getConverteValue(Double value) {

        Double precDoll = StartVar.mDollar;
        if (StartVar.mCurrency == 1) {    //Selector en Bs
            value = value * precDoll;
        }
        return value;
    }

    public static String getMaskConv(Double value, Double tasa, int symb) {

        return getMask(getConv(value, tasa, symb), symb);
    }

    public static String getMaskConv(Double value, int symb) {

        return getMask(getConv(value, null, symb), symb);
    }

    @SuppressLint("DefaultLocale")
    public static String getConv(String value) {
        value = value.replaceAll("([^\\d.,])","");
        value = value.replaceAll(",",".");

        if (value.isEmpty()){
            value = "0";
        }
        Double number = Double.parseDouble(value);
        return String.valueOf(getConv(number));
    }

    public static Double getConv(Double value) {

        Double precDoll = StartVar.mDollar;
        if (StartVar.mCurrency == 1) {    //Selector en Bs
            value = value * precDoll;
        }
        return value;
    }

    public static Double getConv(Double value, Double tasa, int symb) {

        Double precDoll = (tasa == null ? StartVar.mDollar : tasa);
        if(symb == 1) {   //Selector en Bs
            value = value * precDoll;
        }
        return value;
    }

    public static String getMask(String value, int symb) {
        value = setFormatterEs(value);

        if(symb == 0){   //Selector en $
            value = value + " $";
        }
        if(symb == 1){   //Selector en Bs
            value = value + " Bs";
        }
        return value;
    }

    public static String getMask(Double nr, int sing) {
        String value = setFormatterEs(nr);

        if(sing == 0){
            value = value + " $";
        }
        if(sing == 1){
            value = value + " Bs";
        }
        return value;
    }

    @SuppressLint("DefaultLocale")
    public static String getValueFormatter(String value) {
        return setFormatter(getConverteValue(value));
    }
    public static String getValueFormatter(Double value) {
        return setFormatter(getConverteValue(value).toString());
    }

    public static Float floatFormat(String value) {
        String mValue = value.replaceAll("([^.\\d])", "");
        mValue = mValue.replaceAll("^.$", "0.00");

        return mValue.isEmpty() ? (float)0 : Float.parseFloat(mValue);
    }

    public static String setFormatterEs(String value){
        value = value.replaceAll("([^\\d.,-])","");
        if (value.isEmpty()){
            value = "0";
        }
        return setFormatterInternal(Double.parseDouble(value), new Locale("es", "VE"));
    }

    public static String setFormatterEn(Double value) {
        return setFormatterInternal(value, Locale.US);           // 1,234.56
    }

    public static String setFormatterEs(Double value) {
        return setFormatterInternal(value, new Locale("es", "VE"));
    }

    public static String setFormatterInternal(Double value, Locale locale){
        if (value == null) return "";
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(locale);
        df.applyPattern("#,##0.00");
        return df.format(value);
    }

    public static Double getDebt(int mult, Double mont, Double debt) {
//        mont = mont.replaceAll("([^.0-9]+)", "");
//        debt = debt.replaceAll("([^.0-9]+)", "");

        double numA = mont;
        double numB = debt;

        double result = numA*mult;

        result -= numB;

        return result;

    }

    public static String setMask(String value, String sing) {
        value = setFormatter(value);

        return value;
    }

    public static String formatDecimal(Double value) {
        if (value == null) return "";

        return getFormatDecimal().format(value);
    }

    public static String formatDecimal(Float value) {
        if (value == null) return "";
        return getFormatDecimal().format(value);
    }

    public static DecimalFormat getFormatDecimal() {
        // Forzamos el locale de España para asegurar punto en miles y coma en decimales
        Locale spanishLocale = Locale.forLanguageTag("es-ES");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(spanishLocale);

        // El '#' oculta los ceros innecesarios
        DecimalFormat df = new DecimalFormat("###,###.##", symbols);

        return df;
    }

    public static String nameProcessor(String value){
        String text = value.replaceAll("([^\\s0-9a-zA-Z]+)", "");
        text = text.replaceAll("(\\s{2,})", " ");
        text = text.replaceAll("(^\\s)|(\\s$)", "");
        return text;
    }

    public static String inputProcessor(String value){
        return value.replaceAll("([;,\"<>]+)", "");
    }

    public static String parseMoneyValue(String value, String groupingSeparator, String currencySymbol) {
        return value.replace(groupingSeparator, "").replace(currencySymbol, "");
    }

    public static Number parseMoneyValueWithLocale(Locale locale, String value, String groupingSeparator, String currencySymbol) {
        String valueWithoutSeparator = parseMoneyValue(value, groupingSeparator, currencySymbol);
        try {
            return NumberFormat.getInstance(locale).parse(valueWithoutSeparator);
        } catch (ParseException exception) {
            return 0;
        }
    }

    public static Locale getLocaleFromTag(String localeTag) {
        try {
            return new Locale.Builder().setLanguageTag(localeTag).build();
        } catch (IllformedLocaleException e) {
            return Locale.getDefault();
        }
    }

    public static boolean isLollipopAndAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

//    public static void sendFileUploadedBroadcast(Context context, String[] filePaths, String senderType) {
//        //LOG.debug("Sending file uploaded broadcast para: " + senderType);
//        Intent intent = new Intent(ACTION_APP_EVENT);
//        intent.putExtra(EXTRA_EVENT_TYPE, EVENT_FILE_UPLOADED);
//        intent.putExtra(EXTRA_FILE_PATHS, filePaths);
//        intent.putExtra(EXTRA_SENDER_TYPE, senderType);
//        context.sendBroadcast(intent);
//    }
//
//    // También para errores
//    public static void sendUploadErrorBroadcast(Context context, String errorMessage, String senderType) {
//        //LOG.debug("Sending upload error broadcast: " + errorMessage);
//        Intent intent = new Intent(ACTION_APP_EVENT);
//        intent.putExtra(EXTRA_EVENT_TYPE, "upload_error");
//        intent.putExtra("error_message", errorMessage);
//        intent.putExtra(EXTRA_SENDER_TYPE, senderType);
//        context.sendBroadcast(intent);
//    }
}