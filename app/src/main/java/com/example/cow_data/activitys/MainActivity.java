package com.example.cow_data.activitys;

import static android.service.controls.ControlsProviderService.TAG;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.CheckBox;
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

import com.example.cow_data.Basic;
import com.example.cow_data.DBListCreator;
import com.example.cow_data.FilesManager;
import com.example.cow_data.SettingsActivity;
import com.example.cow_data.adapters.GalleryAdapter;
import com.example.cow_data.R;
import com.example.cow_data.adapters.SearchAdapter;
import com.example.cow_data.StartVar;
import com.example.cow_data.databinding.ActivityMainBinding;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.cow_data.db.AppDatabase;
import com.example.cow_data.db.Configdb;
import com.example.cow_data.db.DaoConf;
import com.example.cow_data.db.DaoUser;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.db.UsuarioQueue;
import com.example.cow_data.ex.GoogleDriveManager;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.ex.SetWorkResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.api.services.drive.DriveScopes;

import com.google.android.gms.common.api.Scope;

import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthState;
import io.reactivex.annotations.NonNull;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private ActivityMainBinding binding;
    private ExtendedFloatingActionButton mBtnNew;
    private CoordinatorLayout mLayout;
    private Spinner mSpin2;

    private List<Usuario> listuser;

    private static final int REQUEST_PERMISSION_CAMERA = 100;
    private static final int STORAGE_PERMISSION_CODE = 23;

    private boolean mPermiss = false;

    //---------------------------------------------------------------------
    public SearchView searchBar;
    public ListView mlv;
    private SearchAdapter mAdapter;
    private GridView gridView;
    private ArrayList<Object> nameList = new ArrayList<>();
    private ArrayList<Object> ltrosList = new ArrayList<>();
    private ArrayList<Object> datePreList = new ArrayList<>();
    private ArrayList<Object> dateBrithList = new ArrayList<>();
    private ArrayList<Object> swPreList = new ArrayList<>();
    private ArrayList<Object> typeList = new ArrayList<>();
    private ArrayList<Object> selList = new ArrayList<>();
    private ArrayList<Object> dirList = new ArrayList<>();
    //---------------------------------------------------------------------

    // Para el selector de tipo gando--------------------------------------------
    private int currSel2 = 4;
    private List<String> mSpinL2= Arrays.asList("Vacas", "Novillas", "Becerros", "Toros", "Todos");
    //-----------------------------------------------------------------------

    // Classs para la gestion de archivos
    FilesManager fmang = new FilesManager();

    public StartVar startVar;

    //Type of import for csv
    private int importType = 0;

    //ChckBox for pre estatus
    private CheckBox mCheck1;
    private boolean isPre = false;

    private Handler mainHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Basic(getApplicationContext());

        // Inicializar Handler para el hilo principal
        mainHandler = new Handler(Looper.getMainLooper());

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

        mBtnNew = findViewById(R.id.buttNew);
        mLayout = findViewById(R.id.layout);
        gridView = findViewById(R.id.gcImg);
        searchBar = findViewById(R.id.searchBar);
        mlv = findViewById(R.id.lv);
        mSpin2 = findViewById(R.id.spinType);
        mCheck1 = findViewById(R.id.check1);

        mBtnNew.setOnClickListener(this);
        gridView.setOnItemClickListener(this);
        mlv.setOnItemClickListener(this);

        //Satrted variables
        startVar = new StartVar(getApplicationContext());
        startVar.setUserListDB();
        startVar.setmPermiss(mPermiss);
        startVar.setmActivity(this);

        File dbFile = new File(getApplicationContext().getDatabasePath(StartVar.nameDBconf).getPath());
        Log.d(TAG, "Ruta de la base de datos: " + dbFile.getAbsolutePath());

       // Basic.msg(dbFile.getAbsolutePath());

