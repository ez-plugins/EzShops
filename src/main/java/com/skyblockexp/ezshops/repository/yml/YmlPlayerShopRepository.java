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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * YML-based implementation of PlayerShopRepository.
 * Handles persistence of player shops to player-shops.yml file.
 */
public class YmlPlayerShopRepository implements PlayerShopRepository {
    
    private final File dataFile;
    private final Logger logger;
    private final Map<String, Map<String, Object>> deferredEntries;
    
    public YmlPlayerShopRepository(File dataFolder, Logger logger) {
        this.dataFile = new File(dataFolder, "player-shops.yml");
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
    public void saveShops(Map<String, PlayerShop> shopsBySign, Map<String, Map<String, Object>> deferredEntries) {
        try {
            ensureDataFile();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to create player shop data file", ex);
            return;
        }
        
        YamlConfiguration configuration = new YamlConfiguration();
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
        
        for (Map.Entry<String, Map<String, Object>> entry : deferredEntries.entrySet()) {
            if (section.getConfigurationSection(entry.getKey()) != null) {
                continue;
            }
            section.createSection(entry.getKey(), entry.getValue());
        }
        
        try {
            configuration.save(dataFile);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to save player shop data", ex);
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
    
    @Override
    public Map<String, Map<String, Object>> getDeferredEntries() {
        return new HashMap<>(deferredEntries);
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
