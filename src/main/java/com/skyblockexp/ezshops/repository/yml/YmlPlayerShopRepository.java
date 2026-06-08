package com.skyblockexp.ezshops.repository.yml;

import com.skyblockexp.ezshops.repository.PlayerShopRepository;
import com.skyblockexp.ezshops.playershop.PlayerShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * YML-based implementation of PlayerShopRepository.
 * Handles persistence of player shops to player-shops.yml.
 *
 * <p>Saves are written to a temporary file first and then atomically renamed
 * over the real file to prevent data corruption on unexpected shutdowns.
 * Deferred entries (shops whose worlds were not loaded at startup) are
 * merged back on every save automatically.
 */
public class YmlPlayerShopRepository implements PlayerShopRepository {

    private static final int DATA_VERSION = 1;

    private final File dataFile;
    private final File tempFile;
    private final Logger logger;
    /** Raw YAML entries for shops whose worlds were not loaded; preserved across save cycles. */
    private final Map<String, Map<String, Object>> deferredEntries;

    public YmlPlayerShopRepository(File dataFolder, Logger logger) {
        this.dataFile = new File(dataFolder, "player-shops.yml");
        this.tempFile = new File(dataFolder, "player-shops.tmp");
        this.logger = logger;
        this.deferredEntries = new HashMap<>();
    }

    @Override
    public Collection<PlayerShop> loadShops() {
        deferredEntries.clear();
        List<PlayerShop> shops = new ArrayList<>();

        try {
            ensureDataFile();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to prepare player shop data file", ex);
            return shops;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = configuration.getConfigurationSection("shops");
        if (section == null) {
            return shops;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection shopSection = section.getConfigurationSection(key);
            if (shopSection == null) {
                continue;
            }

            Location signLocation = parseLocation(key);
            if (signLocation == null) {
                String signWorld = worldNameForKey(key);
                if (signWorld != null && Bukkit.getWorld(signWorld) == null) {
                    logger.log(Level.INFO,
                            "Skipping player shop at {0} because world ''{1}'' is not loaded; preserving entry.",
                            new Object[] { key, signWorld });
                    deferredEntries.put(key, new HashMap<>(shopSection.getValues(false)));
                }
                continue;
            }

            UUID ownerId = null;
            String ownerText = shopSection.getString("owner");
            if (ownerText != null) {
                try {
                    ownerId = UUID.fromString(ownerText);
                } catch (IllegalArgumentException ignored) {
                    ownerId = null;
                }
            }
            if (ownerId == null) {
                continue;
            }

            int quantity = shopSection.getInt("quantity");
            double price = shopSection.getDouble("price");
            ItemStack item = shopSection.getItemStack("item");
            List<String> chestKeys = shopSection.getStringList("chests");
            if (quantity <= 0 || price <= 0 || item == null || chestKeys.isEmpty()) {
                continue;
            }

            List<Location> chestLocations = new ArrayList<>();
            Set<String> missingWorlds = new HashSet<>();
            for (String chestKey : chestKeys) {
                Location chestLocation = parseLocation(chestKey);
                if (chestLocation != null) {
                    chestLocations.add(chestLocation);
                } else {
                    String chestWorld = worldNameForKey(chestKey);
                    if (chestWorld != null && Bukkit.getWorld(chestWorld) == null) {
                        missingWorlds.add(chestWorld);
                    }
                }
            }

            if (!missingWorlds.isEmpty()) {
                logger.log(Level.INFO,
                        "Skipping player shop at {0} because world(s) {1} are not loaded; preserving entry.",
                        new Object[] { key, String.join(", ", missingWorlds) });
                deferredEntries.put(key, new HashMap<>(shopSection.getValues(false)));
                continue;
            }

            if (chestLocations.isEmpty()) {
                continue;
            }

            Block signBlock = signLocation.getBlock();
            if (!(signBlock.getState() instanceof Sign)) {
                continue;
            }

            PlayerShop shop = new PlayerShop(ownerId, signLocation, chestLocations.get(0), chestLocations, item,
                    quantity, price);
            shops.add(shop);
        }

        return shops;
    }

    @Override
    public void saveShops(Map<String, PlayerShop> shopsBySign) {
        try {
            ensureDataFile();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to create player shop data file", ex);
            return;
        }

        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("version", DATA_VERSION);
        ConfigurationSection section = configuration.createSection("shops");

        for (PlayerShop shop : shopsBySign.values()) {
            String key = locationKey(shop.signLocation());
            ConfigurationSection shopSection = section.createSection(key);
            shopSection.set("owner", shop.ownerId().toString());
            shopSection.set("quantity", shop.quantityPerSale());
            shopSection.set("price", shop.price());
            shopSection.set("item", shop.itemTemplate());
            List<String> chestKeys = shop.chestLocations().stream()
                    .map(this::locationKey)
                    .collect(Collectors.toList());
            shopSection.set("chests", chestKeys);
        }

        // Merge deferred entries for worlds that are still unloaded
        for (Map.Entry<String, Map<String, Object>> entry : deferredEntries.entrySet()) {
            if (section.getConfigurationSection(entry.getKey()) != null) {
                continue; // active shop now covers this key
            }
            section.createSection(entry.getKey(), entry.getValue());
        }

        try {
            configuration.save(tempFile);
            atomicReplace(tempFile, dataFile);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to save player shop data", ex);
        }
    }

    /**
     * Replaces {@code target} with {@code source} using an atomic move where
     * the filesystem supports it, falling back to a plain replace otherwise.
     */
    private static void atomicReplace(File source, File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public String locationKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ',' + location.getBlockX() + ',' + location.getBlockY() + ','
                + location.getBlockZ();
    }

    @Override
    public Location parseLocation(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String[] parts = key.split(",");
        if (parts.length != 4) {
            return null;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return Optional.ofNullable(Bukkit.getWorld(parts[0]))
                    .map(world -> new Location(world, x, y, z))
                    .orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public String worldNameForKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String[] parts = key.split(",");
        if (parts.length != 4) {
            return null;
        }
        return parts[0].isEmpty() ? null : parts[0];
    }

    private void ensureDataFile() throws IOException {
        if (!dataFile.exists()) {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!dataFile.createNewFile()) {
                throw new IOException("Unable to create player-shops.yml");
            }
        }
    }
}
