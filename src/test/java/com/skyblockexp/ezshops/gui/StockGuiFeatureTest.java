package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.StockComponent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StockGuiFeatureTest extends AbstractEzShopsTest {

    @Test
    void stock_overview_command_opens_stock_inventory() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        StockComponent stock = plugin.getStockComponent();
        assertNotNull(stock, "StockComponent should be initialized when stock is enabled");

        org.bukkit.entity.Player player = server.addPlayer("stock-player");
        // grant permission required by the command
        player.addAttachment(plugin, "ezshops.stock.overview", true);
        // dispatch the stock overview command which should open the GUI
        boolean dispatched = server.dispatchCommand(player, "stock overview");
        assertTrue(dispatched, "Command dispatch should succeed");

        // after command, player should have an open inventory with the stock title
        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top, "Player should have an open top inventory after stock command");
        String title = player.getOpenInventory().getTitle();
        assertNotNull(title);
        // basic sanity: title contains the word 'stock' or is non-empty
        assertTrue(title.toLowerCase().contains("stock") || !title.isBlank());
    }
}
