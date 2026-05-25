package com.skyblockexp.ezshops.repository;

import com.skyblockexp.ezshops.database.jaloquent.JaloquentPlayerShopRepository;
import com.skyblockexp.ezshops.database.jaloquent.JaloquentTransaction;
import com.skyblockexp.ezshops.database.jaloquent.JaloquentTransactionalCallback;
import com.skyblockexp.ezshops.database.jaloquent.JaloquentClientAdapter;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JaloquentPlayerShopRepositoryTest {

    private ServerMock server;

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
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
        server = MockBukkit.mock();
        server.addSimpleWorld("world");

        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"));
        Location loc = repo.parseLocation("world,1,2,3");
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
}
