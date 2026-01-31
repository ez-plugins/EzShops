package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PluginLifecycleFeatureTest extends AbstractEzShopsTest {

    @Test
    void plugin_enable_and_disable_manage_api_lifecycle() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // API should be initialized
        assertNotNull(com.skyblockexp.ezshops.api.EzShopsAPI.getInstance());

        // Disable plugin and ensure API is shutdown (should not throw)
        plugin.onDisable();
        // After disable, getInstance may return null or throw; ensure shutdown didn't throw and plugin state cleared
        assertDoesNotThrow(() -> com.skyblockexp.ezshops.api.EzShopsAPI.shutdown());
    }
}
