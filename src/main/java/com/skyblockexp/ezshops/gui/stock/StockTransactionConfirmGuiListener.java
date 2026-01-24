package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.stock.StockMarketManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for handling clicks in the transaction confirmation GUI.
 */
public class StockTransactionConfirmGuiListener implements Listener {
    private final StockMarketManager stockMarketManager;
    private final AllStocksGui allStocksGui;

    public StockTransactionConfirmGuiListener(StockMarketManager stockMarketManager, AllStocksGui allStocksGui) {
        this.stockMarketManager = stockMarketManager;
        this.allStocksGui = allStocksGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        String strippedTitle = ChatColor.stripColor(title);
        
        // Only handle if this is a transaction confirmation GUI
        if (!strippedTitle.equals("Buy Stock") && !strippedTitle.equals("Sell Stock")) return;
        
        event.setCancelled(true);
        
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        
        // Get the product ID from the display item (slot 13)
        ItemStack displayItem = event.getInventory().getItem(13);
        if (displayItem == null) return;
        String productId = displayItem.getType().name();
        
        StockTransactionConfirmGui.TransactionType type = strippedTitle.equals("Buy Stock") ? 
            StockTransactionConfirmGui.TransactionType.BUY : 
            StockTransactionConfirmGui.TransactionType.SELL;
        
        // Try to handle the click
        StockTransactionConfirmGui.handleClick(player, event.getInventory(), slot, productId, type, 
            stockMarketManager, allStocksGui, 1, "all");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        String strippedTitle = ChatColor.stripColor(title);
        
        // Only handle if this is a transaction confirmation GUI
        if (!strippedTitle.equals("Buy Stock") && !strippedTitle.equals("Sell Stock")) return;
        
        event.setCancelled(true);
    }
}
