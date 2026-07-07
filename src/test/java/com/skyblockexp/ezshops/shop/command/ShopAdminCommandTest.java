package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.AbstractEzShopsTest;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ShopAdminCommandTest extends AbstractEzShopsTest {

    @Test
    void shopAdminCommand_sendsMessage_whenNotPlayer() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        // Dispatch command as console (not a player)
        boolean dispatched = server.dispatchCommand(server.getConsoleSender(), "shopadmin");
        assertTrue(dispatched);
    }

    @Test
    void shopAdminCommand_opensGui_whenPlayerHasPermission() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("shop-admin");
        player.addAttachment(plugin, "ezshops.shop.admin", true);

        boolean dispatched = server.dispatchCommand(player, "shopadmin");
        assertTrue(dispatched);

        // Verify GUI opened
        Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top);
    }

    @Test
    void shopAdminCommand_sendsPermissionDenied_whenNoPermission() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("no-perm-admin");
        // No permission

        server.dispatchCommand(player, "shopadmin");

        // No GUI should open - top inventory should be null
        assertNull(player.getOpenInventory().getTopInventory());
    }

    @Test
    void shopAdminCommand_tabComplete_returnsOptions() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("admin-tab");
        player.addAttachment(plugin, "ezshops.shop.admin", true);

        // Test tab completion
        String[] args = {"b"};
        var completions = plugin.getCommand("shopadmin").getTabCompleter().onTabComplete(player, plugin.getCommand("shopadmin"), "shopadmin", args);
        assertEquals(1, completions.size());
        assertEquals("browse", completions.get(0));
    }

    @Test
    void shopAdminCommand_tabComplete_reseedMode_returnsBundledModes() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("admin-reseed-tab");
        player.addAttachment(plugin, "ezshops.shop.admin", true);

        String[] args = {"reseed", "s"};
        var completions = plugin.getCommand("shopadmin").getTabCompleter()
                .onTabComplete(player, plugin.getCommand("shopadmin"), "shopadmin", args);
        assertTrue(completions.contains("smp"), "Expected smp mode suggestion for reseed tab completion");
    }

    @Test
    void shopAdminCommand_reseed_rejects_unknown_mode() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        PlayerMock player = server.addPlayer("admin-reseed-invalid");
        player.addAttachment(plugin, "ezshops.shop.admin", true);

        boolean dispatched = server.dispatchCommand(player, "shopadmin reseed unknown-mode");
        assertTrue(dispatched);

        String first = player.nextMessage();
        String second = player.nextMessage();
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.toLowerCase().contains("unknown shop mode"));
        assertTrue(second.toLowerCase().contains("available bundled modes"));
    }

    @Test
    void shopAdminCommand_reseed_accepts_known_mode() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        PlayerMock player = server.addPlayer("admin-reseed-valid");
        player.addAttachment(plugin, "ezshops.shop.admin", true);

        boolean dispatched = server.dispatchCommand(player, "shopadmin reseed smp");
        assertTrue(dispatched);

        String first = player.nextMessage();
        String second = player.nextMessage();
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.toLowerCase().contains("reseed complete"));
        assertTrue(second.toLowerCase().contains("left unchanged"));
    }
}