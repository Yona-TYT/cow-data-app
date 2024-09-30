package com.example.cow_data;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
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


public class ViewActivity extends AppCompatActivity implements View.OnClickListener {

    //Bae de datos
    public AppDatabase appDatabase;

    public TextView mView1;
    public TextView mView2;
    public TextView mView3;
    public TextView mView4;
    public TextView mView5;

    public TextView mMore1;
    public TextView mMore2;
    public TextView mMore3;
    public TextView mMore4;
    public TextView mMore5;

    private ImageView mImageView;
    private Button mButtEdit;

    private List<TextView> mviewList = new ArrayList<>();
    private List<Usuario> listuser;
    private List<String> mList = new ArrayList<>();
    private ArrayList<String> morlist = new ArrayList<>();

    private CoordinatorLayout mLayout;
    private LinearLayout imgLayout;
    private HorizontalScrollView mScroll;

    private ActivityResultLauncher<Intent> launcher; // Initialise this object in Activity.onCreate()
    private Uri baseDocumentTreeUri;

    // Para guardar los permisos de app comprobados en main
    private boolean mPermiss = false;
    // El index actual de bd
    private int currIdx = 0;
    private String currDir = "";

    // Classs para la gestion de archivos
    FilesManager fmang = new FilesManager();

    //Nombre de data Base
    private String nameDB = "";

    // Para el selector de edades--------------------------------------------
    private int currSelec = 0;
    private List<String> mSpinList = Arrays.asList("Años", "Meses", "Dias", "");
    //-----------------------------------------------------------------------

    @SuppressLint({"MissingInflatedId", "RestrictedApi"})
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

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view);

        //Activate ToolBar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Vista en Detalles");
        actionBar.setDisplayShowHomeEnabled(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mView1 = findViewById(R.id.txView1);
        mView2 = findViewById(R.id.txView2);
        mView3 = findViewById(R.id.txView3);
        mView4 = findViewById(R.id.txView4);

        mMore1 = findViewById(R.id.txMore1);
        mMore2 = findViewById(R.id.txMore2);
        mMore3 = findViewById(R.id.txMore3);
        mMore4 = findViewById(R.id.txMore4);
        mMore5 = findViewById(R.id.txMore5);

        mImageView = findViewById(R.id.imageView);
        mButtEdit = findViewById(R.id.buttEdit);
        mLayout = findViewById(R.id.layout2);

        mImageView.setOnClickListener(this);
        mButtEdit.setOnClickListener(this);

        mviewList.add(mView1);
        mviewList.add(mView2);
        mviewList.add(mView3);
        mviewList.add(mView4);
        mviewList.add(mMore1);
        mviewList.add(mMore2);
        mviewList.add(mMore3);
        mviewList.add(mMore4);
        mviewList.add(mMore5);

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            mPermiss = intent.getBooleanExtra("perm", false);
            currIdx = intent.getIntExtra("index", 0);
            nameDB = intent.getStringExtra("dbname" );
            appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, nameDB).allowMainThreadQueries().build();
            listuser =  appDatabase.daoUser().getUsers();

            int i = 0;
            currSelec = Integer.parseInt(listuser.get(currIdx).selec);
            if (currIdx < listuser.size()) {
                mviewList.get(i).setText("Nombre:   "+listuser.get(currIdx).nombre.toUpperCase());
                i++;
                mviewList.get(i).setText("Color:   "+listuser.get(currIdx).color.toUpperCase());
                i++;
                mviewList.get(i).setText("Litros:   "+listuser.get(currIdx).litros+" Litros Diarios");
                i++;
                mviewList.get(i).setText("Edad:   "+dataConverted(listuser.get(currIdx).edad, currSelec)+ " "+mSpinList.get(currSelec));
                currDir = fmang.getImage(listuser.get(currIdx).imagen, mImageView);
                i++;
                setTextView(mviewList.get(i), "Otros:   ", listuser.get(currIdx).more1);
                i++;
                setTextView(mviewList.get(i), "Otros:   ", listuser.get(currIdx).more2);
                i++;
                setTextView(mviewList.get(i), "Otros:   ", listuser.get(currIdx).more3);
                i++;
                setTextView(mviewList.get(i), "Otros:   ", listuser.get(currIdx).more4);
                i++;
                setTextView(mviewList.get(i), "Otros:   ", listuser.get(currIdx).more5);
            }
        }
        else {
            textSnackbar("Aqui no hay :(");
        }
        mList.clear();
    }

    private void setTextView(TextView view, String txDesc, String txValue){
        if(txValue.isEmpty()){
            view.setVisibility(View.INVISIBLE);
        }
        else {
            morlist.add(txValue);
            view.setText(txDesc + txValue);
        }
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            Intent mIntent = new Intent(this, MainActivity.class);
            startActivity(mIntent);
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        int itemId = view.getId();
        if (itemId == R.id.buttEdit) {
            Intent mIntent = new Intent(this, EditActivity.class);
            Bundle mBundle = new Bundle();
            mBundle.putInt("index", currIdx);
            mBundle.putBoolean("perm", mPermiss);
            mBundle.putString("dbname", nameDB);
            mBundle.putStringArrayList("morelist", morlist);
            mIntent.putExtras(mBundle);
            startActivity(mIntent);
        }
        if(itemId == R.id.imageView){
            Intent mIntent = new Intent(this, ImgFullscreenActivity.class);
            Bundle mBundle = new Bundle();
            mBundle.putString("dir", currDir);
            mBundle.putInt("index", currIdx);
            mBundle.putBoolean("perm", mPermiss);
            mBundle.putString("dbname", nameDB);
            mIntent.putExtras(mBundle);
            startActivity(mIntent);
        }
    }

    private void textSnackbar(String text) {
        Snackbar mySnackbar = Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT);
        mySnackbar.show();
    }

    boolean isBlockedPath(Context ctx, String fdCanonical) {
        // Paths that should rarely be exposed
        if (fdCanonical.startsWith("content://media/"+MediaStore.VOLUME_EXTERNAL_PRIMARY) || fdCanonical.startsWith("/data/misc/")) {
            return true;
        }
        return false;
    }
    public String dataConverted(String text, int selec){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Convierte Sting  a forrmato de fecha
            LocalDate date = LocalDate.parse(text);
            //Inicia la fecha actual
            LocalDate currdate = LocalDate.now();

            long vlresult = 0;
            //Para años
            if(selec == 0){
                vlresult = ChronoUnit.YEARS.between(date, currdate);
            }
            //Para meses
            else if(selec == 1){
                vlresult = ChronoUnit.MONTHS.between(date, currdate );
            }
            //Para Dias
            else if(selec == 2){
                vlresult = ChronoUnit.DAYS.between(date, currdate );
            }
            //Para Formato de fecha
            else if(selec == 3){
                Period result = date.until(currdate);
                return result.getYears()+" Años y "+result.getMonths()+" Meses";
            }
            return ""+(vlresult < 0? 1 : vlresult);
        }
        return "1";
    }
}