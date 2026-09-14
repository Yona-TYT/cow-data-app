package com.example.cow_data.db.dao;

import java.util.List;

public interface GenericDao<T> {
    List<T> getUsers();  // Método para obtener todas las entidades (adapta si tus DAOs usan nombres diferentes, ej. getUsers())
    T getUsers(String id);  // Método para obtener una entidad por ID string (adapta si es necesario)
}
