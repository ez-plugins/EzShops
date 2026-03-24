package com.skyblockexp.ezshops.shop.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates the icon definition for a `ShopItem`.
 * Provides a builder and conversion to a serializable map.
 */
public final class ShopIcon {
    private final String material;
    private final Integer amount;
    private final String displayName;
    private final List<String> lore;
    private final String serializedItem; // optional base64 itemstack

    private ShopIcon(Builder b) {
        this.material = b.material;
        this.amount = b.amount;
        this.displayName = b.displayName;
        this.lore = b.lore == null ? List.of() : List.copyOf(b.lore);
        this.serializedItem = b.serializedItem;
    }

    public String material() { return material; }
    public Integer amount() { return amount; }
    public String displayName() { return displayName; }
    public List<String> lore() { return lore; }
    public String serializedItem() { return serializedItem; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (material != null) m.put("material", material);
        if (amount != null) m.put("amount", amount);
        if (displayName != null) m.put("display-name", displayName);
        if (!lore.isEmpty()) m.put("lore", new ArrayList<>(lore));
        if (serializedItem != null) m.put("itemstack-base64", serializedItem);
        return m;
    }

    public static Builder builder() { return new Builder(); }

    public static ShopIcon fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        Builder b = new Builder();
        Object mat = map.get("material");
        if (mat != null) b.material(mat.toString());
        Object amt = map.get("amount");
        if (amt instanceof Number) b.amount(((Number) amt).intValue());
        Object dn = map.get("display-name");
        if (dn != null) b.displayName(dn.toString());
        Object lore = map.get("lore");
        if (lore instanceof List) b.lore((List<String>) lore);
        Object item = map.get("itemstack-base64");
        if (item == null) item = map.get("item");
        if (item != null) b.serializedItem(item.toString());
        return b.build();
    }

    public static final class Builder {
        private String material;
        private Integer amount;
        private String displayName;
        private List<String> lore;
        private String serializedItem;

        public Builder material(String material) { this.material = material; return this; }
        public Builder amount(int amount) { this.amount = amount; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder lore(List<String> lore) { this.lore = lore == null ? null : List.copyOf(lore); return this; }
        public Builder serializedItem(String base64) { this.serializedItem = base64; return this; }

        public ShopIcon build() { return new ShopIcon(this); }
    }
}
