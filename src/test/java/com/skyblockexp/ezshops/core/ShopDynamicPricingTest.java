package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShopDynamicPricingTest extends AbstractEzShopsTest {

    @Test
    void dynamic_multiplier_updates_after_purchase_and_estimates_change() {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        // Grab pricing manager
        ShopPricingManager pricingManager = core.pricingManager();
        assertNotNull(pricingManager);

        ShopPrice base = new ShopPrice(10.0D, 5.0D);

        pricingManager.putPriceEntryForTesting(Material.DIAMOND.name(), base,
                1.0D, 0.5D, 2.0D, 0.10D, 0.05D, 1.0D);

        // Verify initial current price
        java.util.Optional<ShopPrice> optPrice = pricingManager.getPrice(Material.DIAMOND);
        assertTrue(optPrice.isPresent());
        double initialBuy = optPrice.get().buyPrice();
        assertEquals(10.0D, initialBuy, 0.0001);

        // estimate bulk for 3 units (should account for incremental +10% per unit)
        double estimated = pricingManager.estimateBulkTotal(Material.DIAMOND, 3, ShopTransactionType.BUY);
        assertTrue(estimated > 0);

        // Trigger a purchase of 2 units
        pricingManager.handlePurchase(Material.DIAMOND, 2);

        // After purchase, current buy price should have increased (multiplier > 1)
        optPrice = pricingManager.getPrice(Material.DIAMOND);
        assertTrue(optPrice.isPresent());
        double afterBuy = optPrice.get().buyPrice();
        assertTrue(afterBuy > initialBuy, "Expected increased buy price after purchases");
    }
}
