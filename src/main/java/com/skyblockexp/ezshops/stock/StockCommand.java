package com.skyblockexp.ezshops.stock;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.config.StockMarketConfig;
import com.skyblockexp.ezshops.gui.stock.StockOverviewGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/**
 * Command for interacting with the stock market system.
 */
public class StockCommand implements CommandExecutor {
    private StockOverviewGui stockOverviewGui;
    private final EzShopsPlugin plugin;
    private final StockMarketManager stockMarketManager;
    private final StockMarketConfig stockMarketConfig;
    private final StockMarketFrozenStore frozenStore;
    // Per-player cooldowns (player UUID -> timestamp)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long cooldownMillis;

    public StockCommand(EzShopsPlugin plugin, StockMarketManager stockMarketManager, long cooldownMillis, StockMarketConfig stockMarketConfig, StockMarketFrozenStore frozenStore) {
        this.plugin = plugin;
        this.stockMarketManager = stockMarketManager;
        this.cooldownMillis = cooldownMillis;
        this.stockMarketConfig = stockMarketConfig;
        this.frozenStore = frozenStore;

        // Load GUI config and create StockOverviewGui
        File guiConfigFile = new java.io.File(plugin.getDataFolder(), "stock-gui.yml");
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("Loading stock-gui.yml from: " + guiConfigFile.getAbsolutePath());
        }
        if (!guiConfigFile.exists()) {
            plugin.getLogger().warning("stock-gui.yml does not exist at " + guiConfigFile.getAbsolutePath());
        }
        
        this.stockOverviewGui = new com.skyblockexp.ezshops.gui.stock.StockOverviewGui(
            stockMarketManager,
            stockMarketConfig,
            frozenStore,
            guiConfigFile,
            plugin.isDebugMode()
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/stock <buy|sell|overview> ...");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("overview")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can view the stock market GUI.");
                return true;
            }
            if (!player.hasPermission("ezshops.stock.overview")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to view the stock market.");
                return true;
            }
            // Open GUI with default filter and page 1
            stockOverviewGui.open(player, "all", 1);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can buy or sell stocks.");
            return true;
        }
        if (sub.equals("buy") || sub.equals("sell")) {
            if (!player.hasPermission("ezshops.stock." + sub)) {
                player.sendMessage(ChatColor.RED + "You do not have permission to " + sub + " stocks.");
                return true;
            }
            if (!checkCooldown(player)) {
                player.sendMessage(ChatColor.RED + "You must wait before trading stocks again.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(ChatColor.YELLOW + "/stock " + sub + " <item> <amount>");
                return true;
            }
            String item = args[1].toUpperCase(Locale.ROOT);
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount.");
                return true;
            }
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Amount must be positive.");
                return true;
            }
            if (stockMarketConfig.isBlocked(item)) {
                player.sendMessage(ChatColor.RED + "This item is blocked from stock trading.");
                return true;
            }
            if (frozenStore.isFrozen(item)) {
                player.sendMessage(ChatColor.RED + "This item is currently frozen and cannot be traded.");
                return true;
            }
            Material mat = Material.matchMaterial(item);
            if (mat == null) {
                player.sendMessage(ChatColor.RED + "Unknown item: " + item);
                return true;
            }
            if (sub.equals("buy")) {
                stockMarketManager.updatePrice(mat.name(), amount);
                player.sendMessage(ChatColor.GREEN + "Bought " + amount + " of " + mat.name() + " at " + stockMarketManager.getPrice(mat.name()) + " each.");
            } else {
                stockMarketManager.updatePrice(mat.name(), -amount);
                player.sendMessage(ChatColor.GREEN + "Sold " + amount + " of " + mat.name() + " at " + stockMarketManager.getPrice(mat.name()) + " each.");
            }
            setCooldown(player);
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "/stock <buy|sell|overview> ...");
        return true;
    }

    private boolean checkCooldown(Player player) {
        if (cooldownMillis <= 0) return true;
        pruneCooldowns();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        return last == null || (now - last) >= cooldownMillis;
    }

    private void pruneCooldowns() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID playerId = entry.getKey();
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer == null || !onlinePlayer.isOnline() || (now - entry.getValue()) >= cooldownMillis) {
                iterator.remove();
            }
        }
    }

    private void setCooldown(Player player) {
        if (cooldownMillis > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }
}
