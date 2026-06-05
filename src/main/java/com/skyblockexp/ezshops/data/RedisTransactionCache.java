package com.skyblockexp.ezshops.data;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Redis-based transaction cache using Redisson.
 * Falls back to local cache if Redis connection fails.
 */
public class RedisTransactionCache implements TransactionCache {
    private static final String PENDING_KEY = "ezshops:stock:pending";
    private static final int DEFAULT_TTL_SECONDS = 300;

    private final RedissonClient redisson;
    private final LocalTransactionCache fallbackCache;
    private final boolean useFallback;

    public RedisTransactionCache(String host, int port, String password, Logger logger) {
        this.fallbackCache = new LocalTransactionCache();
        RedissonClient client = null;
        boolean fallback = false;

        try {
            Config config = new Config();
            config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password.isEmpty() ? null : password);
            client = Redisson.create(config);
            if (logger != null) {
                logger.info("Connected to Redis for transaction caching");
            }
        } catch (Exception e) {
            fallback = true;
            if (logger != null) {
                logger.warning("Failed to connect to Redis, falling back to local cache: " + e.getMessage());
            }
        }

        this.redisson = client;
        this.useFallback = fallback;
    }

    @Override
    public void add(String productId, int amount) {
        if (useFallback) {
            fallbackCache.add(productId, amount);
            return;
        }
        try {
            RMap<Object, Object> map = redisson.getMap(PENDING_KEY);
            synchronized (this) {
                Object current = map.get(productId);
                if (current == null) {
                    map.put(productId, amount);
                } else {
                    map.put(productId, (Integer) current + amount);
                }
            }
            map.expire(DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            fallbackCache.add(productId, amount);
        }
    }

    @Override
    public Map<String, Integer> drain() {
        if (useFallback) {
            return fallbackCache.drain();
        }
        try {
            RMap<Object, Object> map = redisson.getMap(PENDING_KEY);
            Map<String, Integer> result;
            synchronized (this) {
                Map<String, Integer> temp = new HashMap<>();
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof Integer value) {
                        temp.put(key, value);
                    }
                }
                result = temp;
                map.clear();
            }
            return result;
        } catch (Exception e) {
            return fallbackCache.drain();
        }
    }

    @Override
    public int get(String productId) {
        if (useFallback) {
            return fallbackCache.get(productId);
        }
        try {
            Object value = redisson.getMap(PENDING_KEY).get(productId);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (Exception e) {
            return fallbackCache.get(productId);
        }
    }

    @Override
    public boolean isEmpty() {
        if (useFallback) {
            return fallbackCache.isEmpty();
        }
        try {
            return redisson.getMap(PENDING_KEY).isEmpty();
        } catch (Exception e) {
            return fallbackCache.isEmpty();
        }
    }

    @Override
    public void clear() {
        if (useFallback) {
            fallbackCache.clear();
            return;
        }
        try {
            redisson.getMap(PENDING_KEY).clear();
        } catch (Exception e) {
            fallbackCache.clear();
        }
    }

    @Override
    public Map<String, Integer> snapshot() {
        if (useFallback) {
            return fallbackCache.snapshot();
        }
        try {
            Map<String, Integer> result = new HashMap<>();
            RMap<Object, Object> map = redisson.getMap(PENDING_KEY);
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof Integer value) {
                    result.put(key, value);
                }
            }
            return Collections.unmodifiableMap(result);
        } catch (Exception e) {
            return fallbackCache.snapshot();
        }
    }

    public void shutdown() {
        if (redisson != null && !useFallback) {
            try {
                redisson.shutdown();
            } catch (Exception e) {
                // Ignore shutdown errors
            }
        }
    }
}