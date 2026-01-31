package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.StockComponent;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGui;
import com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGui.TransactionType;
import net.milkbowl.vault.economy.EconomyResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StockTransactionConfirmGuiTest extends AbstractEzShopsTest {

    @Test
    void buy_transaction_processes_and_adds_stock() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComp = plugin.getStockComponent();
        assertNotNull(stockComp);
        StockMarketManager manager = stockComp.getStockMarketManager();
        assertNotNull(manager);

        String product = "DIAMOND";
        manager.setPrice(product, 5.0);

        org.bukkit.entity.Player player = server.addPlayer("buyer");
        // ensure enough balance
        org.mockito.Mockito.when(econ.getBalance(org.mockito.Mockito.eq(player))).thenReturn(1000.0);
        org.mockito.Mockito.when(econ.withdrawPlayer(org.mockito.Mockito.eq(player), org.mockito.Mockito.anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS, "ok"));

        // Open confirm GUI
        StockTransactionConfirmGui.open(player, product, TransactionType.BUY, manager, null, 1, "all");
        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);

        // Process click on first buy slot (10) which corresponds to amount 1
        boolean handled = StockTransactionConfirmGui.handleClick(player, inv, 10, product, TransactionType.BUY, manager, null, 1, "all");
        assertTrue(handled);

        // Assert player now has stock added
        int owned = com.skyblockexp.ezshops.stock.StockManager.getPlayerStockAmount(player, product);
        assertTrue(owned >= 1, "Player should have at least 1 unit of stock after purchase");
    }

    @Test
    void sell_transaction_processes_and_removes_stock() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComp = plugin.getStockComponent();
        StockMarketManager manager = stockComp.getStockMarketManager();

        String product = "DIAMOND";
        manager.setPrice(product, 5.0);

        org.bukkit.entity.Player player = server.addPlayer("seller");
        // give player some stock
        com.skyblockexp.ezshops.stock.StockManager.addPlayerStock(player, product, 5);

        org.mockito.Mockito.when(econ.depositPlayer(org.mockito.Mockito.any(org.bukkit.OfflinePlayer.class), org.mockito.Mockito.anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS, "ok"));

        // Open confirm GUI for SELL
        StockTransactionConfirmGui.open(player, product, TransactionType.SELL, manager, null, 1, "all");
        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);

        // Click sell slot 10 (sell 1)
        boolean handled = StockTransactionConfirmGui.handleClick(player, inv, 10, product, TransactionType.SELL, manager, null, 1, "all");
        assertTrue(handled);

        int owned = com.skyblockexp.ezshops.stock.StockManager.getPlayerStockAmount(player, product);
        assertTrue(owned >= 0 && owned < 5, "Player stock should have decreased after sale");
    }
}
