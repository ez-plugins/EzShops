package com.skyblockexp.ezshops.stock;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
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
    // per-unit deterministic demand factor (matches previous aggregated 0.02 per unit)
    private static final double PER_UNIT_DEMAND_FACTOR = 0.02;
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
        // Apply per-unit multiplicative updates to more closely model progressive trading effects.
        lock.writeLock().lock();
        try {
            double current = getPrice(productId);
            if (demand == 0) {
                // nothing to do
                return;
            }
            // Compute a single random component for the entire bulk operation (preserves similar randomness scale)
            double randomComponent = (random.nextDouble() * 2 - 1) * MAX_CHANGE;
            // per-unit change (positive for buys, negative for sells) plus shared random
            double perUnitChange = (demand > 0 ? PER_UNIT_DEMAND_FACTOR : -PER_UNIT_DEMAND_FACTOR) + randomComponent;
            int steps = Math.abs(demand);
            for (int i = 0; i < steps; i++) {
                current = Math.max(1.0, current * (1.0 + perUnitChange));
            }
            prices.put(productId, current);
            historyManager.recordPrice(productId, current);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Estimate the total cost (sum of per-unit prices) for buying/selling a given amount
     * without mutating stored prices. Uses the deterministic per-unit demand factor and
     * does not include random noise (randomness is unpredictable for previews).
     *
     * @param productId product/material id
     * @param amount amount to buy/sell (must be > 0)
     * @param type BUY to simulate purchases (price increases), SELL to simulate sales (price decreases)
     * @return total estimated price, or -1.0 if product unknown or invalid
     */
    public double estimateBulkTotal(String productId, int amount, ShopTransactionType type) {
        if (productId == null || amount <= 0 || type == null) {
            return -1.0D;
        }
        lock.readLock().lock();
        double base;
        try {
            base = prices.getOrDefault(productId, BASE_PRICE);
        } finally {
            lock.readLock().unlock();
        }
        double total = 0.0D;
        double sim = base;
        boolean isBuy = type == ShopTransactionType.BUY;
        for (int i = 0; i < amount; i++) {
            total += sim;
            double change = isBuy ? PER_UNIT_DEMAND_FACTOR : -PER_UNIT_DEMAND_FACTOR;
            sim = Math.max(1.0, sim * (1.0 + change));
        }
        return total;
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
