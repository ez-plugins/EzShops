package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.stock.StockHistoryManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockHistoryGuiListener implements Listener {
    private final Plugin plugin;
    // Track open GUIs: player UUID -> context
    private final Map<String, GuiContext> openGuis = new HashMap<>();

    public StockHistoryGuiListener(Plugin plugin) {
        this.plugin = plugin;
    }

    // Call this when opening the GUI to track context
    public void trackGui(Player player, String productId, List<StockHistoryManager.PriceEntry> history, int page) {
        openGuis.put(player.getName(), new GuiContext(productId, history, page));
    }

    public void untrackGui(Player player) {
        openGuis.remove(player.getName());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        GuiContext ctx = openGuis.get(player.getName());
        if (ctx == null) return;
        Inventory inv = event.getInventory();
        if (!event.getView().getTitle().contains("Price History")) return;
        event.setCancelled(true); // Prevent taking items
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        int slot = event.getRawSlot();
        // Navigation
        if (slot == 48 && name.contains("Previous Page")) {
            StockHistoryGui.open(player, ctx.productId, ctx.history, ctx.page - 1);
            trackGui(player, ctx.productId, ctx.history, ctx.page - 1);
        } else if (slot == 50 && name.contains("Next Page")) {
            StockHistoryGui.open(player, ctx.productId, ctx.history, ctx.page + 1);
            trackGui(player, ctx.productId, ctx.history, ctx.page + 1);
        } else if (slot == 49 && name.contains("Close")) {
            player.closeInventory();
            untrackGui(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        GuiContext ctx = openGuis.get(player.getName());
        if (ctx == null) return;
        if (!event.getView().getTitle().contains("Price History")) return;
        event.setCancelled(true);
    }

    private static class GuiContext {
        final String productId;
        final List<StockHistoryManager.PriceEntry> history;
        final int page;
        GuiContext(String productId, List<StockHistoryManager.PriceEntry> history, int page) {
            this.productId = productId;
            this.history = history;
            this.page = page;
        }
    }
}
