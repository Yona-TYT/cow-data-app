package com.example.cow_data;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class MoreActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mInput1;
    private EditText mInput2;
    private EditText mInput3;
    private EditText mInput4;
    private EditText mInput5;

    private Button mBtnCanc;
    private Button mBtnOk;

    private CoordinatorLayout mLayout;
    private List<TextView> mInputList = new ArrayList<>();
    private ArrayList<String> morlist = new ArrayList<>();

    private int currIdx = 0;

    //Base de datos
    public AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Se configura el Boton nav Back -----------------------------------------------
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                List<Usuario> listuser = SatrtVar.listuser;
                int i = 0;
                if (currIdx < listuser.size()) {
                    String text = listuser.get(currIdx).more1;
                    if(!text.isEmpty()){
                        morlist.add(text);
                    }
                    text = listuser.get(currIdx).more2;
                    if(!text.isEmpty()){
                        morlist.add(text);
                    }
                    text = listuser.get(currIdx).more3;
                    if(!text.isEmpty()){
                        morlist.add(text);
                    }
                    text = listuser.get(currIdx).more4;
                    if(!text.isEmpty()){
                        morlist.add(text);
                    }
                    text = listuser.get(currIdx).more5;
                    if(!text.isEmpty()){
                        morlist.add(text);
                    }
                    //Se guardan los datos de more list
                    SatrtVar mVars = new SatrtVar(getApplicationContext());
                    mVars.setMorlist(morlist);
                }

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

        mInput1 = findViewById(R.id.txEdit1);
        mInput2 = findViewById(R.id.txEdit2);
        mInput3 = findViewById(R.id.txEdit3);
        mInput4 = findViewById(R.id.txEdit4);
        mInput5 = findViewById(R.id.txEdit5);

        mBtnCanc = findViewById(R.id.buttCANC);
        mBtnOk  = findViewById(R.id.buttOK);

        mBtnCanc.setOnClickListener(this);
        mBtnOk.setOnClickListener(this);

        mInputList.add(mInput1);
        mInputList.add(mInput2);
        mInputList.add(mInput3);
        mInputList.add(mInput4);
        mInputList.add(mInput5);

        mLayout = findViewById(R.id.layout6);

        // Para guardar los permisos de app comprobados en main
        boolean mPermiss = SatrtVar.mPermiss;

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            currIdx = intent.getIntExtra("index", 0);

            List<Usuario> listuser = SatrtVar.listuser;

            int i = 0;
            if (currIdx < listuser.size()) {

                mInputList.get(i).setText(listuser.get(currIdx).more1);
                i++;
                mInputList.get(i).setText(listuser.get(currIdx).more2);
                i++;
                mInputList.get(i).setText(listuser.get(currIdx).more3);
                i++;
                mInputList.get(i).setText(listuser.get(currIdx).more4);
                i++;
                mInputList.get(i).setText(listuser.get(currIdx).more5);
            }
        }
        else {
            textSnackbar("Aqui no hay :(");
        }
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            List<Usuario> listuser = SatrtVar.listuser;
            int i = 0;
            if (currIdx < listuser.size()) {
                String text = listuser.get(currIdx).more1;
                if(!text.isEmpty()){
                    morlist.add(text);
                }
                text = listuser.get(currIdx).more2;
                if(!text.isEmpty()){
                    morlist.add(text);
                }
                text = listuser.get(currIdx).more3;
                if(!text.isEmpty()){
                    morlist.add(text);
                }
                text = listuser.get(currIdx).more4;
                if(!text.isEmpty()){
                    morlist.add(text);
                }
                text = listuser.get(currIdx).more5;
                if(!text.isEmpty()){
                    morlist.add(text);
                }
                //Se guardan los datos de more list
                SatrtVar mVars = new SatrtVar(getApplicationContext());
                mVars.setMorlist(morlist);
            }
            this.finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        int itemId = view.getId();
        if (itemId == R.id.buttOK){

            boolean result = true;
            for(int i = 0; i < mInputList.size(); i++) {
                String text = mInputList.get(i).getText().toString();
                text = text.replaceAll("\"", "");
                text = text.replaceAll(",", "");
                if (text.isEmpty()){
                    result = false;
                    break;
                }
                morlist.add(text);
            }
            if (result) {
                //Para Limpiar Todos Los inputs
                for (int i = 0; i < mInputList.size(); i++) {
                    mInputList.get(i).setText("");
                }
            }
            //Se guardan los datos de more list
            SatrtVar mVars = new SatrtVar(getApplicationContext());
            mVars.setMorlist(morlist);

            this.finish(); //Finaliza la actividad y ya no se accede mas

        }
        if (itemId == R.id.buttCANC) {
            this.finish(); //Finaliza la actividad y ya no se accede mas
        }
    }

    private Bundle getAndSetBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("index", currIdx);
        return bundle;
    }

    private void textSnackbar(String text) {
        Snackbar mySnackbar = Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT);
        mySnackbar.show();
    }

}