package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShopDynamicPricingMoreTest extends AbstractEzShopsTest {

    @Test
    void sale_decreases_multiplier_and_sell_price() {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        ShopPricingManager pricingManager = core.pricingManager();

        ShopPrice base = new ShopPrice(10.0D, 10.0D);
        pricingManager.putPriceEntryForTesting(Material.DIAMOND.name(), base,
                1.0D, 0.5D, 2.0D, 0.0D, 0.20D, 1.0D);

        java.util.Optional<ShopPrice> optPrice = pricingManager.getPrice(Material.DIAMOND);
        assertTrue(optPrice.isPresent());
        double initialSell = optPrice.get().sellPrice();

        pricingManager.handleSale(Material.DIAMOND, 2);

        optPrice = pricingManager.getPrice(Material.DIAMOND);
        assertTrue(optPrice.isPresent());
        double afterSell = optPrice.get().sellPrice();
        assertTrue(afterSell < initialSell, "Sell price should decrease after sales");
    }

    @Test
    void multiplier_clamps_to_configured_min_and_max() {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        ShopPricingManager pricingManager = core.pricingManager();

        ShopPrice base = new ShopPrice(20.0D, 10.0D);

        pricingManager.putPriceEntryForTesting(Material.EMERALD.name(), base,
                1.5D, 0.8D, 1.6D, 0.5D, 0.0D, 1.5D);

        java.util.Optional<ShopPrice> optPrice = pricingManager.getPrice(Material.EMERALD);
        assertTrue(optPrice.isPresent());
        double initialBuy = optPrice.get().buyPrice();

        pricingManager.handlePurchase(Material.EMERALD, 10);

        optPrice = pricingManager.getPrice(Material.EMERALD);
        assertTrue(optPrice.isPresent());
        double afterBuy = optPrice.get().buyPrice();

        assertTrue(initialBuy >= base.buyPrice());
        assertTrue(afterBuy <= base.buyPrice() * 1.6001);
    }

    @Test
    void dynamic_state_is_saved_in_memory_after_change() {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        ShopPricingManager pricingManager = core.pricingManager();

        ShopPrice base = new ShopPrice(5.0D, 2.0D);

        pricingManager.putPriceEntryForTesting(Material.GOLD_INGOT.name(), base,
            1.0D, 0.5D, 2.0D, 0.10D, 0.10D, 1.0D);

        pricingManager.handlePurchase(Material.GOLD_INGOT, 1);

        // Reset returns true when a persisted entry exists and gets removed.
        assertTrue(pricingManager.resetDynamicPricing(Material.GOLD_INGOT.name()));
    }
}
