package com.skyblockexp.ezshops.shop.pricing.domain;

import com.skyblockexp.ezshops.common.EconomyUtils;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPriceType;

/**
 * Price entry state and dynamic-pricing math for one configured price key.
 */
public final class ShopManagedPriceEntry {

    private final ShopPrice basePrice;
    private final ShopDynamicSettings settings;
    private double multiplier;
    private final ShopPriceType priceType;

    public ShopManagedPriceEntry(ShopPrice basePrice, ShopDynamicSettings settings, double initialMultiplier) {
        this(basePrice, settings, initialMultiplier, ShopPriceType.STATIC);
    }

    public ShopManagedPriceEntry(ShopPrice basePrice, ShopDynamicSettings settings, double initialMultiplier,
            ShopPriceType priceType) {
        this.basePrice = basePrice;
        this.settings = settings;
        if (settings != null) {
            this.multiplier = settings.clamp(initialMultiplier);
        } else {
            this.multiplier = 1.0D;
        }
        this.priceType = priceType == null ? ShopPriceType.STATIC : priceType;
    }

    public ShopPrice basePrice() {
        return basePrice;
    }

    public ShopDynamicSettings settings() {
        return settings;
    }

    public ShopPriceType priceType() {
        return priceType;
    }

    public double multiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        if (settings != null) {
            this.multiplier = settings.clamp(multiplier);
        } else {
            this.multiplier = 1.0D;
        }
    }

    public boolean resetToStartingMultiplier() {
        if (!hasDynamicPricing()) {
            return false;
        }
        double previous = multiplier;
        multiplier = settings.clamp(settings.startingMultiplier());
        return Double.compare(previous, multiplier) != 0;
    }

    public ShopPrice currentPrice() {
        if (!hasDynamicPricing()) {
            return basePrice;
        }
        double buy = basePrice.buyPrice();
        double sell = basePrice.sellPrice();
        if (basePrice.canBuy()) {
            buy = EconomyUtils.normalizeCurrency(buy * multiplier);
        } else {
            buy = -1.0D;
        }
        if (basePrice.canSell()) {
            sell = EconomyUtils.normalizeCurrency(sell * multiplier);
        } else {
            sell = -1.0D;
        }
        return new ShopPrice(buy, sell);
    }

    public boolean hasDynamicPricing() {
        return settings != null && basePrice != null && (basePrice.canBuy() || basePrice.canSell());
    }

    public boolean adjustAfterPurchase(int amount) {
        if (!hasDynamicPricing() || amount <= 0) {
            return false;
        }
        double factor = Math.pow(1.0 + settings.buyChange(), amount);
        double previous = multiplier;
        multiplier = settings.clamp(multiplier * factor);
        return Double.compare(previous, multiplier) != 0;
    }

    public boolean adjustAfterSale(int amount) {
        if (!hasDynamicPricing() || amount <= 0) {
            return false;
        }
        double factor = Math.pow(1.0 - settings.sellChange(), amount);
        double previous = multiplier;
        multiplier = settings.clamp(multiplier * factor);
        return Double.compare(previous, multiplier) != 0;
    }

    public double estimateBulkTotal(int amount, ShopTransactionType type) {
        if (!hasDynamicPricing() || amount <= 0) {
            double unit = type == ShopTransactionType.BUY ? basePrice.buyPrice() : basePrice.sellPrice();
            return unit < 0.0D ? -1.0D : EconomyUtils.normalizeCurrency(unit * amount);
        }
        boolean isBuy = type == ShopTransactionType.BUY;
        double baseUnit = isBuy ? basePrice.buyPrice() : basePrice.sellPrice();
        if (baseUnit < 0.0D) {
            return -1.0D;
        }
        double simMultiplier = multiplier;
        double total = 0.0D;
        for (int i = 0; i < amount; i++) {
            double unitPrice = EconomyUtils.normalizeCurrency(baseUnit * simMultiplier);
            total += unitPrice;
            if (isBuy) {
                simMultiplier = settings.clamp(simMultiplier * (1.0 + settings.buyChange()));
            } else {
                simMultiplier = settings.clamp(simMultiplier * (1.0 - settings.sellChange()));
            }
        }
        return EconomyUtils.normalizeCurrency(total);
    }
}
