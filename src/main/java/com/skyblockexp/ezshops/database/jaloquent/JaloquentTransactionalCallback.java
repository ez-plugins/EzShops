package com.skyblockexp.ezshops.database.jaloquent;

/**
 * Functional callback invoked by {@link JaloquentClientAdapter#transactional}.
 */
@FunctionalInterface
public interface JaloquentTransactionalCallback {
    void execute(JaloquentTransaction tx) throws Exception;
}
