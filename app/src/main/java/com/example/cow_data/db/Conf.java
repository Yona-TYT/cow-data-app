package com.example.cow_data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import io.reactivex.annotations.NonNull;

@Entity
public class Conf {
    @PrimaryKey(autoGenerate = true)
    public long uid;
    public String config;
    public String version;
    public String hexid;
    public String datetasa;
    public Double dolar;
    public Double margen;
    public Long date;
    public Long time;
    public Integer curr;
    public Integer moneda;
    public Integer mes;
    public Integer show;
    public String datos;
    public String dbg;

    public Conf(@NonNull String config, String version, String hexid, String datetasa, Double dolar,
                Double margen, Long date, Long time, Integer curr, Integer moneda, Integer mes,
                Integer show, String datos, String dbg) {
        this.config = config;
        this.version = version;
        this.hexid = hexid;
        this.datetasa = datetasa;
        this.dolar = dolar;
        this.date = date;
        this.time = time;
        this.curr = curr;
        this.moneda = moneda;
        this.margen = margen;
        this.mes = mes;
        this.show = show;
        this.datos = datos;
        this.dbg = dbg;
    }
}