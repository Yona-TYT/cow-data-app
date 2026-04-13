package com.example.cow_data;

import android.app.Application;
import android.content.Context;

public class AppContextProvider extends Application {
    private static AppContextProvider sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        // Inicializamos AppData de forma segura
        //GlobalData.initialize(this);
        sInstance = this;

        new Basic(this);
    }

    public static AppContextProvider getInstance() {
        return sInstance;
    }

    public static Context getAppContext() {
        return sInstance != null ? sInstance.getApplicationContext() : null;
    }
}