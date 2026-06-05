package com.skyblockexp.ezshops.stock;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StockMarketManagerBasicTest {

    @Test
    void getPendingAmount_returns_zero_when_empty() {
        StockMarketManager mgr = new StockMarketManager();
        assertEquals(0, mgr.getPendingAmount("UNKNOWN"));
    }

    @Test
    void getPendingTransactions_returns_empty_map_when_empty() {
        StockMarketManager mgr = new StockMarketManager();
        Map<String, Integer> pending = mgr.getPendingTransactions();
        assertTrue(pending.isEmpty());
    }

    @Test
    void configure_sets_parameters() {
        StockMarketManager mgr = new StockMarketManager();
        mgr.configure(0.0, 0.0, 0.02, 1.0); // No randomness, deterministic
        mgr.setPrice("DIAMOND", 100.0);
        mgr.setTransactionCache(new com.skyblockexp.ezshops.data.LocalTransactionCache());
        
        mgr.updatePrice("DIAMOND", 2);
        
        // With demandFactor=0.02, demand=2: multiplier = (1 + 0.02)^2 = 1.0404
        assertEquals(104.04, mgr.getPrice("DIAMOND"), 0.01);
    }

    @Test
    void estimateBulkTotal_returns_negative_one_for_null_key() {
        StockMarketManager mgr = new StockMarketManager();
        assertEquals(-1.0, mgr.estimateBulkTotal(null, 5, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY));
    }

    @Test
    void estimateBulkTotal_returns_negative_one_for_null_type() {
        StockMarketManager mgr = new StockMarketManager();
        mgr.setPrice("DIAMOND", 100.0);
        assertEquals(-1.0, mgr.estimateBulkTotal("DIAMOND", 5, null));
    }

    @Test
    void setTransactionCache_accepts_null() {
        StockMarketManager mgr = new StockMarketManager();
        mgr.setTransactionCache(null);
        // Should use LocalTransactionCache as fallback
    }

    @Test
    void shutdownCache_is_noop_when_transactionCache_is_null() {
        StockMarketManager mgr = new StockMarketManager();
        mgr.shutdownCache();
    }
}