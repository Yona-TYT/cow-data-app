package com.example.cow_data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "queue_items")
public class QueueItem {
//    @PrimaryKey(autoGenerate = true)
//    public long id;
//    public String usuarioJson; // Almacena el objeto Usuario como JSON
//    public long order; // Para mantener el orden de la cola
//
//    public QueueItem(String usuarioJson, long order) {
//        this.usuarioJson = usuarioJson;
//        this.order = order;
//    }


    @PrimaryKey(autoGenerate = true)
    public int id;
    public String json;
    public String tipo; // Guardará "Deuda", "Cliente", "Cuenta", etc.
    public long order;

    public QueueItem(String json, String tipo, long order) {
        this.json = json;
        this.tipo = tipo;
        this.order = order;
    }
}