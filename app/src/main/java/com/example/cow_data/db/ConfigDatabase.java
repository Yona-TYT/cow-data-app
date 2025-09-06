package com.example.cow_data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {Configdb.class},
        version = 1
)
public abstract class ConfigDatabase extends RoomDatabase {
    public abstract DaoConf daoConf();
}