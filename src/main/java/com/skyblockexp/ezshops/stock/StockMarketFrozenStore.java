package com.skyblockexp.ezshops.stock;

import com.skyblockexp.ezshops.repository.StockMarketRepository;
import java.util.*;

/**
 * Manages persistent frozen state for stock market items.
 * Now delegates storage operations to StockMarketRepository.
 */
public class StockMarketFrozenStore {
    private final StockMarketRepository repository;

    public static class FrozenMeta {
        public final String id;
        public final String by;
        public final long when;
        public FrozenMeta(String id, String by, long when) {
            this.id = id;
            this.by = by;
            this.when = when;
        }
    }

    public StockMarketFrozenStore(StockMarketRepository repository) {
        this.repository = repository;
        load();
    }

    public synchronized void load() {
        repository.load();
    }

    public synchronized void save() {
        repository.save();
    }

    public synchronized void freeze(String id, String by) {
        repository.freeze(id, by);
    }

    public synchronized void unfreeze(String id) {
        repository.unfreeze(id);
    }

    public synchronized boolean isFrozen(String id) {
        return repository.isFrozen(id);
    }

    public synchronized Collection<FrozenMeta> getAllFrozenMeta() {
        // Convert repository FrozenMeta to our FrozenMeta
        Collection<StockMarketRepository.FrozenMeta> repoMeta = repository.getAllFrozenMeta();
        List<FrozenMeta> result = new ArrayList<>();
        for (StockMarketRepository.FrozenMeta meta : repoMeta) {
            result.add(new FrozenMeta(meta.id, meta.by, meta.when));
        }
        return Collections.unmodifiableCollection(result);
    }

    public synchronized Set<String> getAllFrozen() {
        return repository.getAllFrozen();
    }
}

