package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopRotationDefinition;
import com.skyblockexp.ezshops.shop.ShopRotationOption;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/**
 * Visibility checks for active menu entries and rotation-declared items.
 */
public final class ShopPricingVisibilityService {

    public boolean isVisibleInMenu(ShopMenuLayout layout, Material material) {
        if (material == null || layout == null) {
            return false;
        }
        String materialName = material.name();
        for (ShopMenuLayout.Category category : layout.categories()) {
            for (ShopMenuLayout.Item item : category.items()) {
                if (ShopItemIdentifierMatcher.matchesMaterial(item, materialName)
                        || ShopItemIdentifierMatcher.matchesKey(item, materialName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isVisibleInMenu(ShopMenuLayout layout, String priceKey) {
        if (layout == null || priceKey == null || priceKey.isBlank()) {
            return false;
        }
        for (ShopMenuLayout.Category category : layout.categories()) {
            for (ShopMenuLayout.Item item : category.items()) {
                if (ShopItemIdentifierMatcher.matchesKey(item, priceKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPartOfRotation(String priceKey,
            List<ShopPricingCategoryTemplate> templates,
            Map<String, ShopRotationDefinition> rotationDefinitions) {
        if (priceKey == null || priceKey.isBlank() || templates == null || templates.isEmpty()) {
            return false;
        }
        for (ShopPricingCategoryTemplate template : templates) {
            if (template == null || !template.isRotating()) {
                continue;
            }
            ShopPricingRotationBinding binding = template.rotation();
            if (binding == null) {
                continue;
            }
            ShopRotationDefinition def = rotationDefinitions.get(binding.groupId());
            if (def == null) {
                continue;
            }
            for (ShopRotationOption option : def.options()) {
                List<ShopMenuLayout.Item> items = binding.itemsFor(option.id());
                if (items == null) {
                    continue;
                }
                for (ShopMenuLayout.Item item : items) {
                    if (ShopItemIdentifierMatcher.matchesKey(item, priceKey)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
