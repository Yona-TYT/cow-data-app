package com.example.cow_data.db.dao;

import androidx.room.Dao;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Insert;
import androidx.room.Update;

import com.example.cow_data.db.Fecha;

import java.util.List;

@Dao
public interface DaoDat extends GenericDao<Fecha>{
    @Query("SELECT * FROM fecha")
    List<Fecha> getUsers();

    @Query("SELECT * FROM fecha WHERE fecha= :user")
    Fecha getUsers(String user);

    @Insert
    void insertUser(Fecha...fechas);

    @Update
    void update(Fecha fecha);

    @Query("UPDATE fecha SET strdate= :strdate, date= :date, time= :time WHERE fecha= :user")
    void updateUser(String user, String strdate, Long date, Long time);

    @Query("DELETE FROM fecha WHERE  fecha= :user")
    void removerUser(String user);

    @Query("DELETE FROM fecha WHERE  uid= :uid")
    void removerUser(long uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void  insertUser(Fecha user);
}

