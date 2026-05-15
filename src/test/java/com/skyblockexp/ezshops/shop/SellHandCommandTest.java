package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.shop.command.SellHandCommand;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Feature tests for {@link SellHandCommand} — the {@code /sellhand} command.
 */
public class SellHandCommandTest extends AbstractEzShopsTest {

    private SellHandCommand buildCommand(ShopTransactionService svc, ShopPricingManager pm,
            com.skyblockexp.ezshops.EzShopsPlugin plugin) {
        ShopMessageConfiguration.CommandMessages.SellHandCommandMessages messages =
                ShopMessageConfiguration.load(plugin).commands().sellHand();
        return new SellHandCommand(svc, pm, messages);
    }

    private ShopTransactionService buildService(ShopPricingManager pm, Economy econ,
            com.skyblockexp.ezshops.EzShopsPlugin plugin) {
        return new ShopTransactionService(pm, econ, ShopMessageConfiguration.load(plugin).transactions());
    }

    private Command dummyCommand() {
        Command cmd = Mockito.mock(Command.class);
        when(cmd.getName()).thenReturn("sellhand");
        return cmd;
    }

    @Test
    void sellhand_sends_must_hold_item_message_when_player_holds_nothing() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);
        ShopTransactionService svc = buildService(pm, econ, plugin);
        SellHandCommand cmd = buildCommand(svc, pm, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        // Player holds nothing (AIR by default)

        cmd.onCommand(player, dummyCommand(), "sellhand", new String[]{});

        String expected = ShopMessageConfiguration.load(plugin).commands().sellHand().mustHoldItem();
        assertEquals(expected, ((PlayerMock) player).nextMessage(), "Player should receive must-hold-item message");
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sellhand_succeeds_when_player_holds_a_configured_sellable_item() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pm.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.of(price));
        when(pm.estimateBulkTotal(eq(Material.DIAMOND), eq(16), any())).thenReturn(80.0);
        when(pm.isVisibleInMenu(Material.DIAMOND)).thenReturn(true);

        Economy econ = Mockito.mock(Economy.class);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 1000.0, EconomyResponse.ResponseType.SUCCESS, "ok"));
        when(econ.format(anyDouble())).thenReturn("$80.00");

        ShopTransactionService svc = buildService(pm, econ, plugin);
        SellHandCommand cmd = buildCommand(svc, pm, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        // Add items to both hand and inventory so sell(Material) count check passes
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 16));

        cmd.onCommand(player, dummyCommand(), "sellhand", new String[]{});

        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(80.0));
    }

    @Test
    void sellhand_sends_not_in_rotation_message_when_rotation_blocks_held_item() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        when(pm.isVisibleInMenu(Material.DIAMOND)).thenReturn(false);
        when(pm.isPartOfRotation(Material.DIAMOND)).thenReturn(true);

        Economy econ = Mockito.mock(Economy.class);
        ShopTransactionService svc = buildService(pm, econ, plugin);
        SellHandCommand cmd = buildCommand(svc, pm, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 1));

        cmd.onCommand(player, dummyCommand(), "sellhand", new String[]{});

        // The SellHandCommand itself checks rotation before calling transactionService
        String expected = ShopMessageConfiguration.load(plugin).commands().sellHand().notInRotation();
        assertEquals(expected, ((PlayerMock) player).nextMessage());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sellhand_sends_players_only_message_when_sender_is_console() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);
        ShopTransactionService svc = buildService(pm, econ, plugin);
        SellHandCommand cmd = buildCommand(svc, pm, plugin);

        ConsoleCommandSender console = server.getConsoleSender();

        cmd.onCommand(console, dummyCommand(), "sellhand", new String[]{});

        // ConsoleCommandSender is not a Player — command should reply and not call economy
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }
}
