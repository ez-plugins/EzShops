package com.skyblockexp.ezshops.data;

import java.util.Collections;
import java.util.Map;

/**
 * Transaction cache interface for storing pending stock market transactions.
 */
public interface TransactionCache {
    void add(String productId, int amount);
    Map<String, Integer> drain();
    int get(String productId);
    boolean isEmpty();
    void clear();
    Map<String, Integer> snapshot();
}