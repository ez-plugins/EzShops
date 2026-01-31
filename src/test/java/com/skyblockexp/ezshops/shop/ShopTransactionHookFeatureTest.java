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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShopTransactionHookFeatureTest extends AbstractEzShopsTest {

    @Test
    void buy_item_triggers_hookService_with_expected_tokens() {
        // load plugin and register mock economy so plugin remains enabled
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        // create a simple price
        ShopPrice price = new ShopPrice(10.0, 5.0);

        when(pricingManager.getPrice(eq("DIAMOND"))).thenReturn(Optional.of(price));
        when(pricingManager.estimateBulkTotal(eq("DIAMOND"), eq(2), any())).thenReturn(20.0);

        when(econ.getBalance((org.bukkit.OfflinePlayer) any())).thenReturn(1000.0);
        when(econ.withdrawPlayer((org.bukkit.OfflinePlayer) any(), anyDouble())).thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ, com.skyblockexp.ezshops.config.ShopMessageConfiguration.load(plugin).transactions());

        TransactionHookService hook = Mockito.mock(TransactionHookService.class);
        svc.setTransactionHookService(hook);

        Player player = server.addPlayer("buyer");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_BUY, true);

        ShopMenuLayout.ItemDecoration decoration = new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "", List.<String>of());
        List<String> buyCommands = List.of("hookcmd {player} {amount} {total}");
        List<String> sellCommands = List.<String>of();
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("diamond_item", Material.DIAMOND, decoration, 0, 0, 1, 1,
                price, ShopMenuLayout.ItemType.MATERIAL, null, Map.of(), 0, com.skyblockexp.ezshops.shop.ShopPriceType.STATIC,
                buyCommands, sellCommands, Boolean.FALSE, null);

        svc.buy(player, item, 2);

        ArgumentCaptor<Map> tokensCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hook).executeHooks(eq(player), eq(buyCommands), eq(Boolean.FALSE), tokensCaptor.capture());
        Map<String, String> tokens = tokensCaptor.getValue();
        assertEquals("2", tokens.get("amount"));
        assertEquals("diamond_item", tokens.get("item"));
        assertEquals("DIAMOND", tokens.get("material"));
    }

    @Test
    void sell_item_triggers_hookService_with_expected_tokens() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pricingManager.getPrice(eq("DIAMOND"))).thenReturn(Optional.of(price));
        when(pricingManager.estimateBulkTotal(eq("DIAMOND"), eq(3), any())).thenReturn(15.0);

        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble())).thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ, com.skyblockexp.ezshops.config.ShopMessageConfiguration.load(plugin).transactions());

        TransactionHookService hook = Mockito.mock(TransactionHookService.class);
        svc.setTransactionHookService(hook);

        Player player = server.addPlayer("seller");
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 3));
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopMenuLayout.ItemDecoration decoration = new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "", List.<String>of());
        List<String> buyCommands = List.<String>of();
        List<String> sellCommands = List.of("sellhook {player} {amount} {total}");
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("diamond_item", Material.DIAMOND, decoration, 0, 0, 1, 1,
                price, ShopMenuLayout.ItemType.MATERIAL, null, Map.of(), 0, com.skyblockexp.ezshops.shop.ShopPriceType.STATIC,
                buyCommands, sellCommands, Boolean.TRUE, null);

        svc.sell(player, item, 3);

        ArgumentCaptor<Map> tokensCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hook).executeHooks(eq(player), eq(sellCommands), eq(Boolean.TRUE), tokensCaptor.capture());
        Map<String, String> tokens = tokensCaptor.getValue();
        assertEquals("3", tokens.get("amount"));
        assertEquals("diamond_item", tokens.get("item"));
        assertEquals("DIAMOND", tokens.get("material"));
    }
}
