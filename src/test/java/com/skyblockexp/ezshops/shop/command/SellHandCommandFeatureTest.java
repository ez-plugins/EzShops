package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Feature tests for the {@code /sellhand} command executor, verifying that
 * the full command pipeline (permission check → pricing lookup → economy
 * deposit) works correctly end-to-end for birch logs.
 */
public class SellHandCommandFeatureTest extends AbstractEzShopsTest {

    @Test
    void sellhand_birch_log_succeeds() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        when(econ.depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble()))
                .thenReturn(new EconomyResponse(0, 1000, EconomyResponse.ResponseType.SUCCESS, ""));
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        SellHandCommand cmd = getSellHandCommand(plugin);

        PlayerMock player = server.addPlayer("birch-hand-seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.BIRCH_LOG, 32));

        boolean handled = cmd.onCommand(player, null, "sellhand", new String[]{});

        assertTrue(handled);
        String message = player.nextMessage();
        assertNotNull(message, "Player should receive a response message");
        verify(econ, atLeastOnce()).depositPlayer(eq(player), anyDouble());
    }

    @Test
    void sellhand_birch_log_fails_without_permission() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        SellHandCommand cmd = getSellHandCommand(plugin);

        PlayerMock player = server.addPlayer("no-perm-seller");
        // no sell permission granted
        player.getInventory().setItemInMainHand(new ItemStack(Material.BIRCH_LOG, 16));

        cmd.onCommand(player, null, "sellhand", new String[]{});

        verify(econ, never()).depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    @Test
    void sellhand_fails_when_hand_is_empty() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        SellHandCommand cmd = getSellHandCommand(plugin);

        PlayerMock player = server.addPlayer("empty-hand-seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        // main hand is AIR by default

        cmd.onCommand(player, null, "sellhand", new String[]{});

        verify(econ, never()).depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SellHandCommand getSellHandCommand(EzShopsPlugin plugin) throws Exception {
        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core, "CoreShopComponent must not be null");
        Field f = CoreShopComponent.class.getDeclaredField("sellHandCommand");
        f.setAccessible(true);
        SellHandCommand cmd = (SellHandCommand) f.get(core);
        assertNotNull(cmd, "SellHandCommand must not be null");
        return cmd;
    }
}
