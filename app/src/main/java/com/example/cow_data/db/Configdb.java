package com.example.cow_data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import io.reactivex.annotations.NonNull;

@Entity
public class Configdb {
    @PrimaryKey(autoGenerate = true)
    public long uid;
    public String config;
    public String version;
    public String hexid;
    public String date;
    public String time;
    public String save1;
    public String save2;
    public String save3;

    public Configdb(@NonNull String config, String version, String hexid, String date, String time, String save1, String save2, String save3 ) {
        this.config = config;
        this.version = version;
        this.hexid = hexid;
        this.date = date;
        this.time = time;
        this.save1 = save1;
        this.save2 = save2;
        this.save3 = save3;
    }
}