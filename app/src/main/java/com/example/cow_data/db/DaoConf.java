package com.example.cow_data.db;

import androidx.room.Dao;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Insert;

import java.util.List;

@Dao
public interface DaoConf {
    @Query("SELECT * FROM Configdb")
    List<Configdb> getUsers();

    @Query("SELECT * FROM Configdb WHERE config= :user")
    Configdb getUsers(String user);

    @Insert
    void insetUser(Configdb...config);

    @Query("UPDATE Configdb SET version = :version, hexid= :hexid, date= :date, time= :time, save1= :save1, save2= :save2, save3= :save3 WHERE config= :user")
    void updateUser(String user, String version, String hexid, String date, String time, String save1, String save2, String save3);

    @Query("UPDATE Configdb SET date= :date, time= :time WHERE config= :user")
    void updateDateTime(String user, String date, String time);

    @Query("UPDATE Configdb SET save1= :save1, save2= :save2, save3= :save3 WHERE config= :user")
    void updateSaves(String user, String save1, String save2, String save3);

    @Query("DELETE FROM Configdb WHERE  config= :user")
    void removerUser(String user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void  insertUser(Configdb user);
}
