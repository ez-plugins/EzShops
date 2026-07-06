package com.skyblockexp.ezshops.shop.pricing.state;

import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPriceType;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopDynamicSettings;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopManagedPriceEntry;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Encapsulates dynamic pricing state transitions and persistence.
 */
public final class ShopDynamicPricingService {

    private final Map<String, ShopManagedPriceEntry> priceMap;
    private final ShopDynamicStateStore dynamicStateStore;
    private final DynamicPricingConfiguration dynamicConfiguration;
    private final Logger logger;

    public ShopDynamicPricingService(Map<String, ShopManagedPriceEntry> priceMap,
            ShopDynamicStateStore dynamicStateStore,
            DynamicPricingConfiguration dynamicConfiguration,
            Logger logger) {
        this.priceMap = priceMap;
        this.dynamicStateStore = dynamicStateStore;
        this.dynamicConfiguration = dynamicConfiguration;
        this.logger = logger;
    }

    public void registerPrice(String priceKey, ShopPrice price, ShopDynamicSettings dynamicSettings) {
        ShopManagedPriceEntry entry = new ShopManagedPriceEntry(price, dynamicSettings,
                loadSavedMultiplier(priceKey, dynamicSettings));
        ShopManagedPriceEntry previous = priceMap.put(priceKey, entry);
        if (previous != null && !previous.basePrice().equals(price)) {
            logger.fine("Overriding shop price for key " + priceKey + " with new configuration values.");
        }
    }

    public void registerPrice(String priceKey, ShopPrice price, ShopDynamicSettings dynamicSettings,
            ShopPriceType priceType) {
        ShopManagedPriceEntry entry = new ShopManagedPriceEntry(price, dynamicSettings,
                loadSavedMultiplier(priceKey, dynamicSettings), priceType);
        ShopManagedPriceEntry previous = priceMap.put(priceKey, entry);
        if (previous != null && !previous.basePrice().equals(price)) {
            logger.fine("Overriding shop price for key " + priceKey + " with new configuration values.");
        }
    }

    public void handlePurchase(Material material, int amount) {
        adjustDynamicMultiplier(material, amount, true);
    }

    public void handleSale(Material material, int amount) {
        adjustDynamicMultiplier(material, amount, false);
    }

    public void handlePurchase(String priceKey, int amount) {
        adjustDynamicMultiplier(priceKey, amount, true);
    }

    public void handleSale(String priceKey, int amount) {
        adjustDynamicMultiplier(priceKey, amount, false);
    }

    public void cleanupDynamicState(Map<String, Set<String>> validRotationOptions) {
        dynamicStateStore.cleanup(key -> {
            ShopManagedPriceEntry entry = priceMap.get(key);
            return entry != null && entry.hasDynamicPricing();
        }, (rotationId, optionId) -> {
            Set<String> options = validRotationOptions.get(rotationId);
            return options != null && options.contains(optionId);
        });
    }

    public void saveRotationState(String rotationId, String optionId) {
        dynamicStateStore.saveRotationOption(rotationId, optionId);
    }

    public boolean resetDynamicPricing(String priceKey) {
        if (priceKey == null || priceKey.isBlank()) {
            return false;
        }
        boolean removedSaved = dynamicStateStore.removeSavedEntry(priceKey);

        ShopManagedPriceEntry entry = priceMap.get(priceKey);
        boolean resetInMemory = false;
        if (entry != null && entry.hasDynamicPricing()) {
            resetInMemory = entry.resetToStartingMultiplier();
        }

        return removedSaved || resetInMemory;
    }

    public int resetAllDynamicPricing() {
        int count = 0;
        for (Map.Entry<String, ShopManagedPriceEntry> e : priceMap.entrySet()) {
            ShopManagedPriceEntry entry = e.getValue();
            if (entry != null && entry.hasDynamicPricing() && entry.resetToStartingMultiplier()) {
                count++;
            }
        }

        int persistedRemoved = dynamicStateStore.clearAllDynamicEntries();
        if (persistedRemoved > 0) {
            dynamicStateStore.flush();
        }

        return count + persistedRemoved;
    }

