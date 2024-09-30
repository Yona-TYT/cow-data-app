package com.example.cow_data;

import static android.service.controls.ControlsProviderService.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView;

import com.airbnb.lottie.BuildConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.room.Room;

import com.example.cow_data.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.FileOutputStream;
import java.util.Objects;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import io.reactivex.annotations.NonNull;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private ActivityMainBinding binding;
    private ExtendedFloatingActionButton mBtnNew;
    private CoordinatorLayout mLayout;
    private Spinner mSpin2;

    private List<Usuario> listuser;
    private List<String[]> totalList = new ArrayList<>();

    public AppDatabase appDatabase;

    private static final int REQUEST_PERMISSION_CAMERA = 100;
    private static final int STORAGE_PERMISSION_CODE = 23;

    private boolean mPermiss = false;

    //---------------------------------------------------------------------
    public SearchView searchBar;
    public ListView mlv;
    private SearchAdapter mAdapter;
    public GridView gridView;
    public ArrayList<String> dirList = new ArrayList<>();
    public ArrayList<String> textList = new ArrayList<>();
    //---------------------------------------------------------------------

    // Para el selector de tipo gando--------------------------------------------
    private int currSel2 = 4;
    private List<String> mSpinL2= Arrays.asList("Vacas", "Novillas", "Becerros", "Toros", "Todos");
    //-----------------------------------------------------------------------

    // Classs para la gestion de archivos
    FilesManager fmang = new FilesManager();

    //Nombre de data Base
    public static String nameDB = "Registro2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Activate ToolBar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the text in action bar
        actionBar.setTitle("Inicio");
        actionBar.setDisplayShowHomeEnabled(true);

        if (checkStoragePermissions()){
            mPermiss = true;
        }
        else{
            requestForStoragePermissions();
            mPermiss = checkStoragePermissions();
        }

        mBtnNew = findViewById(R.id.buttNew);
        mLayout = findViewById(R.id.layout);
        gridView = findViewById(R.id.gcImg);
        searchBar = findViewById(R.id.searchBar);
        mlv = findViewById(R.id.lv);
        mSpin2 = findViewById(R.id.spinType);

        mBtnNew.setOnClickListener(this);
        gridView.setOnItemClickListener(this);
        mlv.setOnItemClickListener(this);

        //Instancia de la base de datos
        appDatabase = Room.databaseBuilder( getApplicationContext(), AppDatabase.class, nameDB).allowMainThreadQueries().build();
        listuser =  appDatabase.daoUser().getUsers();
        dirList.clear();

        //Se agrega un indicador numerico para identificar nuevas versiones del save.csv
        totalList.add(new String[]{"1"});

        List<List> mlist = new ArrayList<>();

        List<Integer> selList = new ArrayList<>();
        for(int i = 0; i < listuser.size(); i++) {
            // Se definen los datos de la imagen y el nombre--------
            String tximg = listuser.get(i).imagen;
            String txname = listuser.get(i).nombre;
            String txsel = listuser.get(i).sel2;

            //------------------------------------------------------
            // Se crea la lista para esportar a csv  ---------------
            String[] txList= new String[13];

            txList[0]=listuser.get(i).usuario;
            txList[1]=txname;
            txList[2]=listuser.get(i).color;
            txList[3]=listuser.get(i).litros;
            txList[4]=listuser.get(i).edad;
            txList[5]=tximg;
            txList[6]=listuser.get(i).sel1;
            txList[7]=txsel;
            txList[8]=listuser.get(i).more1;
            txList[9]=listuser.get(i).more2;
            txList[10]=listuser.get(i).more3;
            txList[11]=listuser.get(i).more4;
            txList[12]=listuser.get(i).more5;

            totalList.add(txList);
            //--------------------------------------------------------

            // Se obtine la direccion de la image,  el nombre y la listSelec
            textList.add(txname);
            selList.add(Integer.parseInt(txsel));

            if ( fmang.isBlockedPath(this, tximg)) {
                dirList.add(tximg);
            }
            else{
                dirList.add("");
            }
            //------------------------------------------
        }

        if(mPermiss) {
            mAdapter = new SearchAdapter(MainActivity.this, textList);
            mlv.setAdapter(mAdapter);
            mlv.setVisibility(View.INVISIBLE);

            List<String[]> mtxList = new ArrayList<>();
            for(int j =0; j < textList.size(); j++){
                String[] stList= new String[2];
                stList[0] = textList.get(j);
                stList[1] = dirList.get(j);
                mtxList.add(stList);
            }
            gridView.setAdapter(new GalleryAdapter(MainActivity.this, mtxList));

            //PAra la lista del selector Tipo ganado ----------------------------------------------------------------------------------------------
            ArrayAdapter<String> adapt2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL2);
            mSpin2.setAdapter(adapt2);
            mSpin2.setSelection(4); //Set Todos como default
            mSpin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    currSel2 = i;
                    List<String[]> mtxList = new ArrayList<>();
                    for(int j =0; j < textList.size(); j++){
                        if(currSel2 == 4 || currSel2 == selList.get(j)){
                            String[] stList= new String[2];
                            stList[0] = textList.get(j);
                            stList[1] = dirList.get(j);
                            mtxList.add(stList);
                        }
                    }
                    gridView.setAdapter(new GalleryAdapter(MainActivity.this, mtxList));
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            //--------------------------------------------------------------------------------------------

            searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    //Log.d("PhotoPicker", "11100------------------------: " + newText);
                    if (!newText.isEmpty()) {
                        mlv.setVisibility(View.VISIBLE);
                        mAdapter.getFilter().filter(newText);
                    }
                    else {
                        mlv.setVisibility(View.INVISIBLE);
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.save, menu);
        getMenuInflater().inflate(R.menu.impor, menu);

        return true;
    }

    //Para Exportar archivo CSV
    @SuppressLint("SetWorldReadable")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        int itemId = item.getItemId();
        if (itemId == R.id.save) {
            try {
                File file = fmang.csvExport(totalList);
                if(file != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setType("text/comma-separated-values");
                    // Se obtine la Uri , se debe modificar manidest con: android:authorities="com.example.cow_data.provider"
                    Uri fileUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", file);
                    // Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: "+ fileUri.toString());

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // this will not work
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION); // this will not work
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);

                    startActivity(Intent.createChooser(intent, "Enviar datos para GUARDAR"));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (itemId == R.id.impor) {
            if (mPermiss) {
                try {
                    String[] mimetype = {"text/csv", "text/comma-separated-values"};
                    mCsvRequest.launch(mimetype);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    //Para importar archivos CSV
    private final ActivityResultLauncher<String[]> mCsvRequest = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    // call this to persist permission across decice reboots
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    StringBuilder stringBuilder = new StringBuilder();
                    try (InputStream inputStream = getContentResolver().openInputStream(uri);
                         BufferedReader reader = new BufferedReader( new InputStreamReader(Objects.requireNonNull(inputStream)))) {
                            String line;
                            String version = "0";
                            while ((line = reader.readLine()) != null) {
                                line = line.replaceAll("\"", "");
                                String[] spl = line.split(",");
                                //Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: "+ line);
                                int f = spl.length;
                                if(f<2){
                                    version = spl[0];
                                    continue;
                                }
                                if(Objects.equals(version, "0")) {
                                    Usuario obj = new Usuario(
                                            spl[0], spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], "0", (f > 7 ? spl[7] : ""),
                                            (f > 8 ? spl[8] : ""), (f > 9 ? spl[9] : ""), (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : "")
                                    );
                                    appDatabase.daoUser().insetUser(obj);
                                }
                                else if(Objects.equals(version, "1")) {
                                    Usuario obj = new Usuario(
                                            spl[0], spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], spl[7], (f > 8 ? spl[8] : ""),
                                            (f > 9 ? spl[9] : ""), (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : ""), (f > 12 ? spl[12] : "")
                                    );
                                    appDatabase.daoUser().insetUser(obj);
                                }

                                stringBuilder.append(line);
                            }
                            Intent mIntent = new Intent(this, MainActivity.class);
                            startActivity(mIntent);
                            this.finish();
                    }
                    catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    // request denied by user
                }
            }
    );

    @Override
    public void onClick(View view) {
        int itemId = view.getId();
        if (itemId == R.id.buttNew) {
            Intent mIntent = new Intent(this, AddActivity.class);
            Bundle mBundle = new Bundle();
            mBundle.putBoolean("perm", mPermiss);
            mBundle.putString("dbname", nameDB);
            mIntent.putExtras(mBundle);
            startActivity(mIntent);
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int itemId = parent.getId();
        if (itemId == R.id.gcImg) {
            nextViewActivity(position);
        }
        if (itemId == R.id.lv) {
            //Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy 11100------------------------: " + position);
            nextViewActivity(position);
        }
    }

    public void nextViewActivity(int pos){
        Intent mIntent = new Intent(this, ViewActivity.class);
        Bundle mBundle = new Bundle();
        //Log.d("PhotoPicker", "11100------------------------: " + dirList.size());
        mBundle.putInt("pos", pos);
        mBundle.putStringArrayList("dlist", dirList);
        mBundle.putStringArrayList("tlist", textList);
        mBundle.putInt("index", pos);
        mBundle.putBoolean("perm", mPermiss);
        mBundle.putString("dbname", nameDB);

        mIntent.putExtras(mBundle);
        startActivity(mIntent);
    }

    private void textSnackbar(String text){
        Snackbar mySnackbar = Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT);
        mySnackbar.show();
    }

    private boolean checkStoragePermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){

            //Android is 11 (R) or above
            if (Environment.isExternalStorageManager()){
              //  Log.d("PhotoPicker", " Permiso Aquiiiiiiiiii Hayyyyyy 11100------------------------: " );
                return true;
            }
            else {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivityIfNeeded(intent, 101);
                    return true;
                }
                catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    startActivityIfNeeded(intent, 101);
                    return true;
                }
            }
        }
        else {
            //Below android 11
            int write = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);

            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
        }
    }

    private ActivityResultLauncher<Intent> storageActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>(){
                        @Override
                        public void onActivityResult(ActivityResult o) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                                //Android is 11 (R) or above
                                if(Environment.isExternalStorageManager()) {
                                    //Manage External Storage Permissions Granted
                                    Log.d(TAG, "onActivityResult: Manage External Storage Permissions Granted");
                                }
                                else {
                                    Toast.makeText(MainActivity.this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });


    void requestForStoragePermissions() {
        //Android is 11 (R) or above
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            }catch (Exception e){
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }
        else{
            //Below android 11
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE
            );
        }

    }
}