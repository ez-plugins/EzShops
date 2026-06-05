package com.skyblockexp.ezshops.data;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class RedisTransactionCacheTest {

    @Test
    void fallback_to_local_cache_on_connection_failure() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        
        cache.add("DIAMOND", 5);
        assertEquals(5, cache.get("DIAMOND"));
        
        cache.add("DIAMOND", 3);
        assertEquals(8, cache.get("DIAMOND"));
        cache.add("GOLD", 10);
        
        Map<String, Integer> drained = cache.drain();
        assertEquals(2, drained.size());
        assertEquals(8, drained.get("DIAMOND"));
        assertEquals(10, drained.get("GOLD"));
        assertTrue(cache.isEmpty());
    }

    @Test
    void drain_returns_empty_map_when_fallback() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        Map<String, Integer> drained = cache.drain();
        assertTrue(drained.isEmpty());
    }

    @Test
    void snapshot_returns_unmodifiable_map_when_fallback() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        cache.add("DIAMOND", 5);
        
        Map<String, Integer> snapshot = cache.snapshot();
        assertEquals(5, snapshot.get("DIAMOND"));
        
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("GOLD", 10));
    }

    @Test
    void clear_works_in_fallback_mode() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        cache.add("DIAMOND", 5);
        cache.add("GOLD", 10);
        assertFalse(cache.isEmpty());
        
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    void isEmpty_returns_true_for_empty_fallback_cache() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        assertTrue(cache.isEmpty());
    }

    @Test
    void isEmpty_returns_false_for_non_empty_fallback_cache() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        cache.add("IRON", 100);
        assertFalse(cache.isEmpty());
    }

    @Test
    void get_returns_zero_for_missing_key_in_fallback() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        assertEquals(0, cache.get("UNKNOWN"));
    }

    @Test
    void add_with_negative_amount_works_in_fallback() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        cache.add("DIAMOND", -50);
        assertEquals(-50, cache.get("DIAMOND"));
    }

    @Test
    void shutdown_noop_when_fallback() {
        RedisTransactionCache cache = new RedisTransactionCache("invalid-host", 9999, "", Logger.getLogger("test"));
        cache.add("DIAMOND", 10);
        // Should not throw - shutdown is a no-op when in fallback mode
        assertDoesNotThrow(() -> cache.shutdown());
    }
}