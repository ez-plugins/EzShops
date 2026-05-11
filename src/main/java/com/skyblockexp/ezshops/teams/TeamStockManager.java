package com.skyblockexp.ezshops.teams;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * YAML-backed shared stock pool per team UUID.
 * Files are stored at {@code data/team-stocks/<teamId>.yml}.
 * Mirrors the structure of {@link com.skyblockexp.ezshops.stock.StockManager}.
 */
public final class TeamStockManager {

    private final File dataDir;

    public TeamStockManager(File pluginDataFolder) {
        this.dataDir = new File(pluginDataFolder, "team-stocks");
        if (!this.dataDir.exists()) {
            this.dataDir.mkdirs();
        }
    }

    private File fileFor(UUID teamId) {
        return new File(dataDir, teamId.toString() + ".yml");
    }

    private String key(String productId) {
        return productId == null ? null : productId.toUpperCase(java.util.Locale.ROOT);
    }

    public boolean addTeamStock(UUID teamId, String productId, int amount) {
        if (amount <= 0) return false;
        File f = fileFor(teamId);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
        int current = yaml.getInt(key(productId), 0);
        yaml.set(key(productId), current + amount);
        return save(f, yaml);
    }

    public boolean removeTeamStock(UUID teamId, String productId, int amount) {
        File f = fileFor(teamId);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
        int current = yaml.getInt(key(productId), 0);
        if (current < amount) return false;
        int newAmt = current - amount;
        if (newAmt > 0) {
            yaml.set(key(productId), newAmt);
        } else {
            yaml.set(key(productId), null);
        }
        return save(f, yaml);
    }

    public int getTeamStockAmount(UUID teamId, String productId) {
        File f = fileFor(teamId);
        if (!f.exists()) return 0;
        return YamlConfiguration.loadConfiguration(f).getInt(key(productId), 0);
    }

    public List<String> getTeamOwnedStocks(UUID teamId) {
        File f = fileFor(teamId);
        if (!f.exists()) return Collections.emptyList();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
        List<String> result = new ArrayList<>();
        for (String k : yaml.getKeys(false)) {
            if (yaml.getInt(k, 0) > 0) result.add(k);
        }
        return result;
    }

    /** Delete all stock data for a team (called on TeamDeleteEvent). */
    public void deleteTeamData(UUID teamId) {
        File f = fileFor(teamId);
        if (f.exists()) f.delete();
    }

    private static boolean save(File f, YamlConfiguration yaml) {
        try {
            if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
            yaml.save(f);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
