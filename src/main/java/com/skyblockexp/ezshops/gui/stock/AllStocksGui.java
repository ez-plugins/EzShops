package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.config.StockMarketConfig;
import com.skyblockexp.ezshops.stock.StockMarketFrozenStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * GUI to show all possible stocks, regardless of player ownership.
 * This is opened via the 'See All Stocks' item in StockOverviewGui.
 * Fully configurable through stock-gui.yml under 'all-stocks-gui' section.
 */
public class AllStocksGui {
    private final StockMarketManager stockMarketManager;
    private final StockMarketConfig stockMarketConfig;
    private final StockMarketFrozenStore frozenStore;
    private final YamlConfiguration guiConfig;
    
    // Layout configuration
    private final int rows;
    private final String title;
    
    // Back button configuration
    private final boolean backButtonEnabled;
    private final int backButtonSlot;
    private final Material backButtonMaterial;
    private final String backButtonDisplayName;
    private final List<String> backButtonLore;
    
    // Filter button configuration
    private final boolean filterButtonEnabled;
    private final int filterButtonSlot;
    private final Material filterButtonMaterial;
    private final String filterButtonDisplayName;
    private final List<String> filterButtonLore;
    
    // Pagination configuration
    private final int previousPageSlot;
    private final Material previousPageMaterial;
    private final String previousPageDisplayName;
    private final int nextPageSlot;
    private final Material nextPageMaterial;
    private final String nextPageDisplayName;
    
    // Filters configuration
    private final List<Map<String, String>> filters;
    
    // Localization
    private final Map<String, String> localization;
    
    private StockOverviewGui stockOverviewGui;