    public boolean setPriceMultiplierForTesting(String priceKey, double multiplier) {
        if (priceKey == null || priceKey.isBlank()) {
            return false;
        }
        ShopManagedPriceEntry entry = priceMap.get(priceKey);
        if (entry == null || !entry.hasDynamicPricing()) {
            return false;
        }
        entry.setMultiplier(multiplier);
        saveDynamicState(priceKey, entry);
        return true;
    }

    public boolean setPrice(String priceKey, double price) {
        if (priceKey == null || priceKey.isBlank()) {
            return false;
        }
        ShopManagedPriceEntry previous = priceMap.get(priceKey);
        if (previous == null) {
            return false;
        }
        ShopPrice newBase = new ShopPrice(price, price);
        ShopManagedPriceEntry replacement = new ShopManagedPriceEntry(newBase, previous.settings(),
                previous.multiplier(), previous.priceType());
        priceMap.put(priceKey, replacement);
        return true;
    }

    public boolean disableBuy(String priceKey) {
        if (priceKey == null || priceKey.isBlank()) {
            return false;
        }
        ShopManagedPriceEntry previous = priceMap.get(priceKey);
        if (previous == null) {
            return false;
        }
        ShopPrice base = previous.basePrice();
        double sell = base == null ? -1.0D : base.sellPrice();
        ShopPrice newBase = new ShopPrice(-1.0D, sell);
        ShopManagedPriceEntry replacement = new ShopManagedPriceEntry(newBase, previous.settings(),
                previous.multiplier(), previous.priceType());
        priceMap.put(priceKey, replacement);
        return true;
    }

    public boolean disableSell(String priceKey) {
        if (priceKey == null || priceKey.isBlank()) {
            return false;
        }
        ShopManagedPriceEntry previous = priceMap.get(priceKey);
        if (previous == null) {
            return false;
        }
        ShopPrice base = previous.basePrice();
        double buy = base == null ? -1.0D : base.buyPrice();
        ShopPrice newBase = new ShopPrice(buy, -1.0D);
        ShopManagedPriceEntry replacement = new ShopManagedPriceEntry(newBase, previous.settings(),
                previous.multiplier(), previous.priceType());
        priceMap.put(priceKey, replacement);
        return true;
    }

    public Set<String> getConfiguredPriceKeys() {
        return Set.copyOf(new LinkedHashSet<>(priceMap.keySet()));
    }

