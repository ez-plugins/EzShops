package com.skyblockexp.ezshops.shop.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single shop item entry used in categories. Provides a
 * convenient builder and conversion to a serializable map for YAML output.
 */
public final class ShopItem {
    private final String id;
    private final String material;
    private final Integer slot;
    private final Integer amount;
    private final Integer bulkAmount;
    private final Integer iconAmount;
    private final String displayName;
    private final List<String> lore;
    private final Double buy;
    private final Double sell;
    private final String priceId;
    private final ShopIcon icon; // icon description
    private final List<String> onBuyCommands;
    private final Map<String, Object> extra;

    private ShopItem(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.material = b.material;
        this.slot = b.slot;
        this.amount = b.amount;
        this.bulkAmount = b.bulkAmount;
        this.iconAmount = b.iconAmount;
        this.displayName = b.displayName;
        this.lore = b.lore == null ? List.of() : List.copyOf(b.lore);
        this.buy = b.buy;
        this.sell = b.sell;
        this.priceId = b.priceId;
        this.icon = b.icon;
        this.onBuyCommands = b.onBuyCommands == null ? List.of() : List.copyOf(b.onBuyCommands);
        this.extra = b.extra == null ? Map.of() : Map.copyOf(b.extra);
    }

    public String id() { return id; }
    public String material() { return material; }

    /**
     * Convert this ShopItem into a Map suitable for YAML serialization in category files.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (material != null) m.put("material", material);
        if (slot != null) m.put("slot", slot);
        if (amount != null) m.put("amount", amount);
        if (bulkAmount != null) m.put("bulk-amount", bulkAmount);
        if (iconAmount != null) m.put("icon-amount", iconAmount);
        if (displayName != null) m.put("display-name", displayName);
        if (!lore.isEmpty()) m.put("lore", new ArrayList<>(lore));
        if (buy != null) m.put("buy", buy);
        if (sell != null) m.put("sell", sell);
        if (priceId != null) m.put("price-id", priceId);
        if (icon != null) {
            Map<String, Object> im = icon.toMap();
            if (!im.isEmpty()) m.put("icon", im);
        }
        if (!onBuyCommands.isEmpty()) m.put("on-buy", Map.of("execute-as", "console", "commands", new ArrayList<>(onBuyCommands)));
        if (!extra.isEmpty()) m.putAll(extra);
        return m;
    }

    public static Builder builder(String id) { return new Builder(id); }

    public static final class Builder {
        private final String id;
        private String material;
        private Integer slot;
        private Integer amount;
        private Integer bulkAmount;
        private Integer iconAmount;
        private String displayName;
        private List<String> lore;
        private Double buy;
        private Double sell;
        private String priceId;
        private ShopIcon icon;
        private List<String> onBuyCommands;
        private Map<String, Object> extra;

        public Builder(String id) { this.id = id; }

        public Builder material(String material) { this.material = material; return this; }
        public Builder slot(int slot) { this.slot = slot; return this; }
        public Builder amount(int amount) { this.amount = amount; return this; }
        public Builder bulkAmount(int bulkAmount) { this.bulkAmount = bulkAmount; return this; }
        public Builder iconAmount(int iconAmount) { this.iconAmount = iconAmount; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder lore(List<String> lore) { this.lore = lore == null ? null : List.copyOf(lore); return this; }
        public Builder buy(double buy) { this.buy = buy; return this; }
        public Builder sell(double sell) { this.sell = sell; return this; }
        public Builder priceId(String priceId) { this.priceId = priceId; return this; }
        public Builder icon(ShopIcon icon) { this.icon = icon; return this; }
        /**
         * Backwards-compatible setter accepting a raw icon map.
         */
        public Builder iconFromMap(Map<String, Object> icon) { this.icon = icon == null ? null : ShopIcon.fromMap(icon); return this; }
        public Builder onBuyCommands(List<String> commands) { this.onBuyCommands = commands == null ? null : List.copyOf(commands); return this; }
        public Builder extra(Map<String, Object> extra) { this.extra = extra == null ? null : Map.copyOf(extra); return this; }

        public ShopItem build() { return new ShopItem(this); }
    }
}
