package com.skyblockexp.ezshops.config;

import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents the global dynamic pricing configuration loaded from config.yml.
 */
public final class DynamicPricingConfiguration {

    private static final double DEFAULT_STARTING_MULTIPLIER = 1.0D;
    private static final double DEFAULT_MIN_MULTIPLIER = 0.5D;
    private static final double DEFAULT_MAX_MULTIPLIER = 3.0D;
    private static final double DEFAULT_BUY_CHANGE = 0.0D;
    private static final double DEFAULT_SELL_CHANGE = 0.0D;

    private static final DynamicPricingConfiguration DISABLED =
            new DynamicPricingConfiguration(false, DEFAULT_STARTING_MULTIPLIER, DEFAULT_MIN_MULTIPLIER,
                    DEFAULT_MAX_MULTIPLIER, DEFAULT_BUY_CHANGE, DEFAULT_SELL_CHANGE);

    private static final DynamicPricingConfiguration DEFAULTS =
            new DynamicPricingConfiguration(true, DEFAULT_STARTING_MULTIPLIER, DEFAULT_MIN_MULTIPLIER,
                    DEFAULT_MAX_MULTIPLIER, DEFAULT_BUY_CHANGE, DEFAULT_SELL_CHANGE);

    private final boolean enabled;
    private final double defaultStartingMultiplier;
    private final double defaultMinMultiplier;
    private final double defaultMaxMultiplier;
    private final double defaultBuyChange;
    private final double defaultSellChange;

    private DynamicPricingConfiguration(boolean enabled, double defaultStartingMultiplier, double defaultMinMultiplier,
            double defaultMaxMultiplier, double defaultBuyChange, double defaultSellChange) {
        this.enabled = enabled;
        this.defaultStartingMultiplier = defaultStartingMultiplier;
        this.defaultMinMultiplier = defaultMinMultiplier;
        this.defaultMaxMultiplier = defaultMaxMultiplier;
        this.defaultBuyChange = defaultBuyChange;
        this.defaultSellChange = defaultSellChange;
    }

    public boolean enabled() {
        return enabled;
    }

    public double defaultStartingMultiplier() {
        return defaultStartingMultiplier;
    }

    public double defaultMinMultiplier() {
        return defaultMinMultiplier;
    }

    public double defaultMaxMultiplier() {
        return defaultMaxMultiplier;
    }

    public double defaultBuyChange() {
        return defaultBuyChange;
    }

    public double defaultSellChange() {
        return defaultSellChange;
    }

    public static DynamicPricingConfiguration disabled() {
        return DISABLED;
    }

    public static DynamicPricingConfiguration defaults() {
        return DEFAULTS;
    }

    public static DynamicPricingConfiguration from(ConfigurationSection root, Logger logger) {
        if (root == null) {
            return DEFAULTS;
        }
        ConfigurationSection section = root.getConfigurationSection("dynamic-pricing");
        if (section == null) {
            return DEFAULTS;
        }

        boolean enabled = section.getBoolean("enabled", true);
        if (!enabled) {
            return DISABLED;
        }

        ConfigurationSection defaultsSection = section.getConfigurationSection("defaults");

        double startingMultiplier = readDouble(defaultsSection, "starting-multiplier", DEFAULT_STARTING_MULTIPLIER,
                logger);
        double minMultiplier = readDouble(defaultsSection, "min-multiplier", DEFAULT_MIN_MULTIPLIER, logger);
        double maxMultiplier = readDouble(defaultsSection, "max-multiplier", DEFAULT_MAX_MULTIPLIER, logger);
        double buyChange = readDouble(defaultsSection, "buy-change", DEFAULT_BUY_CHANGE, logger);
        double sellChange = readDouble(defaultsSection, "sell-change", DEFAULT_SELL_CHANGE, logger);

        startingMultiplier = sanitizePositive(startingMultiplier, DEFAULT_STARTING_MULTIPLIER, logger,
                "starting-multiplier");
        minMultiplier = sanitizePositive(minMultiplier, DEFAULT_MIN_MULTIPLIER, logger, "min-multiplier");
        maxMultiplier = sanitizePositive(maxMultiplier, DEFAULT_MAX_MULTIPLIER, logger, "max-multiplier");
        buyChange = sanitizeNonNegative(buyChange, DEFAULT_BUY_CHANGE, logger, "buy-change");
        sellChange = sanitizeNonNegative(sellChange, DEFAULT_SELL_CHANGE, logger, "sell-change");

        if (maxMultiplier < minMultiplier) {
            double previousMax = maxMultiplier;
            maxMultiplier = minMultiplier;
            minMultiplier = previousMax;
            logger.warning("dynamic-pricing.defaults.max-multiplier was lower than min-multiplier; the values have been"
                    + " swapped.");
        }

        return new DynamicPricingConfiguration(true, startingMultiplier, minMultiplier, maxMultiplier, buyChange,
                sellChange);
    }

    private static double readDouble(ConfigurationSection section, String path, double fallback, Logger logger) {
        if (section == null || !section.contains(path)) {
            return fallback;
        }
        Object value = section.get(path);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue.trim());
            } catch (NumberFormatException ex) {
                // handled below
            }
        }
        logger.warning("Invalid value for dynamic-pricing.defaults." + path + "; using " + fallback + '.');
        return fallback;
    }

    private static double sanitizePositive(double value, double fallback, Logger logger, String path) {
        if (Double.isNaN(value) || value <= 0.0D) {
            logger.warning("Invalid value for dynamic-pricing.defaults." + path + "; using " + fallback + '.');
            return fallback;
        }
        return value;
    }

    private static double sanitizeNonNegative(double value, double fallback, Logger logger, String path) {
        if (Double.isNaN(value) || value < 0.0D) {
            logger.warning("Invalid value for dynamic-pricing.defaults." + path + "; using " + fallback + '.');
            return fallback;
        }
        return value;
    }
}
