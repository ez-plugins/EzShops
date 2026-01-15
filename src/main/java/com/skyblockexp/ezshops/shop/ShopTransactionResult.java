package com.skyblockexp.ezshops.shop;

/**
 * Represents the outcome of an attempted shop transaction.
 */
public record ShopTransactionResult(boolean success, String message) {

    public static ShopTransactionResult success(String message) {
        return new ShopTransactionResult(true, message);
    }

    public static ShopTransactionResult failure(String message) {
        return new ShopTransactionResult(false, message);
    }
}
