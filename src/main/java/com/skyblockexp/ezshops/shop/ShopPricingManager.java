package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Public facade for shop pricing operations.
 * Delegates implementation details to {@link ShopPricingCore}.
 */
public class ShopPricingManager {

    private final ShopPricingCore core;

    public ShopPricingManager(JavaPlugin plugin, DynamicPricingConfiguration dynamicConfiguration) {
        this.core = new ShopPricingCore(plugin, dynamicConfiguration);
    }

    public final void reload() {
        core.reload();
    }

    public Optional<ShopPrice> getPrice(Material material) {
        return core.getPrice(material);
    }

    public double estimateBulkTotal(Material material, int amount, ShopTransactionType type) {
        return core.estimateBulkTotal(material, amount, type);
    }

    public boolean isConfigured(Material material) {
        return core.isConfigured(material);
    }

    public Collection<Material> getBuyableMaterials() {
        return core.getBuyableMaterials();
    }

    public Collection<Material> getSellableMaterials() {
        return core.getSellableMaterials();
    }

    public Set<Material> getConfiguredMaterials() {
        return core.getConfiguredMaterials();
    }

    public boolean isEmpty() {
        return core.isEmpty();
    }

    public ShopMenuLayout getMenuLayout() {
        return core.getMenuLayout();
    }

    public boolean isVisibleInMenu(Material material) {
        return core.isVisibleInMenu(material);
    }

    public boolean isVisibleInMenu(String priceKey) {
        return core.isVisibleInMenu(priceKey);
    }

    public boolean isPartOfRotation(Material material) {
        return core.isPartOfRotation(material);
    }

    public boolean isPartOfRotation(String priceKey) {
        return core.isPartOfRotation(priceKey);
    }

    public Map<String, ShopRotationDefinition> getRotationDefinitions() {
        return core.getRotationDefinitions();
    }

    public Map<String, String> getActiveRotationOptions() {
        return core.getActiveRotationOptions();
    }

    public boolean setActiveRotationOption(String rotationId, String optionId) {
        return core.setActiveRotationOption(rotationId, optionId);
    }

    public ShopMenuLayout.ItemType getItemType(Material material) {
        return core.getItemType(material);
    }

    public Optional<ShopPrice> getPrice(String priceKey) {
        return core.getPrice(priceKey);
    }

    public double estimateBulkTotal(String priceKey, int amount, ShopTransactionType type) {
        return core.estimateBulkTotal(priceKey, amount, type);
    }

    public void handlePurchase(Material material, int amount) {
        core.handlePurchase(material, amount);
    }

    public void handleSale(Material material, int amount) {
        core.handleSale(material, amount);
    }

    public void handlePurchase(String priceKey, int amount) {
        core.handlePurchase(priceKey, amount);
    }

    public void handleSale(String priceKey, int amount) {
        core.handleSale(priceKey, amount);
    }

    public boolean resetDynamicPricing(String priceKey) {
        return core.resetDynamicPricing(priceKey);
    }

    public int resetAllDynamicPricing() {
        return core.resetAllDynamicPricing();
    }

    public void setMenuLayoutForTesting(ShopMenuLayout layout) {
        core.setMenuLayoutForTesting(layout);
    }

    public void putPriceEntryForTesting(String priceKey, ShopPrice basePrice,
            double startingMultiplier, double minMultiplier, double maxMultiplier,
            double buyChange, double sellChange, double initialMultiplier) {
        core.putPriceEntryForTesting(priceKey, basePrice, startingMultiplier, minMultiplier, maxMultiplier,
                buyChange, sellChange, initialMultiplier);
    }

    public boolean setPriceMultiplierForTesting(String priceKey, double multiplier) {
        return core.setPriceMultiplierForTesting(priceKey, multiplier);
    }

    public boolean setPrice(String priceKey, double price) {
        return core.setPrice(priceKey, price);
    }

    public boolean disableBuy(String priceKey) {
        return core.disableBuy(priceKey);
    }

    public boolean disableSell(String priceKey) {
        return core.disableSell(priceKey);
    }

    public Set<String> getConfiguredPriceKeys() {
        return core.getConfiguredPriceKeys();
    }
}
