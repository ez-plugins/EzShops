package com.skyblockexp.ezshops.repository.mysql;

import com.skyblockexp.ezshops.playershop.PlayerShop;
import com.skyblockexp.ezshops.repository.PlayerShopRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL-backed implementation of {@link PlayerShopRepository}.
 *
 * <p>Requires the MySQL JDBC driver ({@code com.mysql.cj.jdbc.Driver} or
 * {@code com.mysql.jdbc.Driver}) to be available at runtime on the classpath.
 *
 * <p>Item stacks are serialised as YAML strings using Bukkit's built-in
 * {@link YamlConfiguration} serialisation so that the stored format is
 * identical to the YAML file backend.
 *
 * <p>Chest locations for a shop are stored as a newline-delimited list of
 * {@code world,x,y,z} keys in the {@code chests} column; the first entry is
 * always the primary chest.
 */
public final class MysqlPlayerShopRepository implements PlayerShopRepository {

    private final String url;
    private final String username;
    private final String password;
    private final String tablePrefix;
    private final Logger logger;

    /** Raw YAML entries for shops whose worlds were not loaded; preserved across save cycles. */
    private final Map<String, Map<String, String>> deferredEntries = new HashMap<>();

    private Connection connection;

    public MysqlPlayerShopRepository(String host, int port, String database,
            String username, String password, String tablePrefix, Logger logger) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";
        this.username = username;
        this.password = password;
        this.tablePrefix = tablePrefix;
        this.logger = logger;
    }

    /**
     * Initialises the repository: loads the JDBC driver and creates the table
     * if it does not exist.
     *
     * @throws IllegalStateException if the driver cannot be found or the table
     *                               cannot be created
     */
    public void init() {
        loadDriver();
        try {
            createTableIfAbsent(getConnection());
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialise MySQL player-shop table", ex);
        }
    }

    @Override
    public Collection<PlayerShop> loadShops() {
        deferredEntries.clear();
        List<PlayerShop> shops = new ArrayList<>();
        String sql = "SELECT sign_key, owner_uuid, quantity, price, item_data, chests FROM " + shopsTable();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String signKey = rs.getString("sign_key");
                String ownerText = rs.getString("owner_uuid");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                String itemData = rs.getString("item_data");
                String chestsData = rs.getString("chests");

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

                ItemStack item = itemFromYaml(itemData);
                if (item == null) continue;

                List<Location> chestLocations = new ArrayList<>();
                Set<String> missingWorlds = new HashSet<>();
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

                Block signBlock = signLocation.getBlock();
                if (!(signBlock.getState() instanceof Sign)) continue;

                shops.add(new PlayerShop(ownerId, signLocation, chestLocations.get(0),
                        chestLocations, item, quantity, price));
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to load player shops from MySQL", ex);
        }
        return shops;
    }

    @Override
    public void saveShops(Map<String, PlayerShop> shopsBySign) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete all active rows; deferred rows are re-inserted below
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM " + shopsTable())) {
                    del.executeUpdate();
                }

                String insert = "INSERT INTO " + shopsTable()
                        + " (sign_key, owner_uuid, quantity, price, item_data, chests) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    for (PlayerShop shop : shopsBySign.values()) {
                        String signKey = locationKey(shop.signLocation());
                        String itemData = itemToYaml(shop.itemTemplate());
                        StringBuilder chestsBuilder = new StringBuilder();
                        for (Location loc : shop.chestLocations()) {
                            if (chestsBuilder.length() > 0) chestsBuilder.append('\n');
                            chestsBuilder.append(locationKey(loc));
                        }
                        ps.setString(1, signKey);
                        ps.setString(2, shop.ownerId().toString());
                        ps.setInt(3, shop.quantityPerSale());
                        ps.setDouble(4, shop.price());
                        ps.setString(5, itemData);
                        ps.setString(6, chestsBuilder.toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // Re-insert deferred entries for worlds that are still unloaded
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    for (Map.Entry<String, Map<String, String>> entry : deferredEntries.entrySet()) {
                        if (shopsBySign.containsKey(entry.getKey())) continue;
                        Map<String, String> raw = entry.getValue();
                        ps.setString(1, entry.getKey());
                        ps.setString(2, raw.getOrDefault("owner", ""));
                        ps.setInt(3, Integer.parseInt(raw.getOrDefault("quantity", "0")));
                        ps.setDouble(4, Double.parseDouble(raw.getOrDefault("price", "0")));
                        ps.setString(5, raw.getOrDefault("item_data", ""));
                        ps.setString(6, raw.getOrDefault("chests", ""));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to save player shops to MySQL", ex);
        }
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

    // ---- private helpers ----

    private String shopsTable() {
        return tablePrefix + "player_shops";
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            connection = DriverManager.getConnection(url, username, password);
        }
        return connection;
    }

    private void createTableIfAbsent(Connection conn) throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS " + shopsTable() + " ("
                + "sign_key VARCHAR(255) NOT NULL,"
                + "owner_uuid VARCHAR(36) NOT NULL,"
                + "quantity INT NOT NULL,"
                + "price DOUBLE NOT NULL,"
                + "item_data MEDIUMTEXT NOT NULL,"
                + "chests TEXT NOT NULL,"
                + "PRIMARY KEY (sign_key)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
        }
    }

    private static void loadDriver() {
        String[] drivers = {
                "com.mysql.cj.jdbc.Driver",
                "com.mysql.jdbc.Driver",
                "org.mariadb.jdbc.Driver"
        };
        for (String driver : drivers) {
            try {
                Class.forName(driver);
                return;
            } catch (ClassNotFoundException ignored) {
                // try next
            }
        }
        throw new IllegalStateException(
                "No MySQL/MariaDB JDBC driver found on the classpath. "
                        + "Add mysql-connector-j or mariadb-java-client to the server's lib folder.");
    }

    private static String itemToYaml(ItemStack item) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        return config.saveToString();
    }

    private static ItemStack itemFromYaml(String yaml) {
        if (yaml == null || yaml.isBlank()) return null;
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (InvalidConfigurationException ex) {
            return null;
        }
        return config.getItemStack("item");
    }
}
