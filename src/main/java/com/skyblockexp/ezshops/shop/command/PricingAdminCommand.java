package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import java.util.Locale;

/**
 * Admin command handler for resetting dynamic pricing entries.
 *
 * Subcommands:
 *   /pricingadmin reset <item>    - Reset dynamic pricing for an item (perm: ezshops.pricing.admin.reset)
 *   /pricingadmin resetall        - Reset all dynamic pricing entries (perm: ezshops.pricing.admin.resetall)
 */
public class PricingAdminCommand implements CommandExecutor, TabCompleter {
    private final ShopPricingManager pricingManager;
    private final ShopMessageConfiguration.CommandMessages.PricingAdminCommandMessages messages;

    public PricingAdminCommand(ShopPricingManager pricingManager, ShopMessageConfiguration.CommandMessages.PricingAdminCommandMessages messages) {
        this.pricingManager = pricingManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezshops.pricing.admin")) {
            sender.sendMessage(messages.noPermission());
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(messages.usage());
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "set":
                if (!sender.hasPermission("ezshops.pricing.admin.set")) {
                    sender.sendMessage(messages.lackPermission("ezshops.pricing.admin.set"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "/pricingadmin set <item> <price>");
                    return true;
                }
                String item = args[1];
                String itemNorm = item;
                Material mat = Material.matchMaterial(item, false);
                if (mat != null) itemNorm = mat.name();
                double price;
                try {
                    price = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid price.");
                    return true;
                }
                boolean setOk = pricingManager.setPrice(itemNorm, price);
                if (setOk) {
                    sender.sendMessage(messages.setSuccess(itemNorm, Double.toString(price)));
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to set price for " + itemNorm);
                }
                break;
            case "reset":
                if (!sender.hasPermission("ezshops.pricing.admin.reset")) {
                    sender.sendMessage(messages.lackPermission("ezshops.pricing.admin.reset"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(messages.usage());
                    return true;
                }
                String key = args[1];
                Material m = Material.matchMaterial(key, false);
                if (m != null) key = m.name();
                boolean ok = pricingManager.resetDynamicPricing(key);
                if (ok) {
                    sender.sendMessage(messages.resetSuccess(key));
                } else {
                    sender.sendMessage(messages.resetFailed(key));
                }
                break;
            case "resetall":
                if (!sender.hasPermission("ezshops.pricing.admin.resetall")) {
                    sender.sendMessage(messages.lackPermission("ezshops.pricing.admin.resetall"));
                    return true;
                }
                int count = pricingManager.resetAllDynamicPricing();
                sender.sendMessage(messages.resetAllSuccess(count));
                break;
            case "disable":
                if (!sender.hasPermission("ezshops.pricing.admin.disable")) {
                    sender.sendMessage(messages.lackPermission("ezshops.pricing.admin.disable"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "/pricingadmin disable <buy|sell> <item>");
                    return true;
                }
                String action = args[1].toLowerCase(Locale.ROOT);
                String targetKey = args[2];
                Material targetMat = Material.matchMaterial(targetKey, false);
                if (targetMat != null) targetKey = targetMat.name();
                boolean result = false;
                if ("buy".equals(action)) {
                    result = pricingManager.disableBuy(targetKey);
                } else if ("sell".equals(action)) {
                    result = pricingManager.disableSell(targetKey);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown action: must be 'buy' or 'sell'.");
                    return true;
                }
                if (result) {
                    sender.sendMessage(messages.disableSuccess(action, targetKey));
                } else {
                    sender.sendMessage(messages.disableFailed(action, targetKey));
                }
                break;
            case "list":
                if (!sender.hasPermission("ezshops.pricing.admin.list")) {
                    sender.sendMessage(messages.lackPermission("ezshops.pricing.admin.list"));
                    return true;
                }
                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Math.max(1, Integer.parseInt(args[1]));
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "Invalid page number.");
                        return true;
                    }
                }
                java.util.List<String> keys = new java.util.ArrayList<>(pricingManager.getConfiguredPriceKeys());
                int pageSize = 10;
                int totalPages = Math.max(1, (int) Math.ceil((double) keys.size() / pageSize));
                if (page > totalPages) page = totalPages;
                sender.sendMessage(messages.listHeader(page, totalPages));
                int start = (page - 1) * pageSize;
                int end = Math.min(keys.size(), start + pageSize);
                for (int i = start; i < end; i++) {
                    String k = keys.get(i);
                    java.util.Optional<com.skyblockexp.ezshops.shop.ShopPrice> sp = pricingManager.getPrice(k);
                    String buy = "N/A";
                    String sell = "N/A";
                    if (sp.isPresent()) {
                        com.skyblockexp.ezshops.shop.ShopPrice p = sp.get();
                        buy = p.canBuy() ? Double.toString(p.buyPrice()) : "N/A";
                        sell = p.canSell() ? Double.toString(p.sellPrice()) : "N/A";
                    }
                    sender.sendMessage(messages.listEntry(k, buy, sell));
                }
                break;
            default:
                sender.sendMessage(messages.usage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ezshops.pricing.admin")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("ezshops.pricing.admin.reset")) subs.add("reset");
            if (sender.hasPermission("ezshops.pricing.admin.resetall")) subs.add("resetall");
            if (sender.hasPermission("ezshops.pricing.admin.set")) subs.add("set");
            return filterPrefix(args[0], subs);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("reset".equals(sub) || "set".equals(sub)) {
                Set<String> items = new java.util.LinkedHashSet<>();
                for (org.bukkit.Material m : pricingManager.getConfiguredMaterials()) {
                    items.add(m.name().toLowerCase(Locale.ENGLISH));
                }
                items.addAll(pricingManager.getConfiguredPriceKeys());
                return filterPrefix(args[1], new ArrayList<>(items));
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("set".equals(sub)) {
                String target = args[1];
                Material mat = Material.matchMaterial(target, false);
                Optional<com.skyblockexp.ezshops.shop.ShopPrice> priceOpt = Optional.empty();
                if (mat != null) {
                    priceOpt = pricingManager.getPrice(mat);
                }
                if (priceOpt.isEmpty()) {
                    priceOpt = pricingManager.getPrice(target);
                }
                if (priceOpt.isPresent()) {
                    com.skyblockexp.ezshops.shop.ShopPrice p = priceOpt.get();
                    List<String> out = new ArrayList<>();
                    if (p.canBuy() && p.buyPrice() >= 0.0D) out.add(Double.toString(p.buyPrice()));
                    if (p.canSell() && p.sellPrice() >= 0.0D) out.add(Double.toString(p.sellPrice()));
                    return filterPrefix(args[2], out);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(String prefix, List<String> options) {
        if (prefix == null || prefix.isEmpty()) return options;
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(opt);
            }
        }
        return result;
    }
}
