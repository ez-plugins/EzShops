package com.skyblockexp.ezshops.gui.admin;

import com.skyblockexp.ezshops.EzShopsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class SetupShopsGuiListener implements Listener {

    public static final String PERMISSION = "ezshops.setupshops";

    private final EzShopsPlugin plugin;
    private final SetupShopsGui gui;

    public SetupShopsGuiListener(EzShopsPlugin plugin, SetupShopsGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(SetupShopsGui.TITLE)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this GUI.");
            return;
        }

        switch (slot) {
            case 10 -> toggleCoreShops(player);
            case 12 -> toggleQuickSell(player);
            case 14 -> togglePlayerShops(player);
            case 16 -> toggleStockMarket(player);
            case 19 -> cycleGameMode(player);
            case 22 -> player.closeInventory();
        }
    }

    private void toggleCoreShops(Player player) {
        boolean newState = !gui.isCoreShopsEnabled();
        plugin.getConfig().set("categories.enabled", newState);
        // When disabling categories, also disable single-list mode since it requires categories to be disabled first
        if (!newState) {
            plugin.getConfig().set("categories.single-list-when-disabled", false);
        }
        plugin.saveConfig();
        plugin.getCoreShopComponent().reloadFeatures();
        gui.open(player);
        player.sendMessage(ChatColor.GREEN + "Toggled core-shops to " + (newState ? "enabled" : "disabled") + ".");
    }

    private void toggleQuickSell(Player player) {
        try {
            boolean newState = !gui.isQuickSellEnabled();
            plugin.getConfig().set("quick-sell.enabled", newState);
            plugin.saveConfig();
            plugin.getCoreShopComponent().reloadFeatures();
            gui.open(player);
            player.sendMessage(ChatColor.GREEN + "Toggled quick-sell to " + (newState ? "enabled" : "disabled") + ".");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to toggle quick-sell: " + e.getMessage());
        }
    }

    private void togglePlayerShops(Player player) {
        try {
            boolean newState = !gui.isPlayerShopsEnabled();
            plugin.getConfig().set("player-shops.enabled", newState);
            plugin.saveConfig();
            plugin.getPlayerShopComponent().reload();
            gui.open(player);
            player.sendMessage(ChatColor.GREEN + "Toggled player-shops to " + (newState ? "enabled" : "disabled") + ".");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to toggle player-shops: " + e.getMessage());
        }
    }

    private void toggleStockMarket(Player player) {
        try {
            boolean newState = !gui.isStockMarketEnabled();
            plugin.getConfig().set("stock.enabled", newState);
            plugin.saveConfig();
            plugin.getStockComponent().reload();
            gui.open(player);
            player.sendMessage(ChatColor.GREEN + "Toggled stock-market to " + (newState ? "enabled" : "disabled") + ".");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to toggle stock-market: " + e.getMessage());
        }
    }

    private void cycleGameMode(Player player) {
        String nextMode = gui.nextGameMode();
        plugin.getConfig().set("game-mode", nextMode);
        try {
            plugin.saveConfig();
            gui.open(player);
            String display = nextMode.substring(0, 1).toUpperCase() + nextMode.substring(1);
            player.sendMessage(ChatColor.GREEN + "Game mode set to " + display + ". Use /shop reload to apply changes.");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to set game mode: " + e.getMessage());
        }
    }
}