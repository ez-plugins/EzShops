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

public class StockEconomyInsufficientFundsTest extends AbstractEzShopsTest {

    @Test
    void buy_with_insufficient_funds_does_not_add_stock() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComp = plugin.getStockComponent();
        StockMarketManager manager = stockComp.getStockMarketManager();
        String product = "DIAMOND";
        manager.setPrice(product, 10.0);

        org.bukkit.entity.Player player = server.addPlayer("poor-player");

        // insufficient balance
        org.mockito.Mockito.when(econ.getBalance(org.mockito.Mockito.eq(player))).thenReturn(1.0);

        StockTransactionConfirmGui.open(player, product, TransactionType.BUY, manager, null, 1, "all");
        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);

        // Click buy 1 (slot 10)
        boolean handled = StockTransactionConfirmGui.handleClick(player, inv, 10, product, TransactionType.BUY, manager, null, 1, "all");
        assertTrue(handled);

        int owned = com.skyblockexp.ezshops.stock.StockManager.getPlayerStockAmount(player, product);
        assertEquals(0, owned, "Player with insufficient funds should not receive stock");
    }

    @Test
    void sell_more_than_owned_is_rejected_and_no_money_deposited() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComp = plugin.getStockComponent();
        StockMarketManager manager = stockComp.getStockMarketManager();
        String product = "DIAMOND";
        manager.setPrice(product, 5.0);

        org.bukkit.entity.Player player = server.addPlayer("nosell-player");
        // ensure player has zero stock
        int ownedBefore = com.skyblockexp.ezshops.stock.StockManager.getPlayerStockAmount(player, product);
        assertEquals(0, ownedBefore);

        // Click sell 1 (slot 10) even though owned=0
        StockTransactionConfirmGui.open(player, product, TransactionType.SELL, manager, null, 1, "all");
        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);

        boolean handled = StockTransactionConfirmGui.handleClick(player, inv, 10, product, TransactionType.SELL, manager, null, 1, "all");
        assertTrue(handled);

        int ownedAfter = com.skyblockexp.ezshops.stock.StockManager.getPlayerStockAmount(player, product);
        assertEquals(0, ownedAfter, "Player should still have zero stock after attempting to sell more than owned");

        // verify no deposit attempted
        org.mockito.Mockito.verify(econ, org.mockito.Mockito.never()).depositPlayer(org.mockito.Mockito.any(org.bukkit.OfflinePlayer.class), org.mockito.Mockito.anyDouble());
    }
}
