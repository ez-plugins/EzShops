package com.skyblockexp.ezshops.stock;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import com.skyblockexp.ezshops.repository.StockMarketRepository;
import org.bukkit.Material;

/**
 * Manages stock market prices for shop products.
 * Prices fluctuate based on a simple supply/demand simulation.
 */

public class StockMarketManager {
    private final Map<String, Double> prices = new HashMap<>();
    private final Random random = new Random();
    private static final double BASE_PRICE = 100.0;
    private static final double MAX_CHANGE = 0.10;
    private StockMarketRepository stockMarketRepository;
    private final StockHistoryManager historyManager = new StockHistoryManager();

    // Persistence
    private BukkitTask saveTask;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * Call this during plugin/component enable to set up persistence.
     * @param plugin Bukkit plugin instance
     * @param saveIntervalTicks interval in ticks (20 ticks = 1s)
     */
    public void enablePersistence(Plugin plugin, long saveIntervalTicks) {
        // Load prices from repository
        if (stockMarketRepository != null) {
            Map<String, Double> loaded = stockMarketRepository.loadPrices();
            lock.writeLock().lock();
            try {
                prices.clear();
                prices.putAll(loaded);
            } finally {
                lock.writeLock().unlock();
            }
        }
        // Schedule periodic async save
        if (saveTask != null) saveTask.cancel();
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::savePrices, saveIntervalTicks, saveIntervalTicks);
    }

    public void disablePersistence() {
        if (saveTask != null) saveTask.cancel();
        savePrices();
    }

    private void savePrices() {
        lock.readLock().lock();
        try {
            if (stockMarketRepository != null) {
                stockMarketRepository.savePrices(new HashMap<>(prices));
            }
        } finally {
            lock.readLock().unlock();
        }
        }
    /**
     * Get all product IDs including all tradeable Materials.
     * Returns all valid Minecraft Materials that can be items.
     */
    public Set<String> getAllProductIds() {
        Set<String> allIds = new HashSet<>();
        // Include all valid Materials that are items
        for (Material mat : Material.values()) {
            if (mat.isItem() && !mat.isAir()) {
                allIds.add(mat.name());
            }
        }
        return allIds;
    }


    public double getPrice(String productId) {
        lock.readLock().lock();
        try {
            return prices.getOrDefault(productId, BASE_PRICE);
        } finally {
            lock.readLock().unlock();
        }
    }


    public void setStockMarketRepository(StockMarketRepository repository) {
        this.stockMarketRepository = repository;
    }



    public void updatePrice(String productId, int demand) {
        if (stockMarketRepository != null && stockMarketRepository.isFrozen(productId)) {
            return;
        }
        lock.writeLock().lock();
        try {
            double current = getPrice(productId);
            double change = (demand * 0.02) + (random.nextDouble() * 2 - 1) * MAX_CHANGE;
            double newPrice = Math.max(1.0, current * (1.0 + change));
            prices.put(productId, newPrice);
            historyManager.recordPrice(productId, newPrice);
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void setPrice(String productId, double price) {
        double p = Math.max(1.0, price);
        lock.writeLock().lock();
        try {
            prices.put(productId, p);
            historyManager.recordPrice(productId, p);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public StockHistoryManager getHistoryManager() {
        return historyManager;
    }
}