    public AllStocksGui(StockMarketManager stockMarketManager, StockMarketConfig stockMarketConfig, 
                        StockMarketFrozenStore frozenStore, File configFile) {
        this.stockMarketManager = stockMarketManager;
        this.stockMarketConfig = stockMarketConfig;
        this.frozenStore = frozenStore;
        this.guiConfig = YamlConfiguration.loadConfiguration(configFile);
        
        // Load layout configuration
        this.rows = guiConfig.getInt("all-stocks-gui.layout.rows", 6);
        this.title = MessageUtil.translateColors(
            guiConfig.getString("all-stocks-gui.layout.title", "&bAll Stocks"));
        
        // Load back button configuration
        var backSection = guiConfig.getConfigurationSection("all-stocks-gui.back-button");
        if (backSection != null) {
            this.backButtonEnabled = backSection.getBoolean("enabled", true);
            this.backButtonSlot = backSection.getInt("slot", (rows * 9) - 9);
            String matName = backSection.getString("material", "BARRIER");
            Material mat = Material.matchMaterial(matName);
            this.backButtonMaterial = mat != null ? mat : Material.BARRIER;
            this.backButtonDisplayName = MessageUtil.translateColors(
                backSection.getString("display-name", "&cBack to My Stocks"));
            List<String> lore = backSection.getStringList("lore");
            this.backButtonLore = new ArrayList<>();
            for (String l : lore) {
                this.backButtonLore.add(MessageUtil.translateColors(l));
            }
        } else {
            this.backButtonEnabled = true;
            this.backButtonSlot = (rows * 9) - 9;
            this.backButtonMaterial = Material.BARRIER;
            this.backButtonDisplayName = ChatColor.RED + "Back to My Stocks";
            this.backButtonLore = List.of(ChatColor.GRAY + "Return to your stock overview");
        }
        
        // Load filter button configuration
        var filterSection = guiConfig.getConfigurationSection("all-stocks-gui.filter-button");
        if (filterSection != null) {
            this.filterButtonEnabled = filterSection.getBoolean("enabled", true);
            this.filterButtonSlot = filterSection.getInt("slot", (rows * 9) - 8);
            String matName = filterSection.getString("material", "HOPPER");
            Material mat = Material.matchMaterial(matName);
            this.filterButtonMaterial = mat != null ? mat : Material.HOPPER;
            this.filterButtonDisplayName = MessageUtil.translateColors(
                filterSection.getString("display-name", "&aFilter: {filter}"));
            List<String> lore = filterSection.getStringList("lore");
            this.filterButtonLore = new ArrayList<>();
            for (String l : lore) {
                this.filterButtonLore.add(MessageUtil.translateColors(l));
            }
        } else {
            this.filterButtonEnabled = true;
            this.filterButtonSlot = (rows * 9) - 8;
            this.filterButtonMaterial = Material.HOPPER;
            this.filterButtonDisplayName = ChatColor.AQUA + "Filter: {filter}";
            this.filterButtonLore = List.of(ChatColor.GRAY + "Click to cycle filters");
        }
        
        // Load pagination configuration
        var prevSection = guiConfig.getConfigurationSection("all-stocks-gui.pagination.previous");
        if (prevSection != null) {
            this.previousPageSlot = prevSection.getInt("slot", (rows * 9) - 7);
            String matName = prevSection.getString("material", "ARROW");
            Material mat = Material.matchMaterial(matName);
            this.previousPageMaterial = mat != null ? mat : Material.ARROW;
            this.previousPageDisplayName = MessageUtil.translateColors(
                prevSection.getString("display-name", "&aPrevious Page"));
        } else {
            this.previousPageSlot = (rows * 9) - 7;
            this.previousPageMaterial = Material.ARROW;
            this.previousPageDisplayName = ChatColor.GREEN + "Previous Page";
        }
        
        var nextSection = guiConfig.getConfigurationSection("all-stocks-gui.pagination.next");
        if (nextSection != null) {
            this.nextPageSlot = nextSection.getInt("slot", (rows * 9) - 1);
            String matName = nextSection.getString("material", "ARROW");
            Material mat = Material.matchMaterial(matName);
            this.nextPageMaterial = mat != null ? mat : Material.ARROW;
            this.nextPageDisplayName = MessageUtil.translateColors(
                nextSection.getString("display-name", "&aNext Page"));
        } else {
            this.nextPageSlot = (rows * 9) - 1;
            this.nextPageMaterial = Material.ARROW;
            this.nextPageDisplayName = ChatColor.GREEN + "Next Page";
        }
        
        // Load filters
        this.filters = new ArrayList<>();
        List<?> filterList = guiConfig.getList("all-stocks-gui.filters");
        if (filterList != null) {
            for (Object obj : filterList) {
                if (obj instanceof Map<?,?> map) {
                    Map<String, String> f = new HashMap<>();
                    for (Map.Entry<?,?> e : map.entrySet()) {
                        f.put(e.getKey().toString(), e.getValue().toString());
                    }
                    filters.add(f);
                }
            }
        }
        // Default filters if none configured
        if (filters.isEmpty()) {
            Map<String, String> all = new HashMap<>();
            all.put("id", "all");
            all.put("display", "All Items");
            filters.add(all);
            
            Map<String, String> blocks = new HashMap<>();
            blocks.put("id", "blocks");
            blocks.put("display", "Blocks");
            filters.add(blocks);
            
            Map<String, String> items = new HashMap<>();
            items.put("id", "items");
            items.put("display", "Items");
            filters.add(items);
        }
        
        // Load localization
        this.localization = new HashMap<>();
        var locSection = guiConfig.getConfigurationSection("all-stocks-gui.localization");
        if (locSection != null) {
            for (String key : locSection.getKeys(false)) {
                localization.put(key, MessageUtil.translateColors(
                    locSection.getString(key)));
            }
        }
        // Defaults
        if (!localization.containsKey("item-price-format")) {
            localization.put("item-price-format", ChatColor.GRAY + "Current Price: " + ChatColor.GOLD + "{price}");
        }
        if (!localization.containsKey("item-blocked")) {
            localization.put("item-blocked", ChatColor.RED + "Blocked");
        }
        if (!localization.containsKey("item-frozen")) {
            localization.put("item-frozen", ChatColor.AQUA + "Frozen");
        }
    }

    public void setStockOverviewGui(StockOverviewGui stockOverviewGui) {
        this.stockOverviewGui = stockOverviewGui;
    }

    public int getRows() {
        return rows;
    }

