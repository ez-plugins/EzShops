package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.common.EconomyUtils;
import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import com.skyblockexp.ezshops.shop.pricing.config.ShopConfigSourceLoader;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopDynamicSettings;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopManagedPriceEntry;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopPricingMaterialQueryService;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopItemIdentifierMatcher;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingCategoryTemplate;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingItemConfigParser;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingLayoutAssembler;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingLayoutSupport;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingValueParsers;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingVisibilityService;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicPricingService;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicStateStore;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;

/**
 * Handles loading and exposing shop pricing and menu information from the plugin configuration.
 */
final class ShopPricingCore {

    private static final String MAIN_MENU_KEY = "main-menu";
    private static final String CATEGORY_MENU_KEY = "category-menu";
    private static final String CATEGORIES_KEY = "categories";
    private static final String ROTATIONS_KEY = "rotations";

    private final JavaPlugin plugin;
    private final Map<String, ShopManagedPriceEntry> priceMap = new LinkedHashMap<>();
    private final Map<Material, ShopMenuLayout.ItemType> menuItemTypes = new EnumMap<>(Material.class);
    private final Logger logger;
    private final ShopDynamicStateStore dynamicStateStore;
    private final DynamicPricingConfiguration dynamicConfiguration;
    private final ShopConfigSourceLoader configSourceLoader;
    private final ShopPricingLayoutSupport layoutSupport;
    private final ShopPricingValueParsers valueParsers;
    private final ShopPricingLayoutAssembler layoutAssembler;
    private final ShopPricingItemConfigParser itemConfigParser;
    private final ShopDynamicPricingService dynamicPricingService;
    private final ShopPricingConfigParser configParser;
    private final ShopPricingVisibilityService visibilityService;
    private final ShopPricingMaterialQueryService materialQueryService;
    private ShopMenuLayout menuLayout = ShopMenuLayout.empty();
    private final Map<String, ShopRotationDefinition> rotationDefinitions = new LinkedHashMap<>();
    private final Map<String, String> activeRotationOptions = new LinkedHashMap<>();
    private String loadedSourceInfo = "unknown";

