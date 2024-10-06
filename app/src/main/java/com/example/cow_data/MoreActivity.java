package com.example.cow_data;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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

    // Para guardar los permisos de app comprobados en main
    private boolean mPermiss = false;

    private String mIndex = "";
    private String mUser = "";
    private int currIdx = 0;


    //Base de datos
    public AppDatabase appDatabase;

    //Nombre de data Base
    private   String nameDB = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Se configura el Boton nav Back -----------------------------------------------
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent mIntent = new Intent(getApplicationContext(), EditActivity.class);
                mIntent.putExtras(getAndSetBundle());
                startActivity(mIntent);
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);
        //---------------------------------------------------------------------------------
//        Log.d("PhotoPicker", "Aquiiiiiiiiii Hayyyyyyyyyyyyyyyy1 ------------------------: ");

//        //Activate ToolBar
//        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
//        setSupportActionBar(myToolbar);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//
//        // calling the action bar
//        ActionBar actionBar = getSupportActionBar();
//        // showing the back button in action bar
//        actionBar.setDisplayHomeAsUpEnabled(true);
//        actionBar.setTitle("Modo Editar");
//        actionBar.setDisplayShowHomeEnabled(true);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_more);
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

        mPermiss = SatrtVar.mPermiss;

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            currIdx = intent.getIntExtra("index", 0);
            mIndex = ""+currIdx;

            List<Usuario> listuser = SatrtVar.listuser;

            int i = 0;
            if (currIdx < listuser.size()) {
                //Se obtiene el usuario real
                mUser = listuser.get(currIdx).usuario;

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
            Intent mIntent = new Intent(this, MainActivity.class);
            //Log.d("PhotoPicker", "Aquiiiiiiiiii Hayyyyyyyyyyyyyyyy1 ------------------------: ");
            startActivity(mIntent);
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
            Intent mIntent = new Intent(this, EditActivity.class);
            mIntent.putExtras(getAndSetBundle());
            startActivity(mIntent);
            finish(); //Finaliza la actividad y ya no se accede mas

        }
        if (itemId == R.id.buttCANC) {
            Intent mIntent = new Intent(this, EditActivity.class);
            mIntent.putExtras(getAndSetBundle());
            startActivity(mIntent);
            finish(); //Finaliza la actividad y ya no se accede mas
        }
    }

    private Bundle getAndSetBundle() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("perm", mPermiss);
        bundle.putInt("index", currIdx);
        bundle.putStringArrayList("morelist", morlist);
        bundle.putString("dbname", nameDB);

        return bundle;
    }

    private void textSnackbar(String text) {
        Snackbar mySnackbar = Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT);
        mySnackbar.show();
    }

}