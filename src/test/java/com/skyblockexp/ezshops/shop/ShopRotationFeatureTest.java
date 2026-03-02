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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

public class ShopRotationFeatureTest extends AbstractEzShopsTest {

    @Test
    void buy_material_declared_in_rotation_but_not_visible_is_blocked() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pricingManager.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.of(price));
        when(pricingManager.isVisibleInMenu(Material.DIAMOND)).thenReturn(false);
        when(pricingManager.isPartOfRotation(Material.DIAMOND)).thenReturn(true);

        when(econ.getBalance((org.bukkit.OfflinePlayer) any())).thenReturn(1000.0);
        when(econ.withdrawPlayer((org.bukkit.OfflinePlayer) any(), anyDouble())).thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ, ShopMessageConfiguration.load(plugin).transactions());

        Player player = server.addPlayer("buyer");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_BUY, true);

        var result = svc.buy(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().notInRotation(), result.message());
        verify(econ, never()).withdrawPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sell_material_declared_in_rotation_but_not_visible_is_blocked() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pricingManager.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.of(price));
        when(pricingManager.isVisibleInMenu(Material.DIAMOND)).thenReturn(false);
        when(pricingManager.isPartOfRotation(Material.DIAMOND)).thenReturn(true);

        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble())).thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ, ShopMessageConfiguration.load(plugin).transactions());

        Player player = server.addPlayer("seller");
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 2));
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        var result = svc.sell(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().notInRotation(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sellInventory_skips_rotation_only_nonvisible_items() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice priceDiamond = new ShopPrice(10.0, 5.0);
        ShopPrice priceIron = new ShopPrice(3.0, 2.0);

        when(pricingManager.isVisibleInMenu(Material.DIAMOND)).thenReturn(false);
        when(pricingManager.isPartOfRotation(Material.DIAMOND)).thenReturn(true);
        when(pricingManager.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.of(priceDiamond));
        when(pricingManager.estimateBulkTotal(eq(Material.DIAMOND), eq(5), any())).thenReturn(25.0);

        when(pricingManager.isVisibleInMenu(Material.IRON_INGOT)).thenReturn(true);
        when(pricingManager.isPartOfRotation(Material.IRON_INGOT)).thenReturn(false);
        when(pricingManager.getPrice(eq(Material.IRON_INGOT))).thenReturn(Optional.of(priceIron));
        when(pricingManager.estimateBulkTotal(eq(Material.IRON_INGOT), eq(3), any())).thenReturn(6.0);

        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble())).thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ, ShopMessageConfiguration.load(plugin).transactions());

        Player player = server.addPlayer("seller");
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 3));
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        var result = svc.sellInventory(player);

        assertTrue(result.success());
        // verify deposit called only for iron total (6.0)
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(6.0));
        // diamond should remain in inventory because it was rotation-only and not visible
        assertTrue(player.getInventory().containsAtLeast(new ItemStack(Material.DIAMOND), 5));
        // iron should have been removed
        assertFalse(player.getInventory().containsAtLeast(new ItemStack(Material.IRON_INGOT), 1));
    }
}
