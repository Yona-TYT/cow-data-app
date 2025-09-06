package com.example.cow_data.activitys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import com.example.cow_data.CalcCalendar;
import com.example.cow_data.FilesManager;
import com.example.cow_data.R;
import com.example.cow_data.StartVar;
import com.example.cow_data.db.AppDatabase;
import com.example.cow_data.db.Usuario;
import com.google.android.material.snackbar.Snackbar;


public class ViewActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mView1;
    private TextView mView2;
    private TextView mView3;
    private TextView mView4;
    private TextView mView5;

    private TextView mMore1;
    private TextView mMore2;
    private TextView mMore3;
    private TextView mMore4;

    private LinearLayout mLay2;

    private ImageView mImageView;
    private Button mButtEdit;
    private ImageButton buttNext;
    private ImageButton buttPrev;


    private CalendarView mCalen1;
    private Calendar mCalend;
    private Button mButtCale;
    private Button mButtCanc;
    private boolean mSwCale = false;

    private List<TextView> mviewList = new ArrayList<>();
    private ArrayList<String> morlist = new ArrayList<>();
    private ArrayList<Object> typeList = StartVar.typeList;

    private CoordinatorLayout mLay1;
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

    //Base de datos
    public AppDatabase appDatabase = StartVar.appDatabase;

    @SuppressLint({"MissingInflatedId", "RestrictedApi", "SetTextI18n"})
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
        mView5 = findViewById(R.id.txView5);

        mMore1 = findViewById(R.id.txMore1);
        mMore2 = findViewById(R.id.txMore2);
        mMore3 = findViewById(R.id.txMore3);
        mMore4 = findViewById(R.id.txMore4);

        mImageView = findViewById(R.id.imageView);
        mButtEdit = findViewById(R.id.buttEdit);
        buttNext = findViewById(R.id.buttNext);
        buttPrev = findViewById(R.id.buttPrev);
        mButtCale = findViewById(R.id.buttCale);
        mButtCanc = findViewById(R.id.buttClos);
        mLay1 = findViewById(R.id.layout2);
        mLay2 = findViewById(R.id.lay1);

        mCalen1 = findViewById(R.id.calenView1);

        mImageView.setOnClickListener(this);
        mButtEdit.setOnClickListener(this);
        buttNext.setOnClickListener(this);
        buttPrev.setOnClickListener(this);
        mButtCale.setOnClickListener(this);
        mButtCanc.setOnClickListener(this);

        mviewList.add(mView1);
        mviewList.add(mView2);
        mviewList.add(mView3);
        mviewList.add(mView4);
        mviewList.add(mView5);

        mviewList.add(mMore1);
        mviewList.add(mMore2);
        mviewList.add(mMore3);
        mviewList.add(mMore4);

        mCalend = Calendar.getInstance();
        mPermiss = StartVar.mPermiss;
        mainSel = StartVar.currSel2;
        typeList = StartVar.typeList;

        List<Usuario> listuser = StartVar.listuser;

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            currIdx = intent.getIntExtra("index", 0);
            int i = 0;
            Usuario mUser = listuser.get(currIdx);
            if (mUser != null) {
                currSel1 = Integer.parseInt(mUser.sel1);
                currSel2 = Integer.parseInt(mUser.sel2);

                mviewList.get(i).setText(""+ mUser.nombre.toUpperCase()+" ("+mSpinL2.get(currSel2)+")");
                i++;
                mviewList.get(i).setText("Color:   "+ mUser.color.toUpperCase());
                i++;
                if(currSel2 == 0) {
                    mviewList.get(i).setText("Litros:   " + mUser.litros + " Litros Diarios");
                }
                else {
                    mviewList.get(i).setVisibility(View.INVISIBLE);
                }
                i++;
                mviewList.get(i).setText("Edad: "+ CalcCalendar.getBrithDateText(mUser.edad));
                currDir = fmang.getImage(mUser.imagen, mImageView);
                i++;
                if(mUser.sel3.equals("1")) {
                    mviewList.get(i).setText("Fecha de Parto: (" + mUser.pre + ")");

                    mCalen1.setDate(CalcCalendar.getDateFromDays(mCalend, mUser.pre, StartVar.mDayA-1));
                    mCalen1.setMinDate(CalcCalendar.getDateFromDays(mCalend, mUser.pre, StartVar.mDayA));
                    mCalen1.setMaxDate(CalcCalendar.getDateFromDays(mCalend, mUser.pre, StartVar.mDayB));

                    //Log.d("Calendar", "-->>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+CalcCalendar.getDateFromDays(mCalend, mUser.pre, StartVar.mDayA));

                }
                else {
                    mviewList.get(i).setVisibility(View.INVISIBLE);
                    mCalen1.setVisibility(View.INVISIBLE);
                    mButtCale.setVisibility(View.INVISIBLE);

                }
                i++;
                setTextList(mviewList.get(i), mUser.more1,"Madre de: ");
                //setTextView(mviewList.get(i), mUser.more1);
                i++;
                setTextList(mviewList.get(i), mUser.more2, "De la Vaca: ");

                //setTextView(mviewList.get(i), mUser.more2);
                i++;
                setTextView(mviewList.get(i), mUser.more3);
                i++;
                setTextView(mviewList.get(i), mUser.more4);

            }
        }
        else {
            textSnackbar("Aqui no hay :(");
        }
    }

    @SuppressLint("SetTextI18n")
    private void setTextView(TextView view, String txValue){
        txValue = txValue.replaceFirst("@null","");
        if(txValue.isEmpty()){
            view.setVisibility(View.INVISIBLE);
        }
        else {
            morlist.add(txValue);
            String desc = moreValidate(txValue);
            view.setText( desc +"  "+ txValue.replaceAll(desc, ""));
        }
    }
    @SuppressLint("SetTextI18n")
    private void setTextList(TextView view, String txValue, String TxTag){

        String[] mSplit = txValue.split(",");
        String textList = "";

            for (String s : mSplit){
            Usuario mU = appDatabase.daoUser().getUsers(s);
            if( mU != null){
                String name = mU.nombre.replaceAll("\\d","");
                String number = mU.nombre.replaceAll("\\D","");
                if(number.isEmpty()) {
                    textList += name.toLowerCase() + ", ";
                }
                else {
                    textList += number + ", ";
                }
            }
        }
        if(textList.isEmpty()){
            view.setVisibility(View.INVISIBLE);
        }
        textList = textList.replaceAll(",\\s$","");
        view.setText(TxTag+textList);
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

        if (itemId == R.id.buttCale) {
            mSwCale = !mSwCale;

            if(mSwCale){
                mButtCanc.setVisibility(View.VISIBLE);
                mCalen1.setVisibility(View.VISIBLE);

                mButtEdit.setVisibility(View.INVISIBLE);
                buttPrev.setVisibility(View.INVISIBLE);
                buttNext.setVisibility(View.INVISIBLE);
                mLay2.setVisibility(View.INVISIBLE);
            }
        }

        if (itemId == R.id.buttClos) {
           mButtCanc.setVisibility(View.INVISIBLE);
           mCalen1.setVisibility(View.INVISIBLE);

           mButtEdit.setVisibility(View.VISIBLE);
           buttPrev.setVisibility(View.VISIBLE);
           buttNext.setVisibility(View.VISIBLE);
           mLay2.setVisibility(View.VISIBLE);
        }


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
                    if (Integer.parseInt((String)typeList.get(i)) == mainSel) {
                        mIntent.putExtras(getAndSetBundle(i));
                        startActivity(mIntent);
                        this.finish();
                        break;
                    } else if (i == (siz - 1)) {
                        for (int j = 0; j < siz && j != currIdx; j++) {
                            if (Integer.parseInt((String)typeList.get(j)) == mainSel) {
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
                    if(Integer.parseInt((String)typeList.get(i)) == mainSel){
                        mIntent.putExtras(getAndSetBundle(i));
                        startActivity(mIntent);
                        this.finish();
                        break;
                    }
                    else if (i == 0) {
                        for (int j = (siz - 1); j >= 0 && j != currIdx; j--) {
                            if(Integer.parseInt((String)typeList.get(j)) == mainSel){
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
        Snackbar mySnackbar = Snackbar.make(mLay1, text, Snackbar.LENGTH_SHORT);
       //mySnackbar.show();

        Toast.makeText(this, text, Toast.LENGTH_SHORT);
    }

    boolean isBlockedPath(Context ctx, String fdCanonical) {
        // Paths that should rarely be exposed
        if (fdCanonical.startsWith("content://media/"+MediaStore.VOLUME_EXTERNAL_PRIMARY) || fdCanonical.startsWith("/data/misc/")) {
            return true;
        }
        return false;
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