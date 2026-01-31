package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

public class PlayerShopEdgeCasesTest extends AbstractEzShopsTest {

    @Test
    void createShop_with_null_parameters_fails() throws Exception {
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        var config = com.skyblockexp.ezshops.config.PlayerShopConfiguration.defaults();
        var repo = new com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
        PlayerShopManager m = new PlayerShopManager(plugin, econ, config, repo);

        var res = m.createShop(null, null, null, 1, 1.0);
        assertFalse(res.success());
    }

    @Test
    void normalizeSetup_clamps_quantity_and_price() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        var config = com.skyblockexp.ezshops.config.PlayerShopConfiguration.defaults();
        var repo = new com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
        PlayerShopManager m = new PlayerShopManager(plugin, econ, config, repo);

        var setup = new PlayerShopSetup(100, 0.1d, new ItemStack(Material.DIAMOND, 1));
        var normalized = m.normalizeSetup(setup);
        assertTrue(normalized.quantity() >= config.minQuantity());
        assertTrue(normalized.price() >= Math.max(0.01d, config.minPrice()));
    }

    @Test
    void purchase_without_permission_fails() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        var config = com.skyblockexp.ezshops.config.PlayerShopConfiguration.defaults();
        var repo = new com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
        PlayerShopManager manager = new PlayerShopManager(plugin, econ, config, repo);
        manager.enable();

        if (Bukkit.getWorlds().isEmpty()) new org.bukkit.WorldCreator("test").createWorld();
        var world = Bukkit.getWorlds().get(0);
        Location chestLoc = new Location(world, 10, 65, 10);
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);
        Chest chestState = (Chest) chestBlock.getState();
        chestState.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        chestState.update();

        Location signLoc = new Location(world, 10, 66, 10);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_SIGN);
        Sign signState = (Sign) signBlock.getState();
        signState.update();

        org.bukkit.entity.Player owner = server.addPlayer("powner");
        owner.addAttachment(plugin, PlayerShopManager.PERMISSION_CREATE, true);

        ItemStack template = new ItemStack(Material.DIAMOND, 1);
        List<Location> chests = List.of(chestLoc);
        PlayerShop shop = new PlayerShop(owner.getUniqueId(), signLoc, chestLoc, chests, template, 1, 2.0);

        var reg = PlayerShopManager.class.getDeclaredMethod("registerShop", PlayerShop.class);
        reg.setAccessible(true);
        reg.invoke(manager, shop);

        org.bukkit.entity.Player buyer = server.addPlayer("buyerNoPerm");
        // buyer has no PERMISSION_BUY

        var result = manager.purchase(shop, buyer);
        assertFalse(result.success());
    }

    @Test
    void purchase_withdraw_fails_and_is_reported() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble())).thenReturn(new EconomyResponse(0,0,EconomyResponse.ResponseType.FAILURE,"fail"));
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        var config = com.skyblockexp.ezshops.config.PlayerShopConfiguration.defaults();
        var repo = new com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
        PlayerShopManager manager = new PlayerShopManager(plugin, econ, config, repo);
        manager.enable();

        if (Bukkit.getWorlds().isEmpty()) new org.bukkit.WorldCreator("test").createWorld();
        var world = Bukkit.getWorlds().get(0);
        Location chestLoc = new Location(world, 11, 65, 11);
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);
        Chest chestState = (Chest) chestBlock.getState();
        chestState.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        chestState.update();

        Location signLoc = new Location(world, 11, 66, 11);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_SIGN);
        Sign signState = (Sign) signBlock.getState();
        signState.update();

        org.bukkit.entity.Player owner = server.addPlayer("owner3");
        owner.addAttachment(plugin, PlayerShopManager.PERMISSION_CREATE, true);

        ItemStack template = new ItemStack(Material.DIAMOND, 1);
        List<Location> chests = List.of(chestLoc);
        PlayerShop shop = new PlayerShop(owner.getUniqueId(), signLoc, chestLoc, chests, template, 1, 2.0);

        var reg = PlayerShopManager.class.getDeclaredMethod("registerShop", PlayerShop.class);
        reg.setAccessible(true);
        reg.invoke(manager, shop);

        org.bukkit.entity.Player buyer = server.addPlayer("buyerBadWithdraw");
        buyer.addAttachment(plugin, PlayerShopManager.PERMISSION_BUY, true);

        var res = manager.purchase(shop, buyer);
        assertFalse(res.success());
    }

    @Test
    void purchase_deposit_owner_fails_causes_rollback() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble())).thenReturn(new EconomyResponse(5.0,0,EconomyResponse.ResponseType.SUCCESS,"ok"));
        when(econ.depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble())).thenReturn(new EconomyResponse(0,0,EconomyResponse.ResponseType.FAILURE,"fail"));
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        var config = com.skyblockexp.ezshops.config.PlayerShopConfiguration.defaults();
        var repo = new com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
        PlayerShopManager manager = new PlayerShopManager(plugin, econ, config, repo);
        manager.enable();

        if (Bukkit.getWorlds().isEmpty()) new org.bukkit.WorldCreator("test").createWorld();
        var world = Bukkit.getWorlds().get(0);
        Location chestLoc = new Location(world, 12, 65, 12);
        Block chestBlock = world.getBlockAt(chestLoc);
        chestBlock.setType(Material.CHEST);
        Chest chestState = (Chest) chestBlock.getState();
        chestState.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        chestState.update();

        Location signLoc = new Location(world, 12, 66, 12);
        Block signBlock = world.getBlockAt(signLoc);
        signBlock.setType(Material.OAK_SIGN);
        Sign signState = (Sign) signBlock.getState();
        signState.update();

        org.bukkit.entity.Player owner = server.addPlayer("owner4");
        owner.addAttachment(plugin, PlayerShopManager.PERMISSION_CREATE, true);

        ItemStack template = new ItemStack(Material.DIAMOND, 1);
        List<Location> chests = List.of(chestLoc);
        PlayerShop shop = new PlayerShop(owner.getUniqueId(), signLoc, chestLoc, chests, template, 1, 2.0);

        var reg = PlayerShopManager.class.getDeclaredMethod("registerShop", PlayerShop.class);
        reg.setAccessible(true);
        reg.invoke(manager, shop);

        org.bukkit.entity.Player buyer = server.addPlayer("buyerDepositFail");
        buyer.addAttachment(plugin, PlayerShopManager.PERMISSION_BUY, true);

        var res = manager.purchase(shop, buyer);
        assertFalse(res.success());
    }
}
