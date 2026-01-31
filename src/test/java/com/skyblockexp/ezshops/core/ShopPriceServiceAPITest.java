package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.shop.ShopPrice;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ShopPriceServiceAPITest extends AbstractEzShopsTest {

    @Test
    void shop_price_service_returns_prices_for_configured_items() throws Exception {
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

        // inject a price entry for APPLE
        CoreShopComponent core = plugin.getCoreShopComponent();
        Field pricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        pricingField.setAccessible(true);
        Object pricingManager = pricingField.get(core);

        ShopPrice base = new ShopPrice(2.5D, 1.0D);

        Class<?> dynClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$DynamicSettings");
        Constructor<?> dynCtor = dynClass.getDeclaredConstructor(double.class, double.class, double.class, double.class, double.class);
        dynCtor.setAccessible(true);
        Object dynSettings = dynCtor.newInstance(1.0D, 0.5D, 2.0D, 0.0D, 0.0D);

        Class<?> entryClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$PriceEntry");
        Constructor<?> entryCtor = entryClass.getDeclaredConstructor(ShopPrice.class, dynClass, double.class);
        entryCtor.setAccessible(true);
        Object entry = entryCtor.newInstance(base, dynSettings, 1.0D);

        Field priceMapField = pricingManager.getClass().getDeclaredField("priceMap");
        priceMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> priceMap = (Map<String, Object>) priceMapField.get(pricingManager);
        priceMap.put(Material.APPLE.name(), entry);

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
    void shop_price_service_returns_empty_for_unknown_items() throws Exception {
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
