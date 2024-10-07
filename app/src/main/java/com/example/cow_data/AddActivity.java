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

    // DB
    private AppDatabase appDatabase = SatrtVar.appDatabase;

    private static final int REQUEST_PERMISSION_CAMERA = 100;
    private static final int STORAGE_PERMISSION_CODE = 23;
    private ImageButton mBtnCam;
    private ImageView mImgPrev;

    //Todos los Inputs
    private EditText mInput1;
    private EditText mInput2;
    private EditText mInput3;
    private EditText mInput4;

    private Spinner mSpin1;
    private Spinner mSpin2;

    private Button mBtnAdd;

    private List<String> mList = new ArrayList<>();
    private List<TextView> mInputList = new ArrayList<>();

    private String sImage = "";
    private String mIndex = "";
    private Uri oldFile = null;
    private Uri currUri = null;

    // Para guardar los permisos de app comprobados en main
    private boolean mPermiss = false;

    // Classs para la gestion de archivos
    FilesManager fmang = new FilesManager();

    // Para el selector de edades--------------------------------------------
    private int currSel1 = 0;
    private List<String> mSpinL1 = Arrays.asList("Años", "Meses", "Dias", "D-M-A");
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
        mSpin1 = findViewById(R.id.spinAddEdad);
        mSpin2 = findViewById(R.id.spinType);

        mBtnAdd = findViewById(R.id.buttAdd);

        mBtnCam.setOnClickListener(this);
        mBtnAdd.setOnClickListener(this);

        mInputList.add(mInput1);
        mInputList.add(mInput2);
        mInputList.add(mInput3);
        mInputList.add(mInput4);

        //PAra la lista del selector de edades ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL1);
        mSpin1.setAdapter(adapter);
        mSpin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currSel1 = i;
                if(i == 3){
                    mInput4.setInputType(InputType.TYPE_CLASS_DATETIME);
                    String text = mInput4.getText().toString();
                    String[] txlist = CalcCalendar.dataValidate(text);
                    if(txlist == null){
                        mInput4.setText("");
                        mInput4.setHint("Ejemplo: 1-1-1");

                    }
                }
                else {
                    String text = mInput4.getText().toString();
                    String[] txlist = CalcCalendar.dataValidate(text);
                    if(txlist == null){
                        Pattern patt = Pattern.compile("(\\d{1,3})$");
                        Matcher matcher = patt.matcher(text);
                        if(!matcher.find()) {
                            mInput4.setText("");
                            mInput4.setHint("Ingrese Edad");
                        }
                    }
                    else {
                        mInput4.setText("1");
                    }
                    mInput4.setInputType(InputType.TYPE_CLASS_NUMBER);
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
                }
                else {
                    mInput3.setText("0");
                    mInput3.setEnabled(false);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //--------------------------------------------------------------------------------------------

        mPermiss = SatrtVar.mPermiss;
        mIndex = "" + appDatabase.daoUser().getUsers().size();
        if (mIndex.isEmpty()) {
            mIndex = "0";
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
            int msgIdx = 0;
            mList.add("userID"+mIndex);
            for(int i = 0; i < mInputList.size(); i++) {
                String text = mInputList.get(i).getText().toString();
                text = text.replaceAll("\"", "");
                text = text.replaceAll(",", "");
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
                        long vlresult = currSel1==3? 0 : Long.parseLong(text);

                        //Inicia la fecha a comparra en cero
                        LocalDate date = LocalDate.of(1, 1, 1);
                        //Inicia la fecha actual
                        LocalDate currdate = LocalDate.now();

                        String res= "";

                        //Para años
                        if(currSel1 == 0){
                            LocalDate from = currdate.minusYears(vlresult);
                            res = from.toString();
                        }
                        //Para meses
                        else if(currSel1 == 1){
                            LocalDate from = currdate.minusMonths(vlresult);
                            res = from.toString();
                        }
                        //Para Dias
                        else if(currSel1 == 2){
                            LocalDate from = currdate.minusDays(vlresult);
                            res = from.toString();
                        }
                        //Para Validar Fechas completas
                        else if(currSel1 == 3){
                            String[] dateList = CalcCalendar.dataValidate(text);
                            if (dateList != null && dateList.length > 1 ) {
                                LocalDate from = currdate.minusYears(Long.parseLong(dateList[2]));
                                from = from.minusMonths(Long.parseLong(dateList[1]));
                                from = from.minusDays(Long.parseLong(dateList[0]));
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
                        //Log.d("PhotoPicker", "Aqi hayyyyyyyyyyyyy5555----------------------------------: ");
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
                                sImage, Integer.toString(currSel1), Integer.toString(currSel2), "" ,"" ,"" ,"" ,""
                            );
                appDatabase.daoUser().insetUser(obj);

                //SE Limpia la lista
                mList.clear();

                //Se vacia el archivo viejo
                oldFile = null;

                //Recarga La lista de la DB ----------------------------
                SatrtVar mVars = new SatrtVar(getApplicationContext());
                mVars.getUserListDB();
                //-------------------------------------------------------

                //Esto inicia las actividad Main despues de tiempo de espera del preloder
                startActivity(new Intent(AddActivity.this,MainActivity.class));
                finish(); //Finaliza la actividad y ya no se accede mas
            }
            else {
                textSnackbar(getTextMessage(msgIdx));
                mList.clear();
            }

        }
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
        return msg;
    }

    private void textSnackbar(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
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

}