    public ShopDynamicSettings parseDynamicSettings(ConfigurationSection section, String materialKey) {
        if (section == null || !dynamicConfiguration.enabled()) {
            return null;
        }
        ConfigurationSection dynamicSection = section.getConfigurationSection("dynamic-pricing");
        if (dynamicSection == null) {
            return null;
        }

        boolean enabled = dynamicSection.getBoolean("enabled", true);
        if (!enabled) {
            return null;
        }

        double startingMultiplier = readDynamicValue(dynamicSection, materialKey, "starting-multiplier",
                dynamicConfiguration.defaultStartingMultiplier());
        double minMultiplier = readDynamicValue(dynamicSection, materialKey, "min-multiplier",
                dynamicConfiguration.defaultMinMultiplier());
        double maxMultiplier = readDynamicValue(dynamicSection, materialKey, "max-multiplier",
                dynamicConfiguration.defaultMaxMultiplier());
        double buyChange = readDynamicValue(dynamicSection, materialKey, "buy-change",
                dynamicConfiguration.defaultBuyChange());
        double sellChange = readDynamicValue(dynamicSection, materialKey, "sell-change",
                dynamicConfiguration.defaultSellChange());

        if (Double.isNaN(startingMultiplier) || startingMultiplier <= 0.0D) {
            logger.warning("Invalid starting-multiplier for dynamic pricing on material '" + materialKey
                    + "'. Using " + dynamicConfiguration.defaultStartingMultiplier() + '.');
            startingMultiplier = dynamicConfiguration.defaultStartingMultiplier();
        }
        if (Double.isNaN(minMultiplier) || minMultiplier <= 0.0D) {
            logger.warning("Invalid min-multiplier for dynamic pricing on material '" + materialKey
                    + "'. Using " + dynamicConfiguration.defaultMinMultiplier() + '.');
            minMultiplier = dynamicConfiguration.defaultMinMultiplier();
        }
        if (Double.isNaN(maxMultiplier) || maxMultiplier <= 0.0D) {
            logger.warning("Invalid max-multiplier for dynamic pricing on material '" + materialKey
                    + "'. Using " + dynamicConfiguration.defaultMaxMultiplier() + '.');
            maxMultiplier = dynamicConfiguration.defaultMaxMultiplier();
        }
        if (maxMultiplier < minMultiplier) {
            double temp = maxMultiplier;
            maxMultiplier = minMultiplier;
            minMultiplier = temp;
        }
        if (Double.isNaN(buyChange) || buyChange < 0.0D) {
            logger.warning("Invalid buy-change for dynamic pricing on material '" + materialKey
                    + "'. Using " + dynamicConfiguration.defaultBuyChange() + '.');
            buyChange = dynamicConfiguration.defaultBuyChange();
        }
        if (Double.isNaN(sellChange) || sellChange < 0.0D) {
            logger.warning("Invalid sell-change for dynamic pricing on material '" + materialKey
                    + "'. Using " + dynamicConfiguration.defaultSellChange() + '.');
            sellChange = dynamicConfiguration.defaultSellChange();
        }

        return new ShopDynamicSettings(startingMultiplier, minMultiplier, maxMultiplier, buyChange, sellChange);
    }

    private void adjustDynamicMultiplier(String priceKey, int amount, boolean purchase) {
        if (amount <= 0 || priceKey == null) {
            return;
        }
        ShopManagedPriceEntry entry = priceMap.get(priceKey);
        if (entry == null || !entry.hasDynamicPricing()) {
            return;
        }
        boolean changed = purchase ? entry.adjustAfterPurchase(amount) : entry.adjustAfterSale(amount);
        if (changed) {
            saveDynamicState(priceKey, entry);
        }
    }

    private void adjustDynamicMultiplier(Material material, int amount, boolean purchase) {
        if (amount <= 0 || material == null) {
            return;
        }
        String nameKey = material.name();
        ShopManagedPriceEntry entry = priceMap.get(nameKey);
        if (entry == null || !entry.hasDynamicPricing()) {
            return;
        }
        boolean changed = purchase ? entry.adjustAfterPurchase(amount) : entry.adjustAfterSale(amount);
        if (changed) {
            saveDynamicState(nameKey, entry);
        }
    }

    private double loadSavedMultiplier(String priceKey, ShopDynamicSettings settings) {
        if (settings == null) {
            return 1.0D;
        }
        if (!dynamicStateStore.isSet(priceKey)) {
            return settings.clamp(settings.startingMultiplier());
        }
        return settings.clamp(dynamicStateStore.getDouble(priceKey, settings.startingMultiplier()));
    }

    private void saveDynamicState(String priceKey, ShopManagedPriceEntry entry) {
        if (entry == null || !entry.hasDynamicPricing()) {
            return;
        }
        dynamicStateStore.saveMultiplier(priceKey, entry.multiplier());
    }

    private double readDynamicValue(ConfigurationSection section, String materialKey, String path, double fallback) {
        if (!section.contains(path)) {
            return fallback;
        }
        Object value = section.get(path);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue.trim());
            } catch (NumberFormatException ex) {
                // handled below
            }
        }
        logger.warning("Invalid " + path + " for dynamic pricing on material '" + materialKey + "'. Using " + fallback
                + '.');
        return fallback;
    }
}
