package com.example.cow_data;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalcActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mInput1;
    private EditText mInput2;
    private TextView mText1;
    private TextView mText2;
    private Button mButt1;
    private Button mButt2;

    // Para el selector  Fecha de Referencia--------------------------------------------
    private Spinner mSpin1;
    private int currSel1 = 0;
    private List<String> mSpinL1 = Arrays.asList("DE HOY", "Personalizada");
    private String mCustonDate = "";
    //-----------------------------------------------------------------------

    // Para el selector de Fechas--------------------------------------------
    private Spinner mSpin2;
    private int currSel2 = 0;
    private List<String> mSpinL2 = Arrays.asList("Dias", "Meses", "Años", "año/mes/dia");
    //-----------------------------------------------------------------------


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Se configura el Boton nav Back -----------------------------------------------
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                CalcActivity.this.finish();
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);
        //---------------------------------------------------------------------------------

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calc);

        //Activate ToolBar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Calculadora de Fechas");
        actionBar.setDisplayShowHomeEnabled(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mInput1 = findViewById(R.id.txEdit1);
        mInput2 = findViewById(R.id.txEdit2);
        mText1 = findViewById(R.id.textRes1);
        mText2 = findViewById(R.id.textRes2);
        mButt1 = findViewById(R.id.buttCopy1);
        mButt2 = findViewById(R.id.buttCopy2);

        mSpin1 = findViewById(R.id.spinFecha);
        mSpin2 = findViewById(R.id.spinCalc);

        mButt1.setOnClickListener(this);
        mButt2.setOnClickListener(this);


        //Para el input de Fecha Referencia -----------------------------------------------------

        mInput1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {

                //Set dinnamy filtre for input text
                inputDataFiltre( mInput1.getText().toString(), mInput1);

                //Se comprueba el imput de fecha Personalizada-------------------------------------------
                String mDate =  CalcCalendar.isDateFormat(mInput1.getText().toString());
                mCustonDate = CalcCalendar.getFormatDateEN(mDate);

                if(!mCustonDate.isEmpty()) {
                    //Upedate Date Calc
                    dateCalcPlusMinus(mInput2.getText().toString());
                }
                else{
                    clearViews();
                }
                //-----------------------------------------------------------------------------
            }
        });

        mInput1.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String mTxInput = textView.getText().toString();
                if (currSel1 == 1) {
                    if(CalcCalendar.isDateFormat(mTxInput).isEmpty()){
                        Basic.msg("Formato de FECHA incorrecto!.");
                        textView.setError("Fecha Incorrecta!.");
                        return true;
                    }
                }
                else if (currSel1 == 3) {
                    String[] dateList = CalcCalendar.dataValidate(mTxInput);
                    if (dateList == null || dateList.length < 2) {
                        Basic.msg("Formato incorrecto!.");
                        textView.setError("Formato Incorrecta!.");
                        return true;
                    }
                }
                return false;
            }
        });

        mInput1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                String mTxInput = mInput1.getText().toString();
                if(!b){
                    if (currSel1 == 1) {
                        if (CalcCalendar.isDateFormat(mTxInput).isEmpty()) {
                            Basic.msg("Formato de FECHA incorrecto!.");
                            mInput1.setError("Fecha Incorrecta!.");
                        }
                    }
                    else if (currSel1 == 3) {
                        String[] dateList = CalcCalendar.dataValidate(mTxInput);
                        if (dateList == null || dateList.length < 2) {
                            Basic.msg("Formato incorrecto!.");
                            mInput1.setError("Formato Incorrecta!.");
                        }
                    }
                }
            }
        });


        mInput2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String mTxInput = textView.getText().toString();
                if (currSel2 == 3) {
                    String[] dateList = CalcCalendar.dataValidate(mTxInput);
                    if (dateList == null || dateList.length < 2) {
                        Basic.msg("Formato incorrecto!.");
                        textView.setError("Formato Incorrecta!.");
                        return true;
                    }
                }
                return false;
            }
        });

        mInput2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                String mTxInput = mInput2.getText().toString();
                if(!b){
                    if (currSel2 == 3) {
                        String[] dateList = CalcCalendar.dataValidate(mTxInput);
                        if (dateList == null || dateList.length < 2) {
                            Basic.msg("Formato incorrecto!.");
                            mInput2.setError("Formato Incorrecta!.");
                        }
                    }
                }
            }
        });

        //---------------------------------------------------------------------------------------------

        mInput2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                String mText = mInput2.getText().toString();
                if (!mText.isEmpty()) {

                    //Set dinnamy filtre for input text
                    inputDataFiltre( mText, mInput2);

                    //Upedate Date Calc
                    dateCalcPlusMinus(mText);
                }
                else{
                    //Para Sumar
                    mText1.setText("");
                    //Para Restar
                    mText2.setText("");
                }
            }
        });

        //Para la lista del selector de Fecha Referencia ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL1);
        mSpin1.setAdapter(adapter);
        mSpin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currSel1 = i;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    LocalDate currdate = LocalDate.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(StartVar.mDateFormES);

                    if (currSel1 == 0){
                        mInput1.setText(currdate.format(formatter));
                        mInput1.setEnabled(false);
                    }
                    else {
                        mInput1.setText("");
                        mInput1.setEnabled(true);
                    }
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //----------------------------------------------------------------------------------------------------

        //Para la lista del selector de Valores a Calcular ----------------------------------------------------------------------------------------------
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL2);
        mSpin2.setAdapter(adapter);
        mSpin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currSel2 = i;
                if(currSel2 == 3) {
                    mInput2.setHint("año/mes/dia");
                    mInput2.setInputType(InputType.TYPE_CLASS_DATETIME);
                }
                else{
                    mInput2.setHint("Ingrese Valor");
                    mInput2.setInputType(InputType.TYPE_CLASS_NUMBER);
                }

                //Upedate Date Calc
                dateCalcPlusMinus(mInput2.getText().toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //----------------------------------------------------------------------------------------------------
    }

    @Override
    public void onClick(View v) {
        int itemId = v.getId();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (itemId == R.id.buttCopy1) {
            String mText = mText1.getText().toString();
            if(!mText.isEmpty()) {
                ClipData clipData = ClipData.newPlainText("Clip Data", mText);
                clipboard.setPrimaryClip(clipData);
                Basic.msg("FECHA Copiada al Portapapeles!.");
            }
            else{
                Basic.msg("El campo de FECHA esta VACIO!");
            }
        }

        if (itemId == R.id.buttCopy2) {
            String mText = mText2.getText().toString();
            if(!mText.isEmpty()) {
                ClipData clipData = ClipData.newPlainText("Clip Data", mText);
                clipboard.setPrimaryClip(clipData);
                Basic.msg("FECHA Copiada al Portapapeles!.");
            }
            else{
                Basic.msg("El campo de FECHA esta VACIO!");
            }
        }
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void dateCalcPlusMinus(String mText){
        DateTimeFormatter formatter = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            formatter = DateTimeFormatter.ofPattern(StartVar.mDateFormES);

            String myParse = mText.replaceAll("\\D", "");
            long vlresult = Long.parseLong(myParse.isEmpty()?"0" : myParse);

            LocalDate currdate = currSel1 == 0 || mCustonDate.isEmpty()? LocalDate.now() : LocalDate.parse(mCustonDate);

            //Dias
            if (currSel2 == 0) {
                //Para Sumar
                mText1.setText(currdate.plusDays(vlresult).format(formatter));
                //Para Restar
                mText2.setText(currdate.minusDays(vlresult).format(formatter));
            }
            //Meses
            else if (currSel2 == 1) {
                //Para Sumar
                mText1.setText(currdate.plusMonths(vlresult).format(formatter));
                //Para Restar
                mText2.setText(currdate.minusMonths(vlresult).format(formatter));
            }
            //Años
            else if (currSel2 == 2){
                //Para Sumar
                mText1.setText(currdate.plusYears(vlresult).format(formatter));
                //Para Restar
                mText2.setText(currdate.minusYears(vlresult).format(formatter));
            }
            //Para Formato de fecha
            else {
                String[] dateList = CalcCalendar.dataValidate(mText);
                if (dateList != null && dateList.length > 1) {

                    //Para Sumar
                    LocalDate fromPlus = currdate.plusYears(Long.parseLong(dateList[0]));
                    fromPlus = fromPlus.plusMonths(Long.parseLong(dateList[1]));
                    fromPlus = fromPlus.plusDays(Long.parseLong(dateList[2]));
                    mText1.setText(fromPlus.format(formatter));

                    //Para Restar
                    LocalDate fromMinus = currdate.minusYears(Long.parseLong(dateList[0]));
                    fromMinus = fromMinus.minusMonths(Long.parseLong(dateList[1]));
                    fromMinus = fromMinus.minusDays(Long.parseLong(dateList[2]));
                    mText2.setText(fromMinus.format(formatter));
                }
                else{
                    clearViews();
                }
            }
        }
    }

    public void clearViews(){
        mText1.setText("");
        mText2.setText("");
    }

    public void inputDataFiltre(String mText, EditText mInput){
        Pattern patt = Pattern.compile("(^\\D+)");
        Matcher m = patt.matcher(mText);
        String copyTx = "";
        if(m.find()) {
            String gr = m.group(1);
            //Basic.msg("-> "+gr);
            assert gr != null;
            copyTx = mText.replaceFirst(gr, "");
            mInput.setText(copyTx);
        }

        patt = Pattern.compile("(\\D{2,})");
        m = patt.matcher(mText);
        if(m.find()) {
            String gr = m.group(1);
            //Basic.msg("-> "+gr);
            assert gr != null;
            //Basic.msg(s.toString().replaceAll(gr, "-"));
            copyTx = mText.replaceAll(gr, "-");
            copyTx = copyTx.replaceAll("^\\D", "");
            mInput.setText(copyTx);
            mInput.setSelection(copyTx.length());
        }

        patt = Pattern.compile("(\\d)((\\D)(\\d+|$)){3,}");
        m = patt.matcher(mText);
        if(m.find()) {
            //String gr = m.group(1);
            //Basic.msg("-> "+gr);
            //assert gr != null;
            copyTx = mText.replaceFirst("\\D+$", "");
            mInput.setText(copyTx);
            mInput.setSelection(copyTx.length());
        }
    }

}