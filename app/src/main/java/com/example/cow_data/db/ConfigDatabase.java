package com.example.cow_data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.cow_data.db.dao.DaoCfg;

@Database(
        entities = {Conf.class},
        version = 1
)
public abstract class ConfigDatabase extends RoomDatabase {
    public abstract DaoCfg daoCfg();
}