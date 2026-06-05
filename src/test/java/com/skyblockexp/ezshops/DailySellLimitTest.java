package com.skyblockexp.ezshops;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DailySellLimitTest extends AbstractEzShopsTest {

    private ShopTransactionService buildService(ShopPricingManager pm, Economy econ, com.skyblockexp.ezshops.EzShopsPlugin plugin) {
        return new ShopTransactionService(pm, econ, ShopMessageConfiguration.load(plugin).transactions());
    }

    private ShopPricingManager sellableManager(Material material, double sellPrice) {
        ShopPricingManager pm = mock(ShopPricingManager.class);
        ShopPrice price = new ShopPrice(sellPrice * 2, sellPrice);
        when(pm.getPrice(eq(material))).thenReturn(Optional.of(price));
        when(pm.estimateBulkTotal(eq(material), anyInt(), any())).thenReturn(sellPrice * 16.0);
        when(pm.isVisibleInMenu(any(Material.class))).thenReturn(true);
        when(pm.isPartOfRotation(any(Material.class))).thenReturn(false);
        return pm;
    }

    private Economy successEconomy() {
        Economy econ = mock(Economy.class);
        when(econ.depositPlayer((org.bukkit.OfflinePlayer) any(), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));
        when(econ.format(anyDouble())).thenReturn("$0.00");
        return econ;
    }

    private ConfigurationSection createLimitEntry(String mode, int limit) {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString(eq("mode"), anyString())).thenReturn(mode);
        when(section.getInt(eq("limit"), anyInt())).thenReturn(limit);
        return section;
    }

    private void recordSolds(com.skyblockexp.ezshops.EzShopsPlugin plugin, Player player, int amount) {
        File dataDir = new File(plugin.getDataFolder(), "daily-sells");
        if (!dataDir.exists()) dataDir.mkdirs();
        File dataFile = new File(dataDir, player.getUniqueId() + ".yml");
        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
        String today = java.time.LocalDate.now().toString();
        config.set(today, amount);
        try {
            config.save(dataFile);
        } catch (IOException ignored) {}
    }

    @Test
    void sellFails_whenDailyLimitReached() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", true);
        plugin.getConfig().set("daily-sell-limits.limits", Collections.singletonList(createLimitEntry("smp", 512)));
        plugin.getConfig().set("game-mode", "smp");

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller1");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));

        recordSolds(plugin, player, 512);

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertTrue(result.message().contains("daily sell limit"), "Expected daily limit message, got: " + result.message());
    }

    @Test
    void sellSucceeds_whenUnderDailyLimit() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", true);
        plugin.getConfig().set("daily-sell-limits.limits", Collections.singletonList(createLimitEntry("smp", 512)));
        plugin.getConfig().set("game-mode", "smp");

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller2");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));

        recordSolds(plugin, player, 500);

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 10);

        assertTrue(result.success(), "Expected success but got: " + result.message());
    }

    @Test
    void sellSucceeds_whenDailyLimitDisabled() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", false);

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller3");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 16);

        assertTrue(result.success(), "Expected success when limits disabled, got: " + result.message());
    }

    @Test
    void sellSucceeds_whenGameModeHasNoLimitDefined() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", true);
        plugin.getConfig().set("daily-sell-limits.limits", Collections.singletonList(createLimitEntry("smp", 512)));
        plugin.getConfig().set("game-mode", "prison");

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller4");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 16);

        assertTrue(result.success(), "Expected success when current mode has no limit, got: " + result.message());
    }

    @Test
    void sellDirectFails_whenDailyLimitReached() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", true);
        plugin.getConfig().set("daily-sell-limits.limits", Collections.singletonList(createLimitEntry("smp", 512)));
        plugin.getConfig().set("game-mode", "smp");

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller5");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        recordSolds(plugin, player, 512);

        ShopTransactionResult result = svc.sellDirect(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertTrue(result.message().contains("daily sell limit"));
    }

    @Test
    void sellFails_whenAmountWouldExceedRemainingLimit() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", true);
        plugin.getConfig().set("daily-sell-limits.limits", Collections.singletonList(createLimitEntry("smp", 512)));
        plugin.getConfig().set("game-mode", "smp");

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller6");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));

        // Already sold 500, limit is 512, only 12 remaining
        recordSolds(plugin, player, 500);

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 20);

        assertFalse(result.success());
        assertTrue(result.message().contains("can only sell") && result.message().contains("items today"),
                "Expected limit exceeded message, got: " + result.message());
    }

    @Test
    void sellInventoryFails_whenDailyLimitReached() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", true);
        plugin.getConfig().set("daily-sell-limits.limits", Collections.singletonList(createLimitEntry("smp", 512)));
        plugin.getConfig().set("game-mode", "smp");

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller7");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 64));

        recordSolds(plugin, player, 512);

        ShopTransactionResult result = svc.sellInventory(player);

        assertFalse(result.success());
        assertTrue(result.message().contains("daily sell limit"));
    }

    @Test
    void sellInventorySucceeds_whenUnderDailyLimit() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", true);
        plugin.getConfig().set("daily-sell-limits.limits", Collections.singletonList(createLimitEntry("smp", 512)));
        plugin.getConfig().set("game-mode", "smp");

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller8");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 64));

        recordSolds(plugin, player, 100);

        ShopTransactionResult result = svc.sellInventory(player);

        assertTrue(result.success(), "Expected success but got: " + result.message());
    }

    @Test
    void multipleLimitsConfigured_onlyMatchingModeApplies() {
        loadProviderPlugin(mock(Economy.class));
        var plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        plugin.getConfig().set("daily-sell-limits.enabled", true);
        plugin.getConfig().set("daily-sell-limits.limits", java.util.List.of(
                createLimitEntry("smp", 100),
                createLimitEntry("prison", 1000)
        ));
        plugin.getConfig().set("game-mode", "smp");

        ShopPricingManager pm = sellableManager(Material.DIAMOND, 5.0);
        Economy econ = successEconomy();
        ShopTransactionService svc = buildService(pm, econ, plugin);

        Player player = server.addPlayer("seller9");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));

        // Record exactly 100 items (hit SMP limit)
        recordSolds(plugin, player, 100);

        ShopTransactionResult result = svc.sell(player, Material.DIAMOND, 1);

        assertFalse(result.success());
        assertTrue(result.message().contains("daily sell limit"));
    }
}