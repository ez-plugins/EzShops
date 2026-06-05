package com.skyblockexp.ezshops.stock;

import com.skyblockexp.ezshops.common.SchedulerAdapter;
import com.skyblockexp.ezshops.common.TaskHandle;
import org.bukkit.plugin.Plugin;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import com.skyblockexp.ezshops.repository.StockMarketRepository;
import com.skyblockexp.ezshops.data.TransactionCache;
import com.skyblockexp.ezshops.data.RedisTransactionCache;
import org.bukkit.Material;

/**
 * Manages stock market prices for shop products.
 * Prices fluctuate based on a simple supply/demand simulation.
 */
public class StockMarketManager {
    private final Map<String, Double> prices = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private static final double BASE_PRICE = 100.0;
    // Engine parameters – defaults match original hardcoded values; override via configure().
    private double volatilityMin = -0.10;
    private double volatilityMax =  0.10;
    private double demandFactor  =  0.02;
    private double minPrice      =  1.0;
    private StockMarketRepository stockMarketRepository;
    private final StockHistoryManager historyManager = new StockHistoryManager();
    
    private TransactionCache transactionCache = new com.skyblockexp.ezshops.data.LocalTransactionCache();
    
    // Configurable intervals
    private long saveIntervalTicks = 6000; // Default 5 minutes (20 ticks * 60 seconds * 5)
    private long flushIntervalTicks = 20; // Default 1 second
    
    // Flush task handle
    private TaskHandle flushTask;
    
    // Persistence
    private TaskHandle saveTask;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Configure the save and flush intervals.
     * @param saveIntervalTicks interval in ticks for saving prices to disk
     * @param flushIntervalTicks interval in ticks for flushing pending transactions
     */
    public void configureIntervals(long saveIntervalTicks, long flushIntervalTicks) {
        if (saveIntervalTicks > 0) this.saveIntervalTicks = saveIntervalTicks;
        if (flushIntervalTicks > 0) this.flushIntervalTicks = flushIntervalTicks;
    }

/**
      * Apply configurable price-engine parameters.
      * Call this before {@link #enablePersistence} so the loaded prices are
      * immediately governed by the configured floor.
      */
    public void configure(double volatilityMin, double volatilityMax,
                          double demandFactor, double minPrice) {
        this.volatilityMin = volatilityMin;
        this.volatilityMax = volatilityMax;
        this.demandFactor  = Math.max(0.0, demandFactor);
        this.minPrice      = Math.max(0.0, minPrice);
    }

    /**
     * Set the transaction cache implementation.
     * @param transactionCache the cache to use for pending transactions
     */
    public void setTransactionCache(TransactionCache transactionCache) {
        if (transactionCache != null) {
            this.transactionCache = transactionCache;
        }
    }

    /**
     * Shuts down the transaction cache if it has a shutdown method.
     */
    public void shutdownCache() {
        if (transactionCache instanceof RedisTransactionCache redisCache) {
            redisCache.shutdown();
        }
    }

    /**
     * Call this during plugin/component enable to set up persistence and flush scheduler.
     * @param plugin Bukkit plugin instance
     * @param saveIntervalTicks interval in ticks (20 ticks = 1s) for price saves
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
        saveTask = SchedulerAdapter.runTaskTimerAsync(plugin, this::savePrices, saveIntervalTicks, saveIntervalTicks);
        
        // Schedule periodic flush for pending transactions
        if (flushTask != null) flushTask.cancel();
        flushTask = SchedulerAdapter.runTaskTimerAsync(plugin, this::flushPendingTransactions, flushIntervalTicks, flushIntervalTicks);
    }

    public void disablePersistence() {
        if (saveTask != null) saveTask.cancel();
        if (flushTask != null) flushTask.cancel();
        flushPendingTransactions();
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
     * Queue a transaction amount for batched price updates.
     * Positive amounts for buys, negative for sells.
     * @param productId the product identifier
     * @param amount the transaction amount
     */
    public void queueTransaction(String productId, int amount) {
        if (stockMarketRepository != null && stockMarketRepository.isFrozen(productId)) {
            return;
        }
        transactionCache.add(productId, amount);
    }

