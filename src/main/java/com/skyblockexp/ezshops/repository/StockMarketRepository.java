package com.skyblockexp.ezshops.repository;

import java.util.Collection;
import java.util.Set;

/**
 * Repository interface for stock market frozen state persistence.
 */
import java.util.Map;

public interface StockMarketRepository {
        /**
         * Loads all stock prices from storage.
         * @return map of productId to price
         */
        Map<String, Double> loadPrices();

        /**
         * Saves all stock prices to storage.
         * @param prices map of productId to price
         */
        void savePrices(Map<String, Double> prices);
    
    /**
     * Metadata for a frozen stock item.
     */
    class FrozenMeta {
        public final String id;
        public final String by;
        public final long when;
        
        public FrozenMeta(String id, String by, long when) {
            this.id = id;
            this.by = by;
            this.when = when;
        }
    }
    
    /**
     * Loads frozen stock data from storage.
     */
    void load();
    
    /**
     * Saves frozen stock data to storage.
     */
    void save();
    
    /**
     * Freezes a stock item.
     *
     * @param id the product id
     * @param by who froze it
     */
    void freeze(String id, String by);
    
    /**
     * Unfreezes a stock item.
     *
     * @param id the product id
     */
    void unfreeze(String id);
    
    /**
     * Checks if a stock item is frozen.
     *
     * @param id the product id
     * @return true if frozen
     */
    boolean isFrozen(String id);
    
    /**
     * Gets all frozen stock metadata.
     *
     * @return collection of frozen metadata
     */
    Collection<FrozenMeta> getAllFrozenMeta();
    
    /**
     * Gets all frozen stock IDs.
     *
     * @return set of frozen IDs
     */
    Set<String> getAllFrozen();
}
