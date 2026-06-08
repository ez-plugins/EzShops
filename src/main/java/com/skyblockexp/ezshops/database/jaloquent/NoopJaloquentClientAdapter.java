package com.skyblockexp.ezshops.database.jaloquent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * No-op adapter used as a placeholder until a real Jaloquent adapter is
 * implemented. All operations throw an exception to signal the adapter is
 * not available.
 */
public final class NoopJaloquentClientAdapter implements JaloquentClientAdapter {

    @Override
    public void init(Map<String, String> config) throws Exception {
        throw new IllegalStateException("Jaloquent adapter not configured");
    }

    @Override
    public void createTableIfAbsent() throws Exception {
        throw new IllegalStateException("Jaloquent adapter not configured");
    }

    @Override
    public List<Map<String, String>> selectAllShops() throws Exception {
        return Collections.emptyList();
    }

    @Override
    public void transactional(JaloquentTransactionalCallback callback) throws Exception {
        throw new IllegalStateException("Jaloquent adapter not configured");
    }

    @Override
    public void close() throws Exception {
        // no-op
    }
}
