package com.skyblockexp.ezshops.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigTest {

    @Test
    void readsTopLevelDatabaseSection() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("database.host", "db-host");
        cfg.set("database.port", 4406);
        cfg.set("database.database", "shops");
        cfg.set("database.username", "user");
        cfg.set("database.password", "pass");
        cfg.set("database.table-prefix", "ps_");

        Map<String, String> map = DatabaseConfig.from(cfg);
        assertEquals("db-host", map.get("host"));
        assertEquals("4406", map.get("port"));
        assertEquals("shops", map.get("database"));
        assertEquals("user", map.get("username"));
        assertEquals("pass", map.get("password"));
        assertEquals("ps_", map.get("table-prefix"));
    }

    @Test
    void fallsBackToPlayerShopStorageJaloquentAndStoragePrefix() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("player-shops.storage.table-prefix", "root_");
        cfg.set("player-shops.storage.jaloquent.host", "jalo-host");
        cfg.set("player-shops.storage.jaloquent.port", 3307);
        cfg.set("player-shops.storage.jaloquent.database", "jalo-db");
        cfg.set("player-shops.storage.jaloquent.username", "jalo-user");

        Map<String, String> map = DatabaseConfig.from(cfg);
        assertEquals("jalo-host", map.get("host"));
        assertEquals("3307", map.get("port"));
        assertEquals("jalo-db", map.get("database"));
        assertEquals("jalo-user", map.get("username"));
        assertEquals("root_", map.get("table-prefix"));
    }

    @Test
    void fallsBackToMysqlWhenJaloquentMissing() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("player-shops.storage.mysql.host", "mysql-host");
        cfg.set("player-shops.storage.mysql.port", 3308);

        Map<String, String> map = DatabaseConfig.from(cfg);
        assertEquals("mysql-host", map.get("host"));
        assertEquals("3308", map.get("port"));
    }

    @Test
    void suppliesDefaultTablePrefixWhenMissing() {
        YamlConfiguration cfg = new YamlConfiguration();
        Map<String, String> map = DatabaseConfig.from(cfg);
        assertEquals("ez_", map.get("table-prefix"));
    }
}

