package com.skyblockexp.ezshops.stock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and retrieves historical price data for stocks.
 */
public class StockHistoryManager {
    // Map<ProductId, List<PriceEntry>>
    private final Map<String, List<PriceEntry>> history = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 50; // Limit entries per stock

    public void recordPrice(String productId, double price) {
        String id = productId.toUpperCase(Locale.ROOT);
        history.computeIfAbsent(id, k -> new LinkedList<>());
        List<PriceEntry> entries = history.get(id);
        entries.add(new PriceEntry(System.currentTimeMillis(), price));
        if (entries.size() > MAX_HISTORY) {
            entries.remove(0);
        }
    }

    public List<PriceEntry> getHistory(String productId) {
        String id = productId.toUpperCase(Locale.ROOT);
        return history.getOrDefault(id, Collections.emptyList());
    }

    public static class PriceEntry {
        public final long timestamp;
        public final double price;
        public PriceEntry(long timestamp, double price) {
            this.timestamp = timestamp;
            this.price = price;
        }
    }
}
