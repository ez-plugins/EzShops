package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.shop.ShopPrice;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ShopDynamicPricingMoreTest extends AbstractEzShopsTest {

    @Test
    void sale_decreases_multiplier_and_sell_price() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        Field pricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        pricingField.setAccessible(true);
        Object pricingManager = pricingField.get(core);

        ShopPrice base = new ShopPrice(10.0D, 10.0D);

        Class<?> dynClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$DynamicSettings");
        Constructor<?> dynCtor = dynClass.getDeclaredConstructor(double.class, double.class, double.class, double.class, double.class);
        dynCtor.setAccessible(true);
        Object dynSettings = dynCtor.newInstance(1.0D, 0.5D, 2.0D, 0.0D, 0.20D); // sellChange 20%

        Class<?> entryClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$PriceEntry");
        Constructor<?> entryCtor = entryClass.getDeclaredConstructor(ShopPrice.class, dynClass, double.class);
        entryCtor.setAccessible(true);
        Object entry = entryCtor.newInstance(base, dynSettings, 1.0D);

        Field priceMapField = pricingManager.getClass().getDeclaredField("priceMap");
        priceMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> priceMap = (Map<String, Object>) priceMapField.get(pricingManager);
        priceMap.put(Material.DIAMOND.name(), entry);

        java.lang.reflect.Method getPriceMethod = pricingManager.getClass().getMethod("getPrice", Material.class);
        java.util.Optional<?> optPrice = (java.util.Optional<?>) getPriceMethod.invoke(pricingManager, Material.DIAMOND);
        assertTrue(optPrice.isPresent());
        Object priceObj = optPrice.get();
        double initialSell = (Double) priceObj.getClass().getMethod("sellPrice").invoke(priceObj);

        // perform sale of 2 units -> multiplier should decrease
        java.lang.reflect.Method handleSale = pricingManager.getClass().getMethod("handleSale", Material.class, int.class);
        handleSale.invoke(pricingManager, Material.DIAMOND, 2);

        optPrice = (java.util.Optional<?>) getPriceMethod.invoke(pricingManager, Material.DIAMOND);
        assertTrue(optPrice.isPresent());
        priceObj = optPrice.get();
        double afterSell = (Double) priceObj.getClass().getMethod("sellPrice").invoke(priceObj);
        assertTrue(afterSell < initialSell, "Sell price should decrease after sales");
    }

    @Test
    void multiplier_clamps_to_configured_min_and_max() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        Field pricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        pricingField.setAccessible(true);
        Object pricingManager = pricingField.get(core);

        ShopPrice base = new ShopPrice(20.0D, 10.0D);

        Class<?> dynClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$DynamicSettings");
        Constructor<?> dynCtor = dynClass.getDeclaredConstructor(double.class, double.class, double.class, double.class, double.class);
        dynCtor.setAccessible(true);
        // starting 1.5, min 0.8, max 1.6, buyChange 50% to force clamp quickly
        Object dynSettings = dynCtor.newInstance(1.5D, 0.8D, 1.6D, 0.5D, 0.0D);

        Class<?> entryClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$PriceEntry");
        Constructor<?> entryCtor = entryClass.getDeclaredConstructor(ShopPrice.class, dynClass, double.class);
        entryCtor.setAccessible(true);
        Object entry = entryCtor.newInstance(base, dynSettings, 1.5D);

        Field priceMapField = pricingManager.getClass().getDeclaredField("priceMap");
        priceMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> priceMap = (Map<String, Object>) priceMapField.get(pricingManager);
        priceMap.put(Material.EMERALD.name(), entry);

        java.lang.reflect.Method getPriceMethod = pricingManager.getClass().getMethod("getPrice", Material.class);
        java.util.Optional<?> optPrice = (java.util.Optional<?>) getPriceMethod.invoke(pricingManager, Material.EMERALD);
        assertTrue(optPrice.isPresent());
        Object priceObj = optPrice.get();
        double initialBuy = (Double) priceObj.getClass().getMethod("buyPrice").invoke(priceObj);

        // perform a large purchase to exceed max multiplier
        java.lang.reflect.Method handlePurchase = pricingManager.getClass().getMethod("handlePurchase", Material.class, int.class);
        handlePurchase.invoke(pricingManager, Material.EMERALD, 10);

        optPrice = (java.util.Optional<?>) getPriceMethod.invoke(pricingManager, Material.EMERALD);
        assertTrue(optPrice.isPresent());
        priceObj = optPrice.get();
        double afterBuy = (Double) priceObj.getClass().getMethod("buyPrice").invoke(priceObj);

        // After applying large buy changes, buy price should be clamped to max (base * 1.6)
        assertTrue(afterBuy <= base.buyPrice() * 1.6001);
    }

    @Test
    void dynamic_state_is_saved_in_memory_after_change() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        Field pricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        pricingField.setAccessible(true);
        Object pricingManager = pricingField.get(core);

        ShopPrice base = new ShopPrice(5.0D, 2.0D);

        Class<?> dynClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$DynamicSettings");
        Constructor<?> dynCtor = dynClass.getDeclaredConstructor(double.class, double.class, double.class, double.class, double.class);
        dynCtor.setAccessible(true);
        Object dynSettings = dynCtor.newInstance(1.0D, 0.5D, 2.0D, 0.10D, 0.10D);

        Class<?> entryClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$PriceEntry");
        Constructor<?> entryCtor = entryClass.getDeclaredConstructor(ShopPrice.class, dynClass, double.class);
        entryCtor.setAccessible(true);
        Object entry = entryCtor.newInstance(base, dynSettings, 1.0D);

        Field priceMapField = pricingManager.getClass().getDeclaredField("priceMap");
        priceMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> priceMap = (Map<String, Object>) priceMapField.get(pricingManager);
        priceMap.put(Material.GOLD_INGOT.name(), entry);

        java.lang.reflect.Method handlePurchase = pricingManager.getClass().getMethod("handlePurchase", Material.class, int.class);
        handlePurchase.invoke(pricingManager, Material.GOLD_INGOT, 1);

        Field dynStateField = pricingManager.getClass().getDeclaredField("dynamicStateConfiguration");
        dynStateField.setAccessible(true);
        Object dynCfg = dynStateField.get(pricingManager);
        // call getDouble via reflection to avoid compile-time dependency
        double saved = (Double) dynCfg.getClass().getMethod("getDouble", String.class).invoke(dynCfg, Material.GOLD_INGOT.name());
        assertTrue(saved > 0.0D, "Expected multiplier value saved in dynamic state configuration");
    }
}
