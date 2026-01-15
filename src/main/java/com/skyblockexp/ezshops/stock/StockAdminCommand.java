package com.skyblockexp.ezshops.stock;

import com.skyblockexp.ezshops.config.StockMarketConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import java.util.Locale;
import java.util.ArrayList;

/**
 * Admin command handler for stock market management.
 *
 * Subcommands:
 *   /stockadmin set <item> <price>         - Set price (perm: ezshops.stock.admin.set)
 *   /stockadmin reset <item>               - Reset price to base (perm: ezshops.stock.admin.reset)
 *   /stockadmin freeze <item>              - Freeze item (perm: ezshops.stock.admin.freeze)
 *   /stockadmin unfreeze <item>            - Unfreeze item (perm: ezshops.stock.admin.unfreeze)
 *   /stockadmin reload                     - Reload frozen state (perm: ezshops.stock.admin.reload)
 *   /stockadmin listfrozen [page]          - List frozen items, paginated, with who/when (perm: ezshops.stock.admin.listfrozen)
 *   /stockadmin listoverrides [page]       - List override items, paginated (perm: ezshops.stock.admin.listoverrides)
 *
 * All commands require ezshops.stock.admin as a base permission.
 */
public class StockAdminCommand implements CommandExecutor {
    private final StockMarketManager stockMarketManager;
    private final StockMarketFrozenStore frozenStore;
    private final StockMarketConfig stockMarketConfig;

    public StockAdminCommand(StockMarketManager stockMarketManager, StockMarketFrozenStore frozenStore, StockMarketConfig stockMarketConfig) {
        this.stockMarketManager = stockMarketManager;
        this.frozenStore = frozenStore;
        this.stockMarketConfig = stockMarketConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezshops.stock.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/stockadmin <set|reset|freeze|unfreeze|reload|listfrozen|listoverrides> ...");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set":
                if (!sender.hasPermission("ezshops.stock.admin.set")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: ezshops.stock.admin.set");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "/stockadmin set <item> <price>");
                    return true;
                }
                String item = args[1].toUpperCase(Locale.ROOT);
                double price;
                try {
                    price = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid price.");
                    return true;
                }
                if (frozenStore.isFrozen(item)) {
                    sender.sendMessage(ChatColor.RED + "This item is frozen and cannot be changed.");
                    return true;
                }
                stockMarketManager.setPrice(item, price);
                sender.sendMessage(ChatColor.GREEN + "Set price of " + item + " to " + price);
                break;
            case "reset":
                if (!sender.hasPermission("ezshops.stock.admin.reset")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: ezshops.stock.admin.reset");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "/stockadmin reset <item>");
                    return true;
                }
                item = args[1].toUpperCase(Locale.ROOT);
                if (frozenStore.isFrozen(item)) {
                    sender.sendMessage(ChatColor.RED + "This item is frozen and cannot be reset.");
                    return true;
                }
                stockMarketManager.setPrice(item, 100.0); // Reset to base price
                sender.sendMessage(ChatColor.GREEN + "Reset price of " + item + " to 100.0");
                break;
            case "freeze":
                if (!sender.hasPermission("ezshops.stock.admin.freeze")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: ezshops.stock.admin.freeze");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "/stockadmin freeze <item>");
                    return true;
                }
                item = args[1].toUpperCase(Locale.ROOT);
                String by = sender.getName();
                frozenStore.freeze(item, by);
                sender.sendMessage(ChatColor.GREEN + "Froze " + item + ChatColor.GRAY + " by " + by + ChatColor.GRAY + " at " + java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(java.time.Instant.ofEpochMilli(System.currentTimeMillis()).atZone(java.time.ZoneId.systemDefault())));
                break;
            case "unfreeze":
                if (!sender.hasPermission("ezshops.stock.admin.unfreeze")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: ezshops.stock.admin.unfreeze");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "/stockadmin unfreeze <item>");
                    return true;
                }
                item = args[1].toUpperCase(Locale.ROOT);
                frozenStore.unfreeze(item);
                sender.sendMessage(ChatColor.GREEN + "Unfroze " + item);
                break;
            case "reload":
                if (!sender.hasPermission("ezshops.stock.admin.reload")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: ezshops.stock.admin.reload");
                    return true;
                }
                frozenStore.load();
                sender.sendMessage(ChatColor.GREEN + "Reloaded frozen state from disk.");
                break;
            case "listfrozen": {
                if (!sender.hasPermission("ezshops.stock.admin.listfrozen")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: ezshops.stock.admin.listfrozen");
                    return true;
                }
                int page = 1, pageSize = 8;
                if (args.length >= 2) {
                    try { page = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignore) {}
                }
                var all = new ArrayList<>(frozenStore.getAllFrozenMeta());
                int totalPages = Math.max(1, (all.size() + pageSize - 1) / pageSize);
                page = Math.min(page, totalPages);
                sender.sendMessage(ChatColor.GOLD + "--- Frozen Stock Items (" + page + "/" + totalPages + ") ---");
                int start = (page-1)*pageSize, end = Math.min(start+pageSize, all.size());
                for (int i = start; i < end; i++) {
                    var meta = all.get(i);
                    String when = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(java.time.Instant.ofEpochMilli(meta.when).atZone(java.time.ZoneId.systemDefault()));
                    sender.sendMessage(ChatColor.YELLOW + meta.id + ChatColor.GRAY + " by " + meta.by + ChatColor.GRAY + " at " + when);
                }
                if (totalPages > 1) sender.sendMessage(ChatColor.GRAY + "Use /stockadmin listfrozen <page>");
                break;
            }
            case "listoverrides": {
                if (!sender.hasPermission("ezshops.stock.admin.listoverrides")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: ezshops.stock.admin.listoverrides");
                    return true;
                }
                int page = 1, pageSize = 8;
                if (args.length >= 2) {
                    try { page = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignore) {}
                }
                var all = new ArrayList<>(stockMarketConfig.getAllOverrides());
                int totalPages = Math.max(1, (all.size() + pageSize - 1) / pageSize);
                page = Math.min(page, totalPages);
                sender.sendMessage(ChatColor.GOLD + "--- Override Stock Items (" + page + "/" + totalPages + ") ---");
                int start = (page-1)*pageSize, end = Math.min(start+pageSize, all.size());
                for (int i = start; i < end; i++) {
                    var override = all.get(i);
                    sender.sendMessage(ChatColor.AQUA + override.id + ChatColor.WHITE + ": " + ChatColor.GREEN + override.basePrice + ChatColor.GRAY + " (" + override.display + ")");
                }
                if (totalPages > 1) sender.sendMessage(ChatColor.GRAY + "Use /stockadmin listoverrides <page>");
                break;
            }
            default:
                sender.sendMessage(ChatColor.YELLOW + "/stockadmin <set|reset|freeze|unfreeze|reload|listfrozen|listoverrides> ...");
        }
        return true;
    }
}
