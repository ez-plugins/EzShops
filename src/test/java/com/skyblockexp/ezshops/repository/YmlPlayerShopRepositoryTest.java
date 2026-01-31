package com.skyblockexp.ezshops.repository;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.playershop.PlayerShop;
import com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class YmlPlayerShopRepositoryTest extends AbstractEzShopsTest {

    @Test
    void saveShops_writesShopEntries_andDeferredEntries() throws Exception {
        // Register a mock Vault economy provider so the plugin doesn't disable
        loadProviderPlugin(Mockito.mock(Economy.class));
        JavaPlugin plugin = loadPlugin(com.skyblockexp.ezshops.EzShopsPlugin.class);
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        YmlPlayerShopRepository repo = new YmlPlayerShopRepository(dataFolder, plugin.getLogger());

        // Prepare a world and locations (use a mocked World if none present)
        org.bukkit.World world;
        if (Bukkit.getWorlds().isEmpty()) {
            world = Mockito.mock(org.bukkit.World.class);
            Mockito.when(world.getName()).thenReturn("world");
        } else {
            world = Bukkit.getWorlds().get(0);
        }
        Location signLoc = new Location(world, 10, 64, 10);
        Location chestLoc = new Location(world, 11, 64, 10);

        UUID owner = UUID.randomUUID();
        ItemStack item = new ItemStack(Material.DIAMOND, 1);
        List<Location> chests = List.of(chestLoc);

        PlayerShop shop = new PlayerShop(owner, signLoc, chestLoc, chests, item, 1, 12.5);

        Map<String, PlayerShop> shopsBySign = new HashMap<>();
        shopsBySign.put(repo.locationKey(signLoc), shop);

        Map<String, Map<String, Object>> deferred = new HashMap<>();
        Map<String, Object> deferredEntry = new HashMap<>();
        deferredEntry.put("owner", owner.toString());
        deferred.put("missingworld,0,0,0", deferredEntry);

        repo.saveShops(shopsBySign, deferred);

        // Read raw file to verify
        File dataFile = new File(dataFolder, "player-shops.yml");
        assertTrue(dataFile.exists(), "player-shops.yml should exist");

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        String key = repo.locationKey(signLoc);
        assertNotNull(cfg.getConfigurationSection("shops." + key));
        assertEquals(owner.toString(), cfg.getString("shops." + key + ".owner"));
        assertEquals(1, cfg.getInt("shops." + key + ".quantity"));
        assertEquals(12.5, cfg.getDouble("shops." + key + ".price"));

        // Deferred entry present
        assertNotNull(cfg.getConfigurationSection("shops.missingworld,0,0,0"));
        assertEquals(owner.toString(), cfg.getString("shops.missingworld,0,0,0.owner"));
    }
}
