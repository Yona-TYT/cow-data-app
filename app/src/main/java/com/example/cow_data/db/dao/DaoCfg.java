package com.example.cow_data.db.dao;

import androidx.room.Dao;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Insert;
import androidx.room.Update;

import com.example.cow_data.db.Conf;

import java.util.List;

@Dao
public interface DaoCfg extends GenericDao<Conf>{
    @Query("SELECT * FROM Conf")
    List<Conf> getUsers();

    @Query("SELECT * FROM Conf WHERE config= :user")
    Conf getUsers(String user);

    @Insert
    void insertUser(Conf...config);

    @Update
    void update(Conf config);

    @Query("UPDATE Conf SET version = :version, hexid= :hexid, datetasa= :datetasa, dolar= :dolar, margen= :margen, date= :date, time= :time, curr= :curr, moneda= :moneda, mes= :mes, show= :show, datos= :datos, dbg= :dbg WHERE config= :user")
    void updateUser(String user, String version, String hexid, String datetasa, Double dolar, Double margen, Long date, Long time, Integer curr, Integer moneda, Integer mes, Integer show, String datos, String dbg);

    @Query("UPDATE Conf SET date= :date, time= :time, dbg= :dbg WHERE config= :user")
    void updateDateTime(String user, Long date, Long time, String dbg);

    @Query("UPDATE Conf SET curr= :curr, dolar= :dolar, moneda= :moneda, mes= :mes WHERE config= :user")
    void updateSaves(String user, Integer curr, Double dolar, Integer moneda, Integer mes);

    @Query("UPDATE Conf SET curr= :curr WHERE config= :user")
    void updateCurrAcc(String user, Integer curr);
    @Query("UPDATE Conf SET curr= dolar= :dolar WHERE config= :user")
    void updateDolar(String user, Double dolar);
    @Query("UPDATE Conf SET moneda= :moneda WHERE config= :user")
    void updateMoneda(String user, Integer moneda);
    @Query("UPDATE Conf SET mes= :mes WHERE config= :user")
    void updateMes(String user, Integer mes);
    @Query("UPDATE Conf SET show= :show WHERE config= :user")
    void updateView(String user, Integer show);

    @Query("DELETE FROM Conf WHERE  config= :user")
    void removerUser(String user);

    @Query("DELETE FROM Conf WHERE  uid= :uid")
    void removerUser(long uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void  insertUser(Conf user);
}
