package com.skyblockexp.ezshops;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import net.milkbowl.vault.economy.Economy;
import static org.mockito.Mockito.mock;
import com.skyblockexp.ezshops.api.EzShopsAPI;
import com.skyblockexp.ezshops.shop.api.ShopPriceService;

public class FeatureCoverageTest extends AbstractEzShopsTest {

    @Test
    void api_is_initialized_and_shop_api_available() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // EzShopsAPI should have been initialized during plugin enable
        EzShopsAPI api = EzShopsAPI.getInstance();
        assertNotNull(api, "EzShopsAPI should be initialized after plugin enable");

        // ShopPriceService should be available through ServicesManager
        ShopPriceService shopApi = api.getShopAPI();
        assertNotNull(shopApi, "ShopPriceService should be registered and retrievable via EzShopsAPI");
    }

    @Test
    void stock_api_is_available_and_stock_manager_present() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        EzShopsAPI api = EzShopsAPI.getInstance();
        assertTrue(api.isStockAPIAvailable(), "Stock API should be available when stock component is enabled");
        assertNotNull(api.getStockAPI(), "getStockAPI() should not return null when stock is enabled");
    }

    @Test
    void stock_commands_are_registered() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // Commands defined in plugin.yml should be registered and have executors
        assertNotNull(plugin.getCommand("stock"), "stock command should exist");
        assertNotNull(plugin.getCommand("stockadmin"), "stockadmin command should exist");
        assertNotNull(plugin.getCommand("stock").getExecutor(), "stock command should have an executor");
        assertNotNull(plugin.getCommand("stock").getTabCompleter(), "stock command should have a tab completer");
    }
}
