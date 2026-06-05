package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.gui.admin.SetupShopsGui;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetupShopsGuiTest {

    @Test
    void isCoreShopsEnabled_returnsTrue_whenCategoriesEnabled() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getBoolean(eq("categories.enabled"), anyBoolean())).thenReturn(true);

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison"));

        assertTrue(gui.isCoreShopsEnabled());
    }

    @Test
    void isCoreShopsEnabled_returnsFalse_whenCategoriesDisabled() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getBoolean(eq("categories.enabled"), anyBoolean())).thenReturn(false);

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison"));

        assertFalse(gui.isCoreShopsEnabled());
    }

    @Test
    void isQuickSellEnabled_returnsTrue_whenQuickSellEnabled() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getBoolean(eq("quick-sell.enabled"), anyBoolean())).thenReturn(true);

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison"));

        assertTrue(gui.isQuickSellEnabled());
    }

    @Test
    void isQuickSellEnabled_returnsFalse_whenQuickSellDisabled() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getBoolean(eq("quick-sell.enabled"), anyBoolean())).thenReturn(false);

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison"));

        assertFalse(gui.isQuickSellEnabled());
    }

    @Test
    void getCurrentGameMode_returnsDefault_whenNotConfigured() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getString(eq("game-mode"), anyString())).thenReturn("prison");

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison", "smp"));

        assertEquals("prison", gui.getCurrentGameMode());
    }

    @Test
    void nextGameMode_cyclesThroughModes() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getString(eq("game-mode"), anyString())).thenReturn("prison");

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison", "smp"));

        assertEquals("smp", gui.nextGameMode());
    }

    @Test
    void nextGameMode_wrapsAround() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getString(eq("game-mode"), anyString())).thenReturn("smp");

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison", "smp"));

        assertEquals("prison", gui.nextGameMode());
    }

    @Test
    void isPlayerShopsEnabled_returnsTrue_whenPlayerShopsEnabled() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getBoolean(eq("player-shops.enabled"), anyBoolean())).thenReturn(true);

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison"));

        assertTrue(gui.isPlayerShopsEnabled());
    }

    @Test
    void isPlayerShopsEnabled_returnsFalse_whenPlayerShopsDisabled() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getBoolean(eq("player-shops.enabled"), anyBoolean())).thenReturn(false);

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison"));

        assertFalse(gui.isPlayerShopsEnabled());
    }

    @Test
    void isStockMarketEnabled_returnsTrue_whenStockEnabled() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getBoolean(eq("stock.enabled"), anyBoolean())).thenReturn(true);

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison"));

        assertTrue(gui.isStockMarketEnabled());
    }

    @Test
    void isStockMarketEnabled_returnsFalse_whenStockDisabled() {
        FileConfiguration config = mock(FileConfiguration.class);
        when(config.getBoolean(eq("stock.enabled"), anyBoolean())).thenReturn(false);

        SetupShopsGui gui = new SetupShopsGui(config, Set.of("prison"));

        assertFalse(gui.isStockMarketEnabled());
    }
}