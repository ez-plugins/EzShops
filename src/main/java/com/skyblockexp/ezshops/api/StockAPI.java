package com.skyblockexp.ezshops.api;

import com.skyblockexp.ezshops.stock.StockMarketManager;
import com.skyblockexp.ezshops.stock.StockManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * Public API for interacting with the EzShops stock market system.
 * 
 * <p>This API provides methods to query and manipulate stock prices,
 * manage player stock holdings, and interact with the stock market.</p>
 * 
 * <p><b>Thread Safety:</b> All methods in this API are thread-safe and can be
 * called from any thread, including async tasks.</p>
 * 
 * @since 2.0.0
 */
public class StockAPI {
    
    private final StockMarketManager stockMarketManager;
    
    /**
     * Creates a new StockAPI instance.
     * 
     * @param stockMarketManager the stock market manager instance
     * @throws IllegalArgumentException if stockMarketManager is null
     */
    public StockAPI(StockMarketManager stockMarketManager) {
        if (stockMarketManager == null) {
            throw new IllegalArgumentException("StockMarketManager cannot be null");
        }
        this.stockMarketManager = stockMarketManager;
    }
    
    /**
     * Gets the current price of a stock product.
     * 
     * @param productId the product ID (material name, e.g., "DIAMOND", "IRON_INGOT")
     * @return the current price of the product, or the base price if not found
     * @throws IllegalArgumentException if productId is null or empty
     */
    public double getStockPrice(String productId) {
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        return stockMarketManager.getPrice(productId.toUpperCase());
    }
    
    /**
     * Sets the price of a stock product.
     * 
     * <p>This will override the current market price. The price will be adjusted
     * to a minimum of 1.0 if a lower value is provided.</p>
     * 
     * @param productId the product ID (material name, e.g., "DIAMOND", "IRON_INGOT")
     * @param price the new price to set (minimum 1.0)
     * @throws IllegalArgumentException if productId is null or empty, or if price is negative
     */
    public void setStockPrice(String productId, double price) {
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        stockMarketManager.setPrice(productId.toUpperCase(), price);
    }
    
    /**
     * Updates the price of a stock product based on demand.
     * 
     * <p>This simulates market demand by adjusting the price based on the
     * provided demand value. Positive demand increases the price, while
     * negative demand decreases it.</p>
     * 
     * @param productId the product ID (material name, e.g., "DIAMOND", "IRON_INGOT")
     * @param demand the demand value (positive = increase, negative = decrease)
     * @throws IllegalArgumentException if productId is null or empty
     */
    public void updateStockPrice(String productId, int demand) {
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        stockMarketManager.updatePrice(productId.toUpperCase(), demand);
    }
    
    /**
     * Gets the amount of stock a player owns for a specific product.
     * 
     * @param player the player to query
     * @param productId the product ID (material name, e.g., "DIAMOND", "IRON_INGOT")
     * @return the amount of stock owned by the player, or 0 if none
     * @throws IllegalArgumentException if player or productId is null or empty
     */
    public int getPlayerStockAmount(Player player, String productId) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        return StockManager.getPlayerStockAmount(player, productId.toUpperCase());
    }
    
    /**
     * Adds stock to a player's holdings.
     * 
     * @param player the player to add stock to
     * @param productId the product ID (material name, e.g., "DIAMOND", "IRON_INGOT")
     * @param amount the amount of stock to add (must be positive)
     * @return true if the stock was added successfully, false otherwise
     * @throws IllegalArgumentException if player or productId is null, or amount is non-positive
     */
    public boolean addPlayerStock(Player player, String productId, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return StockManager.addPlayerStock(player, productId.toUpperCase(), amount);
    }
    
    /**
     * Removes stock from a player's holdings.
     * 
     * @param player the player to remove stock from
     * @param productId the product ID (material name, e.g., "DIAMOND", "IRON_INGOT")
     * @param amount the amount of stock to remove (must be positive)
     * @return true if the stock was removed successfully, false if the player doesn't have enough
     * @throws IllegalArgumentException if player or productId is null, or amount is non-positive
     */
    public boolean removePlayerStock(Player player, String productId, int amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return StockManager.removePlayerStock(player, productId.toUpperCase(), amount);
    }
    
    /**
     * Gets a list of all stock products that a player owns.
     * 
     * @param player the player to query
     * @return a list of product IDs that the player owns stock in
     * @throws IllegalArgumentException if player is null
     */
    public List<String> getPlayerOwnedStocks(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        return StockManager.getPlayerOwnedStocks(player);
    }
    
    /**
     * Gets all available product IDs in the stock market.
     * 
     * <p>This includes all valid Minecraft materials that can be traded as items.</p>
     * 
     * @return a set of all available product IDs
     */
    public Set<String> getAllProductIds() {
        return stockMarketManager.getAllProductIds();
    }
}
