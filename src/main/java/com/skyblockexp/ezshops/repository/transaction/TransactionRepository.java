package com.skyblockexp.ezshops.repository.transaction;

/** Simple persistence API for transaction records. */
public interface TransactionRepository {
    /** Persist a single record. Implementations may be synchronous. */
    void record(TransactionRecord record) throws Exception;

    /** Close any resources held by the repository. */
    default void close() throws Exception {}
}
