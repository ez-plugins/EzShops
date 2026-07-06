package com.skyblockexp.ezshops.shop.pricing.layout;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Utility methods for reading and merging nested item template data sections.
 */
public final class ShopItemDataMapper {

    private ShopItemDataMapper() {}

    public static Map<String, Map<String, Object>> readItemData(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) {
                if (logger != null) {
                    logger.warning("Ignoring item template '" + key + "' because it is not a section.");
                }
                continue;
            }
            values.put(key, deepCopyItemData(child));
        }
        return values;
    }

    public static Map<String, Map<String, Object>> mergeItemMaps(Map<String, Map<String, Object>> defaults,
            Map<String, Map<String, Object>> overrides) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        if (defaults != null) {
            for (Map.Entry<String, Map<String, Object>> entry : defaults.entrySet()) {
                merged.put(entry.getKey(), deepCopyItemData(entry.getValue()));
            }
        }
        if (overrides != null) {
            for (Map.Entry<String, Map<String, Object>> entry : overrides.entrySet()) {
                Map<String, Object> overrideCopy = deepCopyItemData(entry.getValue());
                Map<String, Object> existing = merged.get(entry.getKey());
                if (existing == null) {
                    merged.put(entry.getKey(), overrideCopy);
                } else {
                    applyOverrides(existing, overrideCopy);
                }
            }
        }
        return merged;
    }

    public static ConfigurationSection createSectionFromMap(Map<String, Object> values) {
        YamlConfiguration configuration = new YamlConfiguration();
        populateSection(configuration, values);
        return configuration;
    }

    private static Map<String, Object> deepCopyItemData(ConfigurationSection section) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                copy.put(key, deepCopyItemData(child));
            } else {
                copy.put(key, value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopyItemData(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopyItemData((Map<String, Object>) nested));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static void applyOverrides(Map<String, Object> target, Map<String, Object> overrides) {
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                Object existing = target.get(entry.getKey());
                if (existing instanceof Map<?, ?> existingMap) {
                    applyOverrides((Map<String, Object>) existingMap, (Map<String, Object>) nested);
                } else {
                    target.put(entry.getKey(), deepCopyItemData((Map<String, Object>) nested));
                }
            } else {
                target.put(entry.getKey(), value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void populateSection(ConfigurationSection target, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                ConfigurationSection child = target.createSection(entry.getKey());
                populateSection(child, (Map<String, Object>) nested);
            } else {
                target.set(entry.getKey(), value);
            }
        }
    }
}
