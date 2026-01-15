package com.skyblockexp.ezshops.gui.stock;

import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.config.StockMarketConfig;
import com.skyblockexp.ezshops.stock.StockMarketFrozenStore;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import java.io.File;
import java.util.*;

public class StockOverviewGui {
    private final StockMarketManager stockMarketManager;
    private final StockMarketConfig stockMarketConfig;
    private final StockMarketFrozenStore frozenStore;
    private final YamlConfiguration guiConfig;
    private final File guiConfigFile;
    private final int rows;
    private final String title;
    private final List<Map<String, String>> filters;
    private final Map<String, String> localization;
    private final Map<String, String> permissions;
    private final boolean debugMode;
    // Configurable 'See All Stocks' item
    private final boolean seeAllStocksEnabled;
    private final int seeAllStocksSlot;
    private final Material seeAllStocksMaterial;
    private final String seeAllStocksDisplayName;
    private final List<String> seeAllStocksLore;

    public StockOverviewGui(StockMarketManager stockMarketManager, StockMarketConfig stockMarketConfig, StockMarketFrozenStore frozenStore, File configFile, boolean debugMode) {
        this.stockMarketManager = stockMarketManager;
        this.stockMarketConfig = stockMarketConfig;
        this.frozenStore = frozenStore;
        this.guiConfigFile = configFile;
        this.debugMode = debugMode;
        this.guiConfig = YamlConfiguration.loadConfiguration(configFile);
        this.rows = guiConfig.getInt("layout.rows", 6);
        this.title = MessageUtil.translateColors(guiConfig.getString("layout.title", "Stock Market Overview"));
        this.filters = new ArrayList<>();
        List<?> filterList = guiConfig.getList("filters");
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
        this.localization = new HashMap<>();
        if (guiConfig.isConfigurationSection("localization")) {
            Set<String> keys = guiConfig.getConfigurationSection("localization").getKeys(false);
            for (String key : keys) {
                localization.put(key, MessageUtil.translateColors(guiConfig.getString("localization." + key)));
            }
        }
        // Ensure 'item-format' is present with a default if missing
        if (!localization.containsKey("item-format")) {
            localization.put("item-format", "&e{item} &7- &6{price}");
        }
        this.permissions = new HashMap<>();
        if (guiConfig.isConfigurationSection("permissions")) {
            for (String key : guiConfig.getConfigurationSection("permissions").getKeys(false)) {
                permissions.put(key, guiConfig.getString("permissions." + key));
            }
        }

        // Read 'see-all-stocks' config section
        if (guiConfig.isConfigurationSection("see-all-stocks")) {
            var section = guiConfig.getConfigurationSection("see-all-stocks");
            this.seeAllStocksEnabled = section.getBoolean("enabled", false);
            this.seeAllStocksSlot = section.getInt("slot", (rows * 9) - 1); // default: last slot
            String matName = section.getString("material", "BOOK");
            Material mat = Material.matchMaterial(matName);
            this.seeAllStocksMaterial = mat != null ? mat : Material.BOOK;
            this.seeAllStocksDisplayName = MessageUtil.translateColors(section.getString("display-name", "&bSee All Stocks"));
            List<String> loreList = section.getStringList("lore");
            if (loreList == null || loreList.isEmpty()) {
                this.seeAllStocksLore = Collections.singletonList(ChatColor.GRAY + "View all available stocks");
            } else {
                List<String> colored = new ArrayList<>();
                for (String l : loreList) colored.add(MessageUtil.translateColors(l));
                this.seeAllStocksLore = colored;
            }
        } else {
            this.seeAllStocksEnabled = false;
            this.seeAllStocksSlot = (rows * 9) - 1;
            this.seeAllStocksMaterial = Material.BOOK;
            this.seeAllStocksDisplayName = MessageUtil.translateColors("&bSee All Stocks");
            this.seeAllStocksLore = Collections.singletonList(ChatColor.GRAY + "View all available stocks");
        }
    }

    public void open(Player player, String filter, int page) {
        if (!player.hasPermission(permissions.getOrDefault("view", "ezshops.stock.view"))) {
            player.sendMessage(ChatColor.RED + "You do not have permission to view the stock market.");
            return;
        }
        // Get only owned stocks for main view
        List<String> ownedStocks = getPlayerOwnedStocks(player, filter);
        int pageSize = (rows * 9) - 9;
        int totalPages = Math.max(1, (ownedStocks.size() + pageSize - 1) / pageSize);
        page = Math.max(1, Math.min(page, totalPages));
        
        String displayTitle = ownedStocks.isEmpty() ? title + " (No Stocks)" : title + " (" + page + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(player, rows * 9, displayTitle);
        
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, ownedStocks.size());
        int itemIndex = 0;
        for (int i = start; i < end; i++) {
            String id = ownedStocks.get(i);
            Material mat = Material.matchMaterial(id);
            if (mat == null) continue;
            // Use override if present
            StockMarketConfig.OverrideItem override = stockMarketConfig.getOverride(id);
            String displayName;
            double price = stockMarketManager.getPrice(id);
            if (override != null) {
                displayName = override.display != null ? MessageUtil.translateColors(override.display) : id;
            } else {
                displayName = id.charAt(0) + id.substring(1).toLowerCase().replace('_', ' ');
            }
            int owned = getPlayerStockAmount(player, id);
            // Worth should always use the current price (not just override.basePrice)
            double worth = owned * stockMarketManager.getPrice(id);
            ItemStack item = new ItemStack(mat);
            var meta = item.getItemMeta();
            String itemFormat = localize("item-format");
            // No need to warn the player, always present due to default above
            String guiDisplayName = MessageUtil.translateColors(itemFormat)
                .replace("{item}", displayName)
                .replace("{price}", String.format("%.2f", price));
            meta.setDisplayName(guiDisplayName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GREEN + "Owned: " + ChatColor.GOLD + owned);
            lore.add(ChatColor.GRAY + "Total Value: " + ChatColor.GOLD + String.format("%.2f", worth));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Left-click to sell");
            lore.add(ChatColor.AQUA + "Right-click for history");
            meta.setLore(lore);
            item.setItemMeta(meta);
            // Place item in next available slot, skip seeAllStocksSlot if enabled
            while (seeAllStocksEnabled && itemIndex == seeAllStocksSlot) itemIndex++;
            if (itemIndex < inv.getSize()) {
                inv.setItem(itemIndex, item);
                itemIndex++;
            }
        }

