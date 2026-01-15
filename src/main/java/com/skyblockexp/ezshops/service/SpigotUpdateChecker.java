package com.skyblockexp.ezshops.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpigotUpdateChecker {
    private static final String UPDATE_ENDPOINT = "https://api.spigotmc.org/legacy/update.php?resource=";
    private final JavaPlugin plugin;
    private final int resourceId;

    public SpigotUpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void checkForUpdates() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String latestVersion = fetchLatestVersion();
                if (latestVersion == null || latestVersion.isBlank()) {
                    plugin.getLogger().warning("SpigotMC update check returned an empty response.");
                    return;
                }
                String currentVersion = plugin.getDescription().getVersion();
                if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                    plugin.getLogger().info("A new EzShops version is available: " + latestVersion
                            + " (current: " + currentVersion + "). Download: https://www.spigotmc.org/resources/"
                            + resourceId + "/");
                } else {
                    plugin.getLogger().info("EzShops is up to date.");
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to check for EzShops updates: " + ex.getMessage());
            }
        });
    }

    private String fetchLatestVersion() throws Exception {
        URL url = new URL(UPDATE_ENDPOINT + resourceId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "EzShops Update Checker");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } finally {
            connection.disconnect();
        }
    }
}
