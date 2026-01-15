package com.skyblockexp.ezshops.common;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Utility class for handling compatibility across different server implementations
 * (Paper, Spigot, Bukkit) and Minecraft versions (1.7-1.21).
 * 
 * Provides detection of server capabilities and compatibility wrappers for APIs
 * that differ by platform or version.
 */
public final class CompatibilityUtil {

    private static final String SERVER_VERSION = Bukkit.getServer().getVersion();
    private static final String BUKKIT_VERSION = Bukkit.getServer().getBukkitVersion();
    
    private static Boolean isPaper = null;
    private static Boolean isSpigot = null;
    private static Boolean hasPersistentData = null;
    private static Integer minecraftVersion = null;

    private CompatibilityUtil() {
    }

    /**
     * Detects if the server is running Paper or a Paper fork.
     *
     * @return true if Paper is detected
     */
    public static boolean isPaper() {
        if (isPaper == null) {
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig");
                isPaper = true;
            } catch (ClassNotFoundException e) {
                try {
                    // Check for newer Paper versions
                    Class.forName("io.papermc.paper.configuration.Configuration");
                    isPaper = true;
                } catch (ClassNotFoundException e2) {
                    isPaper = false;
                }
            }
        }
        return isPaper;
    }

    /**
     * Detects if the server is running Spigot or a Spigot-based server (but not Paper).
     *
     * @return true if Spigot is detected (and not Paper)
     */
    public static boolean isSpigot() {
        if (isSpigot == null) {
            if (isPaper()) {
                isSpigot = false;
            } else {
                try {
                    Class.forName("org.spigotmc.SpigotConfig");
                    isSpigot = true;
                } catch (ClassNotFoundException e) {
                    isSpigot = false;
                }
            }
        }
        return isSpigot;
    }

    /**
     * Detects if the server is running plain Bukkit (CraftBukkit) without Spigot or Paper.
     *
     * @return true if plain Bukkit is detected
     */
    public static boolean isBukkit() {
        return !isPaper() && !isSpigot();
    }

    /**
     * Gets the server type as a string for logging/debugging.
     *
     * @return "Paper", "Spigot", or "Bukkit"
     */
    public static String getServerType() {
        if (isPaper()) {
            return "Paper";
        } else if (isSpigot()) {
            return "Spigot";
        } else {
            return "Bukkit";
        }
    }

    /**
     * Gets the Minecraft version as an integer (e.g., 1.17.1 returns 17, 1.8.8 returns 8).
     * This is useful for version-specific compatibility checks.
     *
     * @return the minor version number, or 21 if detection fails
     */
    public static int getMinecraftVersion() {
        if (minecraftVersion == null) {
            try {
                // Parse from Bukkit version string (e.g., "1.21.4-R0.1-SNAPSHOT")
                String version = BUKKIT_VERSION;
                if (version.contains("-")) {
                    version = version.substring(0, version.indexOf("-"));
                }
                String[] parts = version.split("\\.");
                if (parts.length >= 2) {
                    minecraftVersion = Integer.parseInt(parts[1]);
                } else if (parts.length == 1 && parts[0].length() > 0) {
                    // Fallback for unusual version formats
                    try {
                        minecraftVersion = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException e) {
                        minecraftVersion = 21;
                    }
                } else {
                    minecraftVersion = 21; // Default to modern version
                }
            } catch (Exception e) {
                minecraftVersion = 21; // Default to modern version on any parse error
            }
        }
        return minecraftVersion;
    }

    /**
     * Checks if the server supports PersistentDataContainer API.
     * This API was added in 1.14.
     *
     * @return true if PersistentDataContainer is available
     */
    public static boolean hasPersistentDataSupport() {
        if (hasPersistentData == null) {
            try {
                Class.forName("org.bukkit.persistence.PersistentDataContainer");
                hasPersistentData = getMinecraftVersion() >= 14;
            } catch (ClassNotFoundException e) {
                hasPersistentData = false;
            }
        }
        return hasPersistentData;
    }

    /**
     * Safely gets a PersistentDataContainer from a TileState (e.g., Sign).
     * Returns null if the API is not supported on this version.
     *
     * @param tileState the tile state to get the container from
     * @return the PersistentDataContainer, or null if not supported
     */
    public static PersistentDataContainer getPersistentDataContainer(TileState tileState) {
        if (!hasPersistentDataSupport() || tileState == null) {
            return null;
        }
        try {
            return tileState.getPersistentDataContainer();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely gets a PersistentDataContainer from ItemMeta.
     * Returns null if the API is not supported on this version.
     *
     * @param meta the item meta to get the container from
     * @return the PersistentDataContainer, or null if not supported
     */
    public static PersistentDataContainer getPersistentDataContainer(ItemMeta meta) {
        if (!hasPersistentDataSupport() || meta == null) {
            return null;
        }
        try {
            return meta.getPersistentDataContainer();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely checks if a PersistentDataContainer has a key.
     * Returns false if the API is not supported or the container is null.
     *
     * @param container the container to check
     * @param key the key to look for
     * @param type the data type
     * @param <T> the primitive type
     * @param <Z> the complex type
     * @return true if the key exists, false otherwise
     */
    public static <T, Z> boolean hasKey(PersistentDataContainer container, NamespacedKey key, 
                                        PersistentDataType<T, Z> type) {
        if (container == null || key == null || type == null) {
            return false;
        }
        try {
            return container.has(key, type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Safely gets a value from a PersistentDataContainer.
     * Returns null if the API is not supported, the container is null, or the key doesn't exist.
     *
     * @param container the container to get from
     * @param key the key to look for
     * @param type the data type
     * @param <T> the primitive type
     * @param <Z> the complex type
     * @return the value, or null if not found
     */
    public static <T, Z> Z get(PersistentDataContainer container, NamespacedKey key, 
                               PersistentDataType<T, Z> type) {
        if (container == null || key == null || type == null) {
            return null;
        }
        try {
            return container.get(key, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely sets a value in a PersistentDataContainer.
     * Does nothing if the API is not supported or the container is null.
     *
     * @param container the container to set in
     * @param key the key to set
     * @param type the data type
     * @param value the value to set
     * @param <T> the primitive type
     * @param <Z> the complex type
     */
    public static <T, Z> void set(PersistentDataContainer container, NamespacedKey key, 
                                  PersistentDataType<T, Z> type, Z value) {
        if (container == null || key == null || type == null || value == null) {
            return;
        }
        try {
            container.set(key, type, value);
        } catch (Exception e) {
            // Silently fail on older versions
        }
    }

    /**
     * Safely removes a key from a PersistentDataContainer.
     * Does nothing if the API is not supported or the container is null.
     *
     * @param container the container to remove from
     * @param key the key to remove
     */
    public static void remove(PersistentDataContainer container, NamespacedKey key) {
        if (container == null || key == null) {
            return;
        }
        try {
            container.remove(key);
        } catch (Exception e) {
            // Silently fail on older versions
        }
    }

    /**
     * Gets the server version string for debugging.
     *
     * @return the full server version string
     */
    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    /**
     * Gets the Bukkit version string (e.g., "1.21.4-R0.1-SNAPSHOT").
     *
     * @return the Bukkit version string
     */
    public static String getBukkitVersion() {
        return BUKKIT_VERSION;
    }

    /**
     * Creates a NamespacedKey for the given plugin and key string.
     * This is a convenience method for cross-version compatibility.
     *
     * @param plugin the plugin
     * @param key the key string
     * @return the NamespacedKey
     */
    public static NamespacedKey createKey(Plugin plugin, String key) {
        return new NamespacedKey(plugin, key);
    }
}
