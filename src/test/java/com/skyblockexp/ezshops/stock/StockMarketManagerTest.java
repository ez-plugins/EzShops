package com.skyblockexp.ezshops.stock;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import com.skyblockexp.ezshops.repository.StockMarketRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StockMarketManagerTest extends AbstractEzShopsTest {

    @Test
    void enablePersistence_loads_prices_from_repository() {
        StockMarketManager mgr = new StockMarketManager();
        Map<String, Double> initial = new HashMap<>();
        initial.put("DIAMOND", 55.0);
        StockMarketRepository repo = new StockMarketRepository() {
            private final Map<String, Double> map = initial;
            @Override public Map<String, Double> loadPrices() { return map; }
            @Override public void savePrices(Map<String, Double> prices) { /* no-op */ }
            @Override public void load() {}
            @Override public void save() {}
            @Override public void freeze(String id, String by) {}
            @Override public void unfreeze(String id) {}
            @Override public boolean isFrozen(String id) { return false; }
            @Override public java.util.Collection<FrozenMeta> getAllFrozenMeta() { return java.util.Collections.emptyList(); }
            @Override public java.util.Set<String> getAllFrozen() { return java.util.Collections.emptySet(); }
        };

        mgr.setStockMarketRepository(repo);
        // load plugin so Bukkit server is initialized
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        mgr.enablePersistence(plugin, 1L);
        assertEquals(55.0, mgr.getPrice("DIAMOND"), 0.0001);
    }

    @Test
    void estimateBulkTotal_and_update_behaviour() {
        StockMarketManager mgr = new StockMarketManager();
        mgr.setPrice("DIAMOND", 100.0);

        double estimate = mgr.estimateBulkTotal("DIAMOND", 3, ShopTransactionType.BUY);
        // expected = 100 + 100*(1.02) + 100*(1.02)^2
        double exp = 100.0 + 100.0 * 1.02 + 100.0 * 1.02 * 1.02;
        assertEquals(exp, estimate, 0.0001);

        double before = mgr.getPrice("DIAMOND");
        mgr.updatePrice("DIAMOND", 2);
        double after = mgr.getPrice("DIAMOND");
        assertTrue(after >= 1.0);
        assertNotEquals(before, after);
    }

    @Test
    void frozen_items_do_not_update() {
        StockMarketManager mgr = new StockMarketManager();
        mgr.setPrice("DIAMOND", 100.0);
        StockMarketRepository repo = new StockMarketRepository() {
            @Override public Map<String, Double> loadPrices() { return new HashMap<>(); }
            @Override public void savePrices(Map<String, Double> prices) {}
            @Override public void load() {}
            @Override public void save() {}
            @Override public void freeze(String id, String by) {}
            @Override public void unfreeze(String id) {}
            @Override public boolean isFrozen(String id) { return true; }
            @Override public java.util.Collection<FrozenMeta> getAllFrozenMeta() { return java.util.Collections.emptyList(); }
            @Override public java.util.Set<String> getAllFrozen() { return java.util.Collections.singleton("DIAMOND"); }
        };
        mgr.setStockMarketRepository(repo);
        double before = mgr.getPrice("DIAMOND");
        mgr.updatePrice("DIAMOND", 5);
        double after = mgr.getPrice("DIAMOND");
        assertEquals(before, after, 0.0001, "Frozen item should not have its price updated");
    }
}
