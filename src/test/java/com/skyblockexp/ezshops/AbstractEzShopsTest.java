package com.skyblockexp.ezshops;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Abstract base for MockBukkit-based tests.
 * Sets up a ServerMock and provides helpers for loading plugins and registering an Economy provider.
 */
public abstract class AbstractEzShopsTest {

    protected ServerMock server;

    @BeforeEach
    void setupMockBukkit() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void cleanupMockBukkit() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
        }
    }

    protected <T extends JavaPlugin> T loadPlugin(Class<T> pluginClass) {
        return MockBukkit.load(pluginClass);
    }

    /**
     * Load a simple test plugin that registers the provided Economy instance on enable.
     */
    protected void loadProviderPlugin(Economy econ) {
        TestProviderPlugin.econToRegister = econ;
        MockBukkit.load(TestProviderPlugin.class);
    }

    public static class TestProviderPlugin extends JavaPlugin {
        static Economy econToRegister;

        @Override
        public void onEnable() {
            if (econToRegister != null) {
                getServer().getServicesManager().register(Economy.class, econToRegister, this, org.bukkit.plugin.ServicePriority.Normal);
            }
        }
    }
}
