package com.example.cow_data;

import static android.service.controls.ControlsProviderService.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.SearchView;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.cow_data.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import io.reactivex.annotations.NonNull;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private ActivityMainBinding binding;
    private ExtendedFloatingActionButton mBtnNew;
    private CoordinatorLayout mLayout;
    private Spinner mSpin2;

    private List<Usuario> listuser;
    private List<String[]> totalList = new ArrayList<>();
    private ArrayList<String> typeList = new ArrayList<>();

    public AppDatabase appDatabase ;

    private static final int REQUEST_PERMISSION_CAMERA = 100;
    private static final int STORAGE_PERMISSION_CODE = 23;

    private boolean mPermiss = false;

    //---------------------------------------------------------------------
    public SearchView searchBar;
    public ListView mlv;
    private SearchAdapter mAdapter;
    private GridView gridView;
    private ArrayList<String> dirList = new ArrayList<>();
    private ArrayList<String> nameList = new ArrayList<>();
    private ArrayList<String> ltrosList = new ArrayList<>();
    private ArrayList<String> datePreList = new ArrayList<>();
    private ArrayList<String> dateBrithList = new ArrayList<>();
    private ArrayList<String> swPreList = new ArrayList<>();
    //---------------------------------------------------------------------

    // Para el selector de tipo gando--------------------------------------------
    private int currSel2 = 4;
    private List<String> mSpinL2= Arrays.asList("Vacas", "Novillas", "Becerros", "Toros", "Todos");
    //-----------------------------------------------------------------------

    // Classs para la gestion de archivos
    FilesManager fmang = new FilesManager();

    //Nombre de data Base
    public static String nameDB = "Registro2";

    public StartVar startVar;

    //Type of import for csv
    private int importType = 0;

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
        myToolbar.setTitleTextColor(ContextCompat.getColor(myToolbar.getContext(), R.color.inner_button));

        if (checkStoragePermissions()){
            mPermiss = true;
        }
        else{
            requestForStoragePermissions();
            mPermiss = checkStoragePermissions();
        }

        new Basic(getApplicationContext());



        mBtnNew = findViewById(R.id.buttNew);
        mLayout = findViewById(R.id.layout);
        gridView = findViewById(R.id.gcImg);
        searchBar = findViewById(R.id.searchBar);
        mlv = findViewById(R.id.lv);
        mSpin2 = findViewById(R.id.spinType);

        mBtnNew.setOnClickListener(this);
        gridView.setOnItemClickListener(this);
        mlv.setOnItemClickListener(this);

        //Satrted variables
        startVar = new StartVar(getApplicationContext());
        startVar.setUserListDB();
        startVar.setmPermiss(mPermiss);

        //Instancia de la base de datos
        StartVar.getUserListDB();
        listuser =  StartVar.listuser;
        dirList.clear();

        //Se agrega un indicador numerico para identificar nuevas versiones del save.csv
        totalList.add(new String[]{"2"});

        List<Integer> selList = new ArrayList<>();
        for(int i = 0; i < listuser.size(); i++) {
            // Se definen los datos de la imagen y el nombre--------
            Usuario myUser = listuser.get(i);
            String tximg = myUser.imagen;
            String txname = myUser.nombre;
            String txsel2 = myUser.sel2;
            String txsel3 = myUser.sel3;
            String txpre = myUser.pre;
            String txlitros = myUser.litros;
            String txedad = myUser.edad;

            //------------------------------------------------------
            // Se crea la lista para esportar a csv  ---------------
            String[] txList= new String[14];

            txList[0]=listuser.get(i).usuario;
            txList[1]=txname;
            txList[2]=myUser.color;
            txList[3]=myUser.litros;
            txList[4]=txedad;
            txList[5]=txpre;
            txList[6]=tximg;
            txList[7]=myUser.sel1;
            txList[8]=txsel2;
            txList[9]=txsel3;
            txList[10]=myUser.more1;
            txList[11]=myUser.more2;
            txList[12]=myUser.more3;
            txList[13]=myUser.more4;

            totalList.add(txList);

            //--------------------------------------------------------
            // Se obtine la direccion de la image,  el nombre, la listSelec etc.
            nameList.add(txname);
            ltrosList.add(txlitros);
            datePreList.add(txpre);
            dateBrithList.add(txedad);

            swPreList.add(txsel3);
            typeList.add(txsel2);

            selList.add(Integer.parseInt(txsel2));

            if ( fmang.isBlockedPath(this, tximg)) {
                dirList.add(tximg);
            }
            else{
                dirList.add("");
            }
            //------------------------------------------
        }
        startVar.setArrayList(dirList, dirList, typeList);
        if(mPermiss) {
            int mainSelec = StartVar.currSel2;
            List<String[]> mtxList = new ArrayList<>();
            for(int j = 0; j < nameList.size(); j++){
                mtxList.add(setGalleryArray(j));
            }
            gridView.setAdapter(new GalleryAdapter(MainActivity.this, mtxList));

            mAdapter = new SearchAdapter(MainActivity.this, mtxList);
            mlv.setAdapter(mAdapter);
            mlv.setVisibility(View.INVISIBLE);

            //PAra la lista del selector Tipo ganado ----------------------------------------------------------------------------------------------
            ArrayAdapter<String> adapt2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL2);
            mSpin2.setAdapter(adapt2);
            mSpin2.setSelection(mainSelec); //Set Todos como default
            mSpin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    currSel2 = i;
                    startVar.setCurrSel2(i);
                    CharSequence newText = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        newText = searchBar.getQuery();
                        mAdapter.getFilter().filter(newText);
                    }
                    ArrayList<Integer> idxList = (ArrayList<Integer>)mAdapter.getItem(0);

                   // Toast.makeText(MainActivity.this, "Siz is "+test.getUserList().size(), Toast.LENGTH_LONG).show();

                    List<String[]> mtxList = new ArrayList<>();
                    for(int ii = 0; ii < nameList.size(); ii++){
                        if(currSel2 == 4 || currSel2 == selList.get(ii)){
                            if(idxList.isEmpty()) {
                                mtxList.add(setGalleryArray(ii));
                            }
                            else {
                                for(int j =0; j < idxList.size(); j++){
                                    if(idxList.get(j) == ii){
                                        mtxList.add(setGalleryArray(ii));
                                    }
                                }
                            }
                        }
                    }
                    gridView.setAdapter(new GalleryAdapter(MainActivity.this, mtxList));
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            //Para el adapter del buscador -------------------------------------------------------
            searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    ArrayList<Integer> idxList = (ArrayList<Integer>)mAdapter.getItem(0);
                    //Toast.makeText(MainActivity.this, "---Siz is "+idxList.size(), Toast.LENGTH_LONG).show();
                    List<String[]> mtxList = new ArrayList<>();
                    for(int i = 0; i < nameList.size(); i++){
                        if(currSel2 == 4 || currSel2 == selList.get(i)){
                            if(idxList.isEmpty()) {
                                mtxList.add(setGalleryArray(i));
                            }
                            else {
                                for(int j =0; j < idxList.size(); j++){
                                    if(idxList.get(j) == i){
                                        mtxList.add(setGalleryArray(i));
                                    }
                                }
                            }
                        }
                    }
                    gridView.setAdapter(new GalleryAdapter(MainActivity.this, mtxList));

                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {

                    //Log.d("PhotoPicker", "11100------------------------: " + newText);
                    if (!newText.isEmpty()) {
                        mlv.setVisibility(View.VISIBLE);
                        mAdapter.getFilter().filter(newText);

                        //ArrayList<Integer> idxList = (ArrayList<Integer>)mAdapter.getItem(0);
                        //Toast.makeText(MainActivity.this, "Siz is "+idxList.size(), Toast.LENGTH_LONG).show();

                        //Log.d("PhotoPicker", "11100------------------------: " + indexList.size());
                    }
                    else {
                        mlv.setVisibility(View.INVISIBLE);
                    }
                    return false;
                }
            });
            //----------------------------------------------------------------------------------------
        }
        // Para eventos al mostrar o ocultar el teclado
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                // on below line we are creating a variable for rect
                Rect rect = new Rect();

                ConstraintLayout contain = findViewById(R.id.container);

                // on below line getting frame for our relative layout.
                contain.getWindowVisibleDisplayFrame(rect);

                // on below line getting screen height for relative layout.
                int screenHeight = contain.getRootView().getHeight();

                // on below line getting keypad height.
                int keypadHeight = screenHeight - rect.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    //Toast.makeText(MainActivity.this, "Keyboard is +", Toast.LENGTH_LONG).show();
                }
                else {
                    //Toast.makeText(MainActivity.this, "Keyboard is -", Toast.LENGTH_LONG).show();
                    mlv.setVisibility(View.INVISIBLE);
                }
            }
        });
        //------------------------------------------------------------------------------------------------
    }

    private String[] setGalleryArray(int idx){
        String[] stList = new String[8];
        stList[0] = dirList.get(idx);
        stList[1] = nameList.get(idx);
        stList[2] = ltrosList.get(idx);
        stList[3] = datePreList.get(idx);
        stList[4] = dateBrithList.get(idx);
        stList[5] = typeList.get(idx);
        stList[6] = swPreList.get(idx);
        stList[7] = Integer.toString(idx);
        return stList;
    }
    @SuppressLint("ResourceType")
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.summary, menu);
        getMenuInflater().inflate(R.menu.save, menu);
        getMenuInflater().inflate(R.menu.merge, menu);
        getMenuInflater().inflate(R.menu.impor, menu);

        for(int i = 0; i < menu.size(); i++){
            MenuItem item = menu.getItem(i);
//            Drawable drawable = item.getIcon();

//            if(drawable != null) {
////                drawable.mutate();
////                drawable.setColorFilter(ContextCompat.getColor(this, R.color.inner_button), PorterDuff.Mode.SRC_ATOP);
//            }

            SpannableString spannabl = new SpannableString(item.getTitle().toString());
            spannabl.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.black)),0 ,spannabl.length(),0);
            item.setTitle(spannabl);
        }
        //test.setBackgroundColor(ContextCompat.getColor(test.getContext(), R.color.purple_500));
        return true;
    }

    //Para Exportar archivo CSV
    @SuppressLint("SetWorldReadable")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        int itemId = item.getItemId();
        if (itemId == R.id.summary) {
            Intent mIntent = new Intent(this, SummaryActivity.class);
            startActivity(mIntent);
        }
        if (itemId == R.id.save) {
            try {
                File file = fmang.csvExport(totalList);

                if(file != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setType("text/comma-separated-values");

                    Log.d("Files", " --------Aquiiiiiiiiii Hayyyyyy ------------: "+ file);


                    // Se obtine la Uri , se debe modificar manidest con: android:authorities="com.example.cow_data.provider"
                    Uri fileUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", file);
                    Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy ------------------------: "+ fileUri.toString());

                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // this will not work
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION); // this will not work
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);

                    startActivity(Intent.createChooser(intent, "Enviar datos para GUARDAR"));
                }
            }
            catch (Exception e) {
                Log.d("Files", " Trace --------Aquiiiiiiiiii Hayyyyyy ------------: "+ e.getMessage());

                e.printStackTrace();
            }
        }

        if (itemId == R.id.marge) {
            Basic.msg("???");
            if (mPermiss) {
                try {
                    importType = 0;
                    String[] mimetype = {"text/csv", "text/comma-separated-values"};
                    mCsvRequest.launch(mimetype);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }


        if (itemId == R.id.impor) {
            if (mPermiss) {
                try {
                    importType = 1;
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

                    StartVar mStartVar = new StartVar(getApplicationContext());
                    mStartVar.setUserListDB();

                    appDatabase = StartVar.appDatabase;

                    // call this to persist permission across decice reboots
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    StringBuilder stringBuilder = new StringBuilder();
                    try (InputStream inputStream = getContentResolver().openInputStream(uri);
                         BufferedReader reader = new BufferedReader( new InputStreamReader(Objects.requireNonNull(inputStream)))) {
                            String line;
                            String version = "0";

                            DaoUser mDao = appDatabase.daoUser();
                            for (Usuario mUser : mDao.getUsers()){
                                mDao.removerUser(mUser.usuario);
                            }
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
                                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], "0", (f > 7 ? spl[7] : ""),
                                            "0", (f > 8 ? spl[8] : ""), (f > 9 ? spl[9] : ""), (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : "")
                                    );
                                    mDao.insetUser(obj);
                                }
                                else if(Objects.equals(version, "1")) {
                                    Usuario obj = new Usuario(
                                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], ""/*spl[5]*/, spl[5], spl[6], spl[7], "0",
                                            (f > 8 ? spl[8] : ""), (f > 9 ? spl[9] : ""), (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : "")                                    );
                                    mDao.insetUser(obj);

                                }

                                else if(Objects.equals(version, "2")) {
                                    Usuario obj = new Usuario(
                                            (importType == 0? getUserId(mDao) : spl[0]), spl[1], spl[2], spl[3], spl[4], spl[5], spl[6], spl[7], spl[8], spl[9],
                                            (f > 10 ? spl[10] : ""), (f > 11 ? spl[11] : ""), (f > 12 ? spl[12] : ""), (f > 13 ? spl[13] : "")
                                    );
                                    mDao.insetUser(obj);
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
            startActivity(mIntent);
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int itemId = parent.getId();
        if (itemId == R.id.gcImg) {
            nextViewActivity((int)id);
        }
        if (itemId == R.id.lv) {
            //Log.d("PhotoPicker", " Aquiiiiiiiiii Hayyyyyy 11100------------------------: " + position);
            nextViewActivity((int)id);
        }
    }

    public void nextViewActivity(int pos){
        Intent mIntent = new Intent(this, ViewActivity.class);
        Bundle mBundle = new Bundle();
        //Log.d("PhotoPicker", "11100------------------------: " + dirList.size());
        mBundle.putInt("index", pos);
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
    private String getUserId(DaoUser mDao){
        //Configura el nuevo index-------------------------------------------------------------------
        int mSiz = mDao.getUsers().size();
        String mIdx = "userID0";
        if(mSiz > 0) {
            mIdx = "userID" + mSiz;
        }
        for(int i = 0; i < mSiz; i++){
            Usuario mUser = mDao.getUsers("userID"+i);
            if(mUser == null){
                mIdx =  "userID"+i;
                break;
            }
        }
        Basic.msg(mIdx);
        return mIdx;
        //-------------------------------------------------------------------------------------------
    }
}