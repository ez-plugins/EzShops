package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ShopPricingManagerResetTest extends AbstractEzShopsTest {

    @Test
    void reset_single_price_resets_in_memory_multiplier() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);
        ShopPricingManager pm = core.pricingManager();
        assertNotNull(pm);

        String wheatKey = Material.WHEAT_SEEDS.name();
        String carrotKey = Material.CARROT.name();
        Optional<ShopPrice> beforeOpt = pm.getPrice(wheatKey);
        if (beforeOpt.isEmpty()) {
            try {
                java.lang.reflect.Field f = ShopPricingManager.class.getDeclaredField("priceMap");
                f.setAccessible(true);
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) f.get(pm);
                System.out.println("priceMap keys: " + map.keySet());
            } catch (Throwable t) {
                System.out.println("failed to inspect priceMap: " + t.getMessage());
            }
        }
        assertTrue(beforeOpt.isPresent());
        ShopPrice before = beforeOpt.get();
        double initialBuy = before.buyPrice();

        // Directly mutate the internal multiplier to simulate dynamic change
        setMultiplier(pm, wheatKey, 2.0);

        Optional<ShopPrice> changedOpt = pm.getPrice(wheatKey);
        assertTrue(changedOpt.isPresent());
        ShopPrice changed = changedOpt.get();
        assertTrue(changed.buyPrice() > initialBuy, "Price should be higher after multiplier change");

        boolean ok = pm.resetDynamicPricing(wheatKey);
        assertTrue(ok, "resetDynamicPricing should return true when resetting a modified key");

        Optional<ShopPrice> resetOpt = pm.getPrice(wheatKey);
        assertTrue(resetOpt.isPresent());
        ShopPrice reset = resetOpt.get();
        assertEquals(initialBuy, reset.buyPrice(), 1e-6, "Price should be reset to base buy price");
    }

    @Test
    void reset_all_prices_resets_all_modified_entries() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);
        ShopPricingManager pm = core.pricingManager();
        assertNotNull(pm);

        String wheatKey = Material.WHEAT_SEEDS.name();
        String carrotKey = Material.CARROT.name();
        Optional<ShopPrice> wBefore = pm.getPrice(wheatKey);
        Optional<ShopPrice> cBefore = pm.getPrice(carrotKey);
        if (wBefore.isEmpty() || cBefore.isEmpty()) {
            try {
                java.lang.reflect.Field f = ShopPricingManager.class.getDeclaredField("priceMap");
                f.setAccessible(true);
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) f.get(pm);
                System.out.println("priceMap keys: " + map.keySet());
            } catch (Throwable t) {
                System.out.println("failed to inspect priceMap: " + t.getMessage());
            }
        }
        assertTrue(wBefore.isPresent());
        assertTrue(cBefore.isPresent());
        double wInitial = wBefore.get().buyPrice();
        double cInitial = cBefore.get().buyPrice();


        // Mutate both entries and ensure resetAll resets them
        setMultiplier(pm, wheatKey, 1.9);
        setMultiplier(pm, carrotKey, 1.5);

        // Reset and verify values return to initial (in-memory reset)
        pm.resetAllDynamicPricing();

        assertEquals(wInitial, pm.getPrice(wheatKey).get().buyPrice(), 1e-6);
        assertEquals(cInitial, pm.getPrice(carrotKey).get().buyPrice(), 1e-6);
    }
    
    // reflection helper to inspect private multiplier for debugging
    private double getMultiplier(ShopPricingManager pm, String key) {
        try {
            java.lang.reflect.Field f = ShopPricingManager.class.getDeclaredField("priceMap");
            f.setAccessible(true);
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) f.get(pm);
            Object entry = map.get(key);
            if (entry == null) return -1.0D;
            java.lang.reflect.Field m = entry.getClass().getDeclaredField("multiplier");
            m.setAccessible(true);
            return (double) m.get(entry);
        } catch (Throwable t) {
            return -2.0D;
        }
    }

    private void setMultiplier(ShopPricingManager pm, String key, double value) {
        try {
            java.lang.reflect.Field f = ShopPricingManager.class.getDeclaredField("priceMap");
            f.setAccessible(true);
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) f.get(pm);
            Object entry = map.get(key);
            if (entry == null) return;
            java.lang.reflect.Field m = entry.getClass().getDeclaredField("multiplier");
            m.setAccessible(true);
            m.set(entry, value);
            // also persist to dynamic state to simulate saved state
            java.lang.reflect.Method save = ShopPricingManager.class.getDeclaredMethod("saveDynamicState", String.class, entry.getClass());
            save.setAccessible(true);
            save.invoke(pm, key, entry);
        } catch (Throwable t) {
            // ignore for test
        }
    }

}
