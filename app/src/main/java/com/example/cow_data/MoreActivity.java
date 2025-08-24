package com.example.cow_data;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class MoreActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private EditText mInput1;
    private EditText mInput2;
    private EditText mInput3;
    private EditText mInput4;

    private Button mBtnCanc;
    private Button mBtnOk;

    private ArrayList<String> morlist = new ArrayList<>();

    private int currIdx = 0;

    private Usuario myUser;

    public ListView mlv1;
    private CheckboxAdapter mAdapter1;
    public ListView mlv2;
    private CheckboxAdapter mAdapter2;
    private HashMap<String, ArrayList<String>> arrayMap = StartVar.arrayMap;


    //Base de datos
    public AppDatabase appDatabase = StartVar.appDatabase;

    @SuppressLint({"ClickableViewAccessibility", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Se configura el Boton nav Back -----------------------------------------------
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                //Guarda los datos previos de la DB
                moreSaveCanc();
                MoreActivity.this.finish();
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);
        //---------------------------------------------------------------------------------

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_more);

        //Activate ToolBar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Otros Datos");
        actionBar.setDisplayShowHomeEnabled(true);

        myToolbar.setTitleTextColor(ContextCompat.getColor(myToolbar.getContext(), R.color.inner_button));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mlv1 = findViewById(R.id.listMore1);
        mlv2 = findViewById(R.id.listMore2);


        mInput1 = findViewById(R.id.txEdit1);
        mInput2 = findViewById(R.id.txEdit2);
        mInput3 = findViewById(R.id.txEdit3);
        mInput4 = findViewById(R.id.txEdit4);

        mBtnCanc = findViewById(R.id.buttCANC);
        mBtnOk  = findViewById(R.id.buttOK);

        mBtnCanc.setOnClickListener(this);
        mBtnOk.setOnClickListener(this);
        mlv1.setOnItemClickListener(this);

        //Inizializa el Array Map
        arrayMap = new HashMap<>();
        arrayMap.put(StartVar.mId1,  new ArrayList<>());
        arrayMap.put(StartVar.mId2,  new ArrayList<>());
        StartVar.setArrayMap(arrayMap);

        // Para guardar los permisos de app comprobados en main
        boolean mPermiss = StartVar.mPermiss;

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            currIdx = intent.getIntExtra("index", 0);
            List<Usuario> listuser = StartVar.listuser;
            myUser = listuser.get(currIdx);

            if(myUser != null) {

                if (Integer.parseInt(myUser.sel2) > 1){
                    mInput1.setVisibility(View.INVISIBLE);
                }
                else {
                    checkIdsList(myUser.more1, StartVar.mId1, mInput1);
                }
                checkIdsList(myUser.more2, StartVar.mId2, mInput2);
                mInput3.setText(myUser.more3);
                mInput4.setText(myUser.more4);

                List<String[]> mtxList = new ArrayList<>();
                for (Usuario mU : listuser){
                    mtxList.add(setDbList(mU));

                }
                // Madre de: -------------------------------------------------------------------
                boolean oneValue = false;
                mAdapter1 = new CheckboxAdapter(MoreActivity.this, mtxList, oneValue, StartVar.mId1);
                mlv1.setAdapter(mAdapter1);
                mlv1.setVisibility(View.INVISIBLE);
                //--------------------------------------------------------------------------------

                // Hija de: -------------------------------------------------------------------
                oneValue = true;
                mAdapter2 = new CheckboxAdapter(MoreActivity.this, mtxList, oneValue, StartVar.mId2);
                mlv2.setAdapter(mAdapter2);
                mlv2.setVisibility(View.INVISIBLE);
                //--------------------------------------------------------------------------------
            }
            else {
                Basic.msg("El index: "+currIdx+" no existe!");
            }
        }
        else {
            Basic.msg("Aqui no hay :(");
        }
        setCheckInputs( mInput1, mlv1, StartVar.mId1, mAdapter1);
        setCheckInputs( mInput2, mlv2, StartVar.mId2, mAdapter2);
    }

    private String[] setDbList(Usuario mUser){
        String[] stList = new String[8];
        stList[0] = mUser.usuario;
        stList[1] = mUser.nombre;
        stList[2] = mUser.litros;

        return stList;
    }

    @SuppressLint("SetTextI18n")
    private void checkIdsList(String text, String mapID, EditText input){
        String[] mSplit = text.split(",");
        String textList = "";

        for (String s : mSplit){
            Usuario mU = appDatabase.daoUser().getUsers(s);
            if( mU != null){
                String name = mU.nombre.replaceAll("\\d","");
                String number = mU.nombre.replaceAll("\\D","");
                if(number.isEmpty()) {
                    textList += name.toLowerCase() + ",";
                }
                else {
                    textList += number + ",";
                }
                Objects.requireNonNull(StartVar.arrayMap.get(mapID)).add(mU.usuario);
            }
        }
        textList = textList.replaceAll(",$",";");
        input.setText(textList);
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            //Guarda los datos previos de la DB
            moreSaveCanc();
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        int itemId = view.getId();

        if (itemId == R.id.buttOK){
            morlist.clear(); //Se limpia la lista

            moreSaveArray(StartVar.mId1);
            moreSaveArray(StartVar.mId2);
            moreSaveOk(mInput3);
            moreSaveOk(mInput4);

            //Se guardan los datos de more list
            StartVar mVars = new StartVar(getApplicationContext());
            mVars.setMorlist(morlist);

            this.finish(); //Finaliza la actividad y ya no se accede mas
        }
        if (itemId == R.id.buttCANC) {
            morlist.clear(); //Se limpia la lista

            //Guarda los datos previos de la DB
            moreSaveCanc();
            this.finish(); //Finaliza la actividad y ya no se accede mas
        }
    }

    private void moreSaveOk(EditText mInput){
        String text = mInput.getText().toString();
        text = text.replaceAll("\"", "");
        text = text.replaceAll(",", "");
        morlist.add(text);
    }

    private void moreSaveArray(String mapId){
        ArrayList<String> mArray = StartVar.arrayMap.get(mapId);
        if (mArray != null) {
            String mText = "";
            for (String s : mArray) {
                mText += (s+",");
            }
            morlist.add(mText);
        }
    }

    private void moreSaveCanc(){
        if (myUser != null) {
            String text = myUser.more1;
            if(!text.isEmpty()){
                morlist.add(text);
            }
            text = myUser.more2;
            if(!text.isEmpty()){
                morlist.add(text);
            }
            text = myUser.more3;
            if(!text.isEmpty()){
                morlist.add(text);
            }
            text = myUser.more4;
            if(!text.isEmpty()){
                morlist.add(text);
            }
            //Se guardan los datos de more list
            StartVar mVars = new StartVar(getApplicationContext());
            mVars.setMorlist(morlist);
        }
    }

    private Bundle getAndSetBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("index", currIdx);
        return bundle;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    private void setCheckInputs(EditText mInput, ListView mListV, String mapID, CheckboxAdapter mAdapter){
        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                String newText = s.toString();
                //Log.d("PhotoPicker", "11100------------------------: " + newText);
                ArrayList<String> mArray = StartVar.arrayMap.get(mapID);

                if (!newText.isEmpty()) {
                    mListV.setVisibility(View.VISIBLE);

                    if(!newText.contains(";") && mArray != null){
                        int rest = newText.split(",").length-1;

                        for (int i = mArray.size()-1; i > rest ; i--){
                            StartVar.arrayMap.get(mapID).remove(i);
                        }
                        //Basic.msg(""+newText.split(",").length+"--"+boxlist.size());
                    }
                    mAdapter.getFilter().filter(newText.replaceAll("([a-z\\d,]+);",""));
                }
                else {
                    if (mArray != null){
                        StartVar.arrayMap.get(mapID).clear();
                    }
                    mListV.setVisibility(View.INVISIBLE);
                }
            }
        });

        mInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String textList = "";
                ArrayList<String> mArray = StartVar.arrayMap.get(mapID);
                if (mArray == null){
                    return false;
                }
                textList = getCheckNames(mArray);

                textList = textList.replaceAll(",$",";");
                v.setText(textList);
                v.clearFocus();
                mListV.setVisibility(View.INVISIBLE);
                return false;
            }
        });

        mInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInput.post(() -> {
                    mInput.setSelection(mInput.getText().length(), mInput.getText().length());
                });

            }
        });

        // Configurar el OnTouchListener para seleccionar/desmarcar el texto
        mInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mInput.post(() -> {
                    mInput.setSelection(mInput.getText().length(), mInput.getText().length());

                });
            }
            return false;
        });

        mInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    mInput.post(() -> {
                        mInput.setSelection(mInput.getText().length(), mInput.getText().length());

                    });
                }
            }
        });
    }

    private String getCheckNames(ArrayList<String> mArray){
        String textList = "";
        for (String s : mArray){
            Usuario mU = appDatabase.daoUser().getUsers(s);
            if(mU != null) {
                String name = mU.nombre.replaceAll("\\d","");
                String number = mU.nombre.replaceAll("\\D","");
                if(number.isEmpty()) {
                    textList += name.toLowerCase() + ",";
                }
                else {
                    textList += number + ",";
                }
            }
        }
        return textList;
    }
}