        // Add 'See All Stocks' item if enabled
        if (seeAllStocksEnabled && seeAllStocksSlot < inv.getSize()) {
            ItemStack seeAll = new ItemStack(seeAllStocksMaterial);
            var meta = seeAll.getItemMeta();
            meta.setDisplayName(seeAllStocksDisplayName);
            meta.setLore(seeAllStocksLore);
            seeAll.setItemMeta(meta);
            inv.setItem(seeAllStocksSlot, seeAll);
        }
        player.openInventory(inv);
    }

    public int getPlayerStockAmount(Player player, String productId) {
        return com.skyblockexp.ezshops.stock.StockManager.getPlayerStockAmount(player, productId);
    }

    private List<String> getTradableProductIds(String filter) {
        List<String> ids = new ArrayList<>();
        for (String id : stockMarketManager.getAllProductIds()) {
            if (stockMarketConfig.isBlocked(id) || frozenStore.isFrozen(id)) continue;
            boolean matches = true;
            if (filter != null && !filter.equalsIgnoreCase("all") && !filter.isEmpty()) {
                String category = stockMarketConfig.getCategory(id);
                if (category == null || !category.equalsIgnoreCase(filter)) {
                    matches = false;
                }
                if (filter.startsWith("price:")) {
                    String[] parts = filter.substring(6).split("-");
                    try {
                        double min = Double.parseDouble(parts[0]);
                        double max = parts.length > 1 ? Double.parseDouble(parts[1]) : Double.MAX_VALUE;
                        double price = stockMarketManager.getPrice(id);
                        if (price < min || price > max) matches = false;
                    } catch (Exception ignored) {}
                }
            }
            if (matches) ids.add(id);
        }
        return ids;
    }

    private List<String> getPlayerOwnedStocks(Player player, String filter) {
        List<String> ownedStocks = com.skyblockexp.ezshops.stock.StockManager.getPlayerOwnedStocks(player);
        if (filter == null || filter.isEmpty() || filter.equalsIgnoreCase("all")) {
            return ownedStocks;
        }
        // Text-based filtering: search in item name
        List<String> filtered = new ArrayList<>();
        String lowerFilter = filter.toLowerCase(java.util.Locale.ROOT);
        for (String id : ownedStocks) {
            // Check if the item name contains the filter text
            String itemName = id.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
            if (itemName.contains(lowerFilter)) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    public void showHistory(Player player, String productId) {
        List<com.skyblockexp.ezshops.stock.StockHistoryManager.PriceEntry> history = stockMarketManager.getHistoryManager().getHistory(productId);
        if (history.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No price history for " + productId + ".");
            return;
        }
        com.skyblockexp.ezshops.gui.stock.StockHistoryGui.open(player, productId, history);
    }

    public boolean handleInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;
        if (!seeAllStocksEnabled) return false;
        if (event.getSlot() != seeAllStocksSlot) return false;

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() != seeAllStocksMaterial) return false;

        var meta = clicked.getItemMeta();

        if (meta == null || !seeAllStocksDisplayName.equals(meta.getDisplayName())) return false;

        event.setCancelled(true);

        AllStocksGui allStockGui = new AllStocksGui(stockMarketManager, stockMarketConfig, frozenStore, guiConfigFile);
        allStockGui.setStockOverviewGui(this); // Set reference for back button
        allStockGui.open(player, 1);

        return true;
    }

    private String localize(String key) {
        return localization.getOrDefault(key, key);
    }

    public List<Map<String, String>> getFilters() { return filters; }
    public String getTitle() { return title; }

    // Expose for event handler
    public boolean isSeeAllStocksEnabled() { return seeAllStocksEnabled; }
    public int getSeeAllStocksSlot() { return seeAllStocksSlot; }
    public Material getSeeAllStocksMaterial() { return seeAllStocksMaterial; }
    public String getSeeAllStocksDisplayName() { return seeAllStocksDisplayName; }
    public List<String> getSeeAllStocksLore() { return seeAllStocksLore; }
}
