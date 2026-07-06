package com.skyblockexp.ezshops.shop.pricing.layout;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

/**
 * Shared primitive and enum parsers used by pricing item parsing.
 */
public final class ShopPricingValueParsers {

    private final Logger logger;

    public ShopPricingValueParsers(Logger logger) {
        this.logger = logger;
    }

    public double readPrice(ConfigurationSection section, String materialKey, String path) {
        if (!section.contains(path)) {
            return Double.NaN;
        }

        Object value = section.get(path);
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue.trim());
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }

        logger.warning("Ignoring invalid " + path + " price for material '" + materialKey
                + "': expected a numeric value.");
        return Double.NaN;
    }

    public Map<Enchantment, Integer> parseEnchantments(String context, ConfigurationSection section) {
        if (section == null) {
            logger.warning("Ignoring item '" + context + "' because it does not define any enchantments.");
            return Map.of();
        }

        Map<Enchantment, Integer> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (key == null) {
                continue;
            }
            Enchantment enchantment = parseEnchantment(key);
            if (enchantment == null) {
                logger.warning("Ignoring enchantment '" + key + "' for item '" + context
                        + "' because it is not recognized.");
                continue;
            }
            int level = Math.max(1, section.getInt(key, 0));
            if (level <= 0) {
                logger.warning("Ignoring enchantment '" + key + "' for item '" + context
                        + "' because the configured level is not positive.");
                continue;
            }
            values.put(enchantment, Math.min(level, enchantment.getMaxLevel()));
        }

        return values;
    }

    public Enchantment parseEnchantment(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        String trimmed = key.trim();
        NamespacedKey namespacedKey;
        Enchantment enchantment;
        try {
            namespacedKey = NamespacedKey.fromString(trimmed);
        } catch (IllegalArgumentException ex) {
            namespacedKey = null;
        }
        if (namespacedKey != null) {
            enchantment = Enchantment.getByKey(namespacedKey);
            if (enchantment != null) {
                return enchantment;
            }
        }

        if (!trimmed.contains(":")) {
            enchantment = Enchantment.getByKey(NamespacedKey.minecraft(trimmed.toLowerCase(Locale.ROOT)));
            if (enchantment != null) {
                return enchantment;
            }
        }

        return Enchantment.getByName(trimmed.toUpperCase(Locale.ROOT));
    }

    public EntityType parseEntityType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return EntityType.fromName(value.trim());
        }
    }
}
