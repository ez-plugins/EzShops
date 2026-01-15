package com.skyblockexp.ezshops.api;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.StockComponent;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.shop.api.ShopPriceService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Main API entry point for the EzShops plugin.
 * 
 * <p>This class provides access to both the Shop API (for regular shop pricing)
 * and the Stock API (for stock market features). It serves as a centralized
 * access point for external plugins to interact with EzShops functionality.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Get the EzShops API instance
 * EzShopsAPI api = EzShopsAPI.getInstance();
 * 
 * // Use the Shop API to get item prices
 * ShopPriceService shopApi = api.getShopAPI();
 * OptionalDouble sellPrice = shopApi.findSellPrice(new ItemStack(Material.DIAMOND, 1));
 * 
 * // Use the Stock API to manage stock prices
 * StockAPI stockApi = api.getStockAPI();
 * double diamondStockPrice = stockApi.getStockPrice("DIAMOND");
 * stockApi.setStockPrice("DIAMOND", 150.0);
 * }</pre>
 * 
 * @since 2.0.0
 */
public class EzShopsAPI {
    
    private static EzShopsAPI instance;
    private final EzShopsPlugin plugin;
    private StockAPI stockAPI;
    
    /**
     * Creates a new EzShopsAPI instance.
     * 
     * @param plugin the EzShops plugin instance
     * @throws IllegalArgumentException if plugin is null
     */
    private EzShopsAPI(EzShopsPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        initializeAPIs();
    }
    
    /**
     * Initializes the API instances by retrieving them from the plugin.
     */
    private void initializeAPIs() {
        // Initialize StockAPI if the stock component is enabled
        try {
            StockComponent stockComponent = plugin.getStockComponent();
            if (stockComponent != null) {
                StockMarketManager stockManager = stockComponent.getStockMarketManager();
                if (stockManager != null) {
                    this.stockAPI = new StockAPI(stockManager);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize StockAPI: " + e.getMessage());
        }
    }
    
    /**
     * Initializes the EzShops API with the given plugin instance.
     * 
     * <p>This method should be called once during plugin initialization.</p>
     * 
     * @param plugin the EzShops plugin instance
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalStateException if the API has already been initialized
     */
    public static void initialize(EzShopsPlugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("EzShopsAPI has already been initialized");
        }
        instance = new EzShopsAPI(plugin);
    }
    
    /**
     * Gets the singleton instance of the EzShops API.
     * 
     * @return the EzShops API instance
     * @throws IllegalStateException if the API has not been initialized
     */
    public static EzShopsAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EzShopsAPI has not been initialized. Make sure EzShops is enabled.");
        }
        return instance;
    }
    
    /**
     * Gets the Shop Price Service API for querying shop item prices.
     * 
     * <p>The Shop API provides access to regular shop pricing for buying and
     * selling items. It uses Bukkit's ServicesManager to retrieve the
     * registered {@link ShopPriceService}.</p>
     * 
     * @return the shop price service, or null if not available
     */
    public ShopPriceService getShopAPI() {
        RegisteredServiceProvider<ShopPriceService> provider = 
            Bukkit.getServicesManager().getRegistration(ShopPriceService.class);
        return provider != null ? provider.getProvider() : null;
    }
    
    /**
     * Gets the Stock API for managing stock market prices and player holdings.
     * 
     * <p>The Stock API provides access to the stock market system, allowing
     * you to query and modify stock prices, and manage player stock holdings.</p>
     * 
     * @return the stock API, or null if the stock market feature is disabled
     */
    public StockAPI getStockAPI() {
        return stockAPI;
    }
    
    /**
     * Checks if the Stock API is available.
     * 
     * @return true if the Stock API is enabled and available, false otherwise
     */
    public boolean isStockAPIAvailable() {
        return stockAPI != null;
    }
    
    /**
     * Gets the EzShops plugin instance.
     * 
     * @return the plugin instance
     */
    public EzShopsPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * Cleans up the API instance.
     * 
     * <p>This method should be called during plugin shutdown.</p>
     */
    public static void shutdown() {
        instance = null;
    }
}
