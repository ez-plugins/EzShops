package com.skyblockexp.ezshops.shop;

import java.util.Locale;

/**
 * Represents the strategy used to advance between options in a rotation group.
 */
public enum ShopRotationMode {

    SEQUENTIAL,
    RANDOM;

    public static ShopRotationMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return SEQUENTIAL;
        }
        String normalized = value.trim().toUpperCase(Locale.ENGLISH).replace('-', '_');
        for (ShopRotationMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return SEQUENTIAL;
    }
}

