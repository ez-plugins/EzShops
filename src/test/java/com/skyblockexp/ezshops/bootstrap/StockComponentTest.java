package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockComponentTest {

    @AfterEach
    void resetRegistry() {
        EzShopsRegistry.install(null);
    }

    @Test
    void enable_stock_disabled_logs_when_registry_debug_is_enabled() {
        EzShopsRegistry registry = new EzShopsRegistry();
        registry.setDebugMode(true);
        EzShopsRegistry.install(registry);

        EzShopsPlugin plugin = mock(EzShopsPlugin.class);
        Logger logger = mock(Logger.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("stock.enabled", false);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(logger);

        StockComponent component = new StockComponent();
        component.enable(plugin);

        verify(logger).info("Stock features are disabled via config. Skipping stock system initialization.");
        assertNull(component.getStockMarketManager());
        assertNull(component.getStockMarketConfig());
        assertNull(component.getFrozenStore());
    }

    @Test
    void enable_stock_disabled_does_not_log_when_registry_debug_is_disabled() {
        EzShopsRegistry registry = new EzShopsRegistry();
        registry.setDebugMode(false);
        EzShopsRegistry.install(registry);

        EzShopsPlugin plugin = mock(EzShopsPlugin.class);
        Logger logger = mock(Logger.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("stock.enabled", false);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(logger);

        StockComponent component = new StockComponent();
        component.enable(plugin);

        verify(logger, never()).info("Stock features are disabled via config. Skipping stock system initialization.");
        assertNull(component.getStockMarketManager());
    }
}
