package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.hook.TransactionHookService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShopItemTypeFeatureTest extends AbstractEzShopsTest {

    @Test
    void buy_item_type_item_gives_item_and_runs_hooks() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pricingManager.getPrice(eq("DIAMOND"))).thenReturn(Optional.of(price));
        when(pricingManager.estimateBulkTotal(eq("DIAMOND"), eq(2), any())).thenReturn(20.0);

        when(econ.getBalance((org.bukkit.OfflinePlayer) any())).thenReturn(1000.0);
        when(econ.withdrawPlayer((org.bukkit.OfflinePlayer) any(), anyDouble())).thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ, com.skyblockexp.ezshops.config.ShopMessageConfiguration.load(plugin).transactions());

        TransactionHookService hook = Mockito.mock(TransactionHookService.class);
        svc.setTransactionHookService(hook);

        Player player = server.addPlayer("buyer1");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_BUY, true);

        ShopMenuLayout.ItemDecoration decoration = new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "", List.<String>of());
        List<String> buyCommands = List.of("hookcmd {player} {amount} {total}");
        List<String> sellCommands = List.<String>of();
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("diamond_item", Material.DIAMOND, decoration, 0, 0, 1, 1,
                price, ShopMenuLayout.ItemType.MATERIAL, null, Map.of(), 0, com.skyblockexp.ezshops.shop.ShopPriceType.STATIC,
                buyCommands, sellCommands, false, null, DeliveryType.ITEM);

        svc.buy(player, item, 2);

        // inventory should contain the given items
        int count = player.getInventory().all(Material.DIAMOND).values().stream().mapToInt(ItemStack::getAmount).sum();
        assertTrue(count >= 2, "Player should have received diamonds");

        ArgumentCaptor<Map> tokensCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hook).executeHooks(eq(player), eq(buyCommands), eq(false), tokensCaptor.capture());
    }

    @Test
    void buy_item_type_command_runs_hooks_but_no_item_given() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pricingManager.getPrice(eq("DIAMOND"))).thenReturn(Optional.of(price));
        when(pricingManager.estimateBulkTotal(eq("DIAMOND"), eq(1), any())).thenReturn(10.0);

        when(econ.getBalance((org.bukkit.OfflinePlayer) any())).thenReturn(1000.0);
        when(econ.withdrawPlayer((org.bukkit.OfflinePlayer) any(), anyDouble())).thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ, com.skyblockexp.ezshops.config.ShopMessageConfiguration.load(plugin).transactions());

        TransactionHookService hook = Mockito.mock(TransactionHookService.class);
        svc.setTransactionHookService(hook);

        Player player = server.addPlayer("buyer2");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_BUY, true);

        ShopMenuLayout.ItemDecoration decoration = new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "", List.<String>of());
        List<String> buyCommands = List.of("hookcmd {player} {amount} {total}");
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("diamond_command", Material.DIAMOND, decoration, 0, 0, 1, 1,
                price, ShopMenuLayout.ItemType.MATERIAL, null, Map.of(), 0, com.skyblockexp.ezshops.shop.ShopPriceType.STATIC,
                buyCommands, List.of(), false, null, DeliveryType.COMMAND);

        svc.buy(player, item, 1);

        // inventory should NOT contain the item
        int count = player.getInventory().all(Material.DIAMOND).values().stream().mapToInt(ItemStack::getAmount).sum();
        assertEquals(0, count, "Player should not receive diamonds for COMMAND delivery");

        ArgumentCaptor<Map> tokensCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hook).executeHooks(eq(player), eq(buyCommands), eq(false), tokensCaptor.capture());
    }

    @Test
    void sell_command_delivery_succeeds_without_physical_items_and_runs_hooks() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pricingManager.getPrice(eq("DIAMOND"))).thenReturn(Optional.of(price));
        when(pricingManager.estimateBulkTotal(eq("DIAMOND"), eq(1), any())).thenReturn(5.0);

        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 5.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ,
                com.skyblockexp.ezshops.config.ShopMessageConfiguration.load(plugin).transactions());

        TransactionHookService hook = Mockito.mock(TransactionHookService.class);
        svc.setTransactionHookService(hook);

        // Player has NO DIAMOND in their inventory — a COMMAND sell should not require it
        Player player = server.addPlayer("seller_cmd");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopMenuLayout.ItemDecoration decoration =
                new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "", List.of());
        List<String> sellCommands = List.of("give {player} diamond 1");
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("diamond_command_sell", Material.DIAMOND, decoration,
                0, 0, 1, 1, price, ShopMenuLayout.ItemType.MATERIAL, null, Map.of(), 0,
                ShopPriceType.STATIC, List.of(), sellCommands, Boolean.TRUE, null, DeliveryType.COMMAND);

        ShopTransactionResult result = svc.sell(player, item, 1);

        assertTrue(result.success(), "COMMAND-delivery sell should succeed even without physical items: " + result.message());

        // Economy should have deposited the sell price
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(5.0));

        // Inventory must be untouched
        int remaining = player.getInventory().all(Material.DIAMOND).values().stream()
                .mapToInt(ItemStack::getAmount).sum();
        assertEquals(0, remaining, "COMMAND-delivery sell must not remove items from the player's inventory");

        // Sell hooks must still run
        ArgumentCaptor<Map> tokensCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hook).executeHooks(eq(player), eq(sellCommands), eq(Boolean.TRUE), tokensCaptor.capture());
        assertEquals("1", tokensCaptor.getValue().get("amount"));
    }

    @Test
    void buy_item_type_none_charges_but_no_item_and_no_hooks() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pricingManager.getPrice(eq("DIAMOND"))).thenReturn(Optional.of(price));
        when(pricingManager.estimateBulkTotal(eq("DIAMOND"), eq(1), any())).thenReturn(10.0);

        when(econ.getBalance((org.bukkit.OfflinePlayer) any())).thenReturn(1000.0);
        when(econ.withdrawPlayer((org.bukkit.OfflinePlayer) any(), anyDouble())).thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ, com.skyblockexp.ezshops.config.ShopMessageConfiguration.load(plugin).transactions());

        TransactionHookService hook = Mockito.mock(TransactionHookService.class);
        svc.setTransactionHookService(hook);

        Player player = server.addPlayer("buyer3");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_BUY, true);

        ShopMenuLayout.ItemDecoration decoration = new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "", List.<String>of());
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("diamond_none", Material.DIAMOND, decoration, 0, 0, 1, 1,
            price, ShopMenuLayout.ItemType.MATERIAL, null, Map.of(), 0, com.skyblockexp.ezshops.shop.ShopPriceType.STATIC,
            List.of(), List.of(), false, null, DeliveryType.NONE);

        svc.buy(player, item, 1);

        // inventory should NOT contain the item
        int count = player.getInventory().all(Material.DIAMOND).values().stream().mapToInt(ItemStack::getAmount).sum();
        assertEquals(0, count, "Player should not receive diamonds for NONE delivery");

        // hooks should NOT be executed
        verify(hook, Mockito.never()).executeHooks(any(), any(), anyBoolean(), any());
    }
}
