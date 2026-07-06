package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Rotation binding defaults and per-option parsed items for one category template.
 */
public final class ShopPricingRotationBinding {

    private final String groupId;
    private final ShopMenuLayout.ItemDecoration defaultIcon;
    private final String defaultMenuTitle;
    private final Map<String, List<ShopMenuLayout.Item>> optionItems;

    public ShopPricingRotationBinding(String groupId, ShopMenuLayout.ItemDecoration defaultIcon,
            String defaultMenuTitle, Map<String, List<ShopMenuLayout.Item>> optionItems) {
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.defaultIcon = defaultIcon;
        this.defaultMenuTitle = defaultMenuTitle;
        Map<String, List<ShopMenuLayout.Item>> items = new LinkedHashMap<>();
        if (optionItems != null) {
            for (Map.Entry<String, List<ShopMenuLayout.Item>> entry : optionItems.entrySet()) {
                items.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
            }
        }
        this.optionItems = Collections.unmodifiableMap(items);
    }

    public String groupId() {
        return groupId;
    }

    public ShopMenuLayout.ItemDecoration defaultIcon() {
        return defaultIcon;
    }

    public String defaultMenuTitle() {
        return defaultMenuTitle;
    }

    public List<ShopMenuLayout.Item> itemsFor(String optionId) {
        return optionItems.get(optionId);
    }
}
