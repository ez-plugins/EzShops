package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.config.StockMarketConfig;
import com.skyblockexp.ezshops.stock.StockMarketFrozenStore;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllStocksGuiTest {

    @TempDir
    Path tempDir;

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
}
