package com.skyblockexp.ezshops.shop;

import java.util.Locale;

public enum DeliveryType {
    ITEM,
    COMMAND,
    NONE;

    public static DeliveryType fromConfig(String raw) {
        if (raw == null) return ITEM;
        String v = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return DeliveryType.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return ITEM;
        }
    }
}
