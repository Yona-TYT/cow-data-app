package com.example.cow_data.db;

import io.reactivex.annotations.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Fecha {
    @PrimaryKey(autoGenerate = true)
    public long uid;
    public String fecha;
    public String strdate;
    public Long date;
    public Long time;

    public Fecha(@NonNull String fecha, String strdate, Long date, Long time)
    {
            this.fecha = fecha;
            this.strdate = strdate;
            this.date = date;
            this.time = time;
    }

    // Getter requerido para el sorting (y otros accesos)
    public Long getDate() {
        return date;
    }
}
