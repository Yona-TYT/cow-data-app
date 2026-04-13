package com.example.cow_data.activitys;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cow_data.AppContextProvider;
import com.example.cow_data.Basic;
import com.example.cow_data.R;
import com.example.cow_data.StartVar;
import com.example.cow_data.adapters.CheckboxAdapter;
import com.example.cow_data.adapters.GalleryAdapter;
import com.example.cow_data.adapters.SearchAdapter;
import com.example.cow_data.adapters.SelecAdapter;
import com.example.cow_data.db.AppDatabase;
import com.example.cow_data.db.Usuario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class ExcludedActivity extends AppCompatActivity {

    public AppDatabase appDatabase = StartVar.appDatabase;

    private Usuario myUser;

    private SearchView searchBar;
    private SelecAdapter mAdapter;
    private ListView listView1;
    private int currSel1 = 0;
    private List<String> mViewL1= new ArrayList<>();
    private List<String> mtxList = new ArrayList<>();
    private List<String> usrList = new ArrayList<>();

    private Button buttOk;

    // Para el selector de tipo eliminador--------------------------------------------
    private Spinner mSpin3;
    private int currSel2 = 0;
    private final List<String> mSpinL2= Arrays.asList("Disponible", "Eliminar", "Vender", "Perdida");

    private final List<String> mStatusL= Arrays.asList("Disponible", "Eliminada", "Vendida", "Perdida");

    //-----------------------------------------------------------------------


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_excluded);

        //Se configura el Boton nav Back -----------------------------------------------
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                ExcludedActivity.this.finish();
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);
        //---------------------------------------------------------------------------------

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        searchBar = findViewById(R.id.searchBar);

        buttOk = findViewById(R.id.buttOK);
        mSpin3 = findViewById(R.id.spinDel);
        listView1 = findViewById(R.id.mListV1);

        //Para el adapter del buscador -------------------------------------------------------
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        buttOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myUser != null) {
                    appDatabase.daoUser().updateStatus(
                            myUser.usuario, currSel2
                    );

                    //Encola al usuario para sincronizar
                    myUser = appDatabase.daoUser().getUsers(myUser.usuario);
                    StartVar.usuarioQueue.enqueue(myUser);


                    Intent mIntent = new Intent(AppContextProvider.getAppContext(), MainActivity.class);
                    startActivity(mIntent);
                    finish(); //Finaliza la actividad y ya no se accede mas
                }
            }
        });

        //Para la lista del selector Tipo eliminador ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapt3 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL2);
        mSpin3.setAdapter(adapt3);
        mSpin3.setSelection(currSel2);
        mSpin3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currSel2 = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //--------------------------------------------------------------------------------------------


        for (Usuario mU : StartVar.appDatabase.daoUser().getUsers(1)) {
            mtxList.add(mU.nombre+" ("+mStatusL.get(mU.sel4)+")");
            usrList.add(mU.usuario);
        }

        mAdapter = new SelecAdapter(AppContextProvider.getAppContext(), mtxList);

        listView1.setAdapter(mAdapter);
       //listView1.setVisibility(View.INVISIBLE);
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                currSel1 = i;
                myUser = appDatabase.daoUser().getUsers(usrList.get(i));
            }
        });

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

    private String[] setDbList(Usuario mUser){
        String[] stList = new String[8];
        stList[0] = mUser.usuario;
        stList[1] = mUser.nombre;
        stList[2] = mUser.litros;

        return stList;
    }
}