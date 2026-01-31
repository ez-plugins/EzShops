package com.skyblockexp.ezshops.stock;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.repository.StockMarketRepository;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StockMarketEdgeCasesTest extends AbstractEzShopsTest {

    @Test
    void estimateBulkTotal_invalid_inputs_return_negative_one() {
        StockMarketManager mgr = new StockMarketManager();
        assertEquals(-1.0, mgr.estimateBulkTotal(null, 5, ShopTransactionType.BUY));
        assertEquals(-1.0, mgr.estimateBulkTotal("DIAMOND", 0, ShopTransactionType.BUY));
        assertEquals(-1.0, mgr.estimateBulkTotal("DIAMOND", 5, null));
    }

    @Test
    void updatePrice_with_zero_demand_does_not_change_price() {
        StockMarketManager mgr = new StockMarketManager();
        mgr.setPrice("DIAMOND", 120.0);
        mgr.updatePrice("DIAMOND", 0);
        assertEquals(120.0, mgr.getPrice("DIAMOND"), 0.0001);
    }

    @Test
    void disablePersistence_triggers_repository_save() {
        StockMarketManager mgr = new StockMarketManager();
        Map<String, Double> saved = new HashMap<>();
        StockMarketRepository repo = new StockMarketRepository() {
            @Override public Map<String, Double> loadPrices() { return new HashMap<>(); }
            @Override public void savePrices(Map<String, Double> prices) { saved.putAll(prices); }
            @Override public void load() {}
            @Override public void save() {}
            @Override public void freeze(String id, String by) {}
            @Override public void unfreeze(String id) {}
            @Override public boolean isFrozen(String id) { return false; }
            @Override public java.util.Collection<FrozenMeta> getAllFrozenMeta() { return java.util.Collections.emptyList(); }
            @Override public java.util.Set<String> getAllFrozen() { return java.util.Collections.emptySet(); }
        };

        mgr.setStockMarketRepository(repo);
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        mgr.setPrice("DIAMOND", 88.0);
        // ensure the plugin context exists so savePrices can run
        mgr.disablePersistence();
        assertTrue(saved.containsKey("DIAMOND"));
        assertEquals(88.0, saved.get("DIAMOND"), 0.0001);
    }

    @Test
    void updatePrice_records_history_entries() {
        StockMarketManager mgr = new StockMarketManager();
        mgr.setPrice("DIAMOND", 50.0);
        mgr.updatePrice("DIAMOND", 2);
        var history = mgr.getHistoryManager().getHistory("DIAMOND");
        assertNotNull(history);
        assertTrue(history.size() > 0, "History should have at least one record after update");
    }

    @Test
    void stockmanager_owned_stocks_returns_uppercase_keys() throws Exception {
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        org.bukkit.entity.Player player = server.addPlayer("stockowner");

        java.io.File playerFile = new java.io.File(plugin.getDataFolder(), "player-stocks/" + player.getUniqueId() + ".yml");
        if (playerFile.exists()) playerFile.delete();

        assertTrue(StockManager.addPlayerStock(player, "diamond", 3));
        assertTrue(StockManager.addPlayerStock(player, "iron_ingot", 2));

        var owned = StockManager.getPlayerOwnedStocks(player);
        assertTrue(owned.contains("DIAMOND"));
        assertTrue(owned.contains("IRON_INGOT"));
    }
}
