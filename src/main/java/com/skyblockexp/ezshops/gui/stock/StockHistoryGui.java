package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.stock.StockHistoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StockHistoryGui {
    private static final int GUI_SIZE = 54; // 6 rows for better display
    private static final int ENTRIES_PER_PAGE = 28; // 4 rows of entries (7x4)

    private static StockHistoryGuiListener listener;

    public static void setListener(StockHistoryGuiListener l) {
        listener = l;
    }

    public static void open(Player player, String productId, List<StockHistoryManager.PriceEntry> history) {
        open(player, productId, history, 0);
    }

    public static void open(Player player, String productId, List<StockHistoryManager.PriceEntry> history, int page) {
        int totalPages = Math.max(1, (int) Math.ceil(history.size() / (double) ENTRIES_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        
        String itemName = formatProductName(productId);
        String title = ChatColor.GOLD + "Price History: " + ChatColor.YELLOW + itemName;
        Inventory inv = Bukkit.createInventory(player, GUI_SIZE, title);

        // Fill border with decorative glass
        ItemStack border = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        
        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        // Side borders
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, border);
            inv.setItem(row * 9 + 8, border);
        }

        // Show price entries
        if (history.isEmpty()) {
            ItemStack noData = new ItemStack(Material.PAPER);
            ItemMeta noDataMeta = noData.getItemMeta();
            noDataMeta.setDisplayName(ChatColor.RED + "No Price History");
            List<String> noDataLore = new ArrayList<>();
            noDataLore.add(ChatColor.GRAY + "This stock has no recorded");
            noDataLore.add(ChatColor.GRAY + "price history yet.");
            noDataMeta.setLore(noDataLore);
            noData.setItemMeta(noDataMeta);
            inv.setItem(22, noData);
        } else {
            // Show entries from newest to oldest
            int start = page * ENTRIES_PER_PAGE;
            int end = Math.min(start + ENTRIES_PER_PAGE, history.size());
            
            // Calculate price statistics for this page
            double minPrice = Double.MAX_VALUE;
            double maxPrice = Double.MIN_VALUE;
            double avgPrice = 0;
            int count = 0;
            
            for (int i = Math.max(0, history.size() - end); i < Math.min(history.size(), history.size() - start); i++) {
                StockHistoryManager.PriceEntry entry = history.get(i);
                minPrice = Math.min(minPrice, entry.price);
                maxPrice = Math.max(maxPrice, entry.price);
                avgPrice += entry.price;
                count++;
            }
            if (count > 0) {
                avgPrice /= count;
            }
            
            // Display price statistics in first row
            ItemStack stats = new ItemStack(Material.GOLD_INGOT);
            ItemMeta statsMeta = stats.getItemMeta();
            statsMeta.setDisplayName(ChatColor.GOLD + "Price Statistics");
            List<String> statsLore = new ArrayList<>();
            statsLore.add(ChatColor.GRAY + "Current Page Stats:");
            statsLore.add(ChatColor.GREEN + "Average: " + ChatColor.WHITE + String.format("%.2f", avgPrice));
            statsLore.add(ChatColor.RED + "Highest: " + ChatColor.WHITE + String.format("%.2f", maxPrice));
            statsLore.add(ChatColor.AQUA + "Lowest: " + ChatColor.WHITE + String.format("%.2f", minPrice));
            statsMeta.setLore(statsLore);
            stats.setItemMeta(statsMeta);
            inv.setItem(4, stats);
            
            // Display entries (newest first, reading left to right, top to bottom)
            int slot = 10; // Start at row 2, column 2
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm");
            
            for (int i = history.size() - 1 - start; i >= Math.max(0, history.size() - end); i--) {
                StockHistoryManager.PriceEntry entry = history.get(i);
                
                // Determine material based on price trend
                Material entryMat = Material.PAPER;
                ChatColor priceColor = ChatColor.YELLOW;
                
                ItemStack entryItem = new ItemStack(entryMat);
                ItemMeta entryMeta = entryItem.getItemMeta();
                
                String time = dateFormat.format(new Date(entry.timestamp));
                entryMeta.setDisplayName(priceColor + String.format("%.2f", entry.price));
                
                List<String> entryLore = new ArrayList<>();
                entryLore.add(ChatColor.GRAY + time);
                entryMeta.setLore(entryLore);
                entryItem.setItemMeta(entryMeta);
                
                // Place in grid, skipping borders
                while (slot % 9 == 0 || slot % 9 == 8 || slot < 9 || slot >= 45) {
                    slot++;
                }
                
                inv.setItem(slot, entryItem);
                slot++;
            }
        }

        // Navigation buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(ChatColor.GREEN + "◄ Previous Page");
            List<String> prevLore = new ArrayList<>();
            prevLore.add(ChatColor.GRAY + "Page " + page + " of " + totalPages);
            prevMeta.setLore(prevLore);
            prev.setItemMeta(prevMeta);
            inv.setItem(48, prev);
        }
        
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page ►");
            List<String> nextLore = new ArrayList<>();
            nextLore.add(ChatColor.GRAY + "Page " + (page + 2) + " of " + totalPages);
            nextMeta.setLore(nextLore);
            next.setItemMeta(nextMeta);
            inv.setItem(50, next);
        }

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "✖ Close");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "Return to stock browser");
        closeMeta.setLore(closeLore);
        close.setItemMeta(closeMeta);
        inv.setItem(49, close);

        player.openInventory(inv);
        if (listener != null) {
            listener.trackGui(player, productId, history, page);
        }
    }

    private static String formatProductName(String id) {
        if (id == null || id.isEmpty()) return "Unknown";
        String[] parts = id.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
