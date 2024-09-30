package com.example.cow_data;

import static android.service.controls.ControlsProviderService.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;

import com.example.cow_data.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddActivity extends AppCompatActivity implements View.OnClickListener{
    private ActivityMainBinding binding;
    private static final int REQUEST_PERMISSION_CAMERA = 100;
    private static final int STORAGE_PERMISSION_CODE = 23;
    private ImageButton mBtnCam;
    private ImageView mImgPrev;

    //Todos los Inputs
    private EditText mInput1;
    private EditText mInput2;
    private EditText mInput3;
    private EditText mInput4;

    private Spinner mSpin;

    private Button mBtnAdd;
    private CoordinatorLayout mLayout;

    private List<String> mList = new ArrayList<>();
    private List<TextView> mInputList = new ArrayList<>();

    public AppDatabase appDatabase;

    private String sImage = "";
    private String mIndex = "";
    private Uri oldFile = null;
    private Uri currUri = null;

    TextView mtextCode;

    // Para guardar los permisos de app comprobados en main
    private boolean mPermiss = false;

    // Classs para la gestion de archivos
    FilesManager fmang = new FilesManager();

    //Nombre de data Base
    private   String nameDB = "";

    // Para el selector de edades--------------------------------------------
    private int currSelec = 0;
    private List<String> mSpinList = Arrays.asList("Años", "Meses", "Dias", "d-m-a");
    //-----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent mIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mIntent);
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setContentView(R.layout.activity_add);

        //Activate ToolBar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Agregar Mas a la Lista");
        actionBar.setDisplayShowHomeEnabled(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mBtnCam = findViewById(R.id.buttCam);
        mImgPrev = findViewById(R.id.imgPrev);

        mInput1 = findViewById(R.id.InputData1);
        mInput2 = findViewById(R.id.InputData2);
        mInput3 = findViewById(R.id.InputData3);
        mInput4 = findViewById(R.id.inputData4);
        mSpin = findViewById(R.id.spinAddEdad);

        mBtnAdd = findViewById(R.id.buttAdd);
        mLayout = findViewById(R.id.layout1);

        mtextCode = findViewById(R.id.texCode);

        mBtnCam.setOnClickListener(this);
        mBtnAdd.setOnClickListener(this);

        mInputList.add(mInput1);
        mInputList.add(mInput2);
        mInputList.add(mInput3);
        mInputList.add(mInput4);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinList);
        mSpin.setAdapter(adapter);
        mSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currSelec = i;
                if(i == 3){
                    mInput4.setInputType(InputType.TYPE_CLASS_DATETIME);
                }
                else {
                    String text = mInput4.getText().toString();
                    String[] txlist = dataValidate(text);
                    if(txlist == null){
                        Pattern patt = Pattern.compile("(\\d{1,3})$");
                        Matcher matcher = patt.matcher(text);
                        if(!matcher.find()) {
                            mInput4.setText("");
                        }
                    }
                    else {
                        mInput4.setText("0");
                    }
                    mInput4.setInputType(InputType.TYPE_CLASS_NUMBER);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            mPermiss = intent.getBooleanExtra("perm", false);
            nameDB = intent.getStringExtra("dbname");
            //Instancia de la base de datos
            appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, nameDB).allowMainThreadQueries().build();

            mIndex = "" + appDatabase.daoUser().getUsers().size();
            if (mIndex.isEmpty()) {
                mIndex = "0";
            }
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
        if (itemId == R.id.buttCam) {
            if (mPermiss){
                // Launch the photo picker and let the user choose only images.
                //fmang.FilesManager();
                pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
            }
            else{
                textSnackbar("Error Permiso Denegado!");
            }
        }

        if (itemId == R.id.buttAdd) {
            boolean result = true;
            mList.add("userID"+mIndex);
            for(int i = 0; i < mInputList.size(); i++) {
                String text = mInputList.get(i).getText().toString();
                text = text.replaceAll("\"", "");
                text = text.replaceAll(",", "");
                if (text.isEmpty()){
                    result = false;
                    break;
                }
                //Input de Edad
                if(i == 3){
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        long vlresult = currSelec==3? 0 : Long.parseLong(text);

                        //Inicia la fecha a comparra en cero
                        LocalDate date = LocalDate.of(1, 1, 1);
                        //Inicia la fecha actual
                        LocalDate currdate = LocalDate.now();

                        String res= "";

                        //Para años
                        if(currSelec == 0){
                            LocalDate from = currdate.minusYears(vlresult);
                            res = from.toString();
                        }
                        //Para meses
                        else if(currSelec == 1){
                            LocalDate from = currdate.minusMonths(vlresult);
                            res = from.toString();
                        }
                        //Para Dias
                        else if(currSelec == 2){
                            LocalDate from = currdate.minusDays(vlresult);
                            res = from.toString();
                        }
                        //Para Validar Fechas completas
                        else if(currSelec == 3){
                            String[] dateList = dataValidate(text);
                            if (dateList != null && dateList.length > 1 ) {
                                LocalDate from = currdate.minusYears(Long.parseLong(dateList[2]));
                                from = from.minusMonths(Long.parseLong(dateList[1]));
                                from = from.minusDays(Long.parseLong(dateList[0]));
                                //Log.d("PhotoPicker", "1-->>>>>>>>>>>>>>>>>>>>>>>>>>>> Experimento: "+ from.toString());
                                res = from.toString();
                            }
                            else {
                                result = false;
                                break;
                            }
                        }
                        //En caso de que no este (Dudo q pase) se toma el valor de fecha actual
                        else{
                            res = currdate.toString();
                        }
                        mList.add(res);
                        continue;
                    }
                }
                mList.add(text);
            }
            if (result) {
                //Para Limpiar Todos Los inputs
                for(int i = 0; i < mInputList.size(); i++) {
                    mInputList.get(i).setText("");
                }
                //Se guarda la foto en un nuevo directorio --------------------------------
                Bitmap bitmap = null;
                try {
                    if(!sImage.isEmpty() || currUri == null){
                        oldFile = Uri.parse(sImage);
                    }
                    else {
                        Log.d("PhotoPicker", "Aqi hayyyyyyyyyyyyy5555----------------------------------: ");
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currUri);
                        sImage = fmang.SavePhoto(bitmap, ("userID"+mIndex), oldFile, this, this.getContentResolver());
                    }
                }
                catch (IOException e) {
                    textSnackbar("Error al guardar la IMAGEN!");
                    e.printStackTrace();
                    sImage = "";
                }
                //-------------------------------------------------------------------

                Usuario obj =
                        new Usuario(
                                mList.get(0), mList.get(1), mList.get(2), mList.get(3), mList.get(4),
                                sImage, Integer.toString(currSelec), "" ,"" ,"" ,"" ,""
                            );
                appDatabase.daoUser().insetUser(obj);

                //SE Limpia la lista
                mList.clear();

                //Se vacia el archivo viejo
                oldFile = null;

                //Esto inicia las actividad Main despues de tiempo de espera del preloder
                startActivity(new Intent(AddActivity.this,MainActivity.class));
                finish(); //Finaliza la actividad y ya no se accede mas

                //mEditText.getText().clear();
                //mAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, mList);
               // mListView.setAdapter(mAdapter);
            }
            else {
                textSnackbar("La entrada esta vacia! (SIN TEXTO).");
                mList.clear();
            }

        }
    }
    private void textSnackbar(String text){
        Snackbar mySnackbar = Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT);
        mySnackbar.show();
    }

    // Registers a photo picker activity launcher in single-select mode.
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                // Callback is invoked after the user selects a media item or closes the
                // photo picker.
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: " + uri);
                    mImgPrev.setImageURI(uri);
                    currUri = uri;
                }
                else {
                    Log.d("PhotoPicker", "No media selected");
                }
            });

    public String[] dataValidate(String text){
        Pattern patt = Pattern.compile("((\\d{1,2})(/)(\\d{1,2})(/)(\\d{1,3})$)|((\\d{1,2})(-)(\\d{1,2})(-)(\\d{1,3})$)|(\\d{1,2})(\\.)(\\d{1,2})(\\.)(\\d{1,3})$");
        Matcher matcher = patt.matcher(text);
        if(matcher.find()) {
            if (text.contains("-")) {
                return text.split("-");
            }
            else if (text.contains("/")) {
                return text.split("/");
            }
            else if (text.contains(".")) {
                return text.split("\\.");
            }
            else {
                return null;
            }
        }
        return null;
    }
}