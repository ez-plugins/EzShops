package com.skyblockexp.ezshops.repository;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.repository.yml.YmlStockMarketRepository;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YmlStockMarketRepositoryTest extends AbstractEzShopsTest {

    @Test
    void pricesAndFrozen_arePersistedAndReloaded() throws Exception {
        // Register mock Vault economy provider and load the plugin to get a data folder
        loadProviderPlugin(Mockito.mock(Economy.class));
        JavaPlugin plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        File dataFolder = plugin.getDataFolder();

        // Ensure folder exists
        if (!dataFolder.exists()) dataFolder.mkdirs();

        YmlStockMarketRepository repo = new YmlStockMarketRepository(dataFolder);

        // Save prices
        Map<String, Double> prices = new HashMap<>();
        prices.put("DIAMOND", 123.45);
        prices.put("GOLD_INGOT", 5.0);
        repo.savePrices(prices);

        // Freeze an ID
        repo.freeze("diamond", "tester");

        // Create a fresh instance to load from disk
        YmlStockMarketRepository repo2 = new YmlStockMarketRepository(dataFolder);
        Map<String, Double> loadedPrices = repo2.loadPrices();

        assertEquals(2, loadedPrices.size());
        assertEquals(123.45, loadedPrices.get("DIAMOND"));

        // Verify frozen entries are persisted and can be loaded
        repo2.load();
        assertTrue(repo2.isFrozen("DIAMOND"));
        assertTrue(repo2.getAllFrozen().contains("DIAMOND"));

        // Unfreeze and verify persistence
        repo2.unfreeze("diamond");
        YmlStockMarketRepository repo3 = new YmlStockMarketRepository(dataFolder);
        repo3.load();
        assertFalse(repo3.isFrozen("DIAMOND"));
    }
}
