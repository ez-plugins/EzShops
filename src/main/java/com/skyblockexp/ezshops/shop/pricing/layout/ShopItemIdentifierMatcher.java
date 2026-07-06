package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.Locale;

/**
 * Shared item identifier matching helpers for shop layout lookups.
 */
public final class ShopItemIdentifierMatcher {

    private ShopItemIdentifierMatcher() {}

    public static boolean matchesMaterial(ShopMenuLayout.Item item, String materialName) {
        if (item == null || materialName == null) {
            return false;
        }
        if (item.material() == null) {
            return false;
        }
        return item.material().name().equalsIgnoreCase(materialName);
    }

    public static boolean matchesKey(ShopMenuLayout.Item item, String key) {
        if (item == null || key == null || key.isBlank()) {
            return false;
        }
        if (item.id() != null && item.id().equalsIgnoreCase(key)) {
            return true;
        }
        if (item.priceId() != null && item.priceId().equalsIgnoreCase(key)) {
            return true;
        }
        if (matchesMaterial(item, key)) {
            return true;
        }
        String displayName = item.display() != null ? item.display().displayName() : null;
        return displayName != null && displayName.toLowerCase(Locale.ENGLISH)
                .startsWith(key.toLowerCase(Locale.ENGLISH));
    }
}
