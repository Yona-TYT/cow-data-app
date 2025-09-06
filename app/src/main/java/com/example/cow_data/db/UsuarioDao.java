package com.example.cow_data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UsuarioDao {
    @Insert
    void insert(Usuario usuario);

    @Query("SELECT * FROM usuario")
    List<Usuario> getAllUsuarios();
}
