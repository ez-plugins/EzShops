package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Feature tests for {@link ShopTransactionService#sellInventory(Player)}.
 */
public class ShopSellInventoryTest extends AbstractEzShopsTest {

    private ShopTransactionService buildService(ShopPricingManager pricingManager, Economy economy,
            com.skyblockexp.ezshops.EzShopsPlugin plugin) {
        return new ShopTransactionService(pricingManager, economy,
                ShopMessageConfiguration.load(plugin).transactions());
    }

    @Test
    void sellInventory_succeeds_removes_all_sellable_items_and_deposits() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        ShopPrice diamondPrice = new ShopPrice(10.0, 5.0);
        ShopPrice ironPrice = new ShopPrice(3.0, 1.5);
        when(pm.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.of(diamondPrice));
        when(pm.getPrice(eq(Material.IRON_INGOT))).thenReturn(Optional.of(ironPrice));
        when(pm.estimateBulkTotal(eq(Material.DIAMOND), eq(4), any())).thenReturn(20.0);
        when(pm.estimateBulkTotal(eq(Material.IRON_INGOT), eq(6), any())).thenReturn(9.0);

        Economy econ = Mockito.mock(Economy.class);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 1000.0, EconomyResponse.ResponseType.SUCCESS, "ok"));
        when(econ.format(anyDouble())).thenReturn("$0.00");

        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 4));
        player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 6));

        ShopTransactionResult result = svc.sellInventory(player);

        assertTrue(result.success(), "Expected success but got: " + result.message());
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(29.0));

        // Verify all sellable items were removed
        int diamonds = 0, iron = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null) continue;
            if (stack.getType() == Material.DIAMOND) diamonds += stack.getAmount();
            if (stack.getType() == Material.IRON_INGOT) iron += stack.getAmount();
        }
        assertEquals(0, diamonds, "Diamonds should be removed after sellInventory");
        assertEquals(0, iron, "Iron ingots should be removed after sellInventory");
    }

    @Test
    void sellInventory_fails_when_economy_is_null() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        ShopTransactionService svc = buildService(pm, null, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));

        ShopTransactionResult result = svc.sellInventory(player);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().noEconomy(), result.message());
    }

    @Test
    void sellInventory_fails_when_player_lacks_sell_permission() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller"); // no permission

        ShopTransactionResult result = svc.sellInventory(player);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().noSellPermission(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sellInventory_fails_when_inventory_has_no_sellable_items() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        // No prices configured for anything — all getPrice() calls return empty
        when(pm.getPrice(any(Material.class))).thenReturn(Optional.empty());
        Economy econ = Mockito.mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 64));

        ShopTransactionResult result = svc.sellInventory(player);

        assertFalse(result.success());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sellInventory_skips_items_whose_sell_price_is_disabled() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        // DIAMOND: canSell() = false (sellPrice < 0)
        when(pm.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.of(new ShopPrice(10.0, -1.0)));
        // IRON_INGOT: sellable
        ShopPrice ironPrice = new ShopPrice(3.0, 2.0);
        when(pm.getPrice(eq(Material.IRON_INGOT))).thenReturn(Optional.of(ironPrice));
        when(pm.estimateBulkTotal(eq(Material.IRON_INGOT), eq(3), any())).thenReturn(6.0);

        Economy econ = Mockito.mock(Economy.class);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 1000.0, EconomyResponse.ResponseType.SUCCESS, "ok"));
        when(econ.format(anyDouble())).thenReturn("$0.00");

        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 3));

        ShopTransactionResult result = svc.sellInventory(player);

        assertTrue(result.success(), "Expected success selling iron ingots");
        // Only iron ingots should have been sold — deposit = 6.0
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(6.0));
        // Diamonds should remain in inventory
        int diamonds = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND) diamonds += stack.getAmount();
        }
        assertEquals(5, diamonds, "Non-sellable diamonds should remain in inventory");
    }

    @Test
    void sellInventory_fails_economy_deposit_rejected_and_items_are_refunded() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pm.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.of(price));
        when(pm.estimateBulkTotal(eq(Material.DIAMOND), eq(2), any())).thenReturn(10.0);

        Economy econ = Mockito.mock(Economy.class);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "bank error"));
        when(econ.format(anyDouble())).thenReturn("$0.00");

        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 2));

        ShopTransactionResult result = svc.sellInventory(player);

        assertFalse(result.success());
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
        // Items should be refunded to the player
        int remaining = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND) remaining += stack.getAmount();
        }
        assertEquals(2, remaining, "Items should be returned to player when sellInventory deposit fails");
    }
}
