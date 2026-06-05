package com.skyblockexp.ezshops.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LocalTransactionCacheTest {

    @Test
    void add_and_get_work_correctly() {
        LocalTransactionCache cache = new LocalTransactionCache();
        cache.add("DIAMOND", 5);
        assertEquals(5, cache.get("DIAMOND"));
        cache.add("DIAMOND", 3);
        assertEquals(8, cache.get("DIAMOND"));
        cache.add("GOLD", 10);
        assertEquals(10, cache.get("GOLD"));
    }

    @Test
    void get_returns_zero_for_unknown_product() {
        LocalTransactionCache cache = new LocalTransactionCache();
        assertEquals(0, cache.get("UNKNOWN"));
    }

    @Test
    void drain_returns_all_entries_and_clears_cache() {
        LocalTransactionCache cache = new LocalTransactionCache();
        cache.add("DIAMOND", 5);
        cache.add("GOLD", -3);
        
        Map<String, Integer> drained = cache.drain();
        assertEquals(2, drained.size());
        assertEquals(5, drained.get("DIAMOND"));
        assertEquals(-3, drained.get("GOLD"));
        
        assertTrue(cache.isEmpty());
    }

    @Test
    void isEmpty_returns_true_when_empty() {
        LocalTransactionCache cache = new LocalTransactionCache();
        assertTrue(cache.isEmpty());
        cache.add("DIAMOND", 1);
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    void clear_removes_all_entries() {
        LocalTransactionCache cache = new LocalTransactionCache();
        cache.add("DIAMOND", 5);
        cache.add("GOLD", 10);
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    void snapshot_returns_unmodifiable_copy() {
        LocalTransactionCache cache = new LocalTransactionCache();
        cache.add("DIAMOND", 5);
        
        Map<String, Integer> snapshot = cache.snapshot();
        assertEquals(5, snapshot.get("DIAMOND"));
        
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("GOLD", 10));
    }
}