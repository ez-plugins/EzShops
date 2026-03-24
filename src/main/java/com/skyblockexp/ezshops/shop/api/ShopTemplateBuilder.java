package com.skyblockexp.ezshops.shop.api;

import com.skyblockexp.ezshops.shop.template.ShopTemplate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Helper to build a `ShopTemplate` programmatically. Allows adding categories and items.
 */
public final class ShopTemplateBuilder {
    private final String id;
    private final String name;
    private final Map<String, ShopTemplateCategory> categories = new LinkedHashMap<>();

    public ShopTemplateBuilder(String id, String name) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
    }

    public ShopTemplateBuilder addCategory(ShopTemplateCategory category) {
        categories.put(category.id(), category);
        return this;
    }

    public ShopTemplateBuilder addCategory(String id, java.util.Map<String, Object> properties) {
        ShopTemplateCategory c = new ShopTemplateCategory(id, properties, java.util.Map.of());
        categories.put(id, c);
        return this;
    }

    public ShopTemplateBuilder addItemToCategory(String categoryId, String itemId, ShopItem item) {
        ShopTemplateCategory cat = categories.get(categoryId);
        if (cat == null) {
            cat = new ShopTemplateCategory(categoryId, java.util.Map.of(), new java.util.LinkedHashMap<>());
            categories.put(categoryId, cat);
        }
        // we need to mutate the category's items map -> create new category with merged items
        Map<String, Map<String, Object>> newItems = new LinkedHashMap<>(cat.items());
        newItems.put(itemId, item.toMap());
        ShopTemplateCategory newCat = new ShopTemplateCategory(cat.id(), cat.properties(), newItems);
        categories.put(categoryId, newCat);
        return this;
    }

    public ShopTemplate build() {
        return new ShopTemplate(id, name, java.util.List.of(), java.util.Map.of(), Map.copyOf(categories));
    }
}
