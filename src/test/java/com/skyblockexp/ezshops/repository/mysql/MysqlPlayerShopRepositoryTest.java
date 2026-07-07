package com.skyblockexp.ezshops.repository.mysql;

import com.skyblockexp.ezshops.playershop.PlayerShop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MysqlPlayerShopRepositoryTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        try {
            server = MockBukkit.mock();
        } catch (Throwable throwable) {
            String message = throwable.getMessage();
            boolean incompatibleVersion = throwable.getClass().getName().contains("IncompatiblePaperVersionException")
                    || (message != null && message.contains("Version Mismatch"));
            if (incompatibleVersion) {
                Assumptions.assumeTrue(false,
                        "Skipping MockBukkit-backed test due to Paper/MockBukkit version mismatch");
            }
            throw throwable;
        }
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
        }
    }

    @Test
    void locationKeyReturnsEmptyStringForNullLocation() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertEquals("", repo.locationKey(null));
    }

    @Test
    void locationKeyReturnsEmptyStringForLocationWithNullWorld() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        Location loc = new Location(null, 1, 2, 3);
        assertEquals("", repo.locationKey(loc));
    }

    @Test
    void worldNameForKeyReturnsNullForNullInput() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.worldNameForKey(null));
    }

    @Test
    void worldNameForKeyReturnsNullForEmptyInput() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.worldNameForKey(""));
    }

    @Test
    void worldNameForKeyReturnsNullForWrongPartCount() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.worldNameForKey("world,1,2"));
        assertNull(repo.worldNameForKey("a,b,c,d,e"));
    }

    @Test
    void worldNameForKeyReturnsNullForEmptyWorldName() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.worldNameForKey(",1,2,3"));
    }

    @Test
    void worldNameForKeyExtractsWorld() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertEquals("world", repo.worldNameForKey("world,1,2,3"));
    }

    @Test
    void parseLocationReturnsNullForNullInput() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.parseLocation(null));
    }

    @Test
    void parseLocationReturnsNullForEmptyInput() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.parseLocation(""));
    }

    @Test
    void parseLocationReturnsNullForWrongPartCount() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.parseLocation("world,1,2"));
        assertNull(repo.parseLocation("a,b,c,d,e"));
    }

    @Test
    void parseLocationReturnsNullForInvalidCoordinates() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.parseLocation("world,a,b,c"));
    }

    @Test
    void parseLocationReturnsNullForUnknownWorld() {
        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository("localhost", 3306, "test", "root", "", "ez_", null);
        assertNull(repo.parseLocation("unknown_world,1,2,3"));
    }

    @Test
    void loadShopsReturnsEmptyListWhenNoRows() throws Exception {
        String url = "jdbc:h2:mem:mysql_repo_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            c.createStatement().executeUpdate(
                "CREATE TABLE ez_player_shops (sign_key VARCHAR(255) PRIMARY KEY, owner_uuid VARCHAR(36), quantity INT, price DOUBLE, item_data MEDIUMTEXT, chests TEXT)"
            );
        }

        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository(
                "localhost", 3306, "test", "root", "", "ez_",
                java.util.logging.Logger.getLogger("test")
        );
        injectH2Connection(repo, url);

        Collection<?> shops = repo.loadShops();
        assertNotNull(shops);
        assertTrue(shops.isEmpty());
    }

    @Test
    void loadShopsSkipsShopWithMissingWorldAndPreservesDeferredEntry() throws Exception {
        server.addSimpleWorld("loaded_world");

        String url = "jdbc:h2:mem:mysql_repo_deferred;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            c.createStatement().executeUpdate(
                "CREATE TABLE ez_player_shops (sign_key VARCHAR(255) PRIMARY KEY, owner_uuid VARCHAR(36), quantity INT, price DOUBLE, item_data MEDIUMTEXT, chests TEXT)"
            );
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO ez_player_shops (sign_key, owner_uuid, quantity, price, item_data, chests) VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, "loaded_world,0,64,0");
                ps.setString(2, "00000000-0000-0000-0000-000000000000");
                ps.setInt(3, 5);
                ps.setDouble(4, 10.0);
                ps.setString(5, invokeItemToYaml(new ItemStack(Material.DIAMOND, 1)));
                ps.setString(6, "missing_world,1,64,0\nmissing_world,2,64,0");
                ps.executeUpdate();
            }
        }

        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository(
                "localhost", 3306, "test", "root", "", "ez_",
                java.util.logging.Logger.getLogger("test")
        );
        injectH2Connection(repo, url);

        Collection<?> shops = repo.loadShops();
        assertTrue(shops.isEmpty(), "Should skip shops with missing worlds");

        var deferred = getDeferredEntries(repo);
        assertFalse(deferred.isEmpty(), "Should have deferred entry for missing world");
        assertEquals("00000000-0000-0000-0000-000000000000", deferred.get("loaded_world,0,64,0").get("owner"));
    }

    @Test
    void loadShopsSkipsShopWithInvalidUuid() throws Exception {
        String url = "jdbc:h2:mem:mysql_repo_uuid;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            c.createStatement().executeUpdate(
                "CREATE TABLE ez_player_shops (sign_key VARCHAR(255) PRIMARY KEY, owner_uuid VARCHAR(36), quantity INT, price DOUBLE, item_data MEDIUMTEXT, chests TEXT)"
            );
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO ez_player_shops (sign_key, owner_uuid, quantity, price, item_data, chests) VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, "world,0,64,0");
                ps.setString(2, "not-a-uuid");
                ps.setInt(3, 5);
                ps.setDouble(4, 10.0);
                ps.setString(5, "valid-yaml-with-item");
                ps.setString(6, "world,1,64,0");
                ps.executeUpdate();
            }
        }

        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository(
                "localhost", 3306, "test", "root", "", "ez_",
                java.util.logging.Logger.getLogger("test")
        );
        injectH2Connection(repo, url);

        Collection<?> shops = repo.loadShops();
        assertTrue(shops.isEmpty(), "Should skip shops with invalid UUID");
    }

    @Test
    void loadShopsSkipsShopWithMissingItem() throws Exception {
        String url = "jdbc:h2:mem:mysql_repo_item;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            c.createStatement().executeUpdate(
                "CREATE TABLE ez_player_shops (sign_key VARCHAR(255) PRIMARY KEY, owner_uuid VARCHAR(36), quantity INT, price DOUBLE, item_data MEDIUMTEXT, chests TEXT)"
            );
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO ez_player_shops (sign_key, owner_uuid, quantity, price, item_data, chests) VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, "world,0,64,0");
                ps.setString(2, "00000000-0000-0000-0000-000000000000");
                ps.setInt(3, 5);
                ps.setDouble(4, 10.0);
                ps.setString(5, "invalid-yaml");
                ps.setString(6, "world,1,64,0");
                ps.executeUpdate();
            }
        }

        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository(
                "localhost", 3306, "test", "root", "", "ez_",
                java.util.logging.Logger.getLogger("test")
        );
        injectH2Connection(repo, url);

        Collection<?> shops = repo.loadShops();
        assertTrue(shops.isEmpty(), "Should skip shops with null item");
    }

    @Test
    void saveShopsDeletesAndReinsertsShops() throws Exception {
        org.bukkit.World world = server.addSimpleWorld("world");

        String url = "jdbc:h2:mem:mysql_repo_save;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            c.createStatement().executeUpdate(
                "CREATE TABLE ez_player_shops (sign_key VARCHAR(255) PRIMARY KEY, owner_uuid VARCHAR(36), quantity INT, price DOUBLE, item_data MEDIUMTEXT, chests TEXT)"
            );
        }

        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository(
                "localhost", 3306, "test", "root", "", "ez_",
                java.util.logging.Logger.getLogger("test")
        );
        injectH2Connection(repo, url);

        UUID owner = UUID.randomUUID();
        Location signLoc = new Location(world, 10, 64, 10);
        Location chestLoc = new Location(world, 11, 64, 10);
        List<Location> chests = List.of(chestLoc);
        ItemStack item = new ItemStack(Material.EMERALD, 1);
        PlayerShop shop = new PlayerShop(owner, signLoc, chestLoc, chests, item, 3, 25.5);

        Map<String, PlayerShop> shopsBySign = new HashMap<>();
        shopsBySign.put("", shop);
        repo.saveShops(shopsBySign);

        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM ez_player_shops")) {
                var rs = ps.executeQuery();
                rs.next();
                assertEquals(1, rs.getInt(1));
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT price FROM ez_player_shops")) {
                var rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals(25.5, rs.getDouble("price"));
            }
        }
    }

    @Test
    void saveShopsPreservesDeferredEntries() throws Exception {
        String url = "jdbc:h2:mem:mysql_repo_deferred_save;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            c.createStatement().executeUpdate(
                "CREATE TABLE ez_player_shops (sign_key VARCHAR(255) PRIMARY KEY, owner_uuid VARCHAR(36), quantity INT, price DOUBLE, item_data MEDIUMTEXT, chests TEXT)"
            );
        }

        MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository(
                "localhost", 3306, "test", "root", "", "ez_",
                java.util.logging.Logger.getLogger("test")
        );
        injectH2Connection(repo, url);
        var deferred = getDeferredEntries(repo);
        Map<String, String> entry = new HashMap<>();
        entry.put("owner", "00000000-0000-0000-0000-000000000000");
        entry.put("quantity", "5");
        entry.put("price", "10.0");
        entry.put("item_data", "item-yaml");
        entry.put("chests", "missing_world,1,64,0");
        deferred.put("missing_world,0,64,0", entry);

        Map<String, PlayerShop> shopsBySign = new HashMap<>();
        repo.saveShops(shopsBySign);

        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            try (PreparedStatement ps = c.prepareStatement("SELECT sign_key FROM ez_player_shops WHERE sign_key = 'missing_world,0,64,0'")) {
                var rs = ps.executeQuery();
                assertTrue(rs.next(), "Deferred entry should be preserved");
            }
        }
    }

    @Test
    void itemToYamlAndItemFromYamlRoundTrip() throws Exception {
        ItemStack original = new ItemStack(Material.DIAMOND_SWORD, 3);
        String yaml = invokeItemToYaml(original);
        assertNotNull(yaml);

        ItemStack restored = invokeItemFromYaml(yaml);
        assertNotNull(restored);
        assertEquals(Material.DIAMOND_SWORD, restored.getType());
        assertEquals(3, restored.getAmount());
    }

    private static void injectH2Connection(MysqlPlayerShopRepository repo, String url) throws Exception {
        Field urlField = MysqlPlayerShopRepository.class.getDeclaredField("url");
        urlField.setAccessible(true);
        urlField.set(repo, url);

        Field userField = MysqlPlayerShopRepository.class.getDeclaredField("username");
        userField.setAccessible(true);
        userField.set(repo, "sa");

        Field passwordField = MysqlPlayerShopRepository.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(repo, "");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, String>> getDeferredEntries(MysqlPlayerShopRepository repo) throws Exception {
        Field field = MysqlPlayerShopRepository.class.getDeclaredField("deferredEntries");
        field.setAccessible(true);
        return (Map<String, Map<String, String>>) field.get(repo);
    }

    private static String invokeItemToYaml(ItemStack item) throws Exception {
        Method method = MysqlPlayerShopRepository.class.getDeclaredMethod("itemToYaml", ItemStack.class);
        method.setAccessible(true);
        return (String) method.invoke(null, item);
    }

    private static ItemStack invokeItemFromYaml(String yaml) throws Exception {
        Method method = MysqlPlayerShopRepository.class.getDeclaredMethod("itemFromYaml", String.class);
        method.setAccessible(true);
        return (ItemStack) method.invoke(null, yaml);
    }
}