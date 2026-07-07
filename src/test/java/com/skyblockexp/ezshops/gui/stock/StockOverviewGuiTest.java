package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.config.StockMarketConfig;
import com.skyblockexp.ezshops.stock.StockMarketFrozenStore;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockOverviewGuiTest {

    @TempDir
    Path tempDir;

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
}