    /**
     * Flush all pending transactions and apply price updates.
     * Called periodically by the scheduler or on plugin shutdown.
     */
    private void flushPendingTransactions() {
        if (transactionCache.isEmpty()) return;
        Map<String, Integer> snapshot = transactionCache.drain();
        
        lock.writeLock().lock();
        try {
            for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
                String productId = entry.getKey();
                int demand = entry.getValue();
                double current = prices.getOrDefault(productId, BASE_PRICE);
                if (demand == 0) continue;
                
                double randomComponent = volatilityMin + random.nextDouble() * (volatilityMax - volatilityMin);
                double perUnitChange = (demand > 0 ? demandFactor : -demandFactor) + randomComponent;
                int steps = Math.abs(demand);
                
                if (perUnitChange != 0) {
                    double multiplier = Math.pow(1.0 + perUnitChange, steps);
                    current = Math.max(minPrice, current * multiplier);
                }
                prices.put(productId, current);
                historyManager.recordPrice(productId, current);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get the pending transaction amount for a product.
     * @param productId the product identifier
     * @return the total pending amount
     */
    public int getPendingAmount(String productId) {
        return transactionCache.get(productId);
    }

    /**
     * Get all pending transactions.
     * @return map of product IDs to pending amounts
     */
    public Map<String, Integer> getPendingTransactions() {
        return transactionCache.snapshot();
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
        // Apply per-unit multiplicative updates using closed-form geometric progression: final = initial * (1 + change)^steps
        lock.writeLock().lock();
        try {
            double current = prices.getOrDefault(productId, BASE_PRICE);
            if (demand == 0) {
                return;
            }
            // Compute a single random component for the entire bulk operation (preserves similar randomness scale)
            double randomComponent = volatilityMin + random.nextDouble() * (volatilityMax - volatilityMin);
            // per-unit change (positive for buys, negative for sells) plus shared random
            double perUnitChange = (demand > 0 ? demandFactor : -demandFactor) + randomComponent;
            int steps = Math.abs(demand);
            // Geometric progression: P_final = P_initial * (1 + r)^n
            if (perUnitChange != 0) {
                double multiplier = Math.pow(1.0 + perUnitChange, steps);
                current = Math.max(minPrice, current * multiplier);
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
        boolean isBuy = type == ShopTransactionType.BUY;
        double change = isBuy ? demandFactor : -demandFactor;
        if (Math.abs(change - 0.0) < 1e-10) {
            return base * amount;
        }
        double r = 1.0 + change;
        // Sum of geometric progression: sum = P * (r^n - 1) / (r - 1)
        if (isBuy) {
            double sum = base * (Math.pow(r, amount) - 1.0) / (r - 1.0);
            return Math.max(base * amount, sum);
        }
        // For sells, check if price would hit floor
        double finalPrice = base * Math.pow(r, amount);
        if (finalPrice >= minPrice) {
            double sum = base * (1.0 - Math.pow(r, amount)) / (1.0 - r);
            return sum;
        }
        // Find steps until floor, sum the progression to that point, then rest at floor
        int stepsUntilFloor = (int) Math.ceil(Math.log(minPrice / base) / Math.log(r));
        if (stepsUntilFloor > amount) stepsUntilFloor = amount;
        if (stepsUntilFloor < 1) stepsUntilFloor = 1;
        double sumBeforeFloor = base * (1.0 - Math.pow(r, stepsUntilFloor)) / (1.0 - r);
        double remainderAtFloor = minPrice * (amount - stepsUntilFloor);
        return sumBeforeFloor + remainderAtFloor;
    }

    public void setPrice(String productId, double price) {
        double p = Math.max(minPrice, price);
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