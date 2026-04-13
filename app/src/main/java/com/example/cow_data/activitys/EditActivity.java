package com.example.cow_data.activitys;

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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cow_data.Basic;
import com.example.cow_data.CalendUtls;
import com.example.cow_data.FilesManager;
import com.example.cow_data.R;
import com.example.cow_data.StartVar;
import com.example.cow_data.db.AppDatabase;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.drive.GoogleDriveManager;
import com.example.cow_data.ex.PreferenceHelper;
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

public class EditActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
    //Base de datos
    public AppDatabase appDatabase = StartVar.appDatabase;

    private static final int STORAGE_PERMISSION_CODE = 23;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private boolean mPermiss = false;
    private boolean mCamPermiss = false;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> selectPictureLauncher;
    private Uri photoUri;

    private ImageView mImgPrev;
    private EditText mInput1;
    private EditText mInput2;
    private EditText mInput3;
    private EditText mInput4;
    private EditText mInput5;

    private TextView mTextV;

    private Spinner mSpin1;
    private Spinner mSpin2;
    private Spinner mSpin3;

    private List<EditText> mInputList = new ArrayList<>();
    private List<String> mList = new ArrayList<>();

    private Button mBtnMore;
    private ExtendedFloatingActionButton mBtnAdd;
    private ExtendedFloatingActionButton mBtnDel;
    private SwitchMaterial mSw2;
    private boolean swDel = false;

    private SwitchMaterial mSw1;
    private boolean swPre = false;

    private ImageButton mBtnCam;
    private CoordinatorLayout mLayout;

    private String sImage = "";
    private String saveImage = "null";

    private String mUser = "";
    private Uri oldFile = null;
    private Uri currUri = null;
    private int currIdx = 0;
    private Usuario myUser;

    // Para el selector de edades--------------------------------------------
    private int currSel1 = 0;
    private final List<String> mSpinL1 = Arrays.asList("Fech Nac", "Años", "Meses", "Dias", "año/mes/dia");
    //-----------------------------------------------------------------------

    // Para el selector de tipo gando--------------------------------------------
    private int currSel2 = 0;
    private final List<String> mSpinL2= Arrays.asList("Vacas", "Novillas", "Becerros", "Toros");
    //-----------------------------------------------------------------------

    // Para el selector de tipo eliminador--------------------------------------------
    private int currSel3 = 1;
    private final List<String> mSpinL3= Arrays.asList("Disponible", "Eliminar", "Vender", "Pérdida");
    //-----------------------------------------------------------------------

    // Classs para la gestion de archivos
    FilesManager fmang = new FilesManager();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Se configura el Boton nav Back -----------------------------------------------
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent mIntent = new Intent(getApplicationContext(), ViewActivity.class);
                mIntent.putExtras(getAndSetBundle());
                startActivity(mIntent);
                EditActivity.this.finish();
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);
        //---------------------------------------------------------------------------------

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit);

        //Activate ToolBar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Modo Editar");
        actionBar.setDisplayShowHomeEnabled(true);

        myToolbar.setTitleTextColor(ContextCompat.getColor(myToolbar.getContext(), R.color.inner_button));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mImgPrev = findViewById(R.id.imageView);

        mInput1 = findViewById(R.id.txEdit1);
        mInput2 = findViewById(R.id.txEdit2);
        mInput3 = findViewById(R.id.txEdit3);
        mInput4 = findViewById(R.id.txEdit4);
        mInput5 = findViewById(R.id.txEdit5);

        mTextV = findViewById(R.id.textEditView5);

        mSpin1 = findViewById(R.id.spinEdad);
        mSpin2 = findViewById(R.id.spinType);
        mSpin3 = findViewById(R.id.spinDel);

        mBtnMore = findViewById(R.id.buttMORE);
        mBtnAdd  = findViewById(R.id.buttOK);
        mBtnDel  = findViewById(R.id.buttDEL);
        mSw1 = findViewById(R.id.swPre);
        mSw2 = findViewById(R.id.swDelete);
        mBtnCam = findViewById(R.id.bttGall);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mSw1.setFocusedByDefault(false);
            mSw2.setFocusedByDefault(false);
        }
        mBtnMore.setOnClickListener(this);
        mBtnCam.setOnClickListener(this);
        mBtnCam.setOnLongClickListener(this);

        mBtnAdd.setOnClickListener(this);
        mBtnDel.setOnClickListener(this);
        mSw1.setOnClickListener(this);
        mSw2.setOnClickListener(this);

        mInputList.add(mInput1);
        mInputList.add(mInput2);
        mInputList.add(mInput3);
        mInputList.add(mInput4);

        setupActivityResultLaunchers();

        //--------------------------------------------------------------------------------------------

        mPermiss = StartVar.mPermiss;

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            currIdx = intent.getIntExtra("index", 0);
            List<Usuario> listuser = StartVar.listuser;
            myUser = listuser.get(currIdx);

            int i = 0;

            if (myUser != null) {

                currSel1 = myUser.sel1;
                currSel2 = myUser.sel2;


                if(currSel1 == 3){
                    mInput4.setInputType(InputType.TYPE_CLASS_DATETIME);
                }

                if(currSel2 > 0){
                    mInput3.setText("0");
                    mInput3.setEnabled(false);
                }

                //Se obtiene el usuario real
                mUser = myUser.usuario;

                mInputList.get(i).setText(myUser.nombre);
                i++;
                mInputList.get(i).setText(myUser.color);
                i++;
                mInputList.get(i).setText(myUser.litros);
                i++;
                mInputList.get(i).setText(CalendUtls.dataConverted(myUser.edad, currSel1));

                //Log.d("Calendar", "Calen1 -->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+CalcCalendar.getFormatDateES(mList.edad));
                i++;

                mInput5.setText(CalendUtls.getFormatDateES(myUser.pre));

                swPre = !(myUser.sel3 == 0);
                mInput5.setEnabled(swPre);

                mSw1.setChecked(swPre);

                saveImage = fmang.getImage(myUser.imagen, mImgPrev);
                currUri = Uri.parse(sImage);

                //Comentado para futura eliminacion
                //mSpin1.setSelection(currSel1);
            }
        }
        else {
            Basic.msg("Aqui no hay :(");
        }

        //Inicializa more list


        //Para la lista del selector Tipo ganado ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapt2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL2);
        mSpin2.setAdapter(adapt2);
        mSpin2.setSelection(currSel2);
        mSpin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currSel2 = i;

                if(i == 0){
                    Basic.msg("Hay ? "+i);

                    mInput3.setEnabled(true);

                    mInput5.setVisibility(View.VISIBLE);
                    mSw1.setVisibility(View.VISIBLE);
                    mTextV.setVisibility(View.VISIBLE);
                }
                else if(i == 1){
                    mInput3.setText("0");
                    mInput3.setEnabled(false);

                    mInput5.setVisibility(View.VISIBLE);
                    mSw1.setVisibility(View.VISIBLE);
                    mTextV.setVisibility(View.VISIBLE);
                }

                else {
                    mInput3.setText("0");
                    mInput3.setEnabled(false);

                    mInput5.setVisibility(View.INVISIBLE);
                    mInput5.setEnabled(false);
                    mSw1.setVisibility(View.INVISIBLE);
                    swPre = false;
                    mSw1.setChecked(false);
                    mTextV.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //--------------------------------------------------------------------------------------------


        //Para la lista del selector de edades ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL1);
        mSpin1.setAdapter(adapter);
        mSpin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String text = mInput4.getText().toString();
                String newText = CalendUtls.dataConvertedTo(text, currSel1, i);

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
        //----------------------------------------------------------------------------------------------------

        //Para la lista del selector Tipo eliminador ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapt3 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL3);
        mSpin3.setAdapter(adapt3);
        mSpin3.setSelection(currSel3);
        mSpin3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currSel3 = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

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
                if (currSel1 == 0) {
                    if(CalendUtls.isDateFormat(mTxInput).isEmpty()){
                        Basic.msg("Formato de FECHA incorrecto!.");
                        textView.setError("Fecha Incorrecta!.");
                        return true;
                    }
                }
                else if (currSel1 == 4) {
                    if(Objects.requireNonNull(CalendUtls.dataValidate(mTxInput)).length < 2){
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
                    if(CalendUtls.isDateFormat(mInput4.getText().toString()).isEmpty()){
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
        String mText = CalendUtls.isDateFormat(mInput5.getText().toString());
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
                return false;
            }
        }
        return false;
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
                }
                catch (Exception e) {
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
            Intent mIntent = new Intent(this, ViewActivity.class);
            mIntent.putExtras(getAndSetBundle());
            startActivity(mIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        int itemId = view.getId();
        if (itemId == R.id.bttGall) {
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
        // GUARDA los datos ---------------------------------------------------------------
        if (itemId == R.id.buttOK) {
            boolean result = true;
            int msgIdx = 0;
            mList.add(mUser);
            for(int i = 0; i < mInputList.size(); i++) {
                TextView textv = mInputList.get(i);
                String text = textv.getText().toString();
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

                        //Inicia la fecha a comparar en cero
                        LocalDate date = LocalDate.of(1, 1, 1);
                        //Inicia la fecha actual
                        LocalDate currdate = LocalDate.now();
                        String res= "";
                        //Para Fechas de nacimiento
                        if(currSel1 == 0){
                            String mDate =  CalendUtls.isDateFormat(text);
                            if (mDate.isEmpty()){
                                msgIdx = 4;
                                result = false;
                                break;
                            }
                            res = CalendUtls.getFormatDateEN(mDate);
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
                            String[] dateList = CalendUtls.dataValidate(text);
                            if (dateList != null && dateList.length > 1 ) {
                                LocalDate from = currdate.minusYears(Long.parseLong(dateList[0]));
                                from = from.minusMonths(Long.parseLong(dateList[1]));
                                from = from.minusDays(Long.parseLong(dateList[2]));
                                //Log.d("PhotoPicker", "1-->>>>>>>>>>>>>>>>>>>>>>>>>>>> Experimento: "+ from.toString());
                                res = from.toString();
                            }
                            else {
                                result = false;
                                //MSG para entrada de Fechas
                                msgIdx = 1;
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
                text = text.replaceAll("\"", "");
                text = text.replaceAll(",", "");
                mList.add(text);
            }

            //Se comprueba el imput de fecha pre-------------------------------------------
            String mPreDate =  CalendUtls.isDateFormat(mInput5.getText().toString());
            mPreDate = CalendUtls.getFormatDateEN(mPreDate);
            if (swPre && mPreDate.isEmpty()){
                msgIdx = 4;
                result = false;
            }
            //-----------------------------------------------------------------------------

            if (result) {
                ArrayList<String> morlist = StartVar.morlist;

                //Siz maximo para la lista de more datos
                int max = (morlist == null? 0: morlist.size());

                //Para Limpiar Todos Los inputs
                for(int i = 0; i < mInputList.size(); i++) {
                    mInputList.get(i).setText("");
                }

                //Se guarda la foto en un nuevo directorio --------------------------------
                Bitmap bitmap = null;
                try {
                    if(currUri != null){
                        oldFile = Uri.parse(saveImage);
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currUri);
                        sImage = fmang.SavePhoto(bitmap, mUser, oldFile, this, this.getContentResolver());
                    }
                } catch (IOException e) {
                    //textSnackbar("Erorro con imagen");
                    e.printStackTrace();
                    sImage = "";
                }

                //-------------------------------------------------------------------
                appDatabase.daoUser().updateUser(
                        mList.get(0), mList.get(1), mList.get(2), mList.get(3), mList.get(4), mPreDate,
                        sImage.isEmpty()? saveImage:sImage, currSel1,
                        currSel2,(swPre?1:0)
                );

                if(morlist != null && !morlist.isEmpty()) {
                    appDatabase.daoUser().updateMore(
                            mList.get(0), morlist.get(0),  morlist.get(1), morlist.get(2), morlist.get(3)
                    );
                }

                //listuser.add(currIdx, obj);

                //SE Limpia la lista
                mList.clear();
                //Se limpia la lista more
                StartVar.morlist.clear();;

                        //Se vacia el archivo viejo
                oldFile = null;

                //Recarga La lista de la DB ----------------------------
                StartVar mVars = new StartVar(getApplicationContext());
                mVars.getUserListDB();
                //-------------------------------------------------------

                //Esto inicia las actividad Main y cierra la actual
                Intent mIntent = new Intent(this, ViewActivity.class);
                mIntent.putExtras(getAndSetBundle());
                startActivity(mIntent);

                //Encola al usuario para sincronizar
                myUser = StartVar.listuser.get(currIdx);
                StartVar.usuarioQueue.enqueue(myUser);

                if(!sImage.isEmpty()) {
                    File mFile = new File(sImage);
                    if(mFile.exists()) {
                        GoogleDriveManager manager = new GoogleDriveManager(PreferenceHelper.getInstance());
                        manager.ImportImgToDrive(mFile);
                    }
                }

                finish(); //Finaliza la actividad y ya no se accede mas
            }
            else {
                Basic.msg(getTextMessage(msgIdx));
                mList.clear();
            }
        }
        if (itemId == R.id.swDelete){
            swDel = !swDel;
            if(swDel) {
                mBtnDel.setVisibility(View.VISIBLE);
                mSpin3.setVisibility(View.VISIBLE);
                mBtnAdd.setVisibility(View.INVISIBLE);
            }
            else{
                mBtnDel.setVisibility(View.INVISIBLE);
                mSpin3.setVisibility(View.INVISIBLE);
                mBtnAdd.setVisibility(View.VISIBLE);
            }
        }
        if (itemId == R.id.buttDEL){
            if (currSel3 == 1) {
                fmang.RemoveFile(saveImage, this.getContentResolver());
                appDatabase.daoUser().removerUser(mUser);

                myUser = StartVar.listuser.get(currIdx);
                Usuario mDelUser = new Usuario("@null", myUser.usuario, "", "", "",
                        "", "", 0, 0, 0, 0, "", "",
                        "", "");
                StartVar.usuarioQueue.enqueue(mDelUser);
            }
            else {
                appDatabase.daoUser().updateStatus(
                        myUser.usuario, currSel3
                );

                //Encola al usuario para sincronizar
                myUser = StartVar.listuser.get(currIdx);
                StartVar.usuarioQueue.enqueue(myUser);
            }

            Intent mIntent = new Intent(this, MainActivity.class);
            startActivity(mIntent);
            finish(); //Finaliza la actividad y ya no se accede mas
        }
        if (itemId == R.id.swPre){
            swPre = !swPre;
            mInput5.setEnabled(swPre);
            mInput5.setError(null);
        }
        if (itemId == R.id.buttMORE) {
            Intent mIntent = new Intent(this, MoreActivity.class);
            mIntent.putExtras(getAndSetBundle());
            startActivity(mIntent);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int itemId = view.getId();
        if (itemId == R.id.bttGall) {
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
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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
                                    Toast.makeText(EditActivity.this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
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
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    private boolean requestForCameraPermissions() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(EditActivity.this, "Permisos de Camara ACEPTADOS", Toast.LENGTH_SHORT).show();
                mCamPermiss = true;
            } else {
                Toast.makeText(EditActivity.this, "Permisos de Camara Denegados", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private Bundle getAndSetBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("index", currIdx);
        return bundle;
    }
}