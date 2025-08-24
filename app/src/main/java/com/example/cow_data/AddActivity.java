package com.example.cow_data;

import static android.service.controls.ControlsProviderService.TAG;

import android.Manifest;
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.core.content.FileProvider;

import com.example.cow_data.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AddActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener{
    private ActivityMainBinding binding;

    // DB
    private AppDatabase appDatabase = StartVar.appDatabase;

    private static final int STORAGE_PERMISSION_CODE = 23;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private boolean mPermiss = false;
    private boolean mCamPermiss = false;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> selectPictureLauncher;
    private Uri photoUri;

    private ImageButton mBtnCam;
    private ImageView mImgPrev;

    //Todos los Inputs
    private EditText mInput1;
    private EditText mInput2;
    private EditText mInput3;
    private EditText mInput4;
    private EditText mInput5;

    private Spinner mSpin1;
    private Spinner mSpin2;

    private SwitchMaterial mSw1;
    private boolean swPre = false;

    private ExtendedFloatingActionButton mBtnAdd;

    private List<String> mList = new ArrayList<>();
    private List<TextView> mInputList = new ArrayList<>();

    private String sImage = "";
    private String mIndex = "";
    private Uri oldFile = null;
    private Uri currUri = null;

    // Classs para la gestion de archivos
    FilesManager fmang = new FilesManager();

    // Para el selector de edades--------------------------------------------
    private int currSel1 = 0;
    private List<String> mSpinL1 = Arrays.asList("Fech Nac", "Años", "Meses", "Dias", "año/mes/dia");
    //-----------------------------------------------------------------------

    // Para el selector de tipo gando--------------------------------------------
    private int currSel2 = 0;
    private List<String> mSpinL2= Arrays.asList("Vacas", "Novillas", "Becerros", "Toros");
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
                AddActivity.this.finish();
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setContentView(R.layout.activity_add);

        //Activate ToolBar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Agregar Mas a la Lista");
        actionBar.setDisplayShowHomeEnabled(true);

        myToolbar.setTitleTextColor(ContextCompat.getColor(myToolbar.getContext(), R.color.inner_button));

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
        mInput5 = findViewById(R.id.inputData5);

        mSpin1 = findViewById(R.id.spinAddEdad);
        mSpin2 = findViewById(R.id.spinType);
        mSw1 = findViewById(R.id.ADDswPre);

        mInput5.setEnabled(false);

        mSw1.setChecked(false);

        mBtnAdd = findViewById(R.id.buttAdd);

        mBtnCam.setOnClickListener(this);
        mBtnCam.setOnLongClickListener(this);
        mBtnAdd.setOnClickListener(this);
        mSw1.setOnClickListener(this);

        mInputList.add(mInput1);
        mInputList.add(mInput2);
        mInputList.add(mInput3);
        mInputList.add(mInput4);

        setupActivityResultLaunchers();

        //Inicia el input con la fecha actual
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate currdate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(StartVar.mDateFormES);
            mInput4.setText(currdate.format(formatter));

            mInput4.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        mInput4.post(() -> mInput4.selectAll());
                    }
                }
            });
        }

        //PAra la lista del selector de edades ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL1);
        mSpin1.setAdapter(adapter);
        mSpin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String text = mInput4.getText().toString();
                String newText = CalcCalendar.dataConvertedTo(text, currSel1, i);
                currSel1 = i;

                if(i == 0){
                    mInput4.setInputType(InputType.TYPE_CLASS_DATETIME);
                    mInput4.setHint("dd/mm/aaaa");
                    mInput4.setText(newText);
                }
                else if(i == 4){
                    mInput4.setInputType(InputType.TYPE_CLASS_DATETIME);
                    mInput4.setText(newText);
                    mInput4.setHint("año/mes/dia");
                }
                else {
                    mInput4.setInputType(InputType.TYPE_CLASS_NUMBER);
                    mInput4.setText(newText);
                    mInput4.setHint("");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //--------------------------------------------------------------------------------------------

        //PAra la lista del selector Tipo ganado ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapt2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL2);
        mSpin2.setAdapter(adapt2);
        mSpin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currSel2 = i;
                if(i == 0){
                    mInput3.setEnabled(true);

                    mInput5.setVisibility(View.VISIBLE);
                    mSw1.setVisibility(View.VISIBLE);
                }
                else if(i == 1){
                    mInput3.setText("0");
                    mInput3.setEnabled(false);

                    mInput5.setVisibility(View.VISIBLE);
                    mSw1.setVisibility(View.VISIBLE);
                }
                else {
                    mInput3.setText("0");
                    mInput3.setEnabled(false);

                    mInput5.setVisibility(View.INVISIBLE);
                    mInput5.setEnabled(false);
                    mSw1.setVisibility(View.INVISIBLE);
                    swPre = false;
                    mSw1.setChecked(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //--------------------------------------------------------------------------------------------

        mPermiss = StartVar.mPermiss;
        mIndex = getUserId(appDatabase.daoUser());
    }

    private void setupActivityResultLaunchers() {
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                try {
                    InputStream stream = getContentResolver().openInputStream(photoUri);
                    currUri = photoUri;
                    mImgPrev.setImageURI(currUri);
                    //binding.imageView.setImageBitmap(bitmap);
                    //processImage(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        selectPictureLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    InputStream stream = getContentResolver().openInputStream(uri);
                    currUri = uri;
                    mImgPrev.setImageURI(currUri);

                    //binding.imageView.setImageBitmap(bitmap);
                    //processImage(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //Para el input de Nombre -----------------------------------------------------
        mInput1.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String mTxInput = textView.getText().toString();
                if (mTxInput.isEmpty()) {
                    textView.setError("Ingrese Texto!.");
                    return true;
                }

                return false;
            }
        });

        //Para el input de Color -----------------------------------------------------
        mInput2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String mTxInput = textView.getText().toString();
                if (mTxInput.isEmpty()) {
                    textView.setError("Ingrese Texto!.");
                    return true;
                }

                return false;
            }
        });

        //Para el input de Litros -----------------------------------------------------
        mInput3.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String mTxInput = textView.getText().toString();
                if (mTxInput.isEmpty()) {
                    textView.setError("Ingrese Numeros!.");
                    return true;
                }

                return false;
            }
        });

        //Para el input de Edad -----------------------------------------------------
        mInput4.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                String mTxInput = textView.getText().toString();
                textView.clearFocus();
                if (currSel1 == 0) {
                    if(CalcCalendar.isDateFormat(mTxInput).isEmpty()){
                        Basic.msg("Formato de FECHA incorrecto!.");
                        textView.setError("Fecha Incorrecta!, Ejm: 20/10/2020");
                        return true;
                    }
                }
                else if (currSel1 == 4) {
                    if(Objects.requireNonNull(CalcCalendar.dataValidate(mTxInput)).length < 2){
                        Basic.msg("Formato de FECHA incorrecto!.");
                        textView.setError("Ingrese 3 digitos Ejm: 2/5/3");
                        return true;
                    }
                }
                return false;
            }
        });

        mInput4.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b && currSel1 == 0) {
                    if(CalcCalendar.isDateFormat(mInput4.getText().toString()).isEmpty()){
                        Basic.msg("Formato de FECHA incorrecto!.");
                    }
                }
            }
        });

        //Para el input de pre -----------------------------------------------------
        mInput5.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (textView.hasFocus() && chehkingPreInput() && swPre) {
                    textView.setError("Fecha Incorrecta!.");
                    return true;
                }
                return false;
            }
        });

        mInput5.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b && swPre) {
                    chehkingPreInput();
                }
            }
        });
        //--------------------------------------------------------------------------
    }

    private boolean chehkingPreInput(){
        String mText = CalcCalendar.isDateFormat(mInput5.getText().toString());
        if (mText.isEmpty()) {
            Basic.msg("Formato de FECHA incorrecto!.");
            return true;
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(StartVar.mDateFormES);
                LocalDate mDateA = LocalDate.parse(mText, formatter).plusDays(StartVar.mDayA);
                LocalDate mDateB = LocalDate.parse(mText, formatter).plusDays(StartVar.mDayB);
                Basic.msg("Parto estimado del: " + mDateA.format(formatter) + " al " + mDateB.format(formatter));
            }
        }
        return false;
    }

    private void dispatchSelectPictureIntent() {
        selectPictureLauncher.launch("image/*");
    }

    private void dispatchTakePictureIntent() throws IOException {
        File imageFile = File.createTempFile("IMG_", ".jpg", getCacheDir());
        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
        takePictureLauncher.launch(photoUri);
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
            if(!mPermiss) {
                mPermiss = checkStoragePermissions();
                if (!mPermiss){
                    requestForStoragePermissions();
                }
            }
            if (mPermiss) {
                // Launch the photo picker and let the user choose only images.
                dispatchSelectPictureIntent();
            }
            else {
                Basic.msg("Error Permiso Denegado!");
            }
        }

        if (itemId == R.id.ADDswPre){
            swPre = !swPre;
            mInput5.setEnabled(swPre);
            mInput5.setError(null);
        }

        if (itemId == R.id.buttAdd) {
            boolean result = true;
            int msgIdx = 0;
            mList.add(mIndex);
            for(int i = 0; i < mInputList.size(); i++) {
                String text = mInputList.get(i).getText().toString();
                text = text.replaceAll("\"", "");
                text = text.replaceAll(",", "");
                text = text.replaceAll("(^\\s+)|(\\s+$)", "");

                if (text.isEmpty()){
                    if(i == 2) {
                        //MSG para entrada de Litros
                        msgIdx = 3;
                    }
                    else if (i == 3) {
                        //MSG para entrada de Edad
                        msgIdx = 2;
                    }
                    result = false;
                    break;
                }
                //Input de Edad
                if(i == 3){
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        long vlresult = Long.parseLong(text.replaceAll("\\D",""));

                        //Inicia la fecha a comparra en cero
                        LocalDate date = LocalDate.of(1, 1, 1);
                        //Inicia la fecha actual
                        LocalDate currdate = LocalDate.now();

                        String res= "";
                        //Para Fechas de nacimiento
                        if(currSel1 == 0){
                            String mDate =  CalcCalendar.isDateFormat(text);
                            if (mDate.isEmpty()){
                                msgIdx = 4;
                                result = false;
                                break;
                            }
                            res = CalcCalendar.getFormatDateEN(mDate);
                        }
                        //Para años
                        else if(currSel1 == 1){
                            LocalDate from = currdate.minusYears(vlresult);
                            res = from.toString();
                        }
                        //Para meses
                        else if(currSel1 == 2){
                            LocalDate from = currdate.minusMonths(vlresult);
                            res = from.toString();
                        }
                        //Para Dias
                        else if(currSel1 == 3){
                            LocalDate from = currdate.minusDays(vlresult);
                            res = from.toString();
                        }
                        //Para Validar Fechas completas
                        else if(currSel1 == 4){
                            String[] dateList = CalcCalendar.dataValidate(text);
                            if (dateList != null && dateList.length > 1 ) {
                                LocalDate from = currdate.minusYears(Long.parseLong(dateList[0]));
                                from = from.minusMonths(Long.parseLong(dateList[1]));
                                from = from.minusDays(Long.parseLong(dateList[2]));
                                //Log.d("PhotoPicker", "1-->>>>>>>>>>>>>>>>>>>>>>>>>>>> Experimento: "+ from.toString());
                                res = from.toString();
                            }
                            else {
                                //MSG para entrada de Fechas
                                msgIdx = 1;
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
            //Se comprueba el imput de fecha pre-------------------------------------------
            String mPreDate =  CalcCalendar.isDateFormat(mInput5.getText().toString());
            mPreDate = CalcCalendar.getFormatDateEN(mPreDate);
            if (swPre && mPreDate.isEmpty()){
                msgIdx = 4;
                result = false;
            }
            //-----------------------------------------------------------------------------
            if (result) {
                //Para Limpiar Todos Los inputs
                for(int i = 0; i < mInputList.size(); i++) {
                    mInputList.get(i).setText("");
                }
                //Se guarda la foto en un nuevo directorio --------------------------------
                Bitmap bitmap;
                try {
                    if(!sImage.isEmpty() || currUri == null){
                        oldFile = Uri.parse(sImage);
                    }
                    else {
                        //Log.d("PhotoPicker", "Aqi hayyyyyyyyyyyyy5555----------------------------------: ");
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currUri);
                        sImage = fmang.SavePhoto(bitmap, mIndex, oldFile, this, this.getContentResolver());
                    }
                }
                catch (IOException e) {
                    Basic.msg("Error al guardar la IMAGEN!");
                    e.printStackTrace();
                    sImage = "";
                }
                //-------------------------------------------------------------------

                Usuario obj =
                        new Usuario(
                                mList.get(0), mList.get(1), mList.get(2), mList.get(3), mList.get(4), mPreDate,
                                sImage, Integer.toString(currSel1), Integer.toString(currSel2), (swPre?"1":"0") ,"" ,"" ,"" ,""
                            );
                appDatabase.daoUser().insetUser(obj);

                //SE Limpia la lista
                mList.clear();

                //Se vacia el archivo viejo
                oldFile = null;

                //Recarga La lista de la DB ----------------------------
                StartVar mVars = new StartVar(getApplicationContext());
                mVars.getUserListDB();
                //-------------------------------------------------------

                //Esto inicia las actividad Main despues de tiempo de espera del preloder
                startActivity(new Intent(AddActivity.this,MainActivity.class));
                finish(); //Finaliza la actividad y ya no se accede mas
            }
            else {
                Basic.msg(getTextMessage(msgIdx));
                mList.clear();
            }

        }
    }

    @Override
    public boolean onLongClick(View view) {
        int itemId = view.getId();
        if (itemId == R.id.buttCam) {
            if( requestForCameraPermissions() || mCamPermiss) {
                try {
                    dispatchTakePictureIntent();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }


    private String getTextMessage(int idx){
        String msg = "Error";
        if (idx == 0) {
            msg = "La entrada esta vacia! (SIN TEXTO).";
        }
        else if (idx == 1) {
            msg = "Fecha formato Invalido, Debe ser: DIA-MES-AÑO ";
        }
        else if (idx == 2) {
            msg = "Ingrese el numero de FECHA/EDAD";
        }
        else if (idx == 3) {
            msg = "Ingrese el numero de LITROS ";
        }
        else if (idx == 4) {
            msg = "Formato de FECHA incorrecto!.";
        }
        return msg;
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

    private boolean checkStoragePermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            //Android is 11 (R) or above
            else if (Environment.isExternalStorageManager()){
                Log.d("PhotoPicker", " Permiso Aquiiiiiiiiii Hayyyyyy 11100------------------------: " );
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
            Log.d("PhotoPicker", " -----Permiso Aquiiiiiiiiii Hayyyyyy 11100------------------------: " );

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
                                    Toast.makeText(AddActivity.this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
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
            }
            catch (Exception e){
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

    private boolean requestForCameraPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
        else{
            return true;
        }
        return false;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(AddActivity.this, "Permisos de Camara ACEPTADOS", Toast.LENGTH_SHORT).show();
                mCamPermiss = true;
            } else {
                Toast.makeText(AddActivity.this, "Permisos de Camara Denegados", Toast.LENGTH_SHORT).show();

            }
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
                return  "userID"+i;
            }
        }
        return mIdx;
        //-------------------------------------------------------------------------------------------
    }
}