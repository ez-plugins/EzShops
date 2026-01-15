package com.skyblockexp.ezshops.shop;

/**
 * Represents the configured buy and sell prices for a material.
 * <p>
 * A negative price indicates that the action is not available for the material.
 */
public record ShopPrice(double buyPrice, double sellPrice) {

    /**
     * Returns {@code true} if the material can be purchased from the shop.
     */
    public boolean canBuy() {
        return buyPrice >= 0.0D;
    }

    /**
     * Returns {@code true} if the material can be sold to the shop.
     */
    public boolean canSell() {
        return sellPrice >= 0.0D;
    }
}
