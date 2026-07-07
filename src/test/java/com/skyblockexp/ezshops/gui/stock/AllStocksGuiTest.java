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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllStocksGuiTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void resetRegistry() throws Exception {
        installRegistry(null);
    }

    @Test
    void getNextFilter_cycles_between_default_filters() {
        File cfgFile = tempDir.resolve("all-stocks.yml").toFile();

        AllStocksGui gui = new AllStocksGui(
                mock(StockMarketManager.class),
                mock(StockMarketConfig.class),
                mock(StockMarketFrozenStore.class),
                cfgFile
        );

        assertEquals("blocks", gui.getNextFilter("all"));
        assertEquals("items", gui.getNextFilter("blocks"));
        assertEquals("all", gui.getNextFilter("items"));
    }

    @Test
    void handleInventoryClick_returns_false_when_title_does_not_match() {
        File cfgFile = tempDir.resolve("all-stocks-click.yml").toFile();

        AllStocksGui gui = new AllStocksGui(
                mock(StockMarketManager.class),
                mock(StockMarketConfig.class),
                mock(StockMarketFrozenStore.class),
                cfgFile
        );

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(mock(Player.class));
        InventoryView view = mock(InventoryView.class);
        when(view.getTitle()).thenReturn("Other Menu");
        when(event.getView()).thenReturn(view);

        assertFalse(gui.handleInventoryClick(event, 1, "all"));
    }

    @Test
    void constructor_keeps_translate_tokens_when_registry_core_is_missing() throws Exception {
        File cfgFile = tempDir.resolve("all-stocks-translate-missing.yml").toFile();
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("all-stocks-gui.layout.title", "{translate:stock.gui.title}");
        cfg.save(cfgFile);

        AllStocksGui gui = new AllStocksGui(
                mock(StockMarketManager.class),
                mock(StockMarketConfig.class),
                mock(StockMarketFrozenStore.class),
                cfgFile
        );

        assertTrue(gui.getTitle().contains("{translate:stock.gui.title}"));
    }

    @Test
    void constructor_resolves_translate_tokens_from_registry_messages() throws Exception {
        installRegistryWithMessages("stock.gui.title", "&aResolved title");

        File cfgFile = tempDir.resolve("all-stocks-translate-resolved.yml").toFile();
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("all-stocks-gui.layout.title", "{translate:stock.gui.title}");
        cfg.save(cfgFile);

        AllStocksGui gui = new AllStocksGui(
                mock(StockMarketManager.class),
                mock(StockMarketConfig.class),
                mock(StockMarketFrozenStore.class),
                cfgFile
        );

        assertEquals(ChatColor.GREEN + "Resolved title", gui.getTitle());
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
