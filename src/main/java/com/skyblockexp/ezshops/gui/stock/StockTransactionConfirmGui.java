package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.stock.StockManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation GUI for buying or selling stocks.
 */
public class StockTransactionConfirmGui {
    private static final int GUI_SIZE = 27;
    
    public enum TransactionType {
        BUY, SELL
    }
    
    public static void open(Player player, String productId, TransactionType type, 
                           StockMarketManager stockMarketManager, 
                           AllStocksGui returnGui, int returnPage, String returnFilter) {
        openWithContext(player, productId, type, stockMarketManager, returnGui, null, returnPage, returnFilter);
    }
    
    public static void openWithOverview(Player player, String productId, TransactionType type, 
                                       StockMarketManager stockMarketManager, 
                                       StockOverviewGui returnOverview) {
        openWithContext(player, productId, type, stockMarketManager, null, returnOverview, 1, "all");
    }
    
    private static void openWithContext(Player player, String productId, TransactionType type, 
                           StockMarketManager stockMarketManager, 
                           AllStocksGui returnGui, StockOverviewGui returnOverview, 
                           int returnPage, String returnFilter) {
        double price = stockMarketManager.getPrice(productId);
        int ownedAmount = StockManager.getPlayerStockAmount(player, productId);
        
        String title = type == TransactionType.BUY ? 
            ChatColor.GREEN + "Buy Stock" : 
            ChatColor.RED + "Sell Stock";
        
        Inventory inv = Bukkit.createInventory(player, GUI_SIZE, title);
        
        // Fill background with gray glass
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < GUI_SIZE; i++) {
            inv.setItem(i, filler);
        }
        
        // Display the item being traded
        Material mat = Material.matchMaterial(productId);
        if (mat != null) {
            ItemStack displayItem = new ItemStack(mat);
            ItemMeta meta = displayItem.getItemMeta();
            String displayName = productId.charAt(0) + productId.substring(1).toLowerCase().replace('_', ' ');
            meta.setDisplayName(ChatColor.YELLOW + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Price per unit: " + ChatColor.GOLD + String.format("%.2f", price));
            if (type == TransactionType.SELL) {
                lore.add(ChatColor.GRAY + "You own: " + ChatColor.GREEN + ownedAmount);
            }
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
            inv.setItem(13, displayItem);
        }
        
        // Buy/Sell amounts: 1, 8, 16, 32, 64
        int[] amounts = {1, 8, 16, 32, 64};
        int[] slots = {10, 11, 12, 14, 15};
        
        for (int i = 0; i < amounts.length; i++) {
            int amount = amounts[i];
            double totalCost = price * amount;
            
            // Skip if selling more than owned
            if (type == TransactionType.SELL && amount > ownedAmount) {
                continue;
            }
            
            Material buttonMat = type == TransactionType.BUY ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            ItemStack button = new ItemStack(buttonMat, amount);
            ItemMeta buttonMeta = button.getItemMeta();
            
            if (type == TransactionType.BUY) {
                buttonMeta.setDisplayName(ChatColor.GREEN + "Buy " + amount);
                List<String> buttonLore = new ArrayList<>();
                buttonLore.add(ChatColor.GRAY + "Total cost: " + ChatColor.GOLD + String.format("%.2f", totalCost));
                buttonLore.add("");
                buttonLore.add(ChatColor.YELLOW + "Click to confirm purchase");
                buttonMeta.setLore(buttonLore);
            } else {
                buttonMeta.setDisplayName(ChatColor.RED + "Sell " + amount);
                List<String> buttonLore = new ArrayList<>();
                buttonLore.add(ChatColor.GRAY + "Total value: " + ChatColor.GOLD + String.format("%.2f", totalCost));
                buttonLore.add("");
                buttonLore.add(ChatColor.YELLOW + "Click to confirm sale");
                buttonMeta.setLore(buttonLore);
            }
            
            button.setItemMeta(buttonMeta);
            inv.setItem(slots[i], button);
        }
        
        // Cancel button
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Return to stock browser");
        cancelMeta.setLore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        inv.setItem(22, cancel);
        
        player.openInventory(inv);
    }
    
    // Backward compatibility wrapper for existing code
    public static boolean handleClick(Player player, Inventory inv, int slot, String productId,
                                     TransactionType type, StockMarketManager stockMarketManager,
                                     AllStocksGui returnGui, int returnPage, String returnFilter) {
        return handleClick(player, inv, slot, productId, type, stockMarketManager, returnGui, null, returnPage, returnFilter);
    }
    
    public static boolean handleClick(Player player, Inventory inv, int slot, String productId,
                                     TransactionType type, StockMarketManager stockMarketManager,
                                     AllStocksGui returnGui, StockOverviewGui returnOverview, 
                                     int returnPage, String returnFilter) {
        // Check if this is a transaction confirmation GUI
        String title = ChatColor.stripColor(inv.getType().toString());
        
        // Cancel button
        if (slot == 22) {
            player.closeInventory();
            if (returnGui != null) {
                returnGui.open(player, returnPage, returnFilter);
            } else if (returnOverview != null) {
                returnOverview.open(player, returnFilter, returnPage);
            }
            return true;
        }
        
        // Transaction buttons
        int[] slots = {10, 11, 12, 14, 15};
        int[] amounts = {1, 8, 16, 32, 64};
        
        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i]) {
                int amount = amounts[i];
                processTransaction(player, productId, amount, type, stockMarketManager);
                player.closeInventory();
                if (returnGui != null) {
                    returnGui.open(player, returnPage, returnFilter);
                } else if (returnOverview != null) {
                    returnOverview.open(player, returnFilter, returnPage);
                }
                return true;
            }
        }
        
        return false;
    }
    
    private static void processTransaction(Player player, String productId, int amount, 
                                          TransactionType type, StockMarketManager stockMarketManager) {
        double price = stockMarketManager.getPrice(productId);
        double totalCost = price * amount;
        
        RegisteredServiceProvider<Economy> rsp = player.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            player.sendMessage(ChatColor.RED + "Economy system not available.");
            return;
        }
        Economy econ = rsp.getProvider();
        
        if (type == TransactionType.BUY) {
            // Check if player has enough money
            if (econ.getBalance(player) < totalCost) {
                player.sendMessage(ChatColor.RED + "Insufficient funds! You need " + String.format("%.2f", totalCost) + " but only have " + String.format("%.2f", econ.getBalance(player)));
                return;
            }
            
            // Deduct money and add stock
            econ.withdrawPlayer(player, totalCost);
            StockManager.addPlayerStock(player, productId, amount);
            stockMarketManager.updatePrice(productId, amount);
            
            player.sendMessage(ChatColor.GREEN + "Successfully bought " + amount + " " + productId + " for " + String.format("%.2f", totalCost));
        } else {
            // Check if player has enough stock
            int owned = StockManager.getPlayerStockAmount(player, productId);
            if (owned < amount) {
                player.sendMessage(ChatColor.RED + "You don't have enough stock to sell! You have " + owned + " but tried to sell " + amount);
                return;
            }
            
            // Remove stock and add money
            boolean removed = StockManager.removePlayerStock(player, productId, amount);
            if (removed) {
                econ.depositPlayer(player, totalCost);
                stockMarketManager.updatePrice(productId, -amount);
                
                player.sendMessage(ChatColor.GREEN + "Successfully sold " + amount + " " + productId + " for " + String.format("%.2f", totalCost));
            } else {
                player.sendMessage(ChatColor.RED + "Failed to sell stock. Please try again.");
            }
        }
    }
}
