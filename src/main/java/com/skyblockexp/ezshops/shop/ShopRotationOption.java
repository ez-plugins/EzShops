package com.skyblockexp.ezshops.shop;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single option within a rotation definition.
 */
public final class ShopRotationOption {

    private final String id;
    private final ShopMenuLayout.ItemDecoration iconOverride;
    private final String menuTitleOverride;
    private final Map<String, Map<String, Object>> itemOverrides;
    private final double weight;

    public ShopRotationOption(String id, ShopMenuLayout.ItemDecoration iconOverride, String menuTitleOverride,
            Map<String, Map<String, Object>> itemOverrides, double weight) {
        this.id = Objects.requireNonNull(id, "id");
        this.iconOverride = iconOverride;
        this.menuTitleOverride = menuTitleOverride;
        this.itemOverrides = toImmutable(itemOverrides);
        this.weight = Math.max(0.0D, weight);
    }

    public String id() {
        return id;
    }

    public ShopMenuLayout.ItemDecoration iconOverride() {
        return iconOverride;
    }

    public String menuTitleOverride() {
        return menuTitleOverride;
    }

    public Map<String, Map<String, Object>> itemOverrides() {
        return itemOverrides;
    }

    public double weight() {
        return weight;
    }

    private Map<String, Map<String, Object>> toImmutable(Map<String, Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopy(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) nested));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return Collections.unmodifiableMap(copy);
    }
}

