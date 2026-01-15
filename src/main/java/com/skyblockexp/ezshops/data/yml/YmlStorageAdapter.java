package com.skyblockexp.ezshops.data.yml;

import com.skyblockexp.ezshops.data.StorageAdapter;
import com.skyblockexp.ezshops.repository.PlayerShopRepository;
import com.skyblockexp.ezshops.repository.StockMarketRepository;
import com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository;
import com.skyblockexp.ezshops.repository.yml.YmlStockMarketRepository;

import java.io.File;
import java.util.logging.Logger;

/**
 * YML-based storage adapter implementation.
 * Provides YML file-based persistence for all plugin data.
 */
public class YmlStorageAdapter implements StorageAdapter {
    
    private final YmlPlayerShopRepository playerShopRepository;
    private final YmlStockMarketRepository stockMarketRepository;
    
    public YmlStorageAdapter(File dataFolder, Logger logger) {
        this.playerShopRepository = new YmlPlayerShopRepository(dataFolder, logger);
        this.stockMarketRepository = new YmlStockMarketRepository(dataFolder);
    }
    
    @Override
    public PlayerShopRepository getPlayerShopRepository() {
        return playerShopRepository;
    }
    
    @Override
    public StockMarketRepository getStockMarketRepository() {
        return stockMarketRepository;
    }
    
    @Override
    public void initialize() {
        // Load all data from YML files
        stockMarketRepository.load();
    }
    
    @Override
    public void shutdown() {
        // No cleanup needed for YML storage
    }
}
