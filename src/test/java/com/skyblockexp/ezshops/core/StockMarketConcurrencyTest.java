package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.stock.StockMarketManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

public class StockMarketConcurrencyTest {

    @Test
    void concurrent_updates_and_reads_do_not_throw_and_price_remains_valid() throws InterruptedException, ExecutionException {
        StockMarketManager mgr = new StockMarketManager();
        mgr.setPrice("DIAMOND", 100.0);

        int writers = 4;
        int readers = 4;
        ExecutorService ex = Executors.newFixedThreadPool(writers + readers);

        List<Callable<Boolean>> tasks = new ArrayList<>();

        // writers: perform many small updates
        for (int i = 0; i < writers; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 200; j++) {
                    mgr.updatePrice("DIAMOND", (j % 2 == 0) ? 1 : -1);
                }
                return true;
            });
        }

        // readers: frequently read the price
        for (int i = 0; i < readers; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 400; j++) {
                    double p = mgr.getPrice("DIAMOND");
                    if (p < 1.0) return false;
                }
                return true;
            });
        }

        List<Future<Boolean>> results = ex.invokeAll(tasks);
        ex.shutdown();

        for (Future<Boolean> f : results) {
            assertTrue(f.get(), "Task should complete successfully and maintain price >= 1.0");
        }

        double finalPrice = mgr.getPrice("DIAMOND");
        assertTrue(finalPrice >= 1.0, "Final price must be >= 1.0");
    }
}
