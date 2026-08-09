package com.skyblockexp.ezshops;

import net.milkbowl.vault.economy.Economy;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EzShopsSetupEconomyFeatureTest extends AbstractEzShopsTest {

    @Test
    void servicesManager_registration_before_plugin_enable_allows_economy_detection() {
        // Register a provider plugin that will add an Economy to the ServicesManager
        loadProviderPlugin(mock(Economy.class));

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);
        assertTrue(plugin.isEnabled(), "EzShops should enable when an Economy provider is registered in ServicesManager");
    }

    @Test
    void missingEconomyProvider_disablesPluginCleanly() {
        // Vault present but no economy provider registered. This used to NPE in
        // PlayerShopComponent.<init> because setupEconomy() returned true while
        // the economy field was still null.
        loadBarePlugin("Vault");

        EzShopsPlugin plugin = assertDoesNotThrow(() -> loadPlugin(EzShopsPlugin.class));
        assertFalse(plugin.isEnabled(),
                "EzShops should disable cleanly when Vault has no economy provider");
    }
}
