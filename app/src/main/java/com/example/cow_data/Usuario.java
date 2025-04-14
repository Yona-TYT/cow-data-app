package com.example.cow_data;

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
    public String sel1;
    public String sel2;
    public String sel3;
    public String more1;
    public String more2;
    public String more3;
    public String more4;

    public Usuario(@NonNull String usuario, String nombre, String color, String litros, String edad, String pre, String imagen, String sel1, String sel2, String sel3, String more1, String more2, String more3, String more4) {
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
            this.more1 = more1;
            this.more2 = more2;
            this.more3 = more3;
            this.more4 = more4;


    }
}