    public StockMarketManager getStockMarketManager() {
        return stockMarketManager;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getBackButtonSlot() {
        return backButtonSlot;
    }
    
    public int getFilterButtonSlot() {
        return filterButtonSlot;
    }
    
    public int getPreviousPageSlot() {
        return previousPageSlot;
    }
    
    public int getNextPageSlot() {
        return nextPageSlot;
    }

    public void open(Player player, int page) {
        open(player, page, "all");
    }

    public void open(Player player, int page, String filter) {
        List<String> allIds = getFilteredProductIds(filter);
        // Calculate page size: all slots except the last row
        int pageSize = (rows - 1) * 9;
        int totalPages = Math.max(1, (allIds.size() + pageSize - 1) / pageSize);
        page = Math.max(1, Math.min(page, totalPages));
        
        String displayTitle = MessageUtil.translateColors(title) + " (" + page + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(player, rows * 9, displayTitle);
        
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, allIds.size());
        int itemIndex = 0;
        
        for (int i = start; i < end; i++) {
            String id = allIds.get(i);
            Material mat = Material.matchMaterial(id);
            if (mat == null) continue;
            
            // Use override if present
            StockMarketConfig.OverrideItem override = stockMarketConfig.getOverride(id);
            String displayName;
            double price;
            if (override != null) {
                displayName = override.display != null ? MessageUtil.translateColors(override.display) : id;
                price = override.basePrice;
            } else {
                displayName = id.charAt(0) + id.substring(1).toLowerCase().replace('_', ' ');
                price = stockMarketManager.getPrice(id);
            }
            
            ItemStack item = new ItemStack(mat);
            var meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + displayName);
            
            List<String> lore = new ArrayList<>();
            String priceFormat = localization.get("item-price-format");
            lore.add(priceFormat.replace("{price}", String.format("%.2f", price)));
            // Show bulk totals (preview) for a sensible default amount (64)
            try {
                int bulkAmount = 64;
                double buyTotal = stockMarketManager.estimateBulkTotal(id, bulkAmount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY);
                double sellTotal = stockMarketManager.estimateBulkTotal(id, bulkAmount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.SELL);
                if (buyTotal >= 0 || sellTotal >= 0) {
                    lore.add(ChatColor.GRAY + "Bulk (" + bulkAmount + "): " + ChatColor.GREEN + String.format("%.2f", buyTotal) + ChatColor.GRAY + " / " + ChatColor.RED + String.format("%.2f", sellTotal));
                }
            } catch (Exception ignored) {}
            
            if (stockMarketConfig.isBlocked(id)) {
                lore.add(localization.get("item-blocked"));
            } else if (frozenStore.isFrozen(id)) {
                lore.add(localization.get("item-frozen"));
            } else {
                // Add interactive buy/sell prompts
                lore.add("");
                lore.add(ChatColor.GREEN + "Left-click to buy");
                lore.add(ChatColor.RED + "Right-click to sell");
                lore.add(ChatColor.AQUA + "Shift-click for history");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            // Place item in next available slot, skip last row
            while (itemIndex >= (rows - 1) * 9) itemIndex++;
            if (itemIndex < (rows - 1) * 9) {
                inv.setItem(itemIndex, item);
                itemIndex++;
            }
        }
        
        // Add back button if enabled
        if (backButtonEnabled && backButtonSlot < inv.getSize()) {
            ItemStack back = new ItemStack(backButtonMaterial);
            var backMeta = back.getItemMeta();
            backMeta.setDisplayName(backButtonDisplayName);
            backMeta.setLore(backButtonLore);
            back.setItemMeta(backMeta);
            inv.setItem(backButtonSlot, back);
        }
        
        // Add filter button if enabled
        if (filterButtonEnabled && filterButtonSlot < inv.getSize()) {
            String filterDisplay = getFilterDisplay(filter);
            ItemStack filterItem = new ItemStack(filterButtonMaterial);
            var filterMeta = filterItem.getItemMeta();
            String name = filterButtonDisplayName.replace("{filter}", filterDisplay);
            filterMeta.setDisplayName(name);
            List<String> fLore = new ArrayList<>();
            for (String l : filterButtonLore) {
                fLore.add(l.replace("{filter}", filterDisplay));
            }
            filterMeta.setLore(fLore);
            filterItem.setItemMeta(filterMeta);
            inv.setItem(filterButtonSlot, filterItem);
        }
        
        // Add pagination buttons
        if (page > 1 && previousPageSlot < inv.getSize()) {
            ItemStack prev = new ItemStack(previousPageMaterial);
            var meta = prev.getItemMeta();
            meta.setDisplayName(previousPageDisplayName);
            prev.setItemMeta(meta);
            inv.setItem(previousPageSlot, prev);
        }
        
        if (page < totalPages && nextPageSlot < inv.getSize()) {
            ItemStack next = new ItemStack(nextPageMaterial);
            var meta = next.getItemMeta();
            meta.setDisplayName(nextPageDisplayName);
            next.setItemMeta(meta);
            inv.setItem(nextPageSlot, next);
        }
        
        player.openInventory(inv);
    }

