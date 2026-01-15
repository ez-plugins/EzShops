package com.skyblockexp.ezshops.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

public class StockMarketConfig {
    private final Map<String, String> categories;
    private final Set<String> blocked;
    private final Map<String, OverrideItem> overrides;

    public StockMarketConfig(FileConfiguration config) {
        this.blocked = new HashSet<>();
        this.overrides = new HashMap<>();
        this.categories = new HashMap<>();
        ConfigurationSection stockSection = config.getConfigurationSection("stock");
        if (stockSection != null) {
            List<String> blockedList = stockSection.getStringList("blocked");
            if (blockedList != null) {
                for (String s : blockedList) blocked.add(s.toUpperCase(Locale.ROOT));
            }
            List<?> overrideList = stockSection.getList("overrides");
            if (overrideList != null) {
                for (Object obj : overrideList) {
                    if (obj instanceof Map<?,?> map) {
                        String id = Objects.toString(map.get("id"), null);
                        String display = Objects.toString(map.get("display"), null);
                        Double basePrice = map.get("base-price") instanceof Number n ? n.doubleValue() : null;
                        if (id != null && display != null && basePrice != null) {
                            overrides.put(id.toUpperCase(Locale.ROOT), new OverrideItem(id, display, basePrice));
                        }
                    }
                }
            }
            ConfigurationSection categoriesSection = stockSection.getConfigurationSection("categories");
            if (categoriesSection != null) {
                for (String category : categoriesSection.getKeys(false)) {
                    List<String> items = categoriesSection.getStringList(category);
                    for (String item : items) {
                        categories.put(item.toUpperCase(Locale.ROOT), category);
                    }
                }
            }
        }
    }

    public boolean isBlocked(String id) {
        return blocked.contains(id.toUpperCase(Locale.ROOT));
    }

    public OverrideItem getOverride(String id) {
        return overrides.get(id.toUpperCase(Locale.ROOT));
    }

    public Collection<OverrideItem> getAllOverrides() {
        return overrides.values();
    }

    public String getCategory(String productId) {
        if (productId == null) return null;
        return categories.get(productId.toUpperCase(Locale.ROOT));
    }

    public static class OverrideItem {
        public final String id;
        public final String display;
        public final double basePrice;
        public OverrideItem(String id, String display, double basePrice) {
            this.id = id;
            this.display = display;
            this.basePrice = basePrice;
        }
    }
}
