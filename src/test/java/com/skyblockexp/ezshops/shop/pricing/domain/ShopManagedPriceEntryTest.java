package com.skyblockexp.ezshops.shop.pricing.domain;

import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import com.skyblockexp.ezshops.shop.ShopPrice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopManagedPriceEntryTest {

    @Test
    void current_price_applies_multiplier_and_keeps_disabled_side_unavailable() {
        ShopDynamicSettings dynamic = new ShopDynamicSettings(1.5D, 0.5D, 3.0D, 0.10D, 0.05D);

        ShopManagedPriceEntry bothSides = new ShopManagedPriceEntry(new ShopPrice(10.0D, 6.0D), dynamic, 1.5D);
        ShopPrice bothCurrent = bothSides.currentPrice();
        assertEquals(15.0D, bothCurrent.buyPrice(), 1e-9);
        assertEquals(9.0D, bothCurrent.sellPrice(), 1e-9);

        ShopManagedPriceEntry sellDisabled = new ShopManagedPriceEntry(new ShopPrice(10.0D, -1.0D), dynamic, 1.5D);
        ShopPrice sellDisabledCurrent = sellDisabled.currentPrice();
        assertEquals(15.0D, sellDisabledCurrent.buyPrice(), 1e-9);
        assertEquals(-1.0D, sellDisabledCurrent.sellPrice(), 1e-9);

        ShopManagedPriceEntry buyDisabled = new ShopManagedPriceEntry(new ShopPrice(-1.0D, 6.0D), dynamic, 1.5D);
        ShopPrice buyDisabledCurrent = buyDisabled.currentPrice();
        assertEquals(-1.0D, buyDisabledCurrent.buyPrice(), 1e-9);
        assertEquals(9.0D, buyDisabledCurrent.sellPrice(), 1e-9);
    }

    @Test
    void adjust_operations_respect_configured_min_and_max_multiplier() {
        ShopDynamicSettings dynamic = new ShopDynamicSettings(1.0D, 0.8D, 1.6D, 0.50D, 0.30D);
        ShopManagedPriceEntry entry = new ShopManagedPriceEntry(new ShopPrice(20.0D, 10.0D), dynamic, 1.0D);

        assertTrue(entry.adjustAfterPurchase(3));
        assertEquals(1.6D, entry.multiplier(), 1e-9);

        assertTrue(entry.adjustAfterSale(4));
        assertEquals(0.8D, entry.multiplier(), 1e-9);

        assertFalse(entry.adjustAfterPurchase(0));
        assertFalse(entry.adjustAfterSale(0));
    }

    @Test
    void estimate_bulk_total_uses_stepwise_multiplier_for_dynamic_entries() {
        ShopDynamicSettings dynamic = new ShopDynamicSettings(1.0D, 0.5D, 3.0D, 0.10D, 0.20D);
        ShopManagedPriceEntry entry = new ShopManagedPriceEntry(new ShopPrice(10.0D, 10.0D), dynamic, 1.0D);

        double buyTotal = entry.estimateBulkTotal(3, ShopTransactionType.BUY);
        double sellTotal = entry.estimateBulkTotal(3, ShopTransactionType.SELL);

        assertEquals(33.10D, buyTotal, 1e-9);
        assertEquals(24.40D, sellTotal, 1e-9);
    }

    @Test
    void estimate_bulk_total_returns_linear_value_when_dynamic_disabled() {
        ShopManagedPriceEntry staticEntry = new ShopManagedPriceEntry(new ShopPrice(2.5D, 1.0D), null, 1.0D);

        assertEquals(10.0D, staticEntry.estimateBulkTotal(4, ShopTransactionType.BUY), 1e-9);
        assertEquals(4.0D, staticEntry.estimateBulkTotal(4, ShopTransactionType.SELL), 1e-9);
        assertEquals(2.0D, staticEntry.estimateBulkTotal(2, null), 1e-9);

        ShopManagedPriceEntry buyDisabled = new ShopManagedPriceEntry(new ShopPrice(-1.0D, 1.0D), null, 1.0D);
        assertEquals(-1.0D, buyDisabled.estimateBulkTotal(2, ShopTransactionType.BUY), 1e-9);
    }

    @Test
    void reset_to_starting_multiplier_reports_if_value_changed() {
        ShopDynamicSettings dynamic = new ShopDynamicSettings(1.3D, 0.5D, 3.0D, 0.10D, 0.10D);
        ShopManagedPriceEntry entry = new ShopManagedPriceEntry(new ShopPrice(10.0D, 5.0D), dynamic, 2.0D);

        assertTrue(entry.resetToStartingMultiplier());
        assertEquals(1.3D, entry.multiplier(), 1e-9);
        assertFalse(entry.resetToStartingMultiplier());

        ShopManagedPriceEntry staticEntry = new ShopManagedPriceEntry(new ShopPrice(10.0D, 5.0D), null, 1.0D);
        assertFalse(staticEntry.resetToStartingMultiplier());
    }
}
