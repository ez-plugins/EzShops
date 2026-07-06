package com.skyblockexp.ezshops.shop.pricing.domain;

/**
 * Dynamic pricing settings for one shop price entry.
 */
public record ShopDynamicSettings(double startingMultiplier, double minMultiplier, double maxMultiplier,
        double buyChange, double sellChange) {

    public double clamp(double value) {
        double clamped = Math.min(maxMultiplier, Math.max(minMultiplier, value));
        return clamped <= 0.0D ? minMultiplier : clamped;
    }
}
