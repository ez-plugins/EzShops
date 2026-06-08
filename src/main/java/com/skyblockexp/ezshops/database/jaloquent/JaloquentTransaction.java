package com.skyblockexp.ezshops.database.jaloquent;

import java.util.Map;

/**
 * Simple transactional helper used by {@link JaloquentClientAdapter#transactional}.
 * Implementations should provide atomic delete/insert helpers for the
 * repository to call within a transaction.
 */
public interface JaloquentTransaction {

    /** Delete all rows from the shops table. */
    void deleteAllShops() throws Exception;

    /** Insert a single shop row. Values map column -> object (String/Number). */
    void insertShop(Map<String, Object> values) throws Exception;
}
