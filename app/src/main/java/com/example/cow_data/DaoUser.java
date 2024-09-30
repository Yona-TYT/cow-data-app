package com.example.cow_data;

import androidx.room.Dao;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Insert;
import java.util.List;

@Dao
public interface DaoUser {
    @Query("SELECT * FROM usuario")
    List<Usuario> getUsers();

    @Query("SELECT * FROM usuario WHERE usuario= :user")
    Usuario getUsers(String user);

    @Insert
    void insetUser(Usuario...usuarios);

    @Query("UPDATE usuario SET nombre= :nombre, color= :color, litros= :litros, edad= :edad, imagen= :imagen, sel1= :sel1, sel2= :sel2, more1= :more1, more2= :more2, more3= :more3, more4= :more4, more5= :more5 WHERE usuario= :user")
    void updateUser(String user, String nombre, String color, String litros,String edad, String imagen, String sel1, String sel2, String more1 , String more2, String more3, String more4, String more5 );

    @Query("DELETE FROM usuario WHERE  usuario= :user")
    void removerUser(String user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void  insertUser(Usuario user);
}

