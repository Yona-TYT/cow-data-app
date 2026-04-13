package com.example.cow_data.db;

import androidx.room.Dao;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Insert;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DaoUser {
//    @Query("SELECT * FROM usuario")
//    List<Usuario> getUsers();

    @Query("SELECT * FROM usuario")
    List<Usuario> getUsers();

    @Query("SELECT * FROM usuario WHERE sel4 >= :sel4")
    List<Usuario> getUsers(Integer sel4);

//    @Query("SELECT * FROM usuario WHERE sel3 = 1")
//    List<Usuario> getExclList();

    @Query("SELECT * FROM usuario WHERE usuario= :user")
    Usuario getUsers(String user);

    @Insert
    void insetUser(Usuario...usuarios);

    @Update
    void updateUser(Usuario usuario); // Método para actualizar un usuario

    @Query("UPDATE usuario SET nombre= :nombre, color= :color, litros= :litros, edad= :edad, pre= :pre, imagen= :imagen, sel1= :sel1, sel2= :sel2, sel3= :sel3 WHERE usuario= :user")
    void updateUser(String user, String nombre, String color, String litros,String edad, String pre, String imagen, Integer sel1, Integer sel2, Integer sel3 );

    @Query("UPDATE usuario SET more1= :more1, more2= :more2, more3= :more3, more4= :more4 WHERE usuario= :user")
    void updateMore(String user, String more1, String more2, String more3, String more4 );

    @Query("UPDATE usuario SET sel4= :sel4 WHERE usuario= :user")
    void updateStatus(String user, Integer sel4);

    @Query("UPDATE usuario SET usuario= :user  WHERE uid= :uid")
    void updateUsuario(long uid, String user);

    @Query("UPDATE usuario SET sel3= :sel3 WHERE usuario= :user")
    void updateSelecPre(String user, Integer sel3);

    @Query("DELETE FROM usuario WHERE  usuario= :user")
    void removerUser(String user);

    @Query("DELETE FROM usuario WHERE  uid= :uid")
    void removerUser(long uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void  insertUser(Usuario user);
}

