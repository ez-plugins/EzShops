package com.skyblockexp.ezshops.shop.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for creating ShopTemplateCategory instances programmatically.
 */
public final class ShopTemplateCategoryBuilder {
    private final String id;
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> items = new LinkedHashMap<>();

    public ShopTemplateCategoryBuilder(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public ShopTemplateCategoryBuilder property(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public ShopTemplateCategoryBuilder addItem(String itemId, ShopItem item) {
        items.put(itemId, item.toMap());
        return this;
    }

    public ShopTemplateCategory build() {
        return new ShopTemplateCategory(id, Map.copyOf(properties), Map.copyOf(items));
    }
}
