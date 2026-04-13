package com.example.cow_data.activitys;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cow_data.AppContextProvider;
import com.example.cow_data.Basic;
import com.example.cow_data.FilesManager;
import com.example.cow_data.R;
import com.example.cow_data.StartVar;
import com.example.cow_data.db.Usuario;
import com.example.cow_data.db.UsuarioQueue;
import com.example.cow_data.drive.GoogleDriveManager;
import com.example.cow_data.ex.PreferenceHelper;
import com.example.cow_data.drive.SetWorkResult;

import net.openid.appauth.AuthState;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.lifecycle.ProcessLifecycleOwner;

public class Preloader extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preloder);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS+"/.cowdata/");
        if(file.exists()){
            FilesManager.DeleteFile(file);
        }

        //Check valus before start main activity
        //Satrted variables
        StartVar startVar = new StartVar(getApplicationContext());
        startVar.setUserListDB();
        startVar.setmActivity(this);
        new Basic(getApplicationContext());

        StartVar.mLifecycle = ProcessLifecycleOwner.get();

        //Se reinicia el cursor para el gallery adapter
        PreferenceHelper.getInstance().setGalleryPosition(0, 0);

        //Se crea el directorio de .cowdate
        FilesManager.directoryCreate();

        //Instancia de la base de datos
        StartVar.getUserListDB();
        List<Usuario> listuser =  StartVar.listuser;

        // Inicializar la variable para las colas
        StartVar.usuarioQueue = new UsuarioQueue(StartVar.mLifecycle, AppContextProvider.getAppContext());

        for(Usuario mUser : listuser){
            Integer selecTx = mUser.sel3;
            String userTx = mUser.usuario;

            String dateTx = mUser.pre;

            if (selecTx == 1 && !dateTx.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //Inicia la fecha actual
                    long currDate = LocalDate.now().getLong(ChronoField.EPOCH_DAY);
                    //Inicia la fecha de pre
                    long mDate = LocalDate.parse(dateTx).plusDays(StartVar.mDayB).getLong(ChronoField.EPOCH_DAY);
                    if(currDate > mDate) {
                        //Toast.makeText(Preloder.this, "A: " + currDate + " y B: " + mDate, Toast.LENGTH_LONG).show();
                        //Desactiva el selector pre
                        StartVar.appDatabase.daoUser().updateSelecPre(userTx, 0);
                    }
                }
            }
        }
        GoogleDriveManager manager = new GoogleDriveManager(PreferenceHelper.getInstance());
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        SetWorkResult mWorkResult = new SetWorkResult(StartVar.mLifecycle, executorService, manager);

        AuthState authState = new AuthState();
        authState = GoogleDriveManager.getAuthState();

        //En caso de estancar se forza el inicio de mainActivity
        startMainDelayErr(30000);

        if(authState.isAuthorized()) {
            if(!StartVar.mainStart) {
                Basic.msg("Sincronizando Datos...");
                manager.dataSynchronizeStarting();
                mWorkResult.observeWorkResult();
                return;
            }
        }
        else{
            StartVar.setmMainStart(true);
        }

        if(StartVar.mainStart) {
            startMainDelay(800);
        }
    }

    private void startMainDelay(int s){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Esto inicia las actividad Main despues de tiempo de espera del preloder
                startActivity(new Intent(Preloader.this, MainActivity.class));
                finish(); //Finaliza la actividad y ya no se accede mas
            }
        }, s);
    }

    private void startMainDelayErr(int s){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(StartVar.mActivity == null || StartVar.mActivity.getClass().getSimpleName().equals("Preloader")) {
                    //Esto inicia las actividad Main despues de tiempo de espera del preloder
                    startActivity(new Intent(Preloader.this, MainActivity.class));
                    Basic.msg("Algo fallo, Inicio forzado!");
                    finish(); //Finaliza la actividad y ya no se accede mas
                }
            }
        }, s);
    }
}