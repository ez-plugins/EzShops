package com.skyblockexp.ezshops.database.jaloquent;

import java.util.List;
import java.util.Map;

/**
 * Adapter interface that abstracts Jaloquent (or any DB client) operations
 * used by the Jaloquent-backed repository. Implementations should adapt the
 * real Jaloquent API (or raw JDBC) to these methods.
 */
public interface JaloquentClientAdapter {

    /** Initialise the client with configuration. */
    void init(Map<String, String> config) throws Exception;

    /** Ensure the player shops table exists. */
    void createTableIfAbsent() throws Exception;

    /**
     * Select all rows from the shops table. Each map represents a row with
     * column-name -> string-value entries.
     */
    List<Map<String, String>> selectAllShops() throws Exception;

    /**
     * Execute a transactional block. The provided callback is invoked with a
     * {@link JaloquentTransactionalCallback} instance inside a transaction.
     */
    void transactional(JaloquentTransactionalCallback callback) throws Exception;

    /** Close resources. */
    void close() throws Exception;
}
