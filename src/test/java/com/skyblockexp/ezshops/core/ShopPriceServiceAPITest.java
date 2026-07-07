package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShopPriceServiceAPITest extends AbstractEzShopsTest {

    @Test
    void shop_price_service_returns_prices_for_configured_items() {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // ensure API initialized
        try {
            com.skyblockexp.ezshops.api.EzShopsAPI.initialize(plugin);
        } catch (IllegalStateException ignored) {}

        var api = com.skyblockexp.ezshops.api.EzShopsAPI.getInstance();
        assertNotNull(api.getShopAPI());

        CoreShopComponent core = com.skyblockexp.ezshops.bootstrap.EzShopsRegistry.current().getCoreShopComponent();
        ShopPricingManager pricingManager = core.pricingManager();
        assertNotNull(pricingManager);

        ShopPrice base = new ShopPrice(2.5D, 1.0D);
        pricingManager.putPriceEntryForTesting(Material.APPLE.name(), base,
            1.0D, 0.5D, 2.0D, 0.0D, 0.0D, 1.0D);

        // call service
        var service = api.getShopAPI();
        ItemStack stack = new ItemStack(Material.APPLE, 4);
        var buy = service.findBuyPrice(stack);
        var sell = service.findSellPrice(stack);
        assertTrue(buy.isPresent());
        assertTrue(sell.isPresent());
        assertEquals(2.5D * 4, buy.getAsDouble(), 0.0001);
        assertEquals(1.0D * 4, sell.getAsDouble(), 0.0001);
    }

    @Test
    void shop_price_service_returns_empty_for_unknown_items() {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        try {
            com.skyblockexp.ezshops.api.EzShopsAPI.initialize(plugin);
        } catch (IllegalStateException ignored) {}

        var api = com.skyblockexp.ezshops.api.EzShopsAPI.getInstance();
        var service = api.getShopAPI();
        ItemStack stack = new ItemStack(Material.BARRIER, 1);
        assertTrue(service.findBuyPrice(stack).isEmpty());
        assertTrue(service.findSellPrice(stack).isEmpty());
    }
}