    public ShopPricingCore(JavaPlugin plugin, DynamicPricingConfiguration dynamicConfiguration) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dynamicConfiguration =
                dynamicConfiguration != null ? dynamicConfiguration : DynamicPricingConfiguration.defaults();
        this.configSourceLoader = new ShopConfigSourceLoader(plugin, logger);
        this.layoutSupport = new ShopPricingLayoutSupport(logger);
        this.valueParsers = new ShopPricingValueParsers(logger);
        this.layoutAssembler = new ShopPricingLayoutAssembler(logger, layoutSupport, valueParsers);
        this.dynamicStateStore = new ShopDynamicStateStore(new File(plugin.getDataFolder(), "shop-dynamic.yml"),
            logger);
        this.dynamicPricingService = new ShopDynamicPricingService(priceMap, dynamicStateStore,
            this.dynamicConfiguration, logger);
        this.configParser = new ShopPricingConfigParser(logger, layoutSupport, valueParsers,
                dynamicPricingService, dynamicStateStore);
        this.visibilityService = new ShopPricingVisibilityService();
        this.materialQueryService = new ShopPricingMaterialQueryService(priceMap);
        this.itemConfigParser = new ShopPricingItemConfigParser(logger, layoutSupport, dynamicPricingService,
            menuItemTypes);
        reload();
    }

    public final void reload() {
        priceMap.clear();
        menuItemTypes.clear();
        menuLayout = ShopMenuLayout.empty();
        rotationDefinitions.clear();
        activeRotationOptions.clear();
        layoutAssembler.reset();

        configSourceLoader.ensureDataFolder();
        dynamicStateStore.load();

        loadedSourceInfo = configSourceLoader.buildSourceInfo();
        YamlConfiguration root = configSourceLoader.loadCombinedConfiguration();
        if (root == null) {
            return;
        }
        configParser.loadLegacyEntries(root, loadedSourceInfo);
        configParser.parseRotations(root, rotationDefinitions, activeRotationOptions, loadedSourceInfo);
        menuLayout = layoutAssembler.loadMenuLayout(root, rotationDefinitions, activeRotationOptions,
            loadedSourceInfo, itemConfigParser::parseItem);
        cleanupDynamicState();
        int categories = menuLayout != null ? menuLayout.categories().size() : 0;
        logger.info(loadedSourceInfo + ": Shop configuration loaded: " + priceMap.size() + " item(s) across " + categories + " categor" + (categories == 1 ? "y" : "ies") + ".");
    }

    public Optional<ShopPrice> getPrice(Material material) {
        if (material == null) {
            return Optional.empty();
        }
        ShopManagedPriceEntry entry = priceMap.get(material.name());
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.priceType() == ShopPriceType.STOCK_MARKET) {
            double stockPrice = com.skyblockexp.ezshops.stock.StockMarketManagerHolder.get().getPrice(material.name());
            return Optional.of(new ShopPrice(stockPrice, stockPrice));
        }
        return Optional.of(entry.currentPrice());
    }

    public double estimateBulkTotal(Material material, int amount, ShopTransactionType type) {
        if (material == null || amount <= 0) {
            return -1.0D;
        }
        ShopManagedPriceEntry entry = priceMap.get(material.name());
        if (entry == null) {
            return -1.0D;
        }
        return entry.estimateBulkTotal(amount, type);
    }

    public boolean isConfigured(Material material) {
        return material != null && priceMap.containsKey(material.name());
    }

    public Collection<Material> getBuyableMaterials() {
        return materialQueryService.filterMaterials(ShopPrice::canBuy);
    }

    public Collection<Material> getSellableMaterials() {
        return materialQueryService.filterMaterials(ShopPrice::canSell);
    }

    public Set<Material> getConfiguredMaterials() {
        return materialQueryService.getConfiguredMaterials();
    }

    public boolean isEmpty() {
        return priceMap.isEmpty();
    }

    public ShopMenuLayout getMenuLayout() {
        return menuLayout;
    }

    public boolean isVisibleInMenu(Material material) {
        return visibilityService.isVisibleInMenu(getMenuLayout(), material);
    }

    public boolean isVisibleInMenu(String priceKey) {
        return visibilityService.isVisibleInMenu(getMenuLayout(), priceKey);
    }

    public boolean isPartOfRotation(Material material) {
        if (material == null) return false;
        String name = material.name();
        return isPartOfRotation(name);
    }

    public boolean isPartOfRotation(String priceKey) {
        return visibilityService.isPartOfRotation(priceKey, layoutAssembler.categoryTemplates(), rotationDefinitions);
    }

    public Map<String, ShopRotationDefinition> getRotationDefinitions() {
        return Collections.unmodifiableMap(rotationDefinitions);
    }

    public Map<String, String> getActiveRotationOptions() {
        return Collections.unmodifiableMap(activeRotationOptions);
    }

    public boolean setActiveRotationOption(String rotationId, String optionId) {
        ShopRotationDefinition definition = rotationDefinitions.get(rotationId);
        if (definition == null || !definition.containsOption(optionId)) {
            return false;
        }
        String current = activeRotationOptions.get(rotationId);
        if (Objects.equals(current, optionId)) {
            return true;
        }
        activeRotationOptions.put(rotationId, optionId);
        menuLayout = layoutAssembler.rebuildMenuLayoutFromTemplates(rotationDefinitions, activeRotationOptions);
        dynamicPricingService.saveRotationState(rotationId, optionId);
        return true;
    }

    public ShopMenuLayout.ItemType getItemType(Material material) {
        if (material == null) {
            return ShopMenuLayout.ItemType.MATERIAL;
        }
        return menuItemTypes.getOrDefault(material, ShopMenuLayout.ItemType.MATERIAL);
    }

    public Optional<ShopPrice> getPrice(String priceKey) {
        if (priceKey == null) {
            return Optional.empty();
        }
        ShopManagedPriceEntry entry = priceMap.get(priceKey);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.priceType() == ShopPriceType.STOCK_MARKET) {
            double stockPrice = com.skyblockexp.ezshops.stock.StockMarketManagerHolder.get().getPrice(priceKey);
            return Optional.of(new ShopPrice(stockPrice, stockPrice));
        }
        return Optional.of(entry.currentPrice());
    }

    public double estimateBulkTotal(String priceKey, int amount, ShopTransactionType type) {
        if (priceKey == null || amount <= 0) {
            return -1.0D;
        }
        ShopManagedPriceEntry entry = priceMap.get(priceKey);
        if (entry == null) {
            return -1.0D;
        }
        return entry.estimateBulkTotal(amount, type);
    }

    public void handlePurchase(Material material, int amount) {
        dynamicPricingService.handlePurchase(material, amount);
    }

    public void handleSale(Material material, int amount) {
        dynamicPricingService.handleSale(material, amount);
    }

    public void handlePurchase(String priceKey, int amount) {
        dynamicPricingService.handlePurchase(priceKey, amount);
    }

    public void handleSale(String priceKey, int amount) {
        dynamicPricingService.handleSale(priceKey, amount);
    }

    private void cleanupDynamicState() {
        dynamicPricingService.cleanupDynamicState(buildValidRotationOptionMap());
    }

    private Map<String, Set<String>> buildValidRotationOptionMap() {
        Map<String, Set<String>> validOptions = new LinkedHashMap<>();
        for (Map.Entry<String, ShopRotationDefinition> entry : rotationDefinitions.entrySet()) {
            Set<String> optionIds = new java.util.LinkedHashSet<>();
            for (ShopRotationOption option : entry.getValue().options()) {
                optionIds.add(option.id());
            }
            validOptions.put(entry.getKey(), optionIds);
        }
        return validOptions;
    }

    public boolean resetDynamicPricing(String priceKey) {
        return dynamicPricingService.resetDynamicPricing(priceKey);
    }

    public int resetAllDynamicPricing() {
        return dynamicPricingService.resetAllDynamicPricing();
    }

    public void setMenuLayoutForTesting(ShopMenuLayout layout) {
        menuLayout = layout != null ? layout : ShopMenuLayout.empty();
    }

    public void putPriceEntryForTesting(String priceKey, ShopPrice basePrice,
            double startingMultiplier, double minMultiplier, double maxMultiplier,
            double buyChange, double sellChange, double initialMultiplier) {
        if (priceKey == null || priceKey.isBlank() || basePrice == null) {
            return;
        }
        ShopDynamicSettings settings = new ShopDynamicSettings(startingMultiplier, minMultiplier, maxMultiplier,
                buyChange, sellChange);
        ShopManagedPriceEntry entry = new ShopManagedPriceEntry(basePrice, settings, initialMultiplier,
            ShopPriceType.STATIC);
        priceMap.put(priceKey, entry);
    }

    public boolean setPriceMultiplierForTesting(String priceKey, double multiplier) {
        return dynamicPricingService.setPriceMultiplierForTesting(priceKey, multiplier);
    }

    public boolean setPrice(String priceKey, double price) {
        return dynamicPricingService.setPrice(priceKey, price);
    }

    public boolean disableBuy(String priceKey) {
        return dynamicPricingService.disableBuy(priceKey);
    }

    public boolean disableSell(String priceKey) {
        return dynamicPricingService.disableSell(priceKey);
    }

    public java.util.Set<String> getConfiguredPriceKeys() {
        return dynamicPricingService.getConfiguredPriceKeys();
    }

}
