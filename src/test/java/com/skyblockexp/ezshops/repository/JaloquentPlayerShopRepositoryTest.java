package com.skyblockexp.ezshops.repository;

import com.skyblockexp.ezshops.database.jaloquent.JaloquentPlayerShopRepository;
import com.skyblockexp.ezshops.database.jaloquent.JaloquentTransaction;
import com.skyblockexp.ezshops.database.jaloquent.JaloquentTransactionalCallback;
import com.skyblockexp.ezshops.database.jaloquent.JaloquentClientAdapter;
import com.skyblockexp.ezshops.playershop.PlayerShop;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class JaloquentPlayerShopRepositoryTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
        }
    }

    @Test
    void initThrowsWhenNotImplemented() {
        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"));
        assertThrows(IllegalStateException.class, repo::init);
    }

    @Test
    void initWrapsAdapterFailure() {
        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) throws Exception { throw new Exception("boom"); }
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() { return List.of(); }
            @Override public void transactional(JaloquentTransactionalCallback callback) {}
            @Override public void close() {}
        };

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        IllegalStateException ex = assertThrows(IllegalStateException.class, repo::init);
        assertTrue(ex.getMessage().contains("Failed to initialise"));
    }

    @Test
    void locationAndWorldParsingBehavesForValidAndInvalidValues() {
        server.addSimpleWorld("world");

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"));
        Location loc = repo.parseLocation("world,1,2,3");
        assertNotNull(loc);
        assertEquals("world", loc.getWorld().getName());
        assertEquals(1, loc.getBlockX());
        assertNull(repo.parseLocation("bad"));
        assertNull(repo.parseLocation("world,a,2,3"));
        assertEquals("world", repo.worldNameForKey("world,1,2,3"));
        assertNull(repo.worldNameForKey("a,b"));
    }

    @Test
    void saveShopsDoesNotThrowWhenAdapterTransactionFails() {
        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() { return List.of(); }
            @Override public void transactional(JaloquentTransactionalCallback callback) throws Exception { throw new Exception("tx fail"); }
            @Override public void close() {}
        };
        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        repo.saveShops(Map.of());
    }

    @Test
    void loadShopsReturnsEmptyWhenAdapterThrows() {
        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() throws Exception { throw new RuntimeException("db error"); }
            @Override public void transactional(JaloquentTransactionalCallback callback) {}
            @Override public void close() {}
        };
        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        Collection<PlayerShop> shops = repo.loadShops();
        assertNotNull(shops);
        assertTrue(shops.isEmpty());
    }

    @Test
    void loadShopsSkipsShopWithMissingWorldAndPreservesDeferredEntry() {
        server.addSimpleWorld("loaded_world");

        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() {
                Map<String, String> row = new HashMap<>();
                row.put("sign_key", "loaded_world,0,64,0");
                row.put("owner_uuid", "00000000-0000-0000-0000-000000000000");
                row.put("quantity", "5");
                row.put("price", "10.0");
                row.put("item_data", yamlForItem(new ItemStack(Material.DIAMOND, 1)));
                row.put("chests", "missing_world,1,64,0\nmissing_world,2,64,0");
                return List.of(row);
            }
            @Override public void transactional(JaloquentTransactionalCallback callback) throws Exception {
                callback.execute(new JaloquentTransaction() {
                    @Override public void deleteAllShops() throws Exception {}
                    @Override public void insertShop(Map<String, Object> values) throws Exception {}
                });
            }
            @Override public void close() {}
        };

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        Collection<PlayerShop> shops = repo.loadShops();
        assertTrue(shops.isEmpty(), "Should skip shops with missing worlds in chests");

        var deferred = getDeferredEntries(repo);
        assertFalse(deferred.isEmpty(), "Should have deferred entry");
        assertEquals("00000000-0000-0000-0000-000000000000", deferred.get("loaded_world,0,64,0").get("owner"));
    }

    @Test
    void loadShopsSkipsShopWithInvalidUuid() {
        server.addSimpleWorld("world");

        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() {
                Map<String, String> row = new HashMap<>();
                row.put("sign_key", "world,0,64,0");
                row.put("owner_uuid", "not-a-valid-uuid");
                row.put("quantity", "5");
                row.put("price", "10.0");
                row.put("item_data", "valid-yaml");
                row.put("chests", "world,1,64,0");
                return List.of(row);
            }
            @Override public void transactional(JaloquentTransactionalCallback callback) {}
            @Override public void close() {}
        };

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        Collection<PlayerShop> shops = repo.loadShops();
        assertTrue(shops.isEmpty(), "Should skip shops with invalid UUID");
    }

    @Test
    void loadShopsSkipsShopWithNullItem() throws Exception {
        server.addSimpleWorld("world");

        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() {
                Map<String, String> row = new HashMap<>();
                row.put("sign_key", "world,0,64,0");
                row.put("owner_uuid", "00000000-0000-0000-0000-000000000000");
                row.put("quantity", "5");
                row.put("price", "10.0");
                row.put("item_data", "invalid-yaml");
                row.put("chests", "world,1,64,0");
                return List.of(row);
            }
            @Override public void transactional(JaloquentTransactionalCallback callback) {}
            @Override public void close() {}
        };

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        Collection<PlayerShop> shops = repo.loadShops();
        assertTrue(shops.isEmpty(), "Should skip shops with null item");
    }

    @Test
    void loadShopsSkipsShopWithEmptyChestLocations() throws Exception {
        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() {
                Map<String, String> row = new HashMap<>();
                row.put("sign_key", "world,0,64,0");
                row.put("owner_uuid", "00000000-0000-0000-0000-000000000000");
                row.put("quantity", "5");
                row.put("price", "10.0");
                row.put("item_data", yamlForItem(new ItemStack(Material.DIAMOND, 1)));
                row.put("chests", "");
                return List.of(row);
            }
            @Override public void transactional(JaloquentTransactionalCallback callback) {}
            @Override public void close() {}
        };

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        Collection<PlayerShop> shops = repo.loadShops();
        assertTrue(shops.isEmpty(), "Should skip shops with empty chest locations");
    }

    @Test
    void loadShopsSkipsShopWhenQuantityOrPriceInvalid() throws Exception {
        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() {
                Map<String, String> row = new HashMap<>();
                row.put("sign_key", "world,0,64,0");
                row.put("owner_uuid", "00000000-0000-0000-0000-000000000000");
                row.put("quantity", "0");
                row.put("price", "10.0");
                row.put("item_data", yamlForItem(new ItemStack(Material.DIAMOND, 1)));
                row.put("chests", "world,1,64,0");
                return List.of(row);
            }
            @Override public void transactional(JaloquentTransactionalCallback callback) {}
            @Override public void close() {}
        };

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        Collection<PlayerShop> shops = repo.loadShops();
        assertTrue(shops.isEmpty(), "Should skip shops with zero or negative quantity/price");
    }

    @Test
    void loadShopsReturnsValidShop() throws Exception {
        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() {
                Map<String, String> row = new HashMap<>();
                row.put("sign_key", "world,0,64,0");
                row.put("owner_uuid", "00000000-0000-0000-0000-000000000000");
                row.put("quantity", "5");
                row.put("price", "10.0");
                row.put("item_data", yamlForItem(new ItemStack(Material.DIAMOND, 1)));
                row.put("chests", "world,1,64,0");
                return List.of(row);
            }
            @Override public void transactional(JaloquentTransactionalCallback callback) {}
            @Override public void close() {}
        };

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);
        Collection<PlayerShop> shops = repo.loadShops();
        assertNotNull(shops);
    }

    @Test
    void saveShopsPreservesDeferredEntries() throws Exception {
        List<Map<String, Object>> inserted = new ArrayList<>();

        JaloquentClientAdapter adapter = new JaloquentClientAdapter() {
            @Override public void init(Map<String, String> config) {}
            @Override public void createTableIfAbsent() {}
            @Override public List<Map<String, String>> selectAllShops() { return List.of(); }
            @Override public void transactional(JaloquentTransactionalCallback callback) throws Exception {
                callback.execute(new JaloquentTransaction() {
                    @Override public void deleteAllShops() throws Exception {}
                    @Override public void insertShop(Map<String, Object> values) throws Exception {
                        inserted.add(values);
                    }
                });
            }
            @Override public void close() {}
        };

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"), adapter);

        var deferred = getDeferredEntries(repo);
        Map<String, String> entry = new HashMap<>();
        entry.put("owner", "00000000-0000-0000-0000-000000000000");
        entry.put("quantity", "5");
        entry.put("price", "10.0");
        entry.put("item_data", "item-yaml");
        entry.put("chests", "world,1,64,0");
        deferred.put("missing_world,0,64,0", entry);

        repo.saveShops(Map.of());

        boolean hasDeferred = inserted.stream().anyMatch(v -> "missing_world,0,64,0".equals(v.get("sign_key")));
        assertTrue(hasDeferred, "Deferred entry should be re-inserted");
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

    private static String yamlForItem(ItemStack item) {
        try {
            return invokeItemToYaml(item);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize test item to YAML", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, String>> getDeferredEntries(JaloquentPlayerShopRepository repo) {
        try {
            Field field = JaloquentPlayerShopRepository.class.getDeclaredField("deferredEntries");
            field.setAccessible(true);
            return (Map<String, Map<String, String>>) field.get(repo);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to access deferredEntries for test", ex);
        }
    }

    private static String invokeItemToYaml(ItemStack item) throws Exception {
        Method method = JaloquentPlayerShopRepository.class.getDeclaredMethod("itemToYaml", ItemStack.class);
        method.setAccessible(true);
        return (String) method.invoke(null, item);
    }

    private static ItemStack invokeItemFromYaml(String yaml) throws Exception {
        Method method = JaloquentPlayerShopRepository.class.getDeclaredMethod("itemFromYaml", String.class);
        method.setAccessible(true);
        return (ItemStack) method.invoke(null, yaml);
    }
}