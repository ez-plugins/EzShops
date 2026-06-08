package com.skyblockexp.ezshops.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralised database configuration loader.
 *
 * Reads a top-level `database` section if present, otherwise falls back to
 * existing `player-shops.storage.jaloquent` or `player-shops.storage.mysql`
 * sections. Always provides sensible defaults for missing values.
 */
public final class DatabaseConfig {

    private DatabaseConfig() {}

    public static Map<String, String> from(FileConfiguration cfg) {
        Map<String, String> map = new HashMap<>();

        ConfigurationSection db = cfg.getConfigurationSection("database");
        if (db != null) {
            copySection(db, map);
        } else {
            // fallback to player-shops.storage.*
            ConfigurationSection ps = cfg.getConfigurationSection("player-shops.storage");
            if (ps != null) {
                // prefer an explicit jaloquent block, else mysql
                ConfigurationSection jalo = ps.getConfigurationSection("jaloquent");
                ConfigurationSection mysql = ps.getConfigurationSection("mysql");
                ConfigurationSection chosen = jalo != null ? jalo : mysql;
                if (chosen != null) copySection(chosen, map);
                // also allow table-prefix at the player-shops.storage level
                if (ps.isString("table-prefix")) map.put("table-prefix", ps.getString("table-prefix"));
            }
        }

        // ensure defaults
        map.putIfAbsent("table-prefix", cfg.getString("player-shops.storage.table-prefix", "ez_"));
        return map;
    }

    private static void copySection(ConfigurationSection sec, Map<String, String> map) {
        if (sec.isString("url")) map.put("url", sec.getString("url"));
        if (sec.isString("host")) map.put("host", sec.getString("host"));
        if (sec.isInt("port")) map.put("port", Integer.toString(sec.getInt("port")));
        if (sec.isString("database")) map.put("database", sec.getString("database"));
        if (sec.isString("username")) map.put("username", sec.getString("username"));
        if (sec.isString("password")) map.put("password", sec.getString("password"));
        if (sec.isString("table-prefix")) map.put("table-prefix", sec.getString("table-prefix"));
    }
}
