package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.StockComponent;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.api.StockAPI;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StockAPIIntegrationTest extends AbstractEzShopsTest {

    @Test
    void set_and_get_stock_price_via_api() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComponent = plugin.getStockComponent();
        assertNotNull(stockComponent);
        StockMarketManager mgr = stockComponent.getStockMarketManager();
        assertNotNull(mgr);

        StockAPI api = new StockAPI(mgr);
        double base = api.getStockPrice("DIAMOND");
        assertTrue(base >= 1.0);

        api.setStockPrice("DIAMOND", 150.0);
        assertEquals(150.0, api.getStockPrice("DIAMOND"), 0.0001);

        // negative price should be clamped to >=1 or throw; API forbids negative
        assertThrows(IllegalArgumentException.class, () -> api.setStockPrice("DIAMOND", -5.0));
    }

    @Test
    void add_remove_and_query_player_stock() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComponent = plugin.getStockComponent();
        assertNotNull(stockComponent);
        StockMarketManager mgr = stockComponent.getStockMarketManager();
        assertNotNull(mgr);

        StockAPI api = new StockAPI(mgr);

        Player p = server.addPlayer("stock-user");

        boolean added = api.addPlayerStock(p, "DIAMOND", 5);
        assertTrue(added);
        int amount = api.getPlayerStockAmount(p, "DIAMOND");
        assertEquals(5, amount);

        List<String> owned = api.getPlayerOwnedStocks(p);
        assertTrue(owned.contains("DIAMOND"));

        boolean removed = api.removePlayerStock(p, "DIAMOND", 3);
        assertTrue(removed);
        assertEquals(2, api.getPlayerStockAmount(p, "DIAMOND"));

        // cannot remove more than owned
        assertFalse(api.removePlayerStock(p, "DIAMOND", 10));
    }

    @Test
    void estimate_bulk_total_deterministic_and_update_price_changes_price() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComponent = plugin.getStockComponent();
        assertNotNull(stockComponent);
        StockMarketManager mgr = stockComponent.getStockMarketManager();
        assertNotNull(mgr);

        // set a known price
        mgr.setPrice("IRON_INGOT", 100.0);
        double estimate = mgr.estimateBulkTotal("IRON_INGOT", 3, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY);
        // deterministic: first unit 100, next unit 102, next 104.04 -> sum = 306.04
        assertTrue(estimate > 300 && estimate < 310);

        double before = mgr.getPrice("IRON_INGOT");
        mgr.updatePrice("IRON_INGOT", 2); // demand positive should change price
        double after = mgr.getPrice("IRON_INGOT");
        assertNotEquals(before, after);
    }
}