//        Configdb mConfig = StartVar.configDatabase.daoConf().getUsers(StartVar.mConfID);
//        if(mConfig == null){
//            Basic.msg("Aqui no hay :(");
//        }
//        else {
//            Basic.msg("Aqui hay !! "+mConfig.hexid);
//        }

        //Instancia de la base de datos
        StartVar.getUserListDB();
        listuser =  StartVar.listuser;
        dirList.clear();

        // Inicializar la cola
        StartVar.usuarioQueue = new UsuarioQueue(StartVar.mLifecycle, getApplicationContext());

        // Obtener usuarios de Room y encolarlos
        List<Usuario> testusuarios = StartVar.appDatabase.usuarioDao().getAllUsuarios();
        // Crear y encolar un usuario individual
        //Usuario mUser = new Usuario("fileId123", "Azul", "2025-09-03", "extra");
//        usuarioQueue.enqueue( testusuarios.get(0));


        //Basic.msg("Aquuuuuuuuiiiiii Hayyyyyyyy !: "+listuser.size());

        //Test
        HashMap<String, ArrayList<Object>> arrayMap = DBListCreator.createList();

        if(StartVar.makeUpdate){
            GoogleDriveManager manager = new GoogleDriveManager(PreferenceHelper.getInstance());
            manager.uploadDataBase();
            StartVar.makeUpdate = false;
        }

        //--------------------------------------------------------
        // Se obtine la direccion de la image,  el nombre, la listSelec etc.
        nameList = arrayMap.get("name");
        ltrosList = arrayMap.get("lts");
        datePreList = arrayMap.get("datePre");
        dateBrithList = arrayMap.get("dateBrith");
        swPreList = arrayMap.get("swPre");
        typeList = arrayMap.get("type");
        selList = arrayMap.get("select");
        dirList = arrayMap.get("img");

        startVar.setArrayList(dirList, dirList, typeList);

        if(mPermiss) {
            int mainSelec = StartVar.currSel2;
            List<String[]> mtxList = new ArrayList<>();
            for(int j = 0; j < nameList.size(); j++){
                if(isPre){
                    if(swPreList.get(j).equals("1")){
                        mtxList.add(setGalleryArray(j));
                    }
                }
                else {
                    mtxList.add(setGalleryArray(j));
                }
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

                    //Desactiva el CheckBox cuando no es vaca o novilla--------------------------------
                    if(currSel2 == 2 || currSel2 == 3) {
                        mCheck1.setVisibility(View.INVISIBLE);
                        isPre = false;
                    }
                    else {
                        mCheck1.setVisibility(View.VISIBLE);
                        isPre = mCheck1.isChecked();
                    }

                    List<String[]> mtxList = new ArrayList<>();
                    for(int ii = 0; ii < nameList.size(); ii++){
                        if(currSel2 == 4 || currSel2 == (Integer)selList.get(ii)){
                            if(idxList.isEmpty()) {
                                if(isPre){
                                    if(swPreList.get(ii).equals("1")){
                                        mtxList.add(setGalleryArray(ii));
                                    }
                                }
                                else {
                                    mtxList.add(setGalleryArray(ii));
                                }                            }
                            else {
                                for(int j =0; j < idxList.size(); j++){
                                    if(idxList.get(j) == ii){
                                        if(isPre){
                                            if(swPreList.get(ii).equals("1")){
                                                mtxList.add(setGalleryArray(ii));
                                            }
                                        }
                                        else {
                                            mtxList.add(setGalleryArray(ii));
                                        }                                    }
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

            //PAra CheckBox de estatus pre ------------------------------------------------------------
            mCheck1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(v.getId() == R.id.check1){
                        isPre = !isPre;
                        List<String[]> mtxList = new ArrayList<>();
                        for(int j = 0; j < nameList.size(); j++){
                            if(isPre){
                                if(swPreList.get(j).equals("1")){
                                    mtxList.add(setGalleryArray(j));
                                }
                            }
                            else {
                                mtxList.add(setGalleryArray(j));
                            }
                        }
                        gridView.setAdapter(new GalleryAdapter(MainActivity.this, mtxList));
                    }
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
                        if(currSel2 == 4 || currSel2 == (Integer)selList.get(i)){
                            if(idxList.isEmpty()) {
                                if(isPre){
                                    if(swPreList.get(i).equals("1")){
                                        mtxList.add(setGalleryArray(i));
                                    }
                                }
                                else {
                                    mtxList.add(setGalleryArray(i));
                                }
                            }
                            else {
                                for(int j =0; j < idxList.size(); j++){
                                    if(idxList.get(j) == i){
                                        if(isPre){
                                            if(swPreList.get(i).equals("1")){
                                                mtxList.add(setGalleryArray(i));
                                            }
                                        }
                                        else {
                                            mtxList.add(setGalleryArray(i));
                                        }
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

    /**
     * Copia un mensaje al portapapeles.
     * @param text Texto a copiar.
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("MainActivity Error", text);
        clipboard.setPrimaryClip(clip);
        Log.d(TAG, "Mensaje copiado al portapapeles: " + text);
    }

    /**
     * Muestra un Toast en la UI.
     * @param message Mensaje a mostrar.
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    //=====================================================================================================

    private String[] setGalleryArray(int idx){
        String[] stList = new String[8];
        stList[0] = (String)dirList.get(idx);
        stList[1] = (String)nameList.get(idx);
        stList[2] = (String)ltrosList.get(idx);
        stList[3] = (String)datePreList.get(idx);
        stList[4] = (String)dateBrithList.get(idx);
        stList[5] = (String)typeList.get(idx);
        stList[6] = (String)swPreList.get(idx);
        stList[7] = Integer.toString(idx);
        return stList;
    }
    @SuppressLint("ResourceType")
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.calc, menu);
        getMenuInflater().inflate(R.menu.summary, menu);
        getMenuInflater().inflate(R.menu.save, menu);
        getMenuInflater().inflate(R.menu.impor, menu);
        getMenuInflater().inflate(R.menu.merge, menu);
        getMenuInflater().inflate(R.menu.sync, menu);

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

        if (itemId == R.id.calc) {
            Intent mIntent = new Intent(this, CalcActivity.class);
            startActivity(mIntent);
        }

        if (itemId == R.id.summary) {
            Intent mIntent = new Intent(this, SummaryActivity.class);
            startActivity(mIntent);
        }

        if (itemId == R.id.save) {

            try {
                //Si el nombre esta en blanco sera renombrado internamente

                java.io.File file = fmang.csvExport(StartVar.csvList, "");

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

        if (itemId == R.id.marge) {
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
        if (itemId == R.id.sync) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
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

                    DBListCreator.cvsToDB(this, uri, importType, "Datos importados correctamente!");

                    GoogleDriveManager  manager = new GoogleDriveManager(PreferenceHelper.getInstance());
                    //ExecutorService executorService = Executors.newSingleThreadExecutor();
                    //SetWorkResult mWorkResult = new SetWorkResult(this, executorService, manager);

                    AuthState authState = new AuthState();
                    authState = GoogleDriveManager.getAuthState();
                    if(authState.isAuthorized()) {
                        File mFile = null;
                        try {
                            mFile = FilesManager.getFileFromUri(StartVar.mContex, uri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            mFile = FilesManager.getNewFile(mFile.getAbsolutePath(), "DataSave.csv", StartVar.mContex);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if(mFile != null){
                            Basic.msg("" + mFile.exists() + "" + mFile.getName());
                            manager.ImportDataToDrive(mFile);
                        }
                    }
//                    Intent mIntent = new Intent(this, MainActivity.class);
//                    startActivity(mIntent);
//                    this.finish();

                }
                else {
                    Basic.msg("request denied by user");
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
