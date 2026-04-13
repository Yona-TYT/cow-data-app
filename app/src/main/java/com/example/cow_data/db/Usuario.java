package com.example.cow_data.db;

import io.reactivex.annotations.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public long uid;
    public String usuario;
    public String nombre;
    public String color;
    public String litros;
    public String edad;
    public String pre;
    public String imagen;
    public Integer sel1;
    public Integer sel2;
    public Integer sel3;
    public Integer sel4;
    public String more1;
    public String more2;
    public String more3;
    public String more4;

    public Usuario(@NonNull String usuario, String nombre, String color, String litros, String edad, String pre, String imagen, Integer sel1, Integer sel2, Integer sel3, Integer sel4, String more1, String more2, String more3, String more4) {
            this.usuario = usuario;
            this.nombre = nombre;
            this.color = color;
            this.litros = litros;
            this.edad = edad;
            this.pre = pre;
            this.imagen = imagen;
            this.sel1 = sel1;
            this.sel2 = sel2;
            this.sel3 = sel3;
            this.sel4 = sel4;
            this.more1 = more1;
            this.more2 = more2;
            this.more3 = more3;
            this.more4 = more4;


    }

    public static String getUserId(DaoUser mDao){
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