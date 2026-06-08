package com.skyblockexp.ezshops.database.jaloquent;

import com.skyblockexp.ezshops.playershop.PlayerShop;
import com.skyblockexp.ezshops.repository.PlayerShopRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Skeleton Jaloquent-backed implementation of {@link PlayerShopRepository}.
 */
public final class JaloquentPlayerShopRepository implements PlayerShopRepository {

    private final Map<String, Map<String, String>> deferredEntries = new HashMap<>();
    private final Logger logger;
    private final JaloquentClientAdapter adapter;

    public JaloquentPlayerShopRepository(Logger logger) {
        this(logger, new NoopJaloquentClientAdapter());
    }

    public JaloquentPlayerShopRepository(Logger logger, JaloquentClientAdapter adapter) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public void init() {
        try {
            adapter.init(Collections.emptyMap());
            adapter.createTableIfAbsent();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialise Jaloquent adapter", ex);
        }
    }

    @Override
    public Collection<PlayerShop> loadShops() {
        deferredEntries.clear();
        List<PlayerShop> shops = new ArrayList<>();
        try {
            List<Map<String, String>> rows = adapter.selectAllShops();
            for (Map<String, String> row : rows) {
                String signKey = row.get("sign_key");
                String ownerText = row.get("owner_uuid");
                int quantity = Integer.parseInt(row.getOrDefault("quantity", "0"));
                double price = Double.parseDouble(row.getOrDefault("price", "0"));
                String itemData = row.get("item_data");
                String chestsData = row.get("chests");

                Location signLocation = parseLocation(signKey);
                if (signLocation == null) {
                    String worldName = worldNameForKey(signKey);
                    if (worldName != null && Bukkit.getWorld(worldName) == null) {
                        logger.log(Level.INFO,
                                "Skipping player shop at {0} because world ''{1}'' is not loaded; preserving entry.",
                                new Object[]{signKey, worldName});
                        Map<String, String> raw = new HashMap<>();
                        raw.put("owner", ownerText);
                        raw.put("quantity", Integer.toString(quantity));
                        raw.put("price", Double.toString(price));
                        raw.put("item_data", itemData);
                        raw.put("chests", chestsData);
                        deferredEntries.put(signKey, raw);
                    }
                    continue;
                }

                UUID ownerId;
                try {
                    ownerId = UUID.fromString(ownerText);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                org.bukkit.inventory.ItemStack item = itemFromYaml(itemData);
                if (item == null) continue;

                List<Location> chestLocations = new ArrayList<>();
                Set<String> missingWorlds = new HashSet<>();
                if (chestsData != null) {
                    for (String chestKey : chestsData.split("\n")) {
                        if (chestKey.isBlank()) continue;
                        Location chestLoc = parseLocation(chestKey.trim());
                        if (chestLoc != null) {
                            chestLocations.add(chestLoc);
                        } else {
                            String cw = worldNameForKey(chestKey.trim());
                            if (cw != null) missingWorlds.add(cw);
                        }
                    }
                }

                if (!missingWorlds.isEmpty()) {
                    logger.log(Level.INFO,
                            "Skipping player shop at {0} because world(s) {1} are not loaded; preserving entry.",
                            new Object[]{signKey, String.join(", ", missingWorlds)});
                    Map<String, String> raw = new HashMap<>();
                    raw.put("owner", ownerText);
                    raw.put("quantity", Integer.toString(quantity));
                    raw.put("price", Double.toString(price));
                    raw.put("item_data", itemData);
                    raw.put("chests", chestsData);
                    deferredEntries.put(signKey, raw);
                    continue;
                }

                if (chestLocations.isEmpty() || quantity <= 0 || price <= 0) continue;

                org.bukkit.block.Block signBlock = signLocation.getBlock();
                if (!(signBlock.getState() instanceof org.bukkit.block.Sign)) continue;

                shops.add(new PlayerShop(ownerId, signLocation, chestLocations.get(0),
                        chestLocations, item, quantity, price));
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load player shops from Jaloquent adapter", ex);
        }
        return shops;
    }

    @Override
    public void saveShops(Map<String, PlayerShop> shopsBySign) {
        try {
            adapter.transactional(tx -> {
                tx.deleteAllShops();
                for (PlayerShop shop : shopsBySign.values()) {
                    String signKey = locationKey(shop.signLocation());
                    String itemData = itemToYaml(shop.itemTemplate());
                    StringBuilder chestsBuilder = new StringBuilder();
                    for (Location loc : shop.chestLocations()) {
                        if (chestsBuilder.length() > 0) chestsBuilder.append('\n');
                        chestsBuilder.append(locationKey(loc));
                    }
                    Map<String, Object> values = new HashMap<>();
                    values.put("sign_key", signKey);
                    values.put("owner_uuid", shop.ownerId().toString());
                    values.put("quantity", shop.quantityPerSale());
                    values.put("price", shop.price());
                    values.put("item_data", itemData);
                    values.put("chests", chestsBuilder.toString());
                    tx.insertShop(values);
                }

                for (Map.Entry<String, Map<String, String>> entry : deferredEntries.entrySet()) {
                    if (shopsBySign.containsKey(entry.getKey())) continue;
                    Map<String, String> raw = entry.getValue();
                    Map<String, Object> values = new HashMap<>();
                    values.put("sign_key", entry.getKey());
                    values.put("owner_uuid", raw.getOrDefault("owner", ""));
                    values.put("quantity", Integer.parseInt(raw.getOrDefault("quantity", "0")));
                    values.put("price", Double.parseDouble(raw.getOrDefault("price", "0")));
                    values.put("item_data", raw.getOrDefault("item_data", ""));
                    values.put("chests", raw.getOrDefault("chests", ""));
                    tx.insertShop(values);
                }
            });
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to save player shops to Jaloquent adapter", ex);
        }
    }

    private static String itemToYaml(org.bukkit.inventory.ItemStack item) {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("item", item);
        return config.saveToString();
    }

    private static org.bukkit.inventory.ItemStack itemFromYaml(String yaml) {
        if (yaml == null || yaml.isBlank()) return null;
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (org.bukkit.configuration.InvalidConfigurationException ex) {
            return null;
        }
        return config.getItemStack("item");
    }

    @Override
    public String locationKey(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return location.getWorld().getName() + ',' + location.getBlockX() + ','
                + location.getBlockY() + ',' + location.getBlockZ();
    }

    @Override
    public Location parseLocation(String key) {
        if (key == null || key.isEmpty()) return null;
        String[] parts = key.split(",");
        if (parts.length != 4) return null;
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
        if (key == null || key.isEmpty()) return null;
        String[] parts = key.split(",");
        if (parts.length != 4) return null;
        return parts[0].isEmpty() ? null : parts[0];
    }
}
