package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.bootstrap.EzShopsRegistry;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.config.StockMarketConfig;
import com.skyblockexp.ezshops.stock.StockMarketFrozenStore;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockOverviewGuiTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void resetRegistry() throws Exception {
        installRegistry(null);
    }

    @Test
    void constructor_exposes_basic_configuration_getters() {
        File cfgFile = tempDir.resolve("stock-gui.yml").toFile();

        StockOverviewGui gui = new StockOverviewGui(
                mock(StockMarketManager.class),
                mock(StockMarketConfig.class),
                mock(StockMarketFrozenStore.class),
                cfgFile,
                false
        );

        assertNotNull(gui.getTitle());
        assertNotNull(gui.getFilters());
        assertNotNull(gui.getSeeAllStocksMaterial());
        assertNotNull(gui.getSeeAllStocksDisplayName());
        assertNotNull(gui.getSeeAllStocksLore());
    }

    @Test
    void handleInventoryClick_returns_false_for_non_player_clicker() {
        File cfgFile = tempDir.resolve("stock-gui-click.yml").toFile();
        StockOverviewGui gui = new StockOverviewGui(
                mock(StockMarketManager.class),
                mock(StockMarketConfig.class),
                mock(StockMarketFrozenStore.class),
                cfgFile,
                false
        );

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(mock(HumanEntity.class));

        assertFalse(gui.handleInventoryClick(event));
    }

    @Test
    void constructor_keeps_translate_tokens_when_registry_core_is_missing() throws Exception {
        File cfgFile = tempDir.resolve("stock-overview-translate-missing.yml").toFile();
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("layout.title", "{translate:stock.overview.title}");
        cfg.save(cfgFile);

        StockOverviewGui gui = new StockOverviewGui(
                mock(StockMarketManager.class),
                mock(StockMarketConfig.class),
                mock(StockMarketFrozenStore.class),
                cfgFile,
                false
        );

        assertTrue(gui.getTitle().contains("{translate:stock.overview.title}"));
    }

    @Test
    void constructor_resolves_translate_tokens_from_registry_messages() throws Exception {
        installRegistryWithMessages("stock.overview.title", "&aOverview Resolved");

        File cfgFile = tempDir.resolve("stock-overview-translate-resolved.yml").toFile();
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("layout.title", "{translate:stock.overview.title}");
        cfg.save(cfgFile);

        StockOverviewGui gui = new StockOverviewGui(
                mock(StockMarketManager.class),
                mock(StockMarketConfig.class),
                mock(StockMarketFrozenStore.class),
                cfgFile,
                false
        );

        assertEquals(ChatColor.GREEN + "Overview Resolved", gui.getTitle());
    }

    private static void installRegistryWithMessages(String key, String value) throws Exception {
        CoreShopComponent core = new CoreShopComponent(mock(Economy.class));

        Field messageField = CoreShopComponent.class.getDeclaredField("messageConfiguration");
        messageField.setAccessible(true);
        messageField.set(core, createMessageConfiguration(key, value));

        EzShopsRegistry registry = new EzShopsRegistry();
        Method setCore = EzShopsRegistry.class.getDeclaredMethod("setCoreShopComponent", CoreShopComponent.class);
        setCore.setAccessible(true);
        setCore.invoke(registry, core);

        installRegistry(registry);
    }

    private static ShopMessageConfiguration createMessageConfiguration(String key, String value) throws Exception {
        YamlConfiguration primary = new YamlConfiguration();
        primary.set(key, value);
        YamlConfiguration fallback = new YamlConfiguration();

        Constructor<ShopMessageConfiguration> ctor = ShopMessageConfiguration.class
                .getDeclaredConstructor(YamlConfiguration.class, YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(primary, fallback);
    }

    private static void installRegistry(EzShopsRegistry registry) throws Exception {
        Method install = EzShopsRegistry.class.getDeclaredMethod("install", EzShopsRegistry.class);
        install.setAccessible(true);
        install.invoke(null, registry);
    }
}
