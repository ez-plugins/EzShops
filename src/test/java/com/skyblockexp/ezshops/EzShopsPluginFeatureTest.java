package com.skyblockexp.ezshops;

import net.milkbowl.vault.economy.Economy;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.skyblockexp.ezshops.shop.api.ShopPriceService;

public class EzShopsPluginFeatureTest extends AbstractEzShopsTest {

    @Test
    void plugin_disables_when_vault_missing() {
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        assertFalse(plugin.isEnabled(), "Plugin should disable itself when Vault is missing");
    }

    @Test
    void plugin_enables_when_vault_present_and_registers_services() {
        // Load a provider plugin which registers a simple Economy implementation
        loadProviderPlugin(mock(Economy.class));

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);
        assertTrue(plugin.isEnabled(), "EzShops should enable when Vault is present");

        // Check that ShopPriceService is registered
        org.bukkit.plugin.RegisteredServiceProvider<ShopPriceService> reg = server.getServicesManager().getRegistration(ShopPriceService.class);
        assertNotNull(reg, "ShopPriceService should be registered by CoreShopComponent");

        // Check core shop component present
        assertNotNull(plugin.getCoreShopComponent());
    }
}
