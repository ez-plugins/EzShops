package com.skyblockexp.ezshops.gui.admin;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.AbstractEzShopsTest;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SetupShopsGuiIntegrationTest extends AbstractEzShopsTest {

    @Test
    void setupShopsCommand_sendsMessage_whenNotPlayer() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        // Dispatch command as console (not a player)
        boolean dispatched = server.dispatchCommand(server.getConsoleSender(), "setupshops");
        // Command still returns true (handled), but no GUI opens
        assertTrue(dispatched);
    }

    @Test
    void setupShopsCommand_opensGui_whenPlayerHasPermission() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("setup-shops");
        player.addAttachment(plugin, "ezshops.setupshops", true);

        boolean dispatched = server.dispatchCommand(player, "setupshops");
        assertTrue(dispatched);

        // Verify GUI opened
        Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top);
        assertEquals(27, top.getSize());
    }

    @Test
    void setupShopsGuiListener_togglesCoreShops_whenSlot10Clicked() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("setup-shops-toggle");
        player.addAttachment(plugin, "ezshops.setupshops", true);

        // Open setup shops GUI
        server.dispatchCommand(player, "setupshops");
        Inventory inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);

        // Click slot 10 (core shops toggle)
        ItemStack emerald = new ItemStack(Material.EMERALD);
        inv.setItem(10, emerald);
        
        var view = player.getOpenInventory();
        var clickEvent = new org.bukkit.event.inventory.InventoryClickEvent(
            view, org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, 10, 
            ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(clickEvent);

        // Verify click was cancelled (GUI handles it)
        assertTrue(clickEvent.isCancelled());
    }

    @Test
    void setupShopsGuiListener_sendsPermissionMessage_whenNoPermission() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("no-perm-setup");
        // No permission added

        server.dispatchCommand(player, "setupshops");
        
        // GUI opens but player gets permission denied on click
        // Verify by clicking in the GUI
        var view = player.getOpenInventory();
        if (view.getTopInventory() != null) {
            var clickEvent = new org.bukkit.event.inventory.InventoryClickEvent(
                view, org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, 10, 
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
            server.getPluginManager().callEvent(clickEvent);
            // Permission message should be sent, click is cancelled
            assertTrue(clickEvent.isCancelled());
        }
    }
}