package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.api.StockAPI;
import com.skyblockexp.ezshops.stock.StockMarketManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StockAPIEdgeCasesTest {

    @Test
    void constructor_throws_when_null_manager() {
        assertThrows(IllegalArgumentException.class, () -> new StockAPI(null));
    }

    @Test
    void setStockPrice_clamps_to_minimum_one() {
        StockMarketManager mgr = new StockMarketManager();
        StockAPI api = new StockAPI(mgr);

        api.setStockPrice("DIAMOND", 0.5);
        double price = api.getStockPrice("DIAMOND");
        assertTrue(price >= 1.0, "Price should be clamped to at least 1.0");
    }

    @Test
    void getStockPrice_invalid_input_throws() {
        StockMarketManager mgr = new StockMarketManager();
        StockAPI api = new StockAPI(mgr);
        assertThrows(IllegalArgumentException.class, () -> api.getStockPrice(null));
        assertThrows(IllegalArgumentException.class, () -> api.getStockPrice(""));
    }

    @Test
    void setStockPrice_negative_throws() {
        StockMarketManager mgr = new StockMarketManager();
        StockAPI api = new StockAPI(mgr);
        assertThrows(IllegalArgumentException.class, () -> api.setStockPrice(null, 5.0));
        assertThrows(IllegalArgumentException.class, () -> api.setStockPrice("", 5.0));
        assertThrows(IllegalArgumentException.class, () -> api.setStockPrice("IRON_INGOT", -1.0));
    }

    @Test
    void updateStockPrice_invalid_input_throws() {
        StockMarketManager mgr = new StockMarketManager();
        StockAPI api = new StockAPI(mgr);
        assertThrows(IllegalArgumentException.class, () -> api.updateStockPrice(null, 1));
        assertThrows(IllegalArgumentException.class, () -> api.updateStockPrice("", -1));
    }
}
