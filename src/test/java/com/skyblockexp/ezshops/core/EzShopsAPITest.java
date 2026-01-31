package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.api.EzShopsAPI;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EzShopsAPITest extends AbstractEzShopsTest {

    @Test
    void initialize_and_shutdown_behaviour_and_basic_accessors() throws Exception {
        // Ensure clean state
        EzShopsAPI.shutdown();

        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // try initialize if not already
        try {
            EzShopsAPI.initialize(plugin);
        } catch (IllegalStateException ignored) {
            // plugin may have initialized the API already during enable
        }

        EzShopsAPI api = EzShopsAPI.getInstance();
        assertNotNull(api);
        assertEquals(plugin, api.getPlugin());

        // ShopPriceService may be registered by the plugin in test env
        assertNotNull(api.getShopAPI());
        // Stock API availability may vary in test env; ensure boolean matches nullness
        assertEquals(api.isStockAPIAvailable(), api.getStockAPI() != null);

        // cleanup
        EzShopsAPI.shutdown();
    }

    @Test
    void getInstance_throws_if_not_initialized() {
        EzShopsAPI.shutdown();
        assertThrows(IllegalStateException.class, EzShopsAPI::getInstance);
    }
}
