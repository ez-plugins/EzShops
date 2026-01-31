package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.StockComponent;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGui;
import com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGui.TransactionType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StockConfirmEdgeCasesTest extends AbstractEzShopsTest {

    @Test
    void clicking_outside_confirmation_buttons_returns_false_and_no_change() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComp = plugin.getStockComponent();
        StockMarketManager manager = stockComp.getStockMarketManager();
        String product = "DIAMOND";
        manager.setPrice(product, 5.0);

        org.bukkit.entity.Player player = server.addPlayer("edge-player");

        // Open confirm GUI
        StockTransactionConfirmGui.open(player, product, TransactionType.BUY, manager, null, 1, "all");
        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);

        // Click an unrelated slot (e.g., 0) which is filler
        boolean handled = StockTransactionConfirmGui.handleClick(player, inv, 0, product, TransactionType.BUY, manager, null, 1, "all");
        assertFalse(handled, "Clicking filler should not be handled as a transaction");

        int owned = com.skyblockexp.ezshops.stock.StockManager.getPlayerStockAmount(player, product);
        assertEquals(0, owned, "No stock should be added when clicking outside confirmation buttons");
    }

    @Test
    void clicking_cancel_closes_and_returns_true_with_no_stock_change() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stockComp = plugin.getStockComponent();
        StockMarketManager manager = stockComp.getStockMarketManager();
        String product = "DIAMOND";
        manager.setPrice(product, 5.0);

        org.bukkit.entity.Player player = server.addPlayer("cancel-player");

        StockTransactionConfirmGui.open(player, product, TransactionType.BUY, manager, null, 1, "all");
        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);

        // Cancel slot is 22
        boolean handled = StockTransactionConfirmGui.handleClick(player, inv, 22, product, TransactionType.BUY, manager, null, 1, "all");
        assertTrue(handled, "Cancel click should be handled and return true");

        int owned = com.skyblockexp.ezshops.stock.StockManager.getPlayerStockAmount(player, product);
        assertEquals(0, owned, "No stock should be added after cancel");
    }
}
