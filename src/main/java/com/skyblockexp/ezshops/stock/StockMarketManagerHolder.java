package com.skyblockexp.ezshops.stock;

/**
 * Singleton holder for StockMarketManager to allow global access.
 */
public class StockMarketManagerHolder {
    private static final StockMarketManager INSTANCE = new StockMarketManager();
    public static StockMarketManager get() {
        return INSTANCE;
    }
}
