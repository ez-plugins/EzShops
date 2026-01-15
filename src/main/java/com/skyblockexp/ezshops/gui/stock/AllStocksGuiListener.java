package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.config.StockMarketConfig;
import com.skyblockexp.ezshops.stock.StockMarketFrozenStore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;

/**
 * Handles clicks in the AllStocksGui, including pagination.
 */
public class AllStocksGuiListener implements Listener {
    private final StockMarketManager stockMarketManager;
    private final StockMarketConfig stockMarketConfig;
    private final StockMarketFrozenStore frozenStore;
    private final File configFile;
    private final StockOverviewGui stockOverviewGui;

    public AllStocksGuiListener(StockMarketManager stockMarketManager, 
                                StockMarketConfig stockMarketConfig,
                                StockMarketFrozenStore frozenStore,
                                File configFile,
                                StockOverviewGui stockOverviewGui) {
        this.stockMarketManager = stockMarketManager;
        this.stockMarketConfig = stockMarketConfig;
        this.frozenStore = frozenStore;
        this.configFile = configFile;
        this.stockOverviewGui = stockOverviewGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        
        // Check if this is a transaction confirmation GUI
        if (ChatColor.stripColor(title).equals("Buy Stock") || ChatColor.stripColor(title).equals("Sell Stock")) {
            event.setCancelled(true);
            // This will be handled by a separate listener registered for the confirmation GUI
            return;
        }
        
        // Check if this is an AllStocksGui by title pattern
        String strippedTitle = ChatColor.stripColor(title);
        if (!strippedTitle.startsWith(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&bAll Stocks")))) return;
        
        // Cancel all clicks in the GUI to prevent item duplication/theft
        event.setCancelled(true);
        
        // Parse current page from title (format: ... (page/total))
        int page = 1;
        int idx1 = title.lastIndexOf('(');
        int idx2 = title.lastIndexOf('/') - 1;
        if (idx1 >= 0 && idx2 > idx1) {
            try {
                String pageStr = title.substring(idx1 + 1, idx2).trim();
                page = Integer.parseInt(pageStr);
            } catch (Exception ignored) {}
        }
        
        // Create a temporary AllStocksGui to get configuration
        AllStocksGui tempGui = new AllStocksGui(stockMarketManager, stockMarketConfig, frozenStore, configFile);
        tempGui.setStockOverviewGui(stockOverviewGui);
        
        // Parse current filter from the filter button (if present)
        String currentFilter = "all";
        int filterSlot = tempGui.getFilterButtonSlot();
        ItemStack filterButton = event.getInventory().getItem(filterSlot);
        if (filterButton != null && filterButton.hasItemMeta() && filterButton.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(filterButton.getItemMeta().getDisplayName());
            if (displayName.startsWith("Filter: ")) {
                String filterValue = displayName.substring(8).toLowerCase();
                if (filterValue.equals("blocks") || filterValue.equals("items") || filterValue.equals("all items")) {
                    currentFilter = filterValue.replace("all items", "all");
                }
            }
        }
        
        if (tempGui.handleInventoryClick(event, page, currentFilter)) {
            return;
        }
        
        // Handle stock item clicks (buy/sell/history)
        int slot = event.getSlot();
        int rows = tempGui.getRows();
        int size = rows * 9;
        // Ignore clicks on control buttons (last row)
        if (slot >= (size - 9)) return;
        // Get the item and its type
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getType() == Material.AIR) return;
        
        // Get the product id from the item type
        String productId = clicked.getType().name();
        
        // Check if item has lore indicating it's blocked or frozen
        if (clicked.getItemMeta().hasLore()) {
            List<String> lore = clicked.getItemMeta().getLore();
            for (String line : lore) {
                if (ChatColor.stripColor(line).equals("Blocked") || ChatColor.stripColor(line).equals("Frozen")) {
                    player.sendMessage(ChatColor.RED + "This item cannot be traded.");
                    return;
                }
            }
        }
        
        // Handle different click types
        if (event.isShiftClick()) {
            // Shift-click: show history
            var historyManager = stockMarketManager.getHistoryManager();
            var history = historyManager.getHistory(productId);
            com.skyblockexp.ezshops.gui.stock.StockHistoryGui.open(player, productId, history);
        } else if (event.isLeftClick()) {
            // Left-click: open buy confirmation
            StockTransactionConfirmGui.open(player, productId, StockTransactionConfirmGui.TransactionType.BUY,
                stockMarketManager, tempGui, page, currentFilter);
        } else if (event.isRightClick()) {
            // Right-click: open sell confirmation
            StockTransactionConfirmGui.open(player, productId, StockTransactionConfirmGui.TransactionType.SELL,
                stockMarketManager, tempGui, page, currentFilter);
        }
    }
}
