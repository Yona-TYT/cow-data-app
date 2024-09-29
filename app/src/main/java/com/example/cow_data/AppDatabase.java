package com.example.cow_data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {Usuario.class},
        version = 1
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DaoUser daoUser();
}
