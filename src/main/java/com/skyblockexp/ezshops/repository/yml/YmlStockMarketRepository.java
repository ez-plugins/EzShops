package com.skyblockexp.ezshops.repository.yml;

import com.skyblockexp.ezshops.repository.StockMarketRepository;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * YML-based implementation of StockMarketRepository.
 * Handles persistence of frozen stock data to stock-frozen.yml file.
 */
public class YmlStockMarketRepository implements StockMarketRepository {
    
    private final File file;
    private final Map<String, FrozenMeta> frozen;
    private final File pricesFile;
    
    public YmlStockMarketRepository(File dataFolder) {
        this.file = new File(dataFolder, "stock-frozen.yml");
        this.frozen = new HashMap<>();
        this.pricesFile = new File(dataFolder, "stock-prices.yml");
    }

    @Override
    public synchronized Map<String, Double> loadPrices() {
        Map<String, Double> prices = new HashMap<>();
        if (!pricesFile.exists()) return prices;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(pricesFile);
        for (String key : yaml.getKeys(false)) {
            prices.put(key, yaml.getDouble(key));
        }
        return prices;
    }

    @Override
    public synchronized void savePrices(Map<String, Double> prices) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(pricesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public synchronized void load() {
        frozen.clear();
        if (!file.exists()) {
            return;
        }
        
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> list = yaml.getMapList("frozen");
        for (Map<?, ?> entry : list) {
            String id = Objects.toString(entry.get("id"), null);
            String by = Objects.toString(entry.get("by"), "?");
            long when = entry.get("when") instanceof Number n ? n.longValue() : 0L;
            if (id != null) {
                frozen.put(id.toUpperCase(), new FrozenMeta(id, by, when));
            }
        }
    }
    
    @Override
    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (FrozenMeta meta : frozen.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", meta.id);
            map.put("by", meta.by);
            map.put("when", meta.when);
            list.add(map);
        }
        yaml.set("frozen", list);
        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public synchronized void freeze(String id, String by) {
        frozen.put(id.toUpperCase(), new FrozenMeta(id.toUpperCase(), by, System.currentTimeMillis()));
        save();
    }
    
    @Override
    public synchronized void unfreeze(String id) {
        frozen.remove(id.toUpperCase());
        save();
    }
    
    @Override
    public synchronized boolean isFrozen(String id) {
        return frozen.containsKey(id.toUpperCase());
    }
    
    @Override
    public synchronized Collection<FrozenMeta> getAllFrozenMeta() {
        return Collections.unmodifiableCollection(frozen.values());
    }
    
    @Override
    public synchronized Set<String> getAllFrozen() {
        return Collections.unmodifiableSet(new HashSet<>(frozen.keySet()));
    }
}
