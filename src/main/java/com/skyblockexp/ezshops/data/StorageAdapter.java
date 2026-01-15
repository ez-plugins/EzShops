package com.skyblockexp.ezshops.data;

import com.skyblockexp.ezshops.repository.PlayerShopRepository;
import com.skyblockexp.ezshops.repository.StockMarketRepository;

/**
 * Main storage adapter interface that aggregates all repository operations.
 * This ensures every storage implementation (YML, MySQL, etc.) provides the same operations.
 */
public interface StorageAdapter {
    
    /**
     * Gets the player shop repository for managing player-created shops.
     *
     * @return the player shop repository
     */
    PlayerShopRepository getPlayerShopRepository();
    
    /**
     * Gets the stock market repository for managing stock market data.
     *
     * @return the stock market repository
     */
    StockMarketRepository getStockMarketRepository();
    
    /**
     * Initializes the storage adapter and prepares all repositories.
     */
    void initialize();
    
    /**
     * Shuts down the storage adapter and releases all resources.
     */
    void shutdown();
}
