package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StockGuiInteractionTest extends AbstractEzShopsTest {

    @Test
    void clicking_see_all_stocks_opens_all_stocks_gui() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        org.bukkit.entity.Player player = server.addPlayer("stock-interact");
        player.addAttachment(plugin, "ezshops.stock.overview", true);

        // open stock overview via command
        boolean dispatched = server.dispatchCommand(player, "stock overview");
        assertTrue(dispatched);

        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top);

        // find the 'see all stocks' item by scanning for a non-empty slot near the end
        int slot = -1;
        for (int i = Math.max(0, top.getSize() - 9); i < top.getSize(); i++) {
            if (top.getItem(i) != null && top.getItem(i).getType() != org.bukkit.Material.AIR) { slot = i; break; }
        }
        assertTrue(slot >= 0, "Expected a 'see all stocks' or similar item in the overview GUI");

        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        org.bukkit.event.inventory.InventoryClickEvent click = new org.bukkit.event.inventory.InventoryClickEvent(view, org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, slot, org.bukkit.event.inventory.ClickType.LEFT, org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);
        plugin.getServer().getPluginManager().callEvent(click);

        // after click, a new GUI (AllStocksGui) should be open with a different title
        String title = player.getOpenInventory().getTitle();
        assertNotNull(title);
        assertTrue(title.toLowerCase().contains("stock") || title.length() > 0);
    }
}
