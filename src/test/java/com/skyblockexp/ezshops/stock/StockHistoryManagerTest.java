package com.skyblockexp.ezshops.stock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StockHistoryManagerTest {

    @Test
    void record_and_retrieve_history_is_capped() {
        StockHistoryManager mgr = new StockHistoryManager();
        String id = "DIAMOND";
        for (int i = 0; i < 60; i++) {
            mgr.recordPrice(id, 100.0 + i);
        }

        List<StockHistoryManager.PriceEntry> history = mgr.getHistory(id);
        assertNotNull(history);
        assertTrue(history.size() <= 50, "History should be capped to MAX_HISTORY (50)");
        // newest price should be last
        assertEquals(100.0 + 59, history.get(history.size() - 1).price, 0.0001);
    }
}