    private String getFilterDisplay(String filterId) {
        for (Map<String, String> filter : filters) {
            if (filter.get("id").equals(filterId)) {
                return filter.get("display");
            }
        }
        return filterId;
    }

    private List<String> getFilteredProductIds(String filter) {
        List<String> filtered = new ArrayList<>();
        for (String id : stockMarketManager.getAllProductIds()) {
            if (filter.equals("all")) {
                filtered.add(id);
            } else if (filter.equals("blocks")) {
                Material mat = Material.matchMaterial(id);
                if (mat != null && mat.isBlock()) {
                    filtered.add(id);
                }
            } else if (filter.equals("items")) {
                Material mat = Material.matchMaterial(id);
                if (mat != null && !mat.isBlock()) {
                    filtered.add(id);
                }
            } else if (filter.startsWith("price:")) {
                // Price range filter: price:min-max
                // This filter can be used programmatically or via future GUI enhancements
                String[] parts = filter.substring(6).split("-");
                try {
                    double min = Double.parseDouble(parts[0]);
                    double max = parts.length > 1 ? Double.parseDouble(parts[1]) : Double.MAX_VALUE;
                    double price = stockMarketManager.getPrice(id);
                    if (price >= min && price <= max) {
                        filtered.add(id);
                    }
                } catch (Exception ignored) {
                    filtered.add(id); // If parsing fails, include the item
                }
            } else {
                // Text-based search: check if item name contains the filter text
                String itemName = id.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
                String lowerFilter = filter.toLowerCase(java.util.Locale.ROOT);
                if (itemName.contains(lowerFilter)) {
                    filtered.add(id);
                }
            }
        }
        return filtered;
    }

    public String getNextFilter(String currentFilter) {
        for (int i = 0; i < filters.size(); i++) {
            if (filters.get(i).get("id").equals(currentFilter)) {
                int nextIndex = (i + 1) % filters.size();
                return filters.get(nextIndex).get("id");
            }
        }
        return "all";
    }

    // Call this from your InventoryClickEvent handler for AllStocksGui
    public boolean handleInventoryClick(InventoryClickEvent event, int currentPage, String currentFilter) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;
        String title = event.getView().getTitle();
        if (!ChatColor.stripColor(title).startsWith(ChatColor.stripColor(this.title))) return false;
        
        int slot = event.getSlot();
        
        // Back button
        if (backButtonEnabled && slot == backButtonSlot) {
            event.setCancelled(true);
            if (stockOverviewGui != null) {
                stockOverviewGui.open(player, "all", 1);
            } else {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Returned to main view");
            }
            return true;
        }
        
        // Filter button
        if (filterButtonEnabled && slot == filterButtonSlot) {
            event.setCancelled(true);
            String nextFilter = getNextFilter(currentFilter);
            open(player, 1, nextFilter); // Reset to page 1 when changing filter
            return true;
        }
        
        // Previous page
        if (slot == previousPageSlot) {
            event.setCancelled(true);
            open(player, currentPage - 1, currentFilter);
            return true;
        }
        
        // Next page
        if (slot == nextPageSlot) {
            event.setCancelled(true);
            open(player, currentPage + 1, currentFilter);
            return true;
        }
        
        return false;
    }
}
