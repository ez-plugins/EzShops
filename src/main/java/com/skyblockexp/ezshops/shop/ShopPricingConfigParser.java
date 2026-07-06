package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.shop.pricing.domain.ShopDynamicSettings;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopItemDataMapper;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingLayoutSupport;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingValueParsers;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicPricingService;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicStateStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Parses legacy item prices and rotation configuration from the root shop config.
 */
final class ShopPricingConfigParser {

    private final Logger logger;
    private final ShopPricingLayoutSupport layoutSupport;
    private final ShopPricingValueParsers valueParsers;
    private final ShopDynamicPricingService dynamicPricingService;
    private final ShopDynamicStateStore dynamicStateStore;

    ShopPricingConfigParser(Logger logger,
            ShopPricingLayoutSupport layoutSupport,
            ShopPricingValueParsers valueParsers,
            ShopDynamicPricingService dynamicPricingService,
            ShopDynamicStateStore dynamicStateStore) {
        this.logger = logger;
        this.layoutSupport = layoutSupport;
        this.valueParsers = valueParsers;
        this.dynamicPricingService = dynamicPricingService;
        this.dynamicStateStore = dynamicStateStore;
    }

    void loadLegacyEntries(ConfigurationSection root, String loadedSourceInfo) {
        for (String key : root.getKeys(false)) {
            if ("main-menu".equalsIgnoreCase(key)
                    || "category-menu".equalsIgnoreCase(key)
                    || "categories".equalsIgnoreCase(key)
                    || "rotations".equalsIgnoreCase(key)) {
                continue;
            }

            Material material = Material.matchMaterial(key, false);
            if (material == null) {
                logger.warning(loadedSourceInfo + ": Ignoring unknown material '" + key
                        + "' in shop pricing configuration. Check for typos or invalid material names.");
                continue;
            }

            ConfigurationSection priceSection = root.getConfigurationSection(key);
            if (priceSection == null) {
                logger.warning(loadedSourceInfo + ": Ignoring entry '" + key
                        + "' because it is not a section in shop pricing configuration.");
                continue;
            }

            double buyPrice = valueParsers.readPrice(priceSection, key, "buy");
            double sellPrice = valueParsers.readPrice(priceSection, key, "sell");
            if (Double.isNaN(buyPrice) && Double.isNaN(sellPrice)) {
                logger.warning(loadedSourceInfo + ": Ignoring entry '" + key
                        + "' because no buy or sell price is defined.");
                continue;
            }

            ShopPrice price = new ShopPrice(Double.isNaN(buyPrice) ? -1.0D : buyPrice,
                    Double.isNaN(sellPrice) ? -1.0D : sellPrice);
            ShopDynamicSettings dynamicSettings = dynamicPricingService.parseDynamicSettings(priceSection,
                    material.name());
            dynamicPricingService.registerPrice(material.name(), price, dynamicSettings);
        }
    }

    void parseRotations(ConfigurationSection root,
            Map<String, ShopRotationDefinition> rotationDefinitions,
            Map<String, String> activeRotationOptions,
            String loadedSourceInfo) {
        ConfigurationSection rotationsSection = root.getConfigurationSection("rotations");
        if (rotationsSection == null) {
            return;
        }

        ConfigurationSection savedRotations = dynamicStateStore.getConfigurationSection("rotations");
        for (String rotationId : rotationsSection.getKeys(false)) {
            ConfigurationSection rotationSection = rotationsSection.getConfigurationSection(rotationId);
            if (rotationSection == null) {
                logger.warning("Ignoring rotation '" + rotationId + "' because it is not a section.");
                continue;
            }

            Duration interval = null;
            String intervalRaw = rotationSection.getString("interval");
            if (intervalRaw != null && !intervalRaw.isBlank()) {
                interval = ShopRotationDurationParser.parse(intervalRaw);
                if (interval == null) {
                    logger.warning("Rotation '" + rotationId + "' has invalid interval '" + intervalRaw + "'.");
                }
            }

            ShopRotationMode mode = ShopRotationMode.fromConfig(rotationSection.getString("mode"));
            ConfigurationSection optionsSection = rotationSection.getConfigurationSection("options");
            if (optionsSection == null || optionsSection.getKeys(false).isEmpty()) {
                logger.warning("Ignoring rotation '" + rotationId + "' because it does not define any options.");
                continue;
            }

            List<ShopRotationOption> options = new ArrayList<>();
            for (String optionId : optionsSection.getKeys(false)) {
                ConfigurationSection optionSection = optionsSection.getConfigurationSection(optionId);
                if (optionSection == null) {
                    logger.warning("Ignoring option '" + optionId + "' in rotation '" + rotationId
                            + "' because it is not a section.");
                    continue;
                }

                ShopMenuLayout.ItemDecoration iconOverride =
                        layoutSupport.parseDecoration(optionSection.getConfigurationSection("icon"), null,
                                loadedSourceInfo);
                String menuTitleOverride = optionSection.getString("menu-title");
                Map<String, Map<String, Object>> itemOverrides =
                        ShopItemDataMapper.readItemData(optionSection.getConfigurationSection("items"), logger);
                double weight = optionSection.contains("weight") ? optionSection.getDouble("weight", 1.0D) : 1.0D;
                if (weight < 0.0D) {
                    logger.warning("Rotation option '" + optionId + "' in group '" + rotationId
                            + "' declares a negative weight. Using zero instead.");
                    weight = 0.0D;
                }
                options.add(new ShopRotationOption(optionId, iconOverride, menuTitleOverride, itemOverrides, weight));
            }

            if (options.isEmpty()) {
                logger.warning("Ignoring rotation '" + rotationId + "' because no valid options were provided.");
                continue;
            }

            String defaultOption = rotationSection.getString("default-option");
            ShopRotationDefinition definition = new ShopRotationDefinition(rotationId, interval, mode, options,
                    defaultOption);
            rotationDefinitions.put(rotationId, definition);

            String activeOption = definition.defaultOptionId();
            if (savedRotations != null) {
                String saved = savedRotations.getString(rotationId);
                if (definition.containsOption(saved)) {
                    activeOption = saved;
                }
            }
            activeRotationOptions.put(rotationId, activeOption);
        }
    }
}
