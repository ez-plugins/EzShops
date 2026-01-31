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

public class ShopDynamicPricingTest extends AbstractEzShopsTest {

    @Test
    void dynamic_multiplier_updates_after_purchase_and_estimates_change() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        // Grab pricing manager
        Field pricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        pricingField.setAccessible(true);
        Object pricingManager = pricingField.get(core);
        assertNotNull(pricingManager);

        Class<?> pmClass = pricingManager.getClass();

        // Prepare a base ShopPrice (buy 10, sell 5)
        ShopPrice base = new ShopPrice(10.0D, 5.0D);

        // Create DynamicSettings record instance via reflection
        Class<?> dynClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$DynamicSettings");
        Constructor<?> dynCtor = dynClass.getDeclaredConstructor(double.class, double.class, double.class, double.class, double.class);
        dynCtor.setAccessible(true);
        Object dynSettings = dynCtor.newInstance(1.0D, 0.5D, 2.0D, 0.10D, 0.05D); // start=1.0, buy+10% per unit, sell-5% per unit

        // Create PriceEntry instance
        Class<?> entryClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager$PriceEntry");
        Constructor<?> entryCtor = entryClass.getDeclaredConstructor(ShopPrice.class, dynClass, double.class);
        entryCtor.setAccessible(true);
        Object entry = entryCtor.newInstance(base, dynSettings, 1.0D);

        // Insert into priceMap
        Field priceMapField = pmClass.getDeclaredField("priceMap");
        priceMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> priceMap = (Map<String, Object>) priceMapField.get(pricingManager);
        priceMap.put(Material.DIAMOND.name(), entry);

        // Verify initial current price
        java.lang.reflect.Method getPriceMethod = pmClass.getMethod("getPrice", Material.class);
        java.util.Optional<?> optPrice = (java.util.Optional<?>) getPriceMethod.invoke(pricingManager, Material.DIAMOND);
        assertTrue(optPrice.isPresent());
        Object priceObj = optPrice.get();
        java.lang.reflect.Method buyMethod = priceObj.getClass().getMethod("buyPrice");
        double initialBuy = (Double) buyMethod.invoke(priceObj);
        assertEquals(10.0D, initialBuy, 0.0001);

        // estimate bulk for 3 units (should account for incremental +10% per unit)
        java.lang.reflect.Method estimate = pmClass.getMethod("estimateBulkTotal", Material.class, int.class, Class.forName("com.skyblockexp.ezshops.gui.shop.ShopTransactionType"));
        double estimated = (Double) estimate.invoke(pricingManager, Material.DIAMOND, 3, Enum.valueOf((Class<Enum>) Class.forName("com.skyblockexp.ezshops.gui.shop.ShopTransactionType"), "BUY"));
        assertTrue(estimated > 0);

        // Trigger a purchase of 2 units
        java.lang.reflect.Method handlePurchase = pmClass.getMethod("handlePurchase", Material.class, int.class);
        handlePurchase.invoke(pricingManager, Material.DIAMOND, 2);

        // After purchase, current buy price should have increased (multiplier > 1)
        optPrice = (java.util.Optional<?>) getPriceMethod.invoke(pricingManager, Material.DIAMOND);
        assertTrue(optPrice.isPresent());
        priceObj = optPrice.get();
        double afterBuy = (Double) buyMethod.invoke(priceObj);
        assertTrue(afterBuy > initialBuy, "Expected increased buy price after purchases");
    }
}
