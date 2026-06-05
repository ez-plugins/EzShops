package com.skyblockexp.ezshops.gui.admin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SetupShopsGui {

    static final String TITLE = ChatColor.BLUE + "" + ChatColor.BOLD + "Shop Setup";

    private static final int SLOT_CORE_SHOPS    = 10;
    private static final int SLOT_QUICK_SELL    = 12;
    private static final int SLOT_PLAYER_SHOPS  = 14;
    private static final int SLOT_STOCK_MARKET  = 16;
    private static final int SLOT_GAME_MODE     = 19;
    private static final int SLOT_CLOSE         = 22;

    private final FileConfiguration config;
    private final Set<String> availableGameModes;

    public SetupShopsGui(FileConfiguration config, Set<String> availableGameModes) {
        this.config = config;
        this.availableGameModes = availableGameModes;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(SLOT_CORE_SHOPS, buildToggleItem(
                Material.EMERALD,
                ChatColor.GREEN + "Core Shops",
                ChatColor.GRAY + "/shop GUI and /shop buy/sell commands",
                isCoreShopsEnabled()));

        inv.setItem(SLOT_QUICK_SELL, buildToggleItem(
                Material.GOLD_INGOT,
                ChatColor.YELLOW + "Quick Sell GUI",
                ChatColor.GRAY + "/sell command for quick selling",
                isQuickSellEnabled()));

        inv.setItem(SLOT_PLAYER_SHOPS, buildToggleItem(
                Material.CHEST,
                ChatColor.YELLOW + "Player Shops",
                ChatColor.GRAY + "Player chest shops",
                isPlayerShopsEnabled()));

        inv.setItem(SLOT_STOCK_MARKET, buildToggleItem(
                Material.DIAMOND,
                ChatColor.AQUA + "Stock Market",
                ChatColor.GRAY + "/stock buy/sell/overview commands",
                isStockMarketEnabled()));

        String currentGameMode = config.getString("game-mode", "prison");
        String gameModeDisplay = currentGameMode.substring(0, 1).toUpperCase() + currentGameMode.substring(1);
        inv.setItem(SLOT_GAME_MODE, buildItem(Material.COMPASS, ChatColor.LIGHT_PURPLE + "Game Mode: " + gameModeDisplay,
                ChatColor.GRAY + "Current: " + gameModeDisplay,
                ChatColor.GRAY + "Click to cycle through modes."));

        inv.setItem(SLOT_CLOSE, buildItem(Material.BARRIER, ChatColor.RED + "Close"));

        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    public boolean isCoreShopsEnabled() {
        return config.getBoolean("categories.enabled", true);
    }

    public boolean isQuickSellEnabled() {
        return config.getBoolean("quick-sell.enabled", true);
    }

    public boolean isPlayerShopsEnabled() {
        return config.getBoolean("player-shops.enabled", true);
    }

    public boolean isStockMarketEnabled() {
        return config.getBoolean("stock.enabled", true);
    }

    public String getCurrentGameMode() {
        return config.getString("game-mode", "prison");
    }

    public String nextGameMode() {
        String current = getCurrentGameMode();
        List<String> modes = new ArrayList<>(availableGameModes);
        int index = modes.indexOf(current.toLowerCase(java.util.Locale.ROOT));
        int nextIndex = (index + 1) % modes.size();
        return modes.get(nextIndex);
    }

    public static ItemStack buildItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildToggleItem(Material enabledMat, String name, String description, boolean enabled) {
        Material mat = enabled ? enabledMat : Material.REDSTONE;
        String status = enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
        List<String> lore = new ArrayList<>();
        lore.add(description);
        lore.add("");
        lore.add(status);
        lore.add(ChatColor.GRAY + "Click to toggle.");
        return buildItem(mat, name, String.join("\n", lore));
    }
}