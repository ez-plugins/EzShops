package com.skyblockexp.ezshops.shop.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a shop category for use within a ShopTemplate.
 *
 * This is a lightweight, flexible representation mirroring the structure
 * of the category YAML files under `shop/categories/*.yml`.
 */
public final class ShopTemplateCategory {
    private final String id;
    private final Map<String, Object> properties;
    private final Map<String, Map<String, Object>> items;

    public ShopTemplateCategory(String id, Map<String, Object> properties, Map<String, Map<String, Object>> items) {
        this.id = Objects.requireNonNull(id);
        this.properties = properties == null ? Collections.emptyMap() : Map.copyOf(properties);
        this.items = items == null ? Collections.emptyMap() : copyItems(items);
    }

    private static Map<String, Map<String, Object>> copyItems(Map<String, Map<String, Object>> items) {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : items.entrySet()) {
            m.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        return m;
    }

    public String id() { return id; }

    /**
     * Returns arbitrary category-level properties (name, slot, icon, menu, etc.).
     */
    public Map<String, Object> properties() { return properties; }

    /**
     * Returns the configured items for this category mapping item-id -> item-properties.
     */
    public Map<String, Map<String, Object>> items() { return items; }

    /**
     * Convert category to a serializable mapping suitable for YAML output.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>(properties);
        if (!items.isEmpty()) out.put("items", items);
        return out;
    }
}
