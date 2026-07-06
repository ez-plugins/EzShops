package com.skyblockexp.ezshops.shop.pricing.state;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Encapsulates persisted dynamic pricing and rotation state in shop-dynamic.yml.
 */
public final class ShopDynamicStateStore {

    private final File stateFile;
    private final Logger logger;
    private YamlConfiguration configuration = new YamlConfiguration();

    public ShopDynamicStateStore(File stateFile, Logger logger) {
        this.stateFile = stateFile;
        this.logger = logger;
    }

    public void load() {
        if (stateFile.exists()) {
            configuration = YamlConfiguration.loadConfiguration(stateFile);
        } else {
            configuration = new YamlConfiguration();
        }
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return configuration.getConfigurationSection(path);
    }

    public Set<String> rootKeys() {
        return new LinkedHashSet<>(configuration.getKeys(false));
    }

    public boolean isSet(String path) {
        return configuration.isSet(path);
    }

    public double getDouble(String path, double defaultValue) {
        return configuration.getDouble(path, defaultValue);
    }

    public void set(String path, Object value) {
        configuration.set(path, value);
    }

    public boolean removeSavedEntry(String path) {
        if (!configuration.isSet(path)) {
            return false;
        }
        configuration.set(path, null);
        return save("Failed to save dynamic shop pricing data");
    }

    public void saveMultiplier(String priceKey, double multiplier) {
        configuration.set(priceKey, multiplier);
        save("Failed to save dynamic shop pricing data");
    }

    public void saveRotationOption(String rotationId, String optionId) {
        ConfigurationSection rotationSection = configuration.getConfigurationSection("rotations");
        if (rotationSection == null) {
            rotationSection = configuration.createSection("rotations");
        }
        rotationSection.set(rotationId, optionId);
        save("Failed to save rotation state");
    }

    public void cleanup(Predicate<String> dynamicKeyIsValid,
            BiPredicate<String, String> rotationOptionIsValid) {
        boolean dirty = false;

        for (String key : new ArrayList<>(configuration.getKeys(false))) {
            if ("rotations".equalsIgnoreCase(key)) {
                continue;
            }
            if (!dynamicKeyIsValid.test(key)) {
                configuration.set(key, null);
                dirty = true;
            }
        }

        ConfigurationSection rotationSection = configuration.getConfigurationSection("rotations");
        if (rotationSection != null) {
            for (String key : new ArrayList<>(rotationSection.getKeys(false))) {
                String optionId = rotationSection.getString(key);
                if (!rotationOptionIsValid.test(key, optionId)) {
                    rotationSection.set(key, null);
                    dirty = true;
                }
            }
            if (rotationSection.getKeys(false).isEmpty()) {
                configuration.set("rotations", null);
                dirty = true;
            }
        }

        if (dirty) {
            save("Failed to clean up dynamic shop pricing data");
        }
    }

    public int clearAllDynamicEntries() {
        int count = 0;
        for (String key : new ArrayList<>(configuration.getKeys(false))) {
            if ("rotations".equalsIgnoreCase(key)) {
                continue;
            }
            if (configuration.isSet(key)) {
                configuration.set(key, null);
                count++;
            }
        }
        return count;
    }

    public void flush() {
        save("Failed to save dynamic shop pricing data");
    }

    private boolean save(String messagePrefix) {
        try {
            configuration.save(stateFile);
            return true;
        } catch (IOException ex) {
            logger.warning(messagePrefix + ": " + ex.getMessage());
            return false;
        }
    }
}
