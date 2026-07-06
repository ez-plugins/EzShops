package com.skyblockexp.ezshops.shop.pricing.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and merges shop configuration files from configured plugin data locations.
 */
public final class ShopConfigSourceLoader {

    private final JavaPlugin plugin;
    private final Logger logger;

    public ShopConfigSourceLoader(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void ensureDataFolder() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Unable to create plugin data folder for dynamic shop pricing state.");
        }
    }

    public YamlConfiguration loadCombinedConfiguration() {
        File dataFolder = plugin.getDataFolder();
        YamlConfiguration combined = new YamlConfiguration();

        File primaryFile = new File(dataFolder, "shop.yml");
        boolean foundConfig = false;
        if (primaryFile.exists()) {
            mergeSections(combined, YamlConfiguration.loadConfiguration(primaryFile));
            foundConfig = true;
        }

        String gameMode = plugin.getConfig().getString("game-mode", "prison");
        File modeSpecificDir = new File(dataFolder, "shop/" + gameMode);
        if (mergeDirectory(combined, modeSpecificDir)) {
            foundConfig = true;
        }

        File legacyDir = new File(dataFolder, "shop");
        if (!foundConfig && mergeDirectory(combined, legacyDir)) {
            foundConfig = true;
        }

        if (!foundConfig) {
            logger.warning("Shop pricing file not found: " + primaryFile.getName());
            return null;
        }

        return combined;
    }

    public String buildSourceInfo() {
        File dataFolder = plugin.getDataFolder();
        String gameMode = plugin.getConfig().getString("game-mode", "prison");
        StringBuilder info = new StringBuilder();
        info.append("EzShops (").append(gameMode).append(" mode: ");

        List<String> sources = new ArrayList<>();
        File modeDir = new File(dataFolder, "shop/" + gameMode);
        if (modeDir.exists() && modeDir.isDirectory()) {
            sources.add("shop/" + gameMode + "/");
        }
        File legacyDir = new File(dataFolder, "shop");
        if (legacyDir.exists() && legacyDir.isDirectory()) {
            if (sources.isEmpty() || !legacyDir.equals(modeDir)) {
                sources.add("shop/");
            }
        }
        File shopYml = new File(dataFolder, "shop.yml");
        if (shopYml.exists()) {
            sources.add("shop.yml");
        }

        if (sources.isEmpty()) {
            info.append("default config)");
        } else {
            info.append(String.join(", ", sources)).append(")");
        }
        return info.toString();
    }

    private boolean mergeDirectory(ConfigurationSection target, File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }

        Arrays.sort(files, Comparator.comparing(File::getName, String::compareToIgnoreCase));

        boolean merged = false;
        for (File file : files) {
            if (file.isDirectory()) {
                merged |= mergeDirectory(target, file);
                continue;
            }

            if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }

            mergeSections(target, YamlConfiguration.loadConfiguration(file));
            merged = true;
        }
        return merged;
    }

    private void mergeSections(ConfigurationSection target, ConfigurationSection source) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection section) {
                ConfigurationSection child = target.getConfigurationSection(key);
                if (child == null) {
                    child = target.createSection(key);
                }
                mergeSections(child, section);
            } else {
                target.set(key, value);
            }
        }
    }
}
