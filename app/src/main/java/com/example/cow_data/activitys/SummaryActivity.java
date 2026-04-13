package com.example.cow_data.activitys;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cow_data.AppContextProvider;
import com.example.cow_data.R;
import com.example.cow_data.StartVar;
import com.example.cow_data.db.Usuario;

import java.util.List;

public class SummaryActivity extends AppCompatActivity {

    private TextView mView1;
    private TextView mView2;
    private TextView mView3;
    private TextView mView4;
    private TextView mView5;
    private TextView mView6;
    private TextView mView7;

    private Button mbutt1;

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Se configura el Boton nav Back -----------------------------------------------
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                SummaryActivity.this.finish();
            }
        };
        onBackPressedDispatcher.addCallback(this, callback);
        //---------------------------------------------------------------------------------

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_summary);

        //Activate ToolBar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();
        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Resumen");
        actionBar.setDisplayShowHomeEnabled(true);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mView1 = findViewById(R.id.textSumm1);
        mView2 = findViewById(R.id.textSumm2);
        mView3 = findViewById(R.id.textSumm3);
        mView4 = findViewById(R.id.textSumm4);
        mView5 = findViewById(R.id.textSumm5);
        mView6 = findViewById(R.id.textSumm6);
        mView7 = findViewById(R.id.textSumm7);

        mbutt1 = findViewById(R.id.buttRet);

        mbutt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mIntent = new Intent(AppContextProvider.getAppContext(), ExcludedActivity.class);
                startActivity(mIntent);
            }
        });

        List<Usuario> listuser = StartVar.listuser;
        int userSiz = listuser.size();
        int allTotal = userSiz;
        int litTotal = 0;
        int vacTotal = 0;
        int novTotal = 0;
        int becTotal = 0;
        int torTotal = 0;
        int preTotal = 0;

        for(Usuario mUser : listuser){
            int type = mUser.sel2;
            // Type Vacas
            if(type == 0){
                //Solo las Vacas tienen valores de litros
                litTotal += Integer.parseInt(mUser.litros);
                vacTotal++;
            }
            // Type Novilla
            else if(type == 1){
                novTotal++;
            }
            // Type Becerro
            else if(type == 2){
                becTotal++;
            }
            // Type Toro
            else if(type == 3){
                torTotal++;
            }
            // Para Preñadas
            if(mUser.sel3 == 1){
                preTotal++;
            }

        }
        mView1.setText("Total Animales: "+ allTotal);
        mView2.setText("Total Litros: "+ litTotal+" L (Diarios)");
        mView3.setText("Total Vacas: "+ vacTotal);
        mView4.setText("Total Novillas: "+ novTotal);
        mView5.setText("Total Becerros: "+ becTotal);
        mView6.setText("Total Toros: "+ torTotal);
        mView7.setText("Total Preñadas: "+preTotal);

    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId == android.R.id.home){
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}