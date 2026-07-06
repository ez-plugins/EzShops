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
        Optional<ShopPrice> beforeOpt = pm.getPrice(wheatKey);
        assertTrue(beforeOpt.isPresent());
        ShopPrice before = beforeOpt.get();
        double initialBuy = before.buyPrice();

        assertTrue(pm.setPriceMultiplierForTesting(wheatKey, 2.0));

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

        String wheatKey = "TEST_WHEAT";
        String carrotKey = "TEST_CARROT";
        pm.putPriceEntryForTesting(wheatKey, new ShopPrice(10.0D, 4.0D),
            1.0D, 0.5D, 2.0D, 0.10D, 0.10D, 1.0D);
        pm.putPriceEntryForTesting(carrotKey, new ShopPrice(8.0D, 3.0D),
            1.0D, 0.5D, 2.0D, 0.10D, 0.10D, 1.0D);
        Optional<ShopPrice> wBefore = pm.getPrice(wheatKey);
        Optional<ShopPrice> cBefore = pm.getPrice(carrotKey);
        assertTrue(wBefore.isPresent());
        assertTrue(cBefore.isPresent());
        double wInitial = wBefore.get().buyPrice();
        double cInitial = cBefore.get().buyPrice();

        assertTrue(pm.setPriceMultiplierForTesting(wheatKey, 1.9));
        assertTrue(pm.setPriceMultiplierForTesting(carrotKey, 1.5));

        // Reset and verify values return to initial (in-memory reset)
        pm.resetAllDynamicPricing();

        assertEquals(wInitial, pm.getPrice(wheatKey).get().buyPrice(), 1e-6);
        assertEquals(cInitial, pm.getPrice(carrotKey).get().buyPrice(), 1e-6);
    }
}
