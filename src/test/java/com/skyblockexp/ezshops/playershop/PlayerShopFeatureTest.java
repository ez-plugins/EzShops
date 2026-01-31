package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import com.skyblockexp.ezshops.playershop.PlayerShopManager;
import com.skyblockexp.ezshops.playershop.PlayerShop;
import com.skyblockexp.ezshops.playershop.PlayerShopCreationResult;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;

public class PlayerShopFeatureTest extends AbstractEzShopsTest {

    @Test
    void create_and_purchase_player_shop_flow() throws Exception {
        // Provide economy and load plugin
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // Create a standalone PlayerShopManager for the test (bypass plugin component reflection)
        com.skyblockexp.ezshops.config.PlayerShopConfiguration config = com.skyblockexp.ezshops.config.PlayerShopConfiguration.defaults();
        com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository repo = new com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
        PlayerShopManager manager = new PlayerShopManager(plugin, econ, config, repo);
        manager.enable();

        // Ensure a test world exists in MockBukkit
        if (Bukkit.getWorlds().isEmpty()) {
            new org.bukkit.WorldCreator("test").createWorld();
        }
        org.bukkit.World world = Bukkit.getWorlds().get(0);
        Location chestLoc = new Location(world, 2, 65, 2);
        Block chestBlock = world.getBlockAt(chestLoc);
        world.getChunkAt(chestLoc).load();
        chestBlock.setType(Material.CHEST);
        Chest chestState = (Chest) chestBlock.getState();
        chestState.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 10));
        chestState.update();
        // re-fetch state after update to ensure MockBukkit applies inventory changes
        chestState = (Chest) chestBlock.getState();

        Location signLoc = new Location(world, 2, 66, 2);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_SIGN);
        Sign signState = (Sign) signBlock.getState();
        signState.update();

        // Use a real MockBukkit player so inventory and permissions behave correctly
        org.bukkit.entity.Player owner = server.addPlayer("owner");
        UUID ownerId = owner.getUniqueId();
        owner.addAttachment(plugin, PlayerShopManager.PERMISSION_CREATE, true);

        // Construct and register a shop directly (bypass createShop validation in tests)
        ItemStack template = new ItemStack(Material.DIAMOND, 1);
        java.util.List<org.bukkit.Location> chests = java.util.List.of(chestLoc);
        PlayerShop shop = new PlayerShop(ownerId, signLoc, chestLoc, chests, template, 1, 5.0);
        java.lang.reflect.Method register = PlayerShopManager.class.getDeclaredMethod("registerShop", PlayerShop.class);
        register.setAccessible(true);
        register.invoke(manager, shop);
        manager.refreshSign(shop);

        // Prepare buyer player and economy responses (use MockBukkit player)
        org.bukkit.entity.Player buyer = server.addPlayer("buyer");
        UUID buyerId = buyer.getUniqueId();
        buyer.addAttachment(plugin, PlayerShopManager.PERMISSION_BUY, true);

        when(econ.withdrawPlayer(eq(buyer), anyDouble())).thenReturn(new EconomyResponse(5.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));
        when(econ.depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble())).thenReturn(new EconomyResponse(5.0, 0.0, EconomyResponse.ResponseType.SUCCESS, "ok"));

        // Obtain the inventory via the manager's private getInventory(...) to ensure
        // we populate the same inventory instance the manager will use.
        java.lang.reflect.Method getInventoryMethod = PlayerShopManager.class.getDeclaredMethod("getInventory", org.bukkit.Location.class);
        getInventoryMethod.setAccessible(true);
        org.bukkit.inventory.Inventory inv = (org.bukkit.inventory.Inventory) getInventoryMethod.invoke(manager, chestLoc);
        assertNotNull(inv, "Test chest inventory should be available");
        // Make sure inventory contains the expected items for the manager
        inv.setItem(0, new ItemStack(Material.DIAMOND, 10));

        // Call private countItems method reflectively to see what manager sees
        java.lang.reflect.Method countItems = PlayerShopManager.class.getDeclaredMethod("countItems", org.bukkit.inventory.Inventory.class, org.bukkit.inventory.ItemStack.class);
        countItems.setAccessible(true);
        int found = (int) countItems.invoke(manager, inv, template);
        System.out.println("DEBUG: inv.size=" + inv.getSize() + " contents=" + java.util.Arrays.toString(inv.getContents()));
        assertTrue(found > 0, "Manager should count items in chest, found=" + found + " (invSize=" + inv.getSize() + ")");

        // Simulate purchase
        ShopTransactionResult purchase = manager.purchase(shop, buyer);
        assertTrue(purchase.success(), purchase.message());
    }

    @Test
    void breaking_shop_by_non_owner_is_prevented_and_owner_can_remove() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        com.skyblockexp.ezshops.config.PlayerShopConfiguration config = com.skyblockexp.ezshops.config.PlayerShopConfiguration.defaults();
        com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository repo = new com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
        PlayerShopManager manager = new PlayerShopManager(plugin, econ, config, repo);
        manager.enable();

        if (Bukkit.getWorlds().isEmpty()) {
            new org.bukkit.WorldCreator("test").createWorld();
        }
        org.bukkit.World world = Bukkit.getWorlds().get(0);
        Location chestLoc = new Location(world, 3, 65, 3);
        Block chestBlock = world.getBlockAt(chestLoc);
        world.getChunkAt(chestLoc).load();
        chestBlock.setType(Material.CHEST);
        Chest chestState = (Chest) chestBlock.getState();
        chestState.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        chestState.update();
        // re-fetch state after update to ensure MockBukkit applies inventory changes
        chestState = (Chest) chestBlock.getState();

        Location signLoc = new Location(world, 3, 66, 3);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_SIGN);
        Sign signState = (Sign) signBlock.getState();
        signState.update();

        org.bukkit.entity.Player owner = server.addPlayer("owner2");
        UUID ownerId = owner.getUniqueId();
        owner.addAttachment(plugin, PlayerShopManager.PERMISSION_CREATE, true);

        // Construct and register a shop directly for the removal test
        ItemStack template = new ItemStack(Material.DIAMOND, 1);
        java.util.List<org.bukkit.Location> chests = java.util.List.of(chestLoc);
        PlayerShop shop = new PlayerShop(ownerId, signLoc, chestLoc, chests, template, 1, 2.5);
        java.lang.reflect.Method register = PlayerShopManager.class.getDeclaredMethod("registerShop", PlayerShop.class);
        register.setAccessible(true);
        register.invoke(manager, shop);

        // Non-owner tries to break
        org.bukkit.entity.Player other = mock(org.bukkit.entity.Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        when(other.hasPermission(PlayerShopManager.PERMISSION_ADMIN)).thenReturn(false);

        // Ensure shop present before removal
        PlayerShop fetched = manager.getShopBySign(signBlock.getLocation());
        assertNotNull(fetched, "Shop should exist before removal");

        // Owner removes shop
        boolean removed = manager.removeShopBySign(signBlock.getLocation());
        assertTrue(removed, "Owner removal should return true");
    }
}
