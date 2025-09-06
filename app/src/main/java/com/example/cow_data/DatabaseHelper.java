package com.example.cow_data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import com.example.cow_data.ex.GoogleDriveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private final Context context;
    private final File dbFile;

    public DatabaseHelper(Context context, File dbFile) {
        this.context = context;
        this.dbFile = dbFile;
    }

    /**
     * Abre un archivo de base de datos SQLite (.db) desde una ruta específica.
     *
     * param dbPath Ruta completa al archivo .db (por ejemplo, "/sdcard/GPSLogger/mydb.db").
     * @return SQLiteDatabase abierta o null si falla.
     */
    public SQLiteDatabase openDatabase() {
        String dbPath = dbFile.getAbsolutePath();
        try {
            if (!dbFile.exists()) {
                Basic.msg("El archivo de base de datos no existe");
                return null;
            }

            SQLiteDatabase database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
            Basic.msg("Base de datos abierta exitosamente");
            return database;

        } catch (Exception e) {
            Basic.msg("Error al abrir la base de datos: " + e.getMessage());
            return null;
        }
    }
    /**
     * Realiza una consulta de ejemplo a una tabla en la base de datos.
     *
     * @param database Instancia de SQLiteDatabase.
     * @param tableName Nombre de la tabla a consultar.
     */
    public void queryDatabase(SQLiteDatabase database, String tableName) {
        if (database == null) {
            Basic.msg("La base de datos es nula, no se puede realizar la consulta");
            return;
        }

        try {
            Cursor cursor = database.rawQuery("SELECT * FROM " + tableName, null);
            if (cursor != null) {
                Basic.msg("Consulta exitosa: " + cursor.getCount() + " filas encontradas");
                cursor.close();
            } else {
                Basic.msg("No se encontraron datos en la tabla " + tableName);
            }
        } catch (Exception e) {
            Basic.msg("Error al consultar la base de datos: " + e.getMessage());
        }
    }


    /**
     * Extrae todos los datos de una tabla y los devuelve como una lista de arreglos de cadenas.
     *
     * @param database Instancia de SQLiteDatabase.
     * @param tableName Nombre de la tabla a consultar.
     * @return Lista de arreglos de cadenas, donde cada arreglo representa una fila.
     */
    public List<String[]> getAllDataFromTable(SQLiteDatabase database, String tableName) {
        List<String[]> dataList = new ArrayList<>();
        if (database == null) {
            Basic.msg("La base de datos es nula, no se puede realizar la consulta");
            return dataList;
        }

        try {
            Cursor cursor = database.rawQuery("SELECT * FROM " + tableName, null);
            if (cursor != null) {
                int columnCount = cursor.getColumnCount();
                String[] columnNames = cursor.getColumnNames();

                while (cursor.moveToNext()) {
                    String[] rowData = new String[columnCount];
                    for (int i = 0; i < columnCount; i++) {
                        // Obtener el valor de la columna como cadena
                        rowData[i] = cursor.getString(i);
                    }
                    dataList.add(rowData);
                }
                cursor.close();
                Basic.msg("Datos extraídos: " + dataList.size() + " filas en la tabla " + tableName);
            } else {
                Basic.msg("No se encontraron datos en la tabla " + tableName);
            }
        } catch (Exception e) {
            Basic.msg("Error al extraer datos: " + e.getMessage());
        }
        return dataList;
    }

    /**
     * Cierra la base de datos si está abierta.
     *
     * @param database Instancia de SQLiteDatabase.
     */
    public void closeDatabase(SQLiteDatabase database) {
        if (database != null && database.isOpen()) {
            database.close();
            Basic.msg("Base de datos cerrada");
        }
    }
}