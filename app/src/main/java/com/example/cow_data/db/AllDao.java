package com.example.cow_data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.cow_data.db.dao.DaoCfg;
import com.example.cow_data.db.dao.DaoDat;
import com.example.cow_data.db.dao.QueueItemDao;

@Database(
        entities = {Conf.class, Usuario.class, Fecha.class, QueueItem.class},
        version = 1,
        exportSchema = false  // Opcional: evita exportar el esquema en builds de debug
)
public abstract class AllDao extends RoomDatabase {
    public abstract DaoCfg daoCfg();
    public abstract DaoUser daoUser();
    public abstract DaoDat daoDat();
    public abstract QueueItemDao daoQueue();
}
