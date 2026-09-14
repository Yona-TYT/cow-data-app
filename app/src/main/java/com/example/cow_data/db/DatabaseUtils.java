package com.example.cow_data.db;

import com.example.cow_data.db.dao.GenericDao;
import java.util.List;

public class DatabaseUtils {

    /**
     * Genera un ID único basado en un prefijo y el DAO genérico.
     * Busca el primer slot vacío en la secuencia (ej. "payID0", "payID1", etc.).
     * @param basePrefix Prefijo para el ID (ej. "payID")
     * @param dao El DAO de la entidad correspondiente
     * @return El nuevo ID generado
     */
    public static <T> String generateId(String basePrefix, GenericDao<T> dao) {
        List<T> allEntities = dao.getUsers();
        int mSiz = allEntities.size();
        String mIdx = basePrefix + "0";
        if (mSiz > 0) {
            mIdx = basePrefix + mSiz;
        }
        for (int i = 0; i < mSiz; i++) {
            T entity = dao.getUsers(basePrefix + i);
            if (entity == null) {
                mIdx = basePrefix + i;
                break;
            }
        }
        return mIdx;
    }

    /**
     * Ejemplo de otra utilidad: Cuenta el número total de entidades en un DAO.
     * Útil para validaciones o reportes.
     * @param dao El DAO genérico
     * @return Número de entidades
     */
    public static <T> int countEntities(GenericDao<T> dao) {
        return dao.getUsers().size();
    }

    /**
     * Ejemplo: Limpia todas las entidades de un DAO (usa con cuidado, en transacciones).
     * @param dao El DAO genérico
     */
    public static <T> void clearAll(GenericDao<T> dao) {
        // Asumiendo que agregas un método deleteAll() en GenericDao o en los DAOs
        // dao.deleteAll();  // Implementa si lo necesitas
    }

    /**
     * Compares two entities safely to check if their data is identical.
     * Prevents unnecessary Room database writes if no changes occurred.
     *
     * @param oldEntity The currently stored object (can be null)
     * @param newEntity The object with new data (can be null)
     * @return true if data is identical, false if there are changes
     */
    public static <T> boolean isIdentical(T oldEntity, T newEntity) {
        return java.util.Objects.equals(oldEntity, newEntity);
    }

    /**
     * Compares two lists of entities to check if database content has changed.
     * Useful before executing bulk inserts or sync operations.
     *
     * @param oldList Current list in Room
     * @param newList New list received (e.g., from server API)
     * @return true if lists contain the exact same data in the same order
     */
    public static <T> boolean isListIdentical(List<T> oldList, List<T> newList) {
        return java.util.Objects.equals(oldList, newList);
    }

}
