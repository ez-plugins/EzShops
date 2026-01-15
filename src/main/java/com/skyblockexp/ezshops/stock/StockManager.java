package com.skyblockexp.ezshops.stock;

import org.bukkit.entity.Player;

public class StockManager {
    public static boolean addPlayerStock(Player player, String productId, int amount) {
        if (amount <= 0) return false;
        String id = productId == null ? null : productId.toUpperCase(java.util.Locale.ROOT);
        java.io.File dataFolder = player.getServer().getPluginManager().getPlugin("EzShops").getDataFolder();
        java.io.File playerFile = new java.io.File(dataFolder, "player-stocks/" + player.getUniqueId() + ".yml");
        if (!playerFile.getParentFile().exists()) playerFile.getParentFile().mkdirs();
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playerFile);
        int current = yaml.getInt(id, 0);
        yaml.set(id, current + amount);
        try {
            yaml.save(playerFile);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean removePlayerStock(Player player, String productId, int amount) {
        String id = productId == null ? null : productId.toUpperCase(java.util.Locale.ROOT);
        java.io.File dataFolder = player.getServer().getPluginManager().getPlugin("EzShops").getDataFolder();
        java.io.File playerFile = new java.io.File(dataFolder, "player-stocks/" + player.getUniqueId() + ".yml");
        if (!playerFile.getParentFile().exists()) playerFile.getParentFile().mkdirs();
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playerFile);
        int current = yaml.getInt(id, 0);
        if (current < amount) {
            return false;
        }
        int newAmount = current - amount;
        if (newAmount > 0) {
            yaml.set(id, newAmount);
        } else {
            yaml.set(id, null);
        }
        try {
            yaml.save(playerFile);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static int getPlayerStockAmount(Player player, String productId) {
        String id = productId == null ? null : productId.toUpperCase(java.util.Locale.ROOT);
        java.io.File dataFolder = player.getServer().getPluginManager().getPlugin("EzShops").getDataFolder();
        java.io.File playerFile = new java.io.File(dataFolder, "player-stocks/" + player.getUniqueId() + ".yml");
        if (!playerFile.exists()) return 0;
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playerFile);
        return yaml.getInt(id, 0);
    }

    public static java.util.List<String> getPlayerOwnedStocks(Player player) {
        java.io.File dataFolder = player.getServer().getPluginManager().getPlugin("EzShops").getDataFolder();
        java.io.File playerFile = new java.io.File(dataFolder, "player-stocks/" + player.getUniqueId() + ".yml");
        if (!playerFile.exists()) return java.util.Collections.emptyList();
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playerFile);
        java.util.List<String> ownedStocks = new java.util.ArrayList<>();
        for (String key : yaml.getKeys(false)) {
            int amount = yaml.getInt(key, 0);
            if (amount > 0) {
                ownedStocks.add(key);
            }
        }
        return ownedStocks;
    }
}
