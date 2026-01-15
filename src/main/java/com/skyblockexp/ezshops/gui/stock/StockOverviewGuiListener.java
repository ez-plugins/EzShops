package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.stock.StockManager;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class StockOverviewGuiListener implements Listener {
    private final StockMarketManager stockMarketManager;
    private final StockOverviewGui stockOverviewGui;

    public StockOverviewGuiListener(StockMarketManager stockMarketManager, StockOverviewGui stockOverviewGui) {
        this.stockMarketManager = stockMarketManager;
        this.stockOverviewGui = stockOverviewGui;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().contains(stockOverviewGui.getTitle())) return;

        // Handle 'See All Stocks' item click
        if (stockOverviewGui.handleInventoryClick(event)) {
            // AllStocksGui opened, nothing else to do
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        // Try to extract the item name from the display name (handle legacy and custom names)
        String itemName = displayName;
        // If displayName contains a price or other info, split by known separator
        if (displayName.contains(" - ")) {
            itemName = displayName.split(" - ")[0].trim();
        } else if (displayName.contains(":")) {
            itemName = displayName.split(":")[0].trim();
        } else if (displayName.contains(" ")) {
            itemName = displayName.split(" ")[0].trim();
        }

        // Right-click: show history. Left-click: open sell confirmation (if owned)
        if (event.isRightClick()) {
            stockOverviewGui.showHistory(player, itemName);
            event.setCancelled(true);
            return;
        }

        // Left-click: open sell confirmation GUI
        // Get the actual material name from the clicked item
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            String productId = clickedItem.getType().name();
            int owned = StockManager.getPlayerStockAmount(player, productId);
            if (owned > 0) {
                // Open sell confirmation GUI with StockOverviewGui as return context
                player.closeInventory();
                StockTransactionConfirmGui.openWithOverview(player, productId, 
                    StockTransactionConfirmGui.TransactionType.SELL,
                    stockMarketManager, stockOverviewGui);
            }
        }
    }
}
