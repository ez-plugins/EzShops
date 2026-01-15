package com.skyblockexp.ezshops.playershop;

/**
 * Represents the result of attempting to create a player shop from a sign.
 */
public record PlayerShopCreationResult(boolean success, String message, PlayerShop shop) {

    public static PlayerShopCreationResult success(String message, PlayerShop shop) {
        return new PlayerShopCreationResult(true, message, shop);
    }

    public static PlayerShopCreationResult failure(String message) {
        return new PlayerShopCreationResult(false, message, null);
    }
}
