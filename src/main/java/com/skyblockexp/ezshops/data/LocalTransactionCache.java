package com.skyblockexp.ezshops.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local in-memory transaction cache using ConcurrentHashMap.
 * This is the default implementation that matches the original StockMarketManager behavior.
 */
public class LocalTransactionCache implements TransactionCache {
    private final Map<String, Integer> pendingTransactions = new ConcurrentHashMap<>();

    @Override
    public void add(String productId, int amount) {
        pendingTransactions.merge(productId, amount, Integer::sum);
    }

    @Override
    public Map<String, Integer> drain() {
        Map<String, Integer> snapshot = new HashMap<>(pendingTransactions);
        pendingTransactions.clear();
        return snapshot;
    }

    @Override
    public int get(String productId) {
        return pendingTransactions.getOrDefault(productId, 0);
    }

    @Override
    public boolean isEmpty() {
        return pendingTransactions.isEmpty();
    }

    @Override
    public void clear() {
        pendingTransactions.clear();
    }

    @Override
    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(pendingTransactions));
    }
}