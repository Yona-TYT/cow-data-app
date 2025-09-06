package com.example.cow_data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Usuario.class, QueueItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DaoUser daoUser();
    public abstract UsuarioDao usuarioDao();
    public abstract QueueItemDao queueItemDao();
}
