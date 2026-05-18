package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.hook.TransactionHookService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
            buyCommands, sellCommands, false, null);

        svc.buy(player, item, 2);

        ArgumentCaptor<Map> tokensCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hook).executeHooks(eq(player), eq(buyCommands), eq(false), tokensCaptor.capture());
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

        svc.setIgnoreItemsWithNBT(false); // This test expects items with no NBT filtering

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

    @Test
    void sell_command_delivery_item_runs_hooks_and_deposits_economy_without_physical_item() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 3.8);
        when(pricingManager.getPrice(eq("AMETHYST_SHARD"))).thenReturn(Optional.of(price));
        when(pricingManager.estimateBulkTotal(eq("AMETHYST_SHARD"), eq(16), any())).thenReturn(60.8);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 60.8, EconomyResponse.ResponseType.SUCCESS, "ok"));

        ShopTransactionService svc = new ShopTransactionService(pricingManager, econ,
                com.skyblockexp.ezshops.config.ShopMessageConfiguration.load(plugin).transactions());

        TransactionHookService hook = Mockito.mock(TransactionHookService.class);
        svc.setTransactionHookService(hook);

        // Player has no AMETHYST_SHARD — COMMAND delivery should not require physical items
        Player player = server.addPlayer("seller_shard");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopMenuLayout.ItemDecoration decoration =
                new ShopMenuLayout.ItemDecoration(org.bukkit.Material.AMETHYST_SHARD, 1, "", List.of());
        List<String> sellCommands = List.of("say hi");
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("AMETHYST_SHARD", org.bukkit.Material.AMETHYST_SHARD,
                decoration, 10, 0, 16, 64, price, ShopMenuLayout.ItemType.MATERIAL, null, Map.of(), 0,
                com.skyblockexp.ezshops.shop.ShopPriceType.STATIC,
                List.of(), sellCommands, Boolean.TRUE, null,
                com.skyblockexp.ezshops.shop.DeliveryType.COMMAND);

        ShopTransactionResult result = svc.sell(player, item, 16);

        assertTrue(result.success(),
                "Sell with COMMAND delivery should succeed even when player has no physical items: " + result.message());

        // Economy deposit must occur
        verify(econ).depositPlayer((org.bukkit.OfflinePlayer) any(), eq(60.8));

        // Sell hook must fire
        verify(hook).executeHooks(eq(player), eq(sellCommands), eq(Boolean.TRUE), any());
    }

    @Test
    void sell_execute_as_is_independent_from_buy_execute_as() {
        // Tests that on-sell execute-as:console does not inherit on-buy execute-as:player.
        // sellCommandsRunAsConsole() must reflect the sell-specific setting.
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

        Player player = server.addPlayer("seller_exec");
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 1));
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        svc.setIgnoreItemsWithNBT(false);

        ShopMenuLayout.ItemDecoration decoration =
                new ShopMenuLayout.ItemDecoration(org.bukkit.Material.DIAMOND, 1, "", List.of());
        List<String> buyCommands = List.of("buycmd");
        List<String> sellCommands = List.of("sellcmd");

        // Simulate: on-buy execute-as: player (commandsRunAsConsole = FALSE)
        //           on-sell execute-as: console (sellCommandsRunAsConsole = TRUE)
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("diamond_exec", org.bukkit.Material.DIAMOND,
                decoration, 0, 0, 1, 1, price, ShopMenuLayout.ItemType.MATERIAL, null, Map.of(), 0,
                com.skyblockexp.ezshops.shop.ShopPriceType.STATIC,
                buyCommands, sellCommands,
                Boolean.FALSE,   // on-buy: execute-as: player
                Boolean.TRUE,    // on-sell: execute-as: console  (independent)
                null,
                com.skyblockexp.ezshops.shop.DeliveryType.ITEM);

        // Verify the accessor honours sell-specific setting
        assertEquals(Boolean.FALSE, item.commandsRunAsConsole(),
                "buy commandsRunAsConsole should be FALSE (player)");
        assertEquals(Boolean.TRUE, item.sellCommandsRunAsConsole(),
                "sell commandsRunAsConsole should be TRUE (console), independent from buy");

        svc.sell(player, item, 1);

        // Sell hook must run as console (TRUE), NOT as player (FALSE)
        verify(hook).executeHooks(eq(player), eq(sellCommands), eq(Boolean.TRUE), any());
    }

    @Test
    void sell_item_with_nbt_is_ignored_when_configured() {
        loadProviderPlugin(Mockito.mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);

        ShopPricingManager pricingManager = Mockito.mock(ShopPricingManager.class);
        Economy econ = Mockito.mock(Economy.class);

        ShopPrice price = new ShopPrice(10.0, 5.0);
        when(pricingManager.getPrice(eq("DIAMOND"))).thenReturn(Optional.of(price));

        ShopTransactionService svc =
                new ShopTransactionService(
                        pricingManager,
                        econ,
                        ShopMessageConfiguration.load(plugin).transactions()
                );

        svc.setIgnoreItemsWithNBT(true);

        TransactionHookService hook = Mockito.mock(TransactionHookService.class);
        svc.setTransactionHookService(hook);

        Player player = server.addPlayer("seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ItemStack nbtItem = new ItemStack(Material.DIAMOND, 3);
        nbtItem.getItemMeta().getPersistentDataContainer()
                .set(new NamespacedKey(plugin, "custom"), PersistentDataType.STRING, "yes");
        player.getInventory().addItem(nbtItem);

        ShopMenuLayout.ItemDecoration decoration =
                new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "", List.of());

        List<String> sellCommands = List.of("sellhook {player} {amount} {total}");

        ShopMenuLayout.Item item =
                new ShopMenuLayout.Item(
                        "diamond_item",
                        Material.DIAMOND,
                        decoration,
                        0, 0, 1, 1,
                        price,
                        ShopMenuLayout.ItemType.MATERIAL,
                        null,
                        Map.of(),
                        0,
                        ShopPriceType.STATIC,
                        List.of(),
                        sellCommands,
                        Boolean.TRUE,
                        null
                );

        ShopTransactionResult result = svc.sell(player, item, 3);

        assertFalse(result.success());
        assertEquals(
                ShopMessageConfiguration.load(plugin).transactions().errors().invalidSellPrice(),
                result.message()
        );

        verify(hook, never()).executeHooks(any(), any(), eq(Boolean.TRUE), any());
    }
}
