package com.example.cow_data;

import static android.widget.GridLayout.spec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.cow_data.activitys.MainActivity;
import com.google.android.material.theme.overlay.MaterialThemeOverlay;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Basic {
    private static Context mContex;
    public static boolean isDow = true;
    public static boolean isUp = false;


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

//    public void keyboardEvent(ConstraintLayout mConstrain, View elm,  List<View> mViewList, int opt) {
//        // Para eventos al mostrar o ocultar el teclado
//        mConstrain.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                // on below line we are creating a variable for rect
//                Rect rect = new Rect();
//                View contain = StartVar.mRootView;
//                // on below line getting frame for our relative layout.
//                contain.getWindowVisibleDisplayFrame(rect);
//                // on below line getting screen height for relative layout.
//                int screenHeight = contain.getRootView().getHeight();
//                // on below line getting keypad height.
//                int keypadHeight = screenHeight - rect.bottom;
//                if (keypadHeight > screenHeight * 0.15) {
//                    isDow = false;
//                    isUp = true;
//
//                    for (View mView : mViewList) {
//                        if(mView != null) {
//                            mView.setVisibility(View.INVISIBLE);
//                        }
//                    }
//                    //Toast.makeText(MainActivity.this, "Keyboard is +", Toast.LENGTH_LONG).show();
//                }
//                else {
//                    isDow = true;
//                    isUp = false;
//                    //Toast.makeText(mContex, "Keyboard is -", Toast.LENGTH_LONG).show();
//
//                    if (elm != null) {
//                        //Toast.makeText(mContex, "Aqui hayyyyyyyy?  " , Toast.LENGTH_LONG).show();
//                        elm.clearFocus();
//                    }
//
//                    for (View mView : mViewList) {
//                        if(mView != null) {
//                            mView.setVisibility(View.VISIBLE);
//                        }
//                    }
//                    mConstrain.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//
//                }
//            }
//        });
//    }

//    public void steAllKeyEvent(ConstraintLayout mConstrain, List<EditText> mInputList) {
//        for (int i = 0; i < mInputList.size(); i++) {
//            // Para eventos al mostrar o ocultar el teclado
//            keyboardEvent(mConstrain, mInputList.get(i), new ArrayList<>(), 0); //opt = 0 is clear elm focus
//            //-------------------------------------------------------------------------------------
//        }
//    }


    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void setAllfocusEvent(View elm, List<EditText> mInputList) {
        for (int i = 0; i < mInputList.size(); i++) {
            mInputList.get(i).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    //Toast.makeText(mContext, "Siz is "+b, Toast.LENGTH_LONG).show();
                    if (b) {
                        elm.setVisibility(View.INVISIBLE);
                    } else {
                        elm.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
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
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("ES"));
        DecimalFormat formatter = (DecimalFormat) nf;
        formatter.applyPattern("###,##0.00");
        return formatter.format(Float.parseFloat(value));
    }

    public static Float notFormatter(String value) throws ParseException {
        value = value.replaceAll("([^\\d.,-])","");
        if (value.isEmpty()){
            value = "0";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("ES"));
        DecimalFormat formatter = (DecimalFormat) nf;
        formatter.applyPattern("###,##0.00");
        return Objects.requireNonNull(formatter.parse(value)).floatValue();
    }

//    @SuppressLint("DefaultLocale")
//    public static String setValue(String value) {
//        value = value.replaceAll("([^\\d.,])","");
//        if (value.isEmpty()){
//            value = "0";
//        }
//        float precDoll = floatFormat(StartVar.mDollar);
//        float number = Float.parseFloat(value);
//        if (StartVar.mCurrency == 1) {    //Selector en Bs
//            number = number / precDoll;
//        }
//        return Float.toString(number);
//    }

//    @SuppressLint("DefaultLocale")
//    public static String getValue(String value) {
//        value = value.replaceAll("([^\\d.,])","");
//        value = value.replaceAll(",",".");
//
//        if (value.isEmpty()){
//            value = "0";
//        }
//        float precDoll = floatFormat(StartVar.mDollar);
//        float number = Float.parseFloat(value);
//        if (StartVar.mCurrency == 1) {    //Selector en Bs
//            number = number * precDoll;
//        }
//        return Float.toString(number);
//    }

//    @SuppressLint("DefaultLocale")
//    public static String getValueFormatter(String value) {
//        return setFormatter(getValue(value));
//    }
    public static Float floatFormat(String value) {
        String mValue = value.replaceAll("([^.\\d])", "");
        mValue = mValue.replaceAll("^.$", "0.00");

        return mValue.isEmpty() ? (float)0 : Float.parseFloat(mValue);
    }

//    public static float getDebt(int mult, String mont, String debt) {
//        mont = mont.replaceAll("([^.0-9]+)", "");
//        debt = debt.replaceAll("([^.0-9]+)", "");
//
//        float precDoll = Basic.floatFormat(StartVar.mDollar);
//        if (!mont.isEmpty() && !debt.isEmpty()) {
//            float numA = Float.parseFloat(mont);
//            float numB = Float.parseFloat(debt);
//
//            float result = numA*mult;
//
//            result -= numB;
//            return result;
//        }
//        return 0;
//    }

    public static String setMask(String value, String sing) {
        value = setFormatter(value);

        return value;
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

    public static void msg(String msg)
    {
        new Handler(Looper.getMainLooper()).post(() -> {
            TextView text = new TextView(mContex);
            // Se ajustan los parametros del Texto ----------------------------------
            text.setText(msg);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setGravity(Gravity.CENTER);
            text.setWidth(R.dimen.spinner_w1);
            //text.setHeight(R.dimen.spinner_h1);

            text.setMaxLines(2);
            text.setTextColor(ContextCompat.getColor(text.getContext(), R.color.inner_button));
            text.setBackgroundColor(ContextCompat.getColor(text.getContext(), R.color.text_background2));
            text.setTextSize(18);
            //text.setTextAppearance(R.style.Theme_CowData);

            text.setPadding(10,5,10,5);

            CardView cardView = new CardView(mContex);
            cardView.setLayoutParams(new GridLayout.LayoutParams(spec(140), spec(150)));
            cardView.addView(text);
            cardView.setRadius(10f);

            Toast mToast = new Toast(mContex);
            mToast.setDuration(Toast.LENGTH_LONG);
            mToast.setView(cardView);

            mToast.show();
        });

    }
    public static void checkClt(){

    }

    public static int bitL(int val, int rota) {
        return val << rota;
    }

    public static int bitR(int val, int rota) {
        return (val >> rota) & 1;
    }

    public static int toHex(String value) {
        return Integer.decode(value);
    }

    public static List<Integer> getBits(int n) {
        List<Integer> list = new ArrayList<>();
        while (true) {
            if(n < 32){
                list.add(bitL(0x1, n));
                break;
            }
            else{
                list.add(0);
                n -= 32;
            }
        }
        return list;
    }

    public static List<Integer> getBits(String text) {
        String[] sList = text.split("'");
        int n = 0;
        List<Integer> list = new ArrayList<>();
        for (String val: sList){
            list.add(Integer.decode(val));
        }
        return list;
    }

    public static String saveBits(List<Integer> list) {
        String text = "";
        for (Integer val: list){
            text = String.format("%x",val);
            if(!text.startsWith("0x")){
                text = "0x"+text;
            }
            //msg(text);
        }
        return text;
    }
    public static String saveNewBit(int r){
        String bit = "";
        for(int i = 0 ; i < r; i += 32) {
            if(r < 32) {
                bit = String.format("0x%x", Basic.bitL(0x1, r))+"'";
            }
            else{
                r -= 32;
            }
        }
        return bit;
    }

    public static String parseMoneyValue(String value, String groupingSeparator, String currencySymbol) {
        return value.replace(groupingSeparator, "").replace(currencySymbol, "");
    }

    public static boolean isLollipopAndAbove() {
        return true;
    }
}