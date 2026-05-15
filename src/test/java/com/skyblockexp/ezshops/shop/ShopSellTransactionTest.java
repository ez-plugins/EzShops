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
 * Feature tests for {@link ShopTransactionService#sell(Player, Material, int)}
 * and {@link ShopTransactionService#sellDirect(Player, Material, int)}.
 */
public class ShopSellTransactionTest extends AbstractEzShopsTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ShopTransactionService buildService(ShopPricingManager pricingManager, Economy economy,
            com.skyblockexp.ezshops.EzShopsPlugin plugin) {
        return new ShopTransactionService(pricingManager, economy,
                ShopMessageConfiguration.load(plugin).transactions());
    }

    private ShopPricingManager sellableManager(Material material, double sellPrice, int amount) {
        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        ShopPrice price = new ShopPrice(sellPrice * 2, sellPrice); // buyPrice > 0, sellPrice > 0
        when(pm.getPrice(eq(material))).thenReturn(Optional.of(price));
        when(pm.estimateBulkTotal(eq(material), eq(amount), any())).thenReturn(sellPrice * amount);
        return pm;
    }

    private Economy successEconomy(double balance) {
        Economy econ = Mockito.mock(Economy.class);
        when(econ.getBalance((org.bukkit.OfflinePlayer) any())).thenReturn(balance);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, balance, EconomyResponse.ResponseType.SUCCESS, "ok"));
        when(econ.format(anyDouble())).thenReturn("$0.00");
        return econ;
    }

    // ─── sell(Player, Material, int) ─────────────────────────────────────────

    @Test
    void sell_succeeds_removes_items_from_inventory_and_deposits_money() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0, 16);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 16);

        assertTrue(result.success(), "Expected success but got: " + result.message());
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(80.0));
        // Items should have been removed from the player's inventory
        int remaining = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND) {
                remaining += stack.getAmount();
            }
        }
        assertEquals(0, remaining, "Items should be removed from player inventory after successful sell");
    }

    @Test
    void sell_fails_when_economy_is_null() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        ShopTransactionService svc = buildService(pm, null, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().noEconomy(), result.message());
    }

    @Test
    void sell_fails_when_player_lacks_sell_permission() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0, 1);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        // Player intentionally has NO sell permission
        Player player = server.addPlayer("seller");

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().noSellPermission(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sell_fails_when_amount_is_not_positive() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopTransactionResult resultZero = svc.sell(player, Material.DIAMOND, 0);
        ShopTransactionResult resultNeg = svc.sell(player, Material.DIAMOND, -5);

        assertFalse(resultZero.success());
        assertFalse(resultNeg.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().amountPositive(), resultZero.message());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().amountPositive(), resultNeg.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sell_fails_when_price_not_configured() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        when(pm.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.empty());
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().notConfigured(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sell_fails_when_material_is_not_sellable() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        // ShopPrice(buyPrice, sellPrice) — sellPrice = -1 means canSell() = false
        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        when(pm.getPrice(eq(Material.DIAMOND))).thenReturn(Optional.of(new ShopPrice(10.0, -1.0)));
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().notSellable(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sell_fails_when_player_has_insufficient_items() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0, 16);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        // Player only has 5 diamonds, tries to sell 16
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 16);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().insufficientItems(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sell_fails_when_economy_deposit_is_rejected_and_items_are_refunded() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0, 4);
        Economy econ = Mockito.mock(Economy.class);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "bank offline"));
        when(econ.format(anyDouble())).thenReturn("$0.00");
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 4));

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 4);

        assertFalse(result.success(), "Expected failure when economy deposit is rejected");
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
        // Items should be refunded — player should have diamonds back
        int remaining = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND) {
                remaining += stack.getAmount();
            }
        }
        assertEquals(4, remaining, "Items should be returned to player when economy deposit fails");
    }

    @Test
    void sell_triggers_handleSale_on_pricing_manager_after_success() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.IRON_INGOT, 2.0, 10);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 10));

        ShopTransactionResult result = svc.sell(player, Material.IRON_INGOT, 10);

        assertTrue(result.success(), "Expected success but got: " + result.message());
        verify(pm).handleSale(eq(Material.IRON_INGOT), eq(10));
    }

    // ─── sellDirect(Player, Material, int) ───────────────────────────────────

    @Test
    void sellDirect_succeeds_without_requiring_items_in_player_inventory() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0, 8);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        // No diamonds in inventory — items are assumed to already be in an external source

        ShopTransactionResult result = svc.sellDirect(player, Material.DIAMOND, 8);

        assertTrue(result.success(), "Expected success but got: " + result.message());
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(40.0));
    }

    @Test
    void sellDirect_fails_when_player_lacks_sell_permission() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0, 1);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller"); // no permission added

        ShopTransactionResult result = svc.sellDirect(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().noSellPermission(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sellDirect_fails_when_amount_is_not_positive() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0, 1);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopTransactionResult result = svc.sellDirect(player, Material.DIAMOND, 0);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().amountPositive(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sellDirect_fails_when_price_not_configured() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        when(pm.getPrice(eq(Material.GOLD_INGOT))).thenReturn(Optional.empty());
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopTransactionResult result = svc.sellDirect(player, Material.GOLD_INGOT, 1);

        assertFalse(result.success());
        assertEquals(ShopMessageConfiguration.load(plugin).transactions().errors().notConfigured(), result.message());
        verify(econ, never()).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
    }

    @Test
    void sellDirect_fails_when_economy_deposit_is_rejected_and_no_item_refund_occurs() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0, 4);
        Economy econ = Mockito.mock(Economy.class);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "error"));
        when(econ.format(anyDouble())).thenReturn("$0.00");
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        // No items in inventory because they were already moved to the GUI

        ShopTransactionResult result = svc.sellDirect(player, Material.DIAMOND, 4);

        assertFalse(result.success());
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble());
        // Player should still have NO items (no refund for direct sell)
        int remaining = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND) {
                remaining += stack.getAmount();
            }
        }
        assertEquals(0, remaining, "sellDirect should not refund items on economy failure — they belong to the GUI");
    }

    @Test
    void sellDirect_succeeds_for_item_that_is_in_rotation_but_not_currently_visible() {
        // Regression: the Quick Sell GUI must accept all configured items regardless of rotation.
        // Previously sellDirect applied the same rotation guard as sell(), so items like BIRCH_LOG
        // that are configured but whose rotation slot is inactive would return notInRotation.
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = Mockito.mock(ShopPricingManager.class);
        ShopPrice price = new ShopPrice(10.0, 3.0);
        when(pm.getPrice(eq(Material.BIRCH_LOG))).thenReturn(Optional.of(price));
        when(pm.estimateBulkTotal(eq(Material.BIRCH_LOG), eq(8), any())).thenReturn(24.0);
        // Simulate: item is part of a rotation but its rotation slot is inactive
        when(pm.isVisibleInMenu(Material.BIRCH_LOG)).thenReturn(false);
        when(pm.isPartOfRotation(Material.BIRCH_LOG)).thenReturn(true);

        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopTransactionResult result = svc.sellDirect(player, Material.BIRCH_LOG, 8);

        assertTrue(result.success(),
                "sellDirect should bypass rotation guard — quick sell GUI accepts all configured items. Got: "
                        + result.message());
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(24.0));
    }

    @Test
    void sellDirect_triggers_handleSale_on_pricing_manager_after_success() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pm = sellableManager(Material.IRON_INGOT, 2.0, 6);
        Economy econ = successEconomy(0.0);
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        svc.sellDirect(player, Material.IRON_INGOT, 6);

        verify(pm).handleSale(eq(Material.IRON_INGOT), eq(6));
    }
}
