package com.skyblockexp.ezshops.hook;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Reflection wrapper for PlaceholderAPI to avoid a hard dependency.
 */
public final class PlaceholderApiAdapter {
    private final Plugin plugin;
    private final boolean available;
    private final Method setPlaceholdersMethod;

    public PlaceholderApiAdapter(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Method method = null;
        boolean ok = false;
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            method = papiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            ok = true;
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.FINE, "PlaceholderAPI not available or API changed: " + ex.getMessage());
        }
        this.setPlaceholdersMethod = method;
        this.available = ok && method != null;
    }

    public boolean isAvailable() {
        return available;
    }

    public String apply(Player player, String input) {
        if (!available || input == null) return input;
        try {
            Object out = setPlaceholdersMethod.invoke(null, player, input);
            if (out instanceof String) return (String) out;
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.WARNING, "PlaceholderAPI substitution failed: " + ex.getMessage(), ex);
        }
        return input;
    }
}
