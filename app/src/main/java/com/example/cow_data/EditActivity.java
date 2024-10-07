package com.example.cow_data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditActivity extends AppCompatActivity implements View.OnClickListener {
    //Base de datos
    public AppDatabase appDatabase = SatrtVar.appDatabase;

    private ImageView mImgPrev;
    private EditText mInput1;
    private EditText mInput2;
    private EditText mInput3;
    private EditText mInput4;

    private Spinner mSpin1;
    private Spinner mSpin2;

    private List<TextView> mInputList = new ArrayList<>();
    private List<String> mList = new ArrayList<>();

    private Button mBtnMore;
    private ExtendedFloatingActionButton mBtnAdd;
    private ExtendedFloatingActionButton mBtnDel;
    private SwitchMaterial mSw;
    private boolean swDel = false;
    private ImageButton mBtnCam;
    private CoordinatorLayout mLayout;

    // Para guardar los permisos de app comprobados en main
    private boolean mPermiss = false;

    private String sImage = "";
    private String saveImage = "null";

    private String mUser = "";
    private Uri oldFile = null;
    private Uri currUri = null;
    private int currIdx = 0;

    // Para el selector de edades--------------------------------------------
    private int currSel1 = 0;
    private List<String> mSpinL1 = Arrays.asList("Años", "Meses", "Dias", "D-M-A");
    //-----------------------------------------------------------------------

    // Para el selector de tipo gando--------------------------------------------
    private int currSel2 = 0;
    private List<String> mSpinL2= Arrays.asList("Vacas", "Novillas", "Becerros", "Toros");
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
        mSpin1 = findViewById(R.id.spinEdad);
        mSpin2 = findViewById(R.id.spinType);

        mBtnMore = findViewById(R.id.buttMORE);
        mBtnAdd  = findViewById(R.id.buttOK);
        mBtnDel  = findViewById(R.id.buttDEL);
        mSw = findViewById(R.id.swDelete);
        mBtnCam = findViewById(R.id.bttGall);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mSw.setFocusedByDefault(false);
        }
        mBtnMore.setOnClickListener(this);
        mBtnCam.setOnClickListener(this);
        mBtnAdd.setOnClickListener(this);
        mBtnDel.setOnClickListener(this);
        mSw.setOnClickListener(this);

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
                        }
                    }
                    else {
                        mInput4.setText("1");
                    }
                    mInput4.setHint("");
                    mInput4.setInputType(InputType.TYPE_CLASS_NUMBER);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //----------------------------------------------------------------------------------------------------

        //PAra la lista del selector Tipo ganado ----------------------------------------------------------------------------------------------
        ArrayAdapter<String> adapt2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mSpinL2);
        mSpin2.setAdapter(adapt2);
        mSpin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                //SatrtVar test = new SatrtVar(EditActivity.this);
                //test.setUserListDB();
                //Toast.makeText(EditActivity.this, "Siz is "+SatrtVar.currSel2, Toast.LENGTH_LONG).show();

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

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            currIdx = intent.getIntExtra("index", 0);
            List<Usuario> listuser = SatrtVar.listuser;

            int i = 0;
            currSel1 = Integer.parseInt(listuser.get(currIdx).sel1);
            currSel2 = Integer.parseInt(listuser.get(currIdx).sel2);
            if(currSel1 == 3){
                mInput4.setInputType(InputType.TYPE_CLASS_DATETIME);
            }

            if(currSel2 != 0){
                mInput3.setText("0");
                mInput3.setEnabled(false);
            }

            if (currIdx < listuser.size()) {
                //Se obtiene el usuario real
                mUser = listuser.get(currIdx).usuario;

                mInputList.get(i).setText(listuser.get(currIdx).nombre);
                i++;
                mInputList.get(i).setText(listuser.get(currIdx).color);
                i++;
                mInputList.get(i).setText(listuser.get(currIdx).litros);
                i++;
                mInputList.get(i).setText(CalcCalendar.dataConverted(listuser.get(currIdx).edad, currSel1));
                i++;
                saveImage = fmang.getImage(listuser.get(currIdx).imagen, mImgPrev);
                currUri = Uri.parse(sImage);
                mSpin1.setSelection(currSel1);
                mSpin2.setSelection(currSel2);
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
            if (mPermiss){
                // Launch the photo picker and let the user choose only images.
                pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
            }
            else{
                textSnackbar("Error Permiso Denegado!");
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
                        long vlresult = currSel1 == 3? 0 : Long.parseLong(text);

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
            if (result) {
                ArrayList<String> morlist = SatrtVar.morlist;

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
                        mList.get(0), mList.get(1), mList.get(2), mList.get(3), mList.get(4),
                        sImage.isEmpty()? saveImage:sImage, Integer.toString(currSel1), Integer.toString(currSel2),
                        (max>0?morlist.get(0):""),(max>1?morlist.get(1):""),(max>2?morlist.get(2):""),(max>3?morlist.get(3):""),(max>4?morlist.get(4):"")
                );

                //listuser.add(currIdx, obj);

                //SE Limpia la lista
                mList.clear();

                //Se vacia el archivo viejo
                oldFile = null;

                //Recarga La lista de la DB ----------------------------
                SatrtVar mVars = new SatrtVar(getApplicationContext());
                mVars.getUserListDB();
                //-------------------------------------------------------

                //Esto inicia las actividad Main despues de tiempo de espera del preloder
                Intent mIntent = new Intent(this, ViewActivity.class);
                mIntent.putExtras(getAndSetBundle());
                startActivity(mIntent);
                finish(); //Finaliza la actividad y ya no se accede mas
            }
            else {
                textSnackbar(getTextMessage(msgIdx));
                mList.clear();
            }
        }
        if (itemId == R.id.swDelete){
            swDel = !swDel;
            if(swDel) {
                mBtnDel.setVisibility(View.VISIBLE);
                mBtnAdd.setVisibility(View.INVISIBLE);
            }
            else{
                mBtnDel.setVisibility(View.INVISIBLE);
                mBtnAdd.setVisibility(View.VISIBLE);
            }
        }
        if (itemId == R.id.buttDEL){
            fmang.RemoveFile(saveImage, this.getContentResolver());
            appDatabase.daoUser().removerUser(mUser);
            Intent mIntent = new Intent(this, MainActivity.class);
            startActivity(mIntent);
            finish(); //Finaliza la actividad y ya no se accede mas

        }
        if (itemId == R.id.buttMORE) {
            Intent mIntent = new Intent(this, MoreActivity.class);
            mIntent.putExtras(getAndSetBundle());
            startActivity(mIntent);
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

    private Bundle getAndSetBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("index", currIdx);
        return bundle;
    }
}