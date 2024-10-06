package com.example.cow_data;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.ImageButton;
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
import androidx.core.content.ContextCompat;
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
    private ImageButton buttNext;
    private ImageButton buttPrev;

    private List<TextView> mviewList = new ArrayList<>();
    private ArrayList<String> morlist = new ArrayList<>();
    private ArrayList<String> typeList = SatrtVar.typeList;

    private CoordinatorLayout mLayout;
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


    // Para el selector de edades--------------------------------------------
    private int currSel1 = 0;
    private final List<String> mSpinList = Arrays.asList("Años", "Meses", "Dias", "");
    //-----------------------------------------------------------------------

    // Para el selector de tipo gando--------------------------------------------
    private int mainSel = 4;
    private int currSel2 = 0;
    private final List<String> mSpinL2 = Arrays.asList("Vaca", "Novilla", "Becerro", "Toro");
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
                Bundle mBundle = new Bundle();
                mBundle.putInt("mainsel", mainSel);
                mIntent.putExtras(mBundle);
                startActivity(mIntent);
                ViewActivity.this.finish();
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

        myToolbar.setTitleTextColor(ContextCompat.getColor(myToolbar.getContext(), R.color.inner_button));

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
        buttNext = findViewById(R.id.buttNext);
        buttPrev = findViewById(R.id.buttPrev);
        mLayout = findViewById(R.id.layout2);

        mImageView.setOnClickListener(this);
        mButtEdit.setOnClickListener(this);
        buttNext.setOnClickListener(this);
        buttPrev.setOnClickListener(this);

        mviewList.add(mView1);
        mviewList.add(mView2);
        mviewList.add(mView3);
        mviewList.add(mView4);
        mviewList.add(mMore1);
        mviewList.add(mMore2);
        mviewList.add(mMore3);
        mviewList.add(mMore4);
        mviewList.add(mMore5);

        mPermiss = SatrtVar.mPermiss;
        mainSel = SatrtVar.currSel2;
        typeList = SatrtVar.typeList;

        List<Usuario> listuser = SatrtVar.listuser;
        int userSiz = listuser.size();

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            currIdx = intent.getIntExtra("index", 0);
            int i = 0;
            currSel1 = Integer.parseInt(listuser.get(currIdx).sel1);
            currSel2 = Integer.parseInt(listuser.get(currIdx).sel2);
            if (currIdx < userSiz) {
                mviewList.get(i).setText(""+ listuser.get(currIdx).nombre.toUpperCase()+" ("+mSpinL2.get(currSel2)+")");
                i++;
                mviewList.get(i).setText("Color:   "+ listuser.get(currIdx).color.toUpperCase());
                i++;
                if(currSel2 == 0) {
                    mviewList.get(i).setText("Litros:   " + listuser.get(currIdx).litros + " Litros Diarios");
                }
                else {
                    mviewList.get(i).setVisibility(View.INVISIBLE);
                }
                i++;
                mviewList.get(i).setText("Edad:   "+dataConverted(listuser.get(currIdx).edad, currSel1)+ " "+mSpinList.get(currSel1));
                currDir = fmang.getImage(listuser.get(currIdx).imagen, mImageView);
                i++;
                setTextView(mviewList.get(i), listuser.get(currIdx).more1);
                i++;
                setTextView(mviewList.get(i), listuser.get(currIdx).more2);
                i++;
                setTextView(mviewList.get(i), listuser.get(currIdx).more3);
                i++;
                setTextView(mviewList.get(i), listuser.get(currIdx).more4);
                i++;
                setTextView(mviewList.get(i), listuser.get(currIdx).more5);
            }
        }
        else {
            textSnackbar("Aqui no hay :(");
        }
    }

    @SuppressLint("SetTextI18n")
    private void setTextView(TextView view, String txValue){
        if(txValue.isEmpty()){
            view.setVisibility(View.INVISIBLE);
        }
        else {
            morlist.add(txValue);
            String desc = moreValidate(txValue);
            view.setText( desc +"  "+ txValue.replaceAll(desc, ""));
        }
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            Intent mIntent = new Intent(getApplicationContext(), MainActivity.class);
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
            mBundle.putStringArrayList("morelist", morlist);
            mIntent.putExtras(mBundle);
            startActivity(mIntent);
            this.finish();
        }
        if(itemId == R.id.imageView){
            Intent mIntent = new Intent(this, ImgFullscreenActivity.class);
            Bundle mBundle = new Bundle();
            mBundle.putString("dir", currDir);
            mBundle.putInt("index", currIdx);
            mIntent.putExtras(mBundle);
            startActivity(mIntent);
        }
        if(itemId == R.id.buttNext){
            Intent mIntent = new Intent(this, ViewActivity.class);
            int newidx = currIdx;
            newidx++;
            int siz = typeList.size();
            newidx = (newidx < siz? newidx : 0 );
            if(mainSel == 4){
                mIntent.putExtras(getAndSetBundle(newidx));
                startActivity(mIntent);
                this.finish();
            }
            else {
                for (int i = newidx; i < siz; i++) {
                    if (Integer.parseInt(typeList.get(i)) == mainSel) {
                        mIntent.putExtras(getAndSetBundle(i));
                        startActivity(mIntent);
                        this.finish();
                        break;
                    } else if (i == (siz - 1)) {
                        for (int j = 0; j < siz && j != currIdx; j++) {
                            if (Integer.parseInt(typeList.get(j)) == mainSel) {
                                mIntent.putExtras(getAndSetBundle(j));
                                startActivity(mIntent);
                                this.finish();
                                break;
                            }
                        }
                    }
                }
            }
        }

        if(itemId == R.id.buttPrev){
            Intent mIntent = new Intent(this, ViewActivity.class);
            int newidx = currIdx;
            newidx--;
            int siz = typeList.size();
            if(siz != 0) {
                newidx = (newidx < 0 ? (siz - 1) : newidx);
            }
            else{
                newidx = 0;
            }
            if(mainSel == 4){
                mIntent.putExtras(getAndSetBundle(newidx));
                startActivity(mIntent);
                this.finish();
            }
            else {
                for(int i = newidx; i >=0 ; i-- ){
                    if(Integer.parseInt(typeList.get(i)) == mainSel){
                        mIntent.putExtras(getAndSetBundle(i));
                        startActivity(mIntent);
                        this.finish();
                        break;
                    }
                    else if (i == 0) {
                        for (int j = (siz - 1); j >= 0 && j != currIdx; j--) {
                            if(Integer.parseInt(typeList.get(j)) == mainSel){
                                mIntent.putExtras(getAndSetBundle(j));
                                startActivity(mIntent);
                                this.finish();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private Bundle  getAndSetBundle(int idx){
        Bundle mBundle = new Bundle();
        mBundle.putInt("index", idx);
        return mBundle;
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
    public String moreValidate(String text){
        Pattern patt = Pattern.compile("^(\\s?\\w{1,10}\\s?:\\s?)");
        Matcher matcher = patt.matcher(text);
        if(matcher.find()) {
            String[] txList =  text.split(":");
            return txList[0]+":";
        }
        return "Otros:";
    }
}