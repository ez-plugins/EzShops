package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.DeliveryType;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPriceType;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopDynamicSettings;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicPricingService;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

/**
 * Parses item entries from category/rotation configuration and registers price metadata.
 */
public final class ShopPricingItemConfigParser {

    private final Logger logger;
    private final ShopPricingLayoutSupport layoutSupport;
    private final ShopDynamicPricingService dynamicPricingService;
    private final Map<Material, ShopMenuLayout.ItemType> menuItemTypes;

    public ShopPricingItemConfigParser(Logger logger,
            ShopPricingLayoutSupport layoutSupport,
            ShopDynamicPricingService dynamicPricingService,
            Map<Material, ShopMenuLayout.ItemType> menuItemTypes) {
        this.logger = logger;
        this.layoutSupport = layoutSupport;
        this.dynamicPricingService = dynamicPricingService;
        this.menuItemTypes = menuItemTypes;
    }

    public ShopMenuLayout.Item parseItem(String contextPrefix, String itemId, ConfigurationSection section,
            int menuSize, ShopPricingValueParsers parsers) {
        String context = contextPrefix + "." + itemId;
        ShopMenuLayout.ItemType type = ShopMenuLayout.ItemType.fromConfig(section.getString("type"));
        ShopPriceType priceType = ShopPriceType.STATIC;
        String priceTypeStr = section.getString("price-type");
        if (priceTypeStr != null) {
            try {
                priceType = ShopPriceType.valueOf(priceTypeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (type == ShopMenuLayout.ItemType.MINION_HEAD) {
            logger.warning("Ignoring item '" + context + "' because minion heads are only obtainable from crates.");
            return null;
        }

        String materialKey = section.getString("material", itemId);
        Material material = materialKey != null ? Material.matchMaterial(materialKey, false) : null;
        if (material == null) {
            logger.warning("Ignoring item '" + context + "' because material '" + materialKey + "' is unknown.");
            return null;
        }

        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= menuSize) {
            logger.warning("Ignoring item '" + context + "' because slot " + slot + " is outside the menu bounds.");
            return null;
        }

        int amount = Math.max(1, section.getInt("amount", 1));
        int bulkAmount = Math.max(amount, section.getInt("bulk-amount", material.getMaxStackSize()));
        bulkAmount = Math.min(64, bulkAmount);

        int page = section.contains("page") ? Math.max(1, section.getInt("page", 1)) : 0;

        double buyPrice = parsers.readPrice(section, context, "buy");
        double sellPrice = parsers.readPrice(section, context, "sell");
        if (Double.isNaN(buyPrice) && Double.isNaN(sellPrice)) {
            logger.warning("Ignoring item '" + context + "' because no buy or sell price is defined.");
            return null;
        }

        ShopPrice price = new ShopPrice(Double.isNaN(buyPrice) ? -1.0D : buyPrice,
                Double.isNaN(sellPrice) ? -1.0D : sellPrice);
        String configuredPriceId = section.getString("price-id", null);
        if (configuredPriceId != null && configuredPriceId.isBlank()) {
            configuredPriceId = null;
        }
        String priceKey = configuredPriceId != null ? configuredPriceId : material.name();
        if (configuredPriceId == null && itemId != null && !itemId.isBlank()
                && !itemId.equalsIgnoreCase(priceKey)) {
            priceKey = itemId;
        }
        ShopDynamicSettings dynamicSettings = dynamicPricingService.parseDynamicSettings(section, priceKey);

        if (type == ShopMenuLayout.ItemType.MATERIAL || type == ShopMenuLayout.ItemType.MINION_CRATE_KEY
                || type == ShopMenuLayout.ItemType.VOTE_CRATE_KEY) {
            dynamicPricingService.registerPrice(priceKey, price, dynamicSettings, priceType);
        }

        EntityType spawnerEntity = null;
        if (type == ShopMenuLayout.ItemType.SPAWNER) {
            String entityKey = section.getString("spawner-entity");
            if (entityKey == null || entityKey.isBlank()) {
                logger.warning("Ignoring item '" + context + "' because no spawner-entity is provided.");
                return null;
            }
            spawnerEntity = parsers.parseEntityType(entityKey);
            if (spawnerEntity == null || !spawnerEntity.isSpawnable() || !spawnerEntity.isAlive()) {
                logger.warning("Ignoring item '" + context + "' because spawner-entity '" + entityKey
                        + "' is not a valid spawnable entity.");
                return null;
            }
        }

        Map<Enchantment, Integer> enchantments = Map.of();
        if (type == ShopMenuLayout.ItemType.ENCHANTED_BOOK) {
            if (material != Material.ENCHANTED_BOOK) {
                logger.warning("Ignoring item '" + context
                        + "' because enchanted books must use the ENCHANTED_BOOK material.");
                return null;
            }
            enchantments = parsers.parseEnchantments(context, section.getConfigurationSection("enchantments"));
            if (enchantments.isEmpty()) {
                logger.warning("Ignoring item '" + context + "' because no enchantments are configured for the book.");
                return null;
            }
        }

        Material iconMaterial = material;
        String iconKey = section.getString("icon");
        if (iconKey != null) {
            Material parsedIcon = Material.matchMaterial(iconKey, false);
            if (parsedIcon == null) {
                logger.warning("Unknown icon material '" + iconKey + "' for item '" + context
                        + "'. Using actual material.");
            } else {
                iconMaterial = parsedIcon;
            }
        }

        int iconAmount = Math.max(1, section.getInt("icon-amount", Math.min(amount, 64)));
        iconAmount = Math.min(64, iconAmount);

        String displayName = layoutSupport.colorize(section.getString("display-name",
                layoutSupport.friendlyName(material.name())));
        java.util.List<String> lore = layoutSupport.colorize(section.getStringList("lore"));
        ShopMenuLayout.ItemDecoration decoration = new ShopMenuLayout.ItemDecoration(iconMaterial, iconAmount,
                displayName, lore);

        int requiredIslandLevel = Math.max(0, section.getInt("required-island-level", 0));

        registerMenuItemType(material, type, context);

        java.util.List<String> buyCommands = section.getStringList("buy-commands");
        java.util.List<String> sellCommands = section.getStringList("sell-commands");
        Boolean buyCommandsRunAsConsole = null;
        Boolean sellCommandsRunAsConsole = null;
        if (section.isConfigurationSection("on-buy")) {
            ConfigurationSection onBuy = section.getConfigurationSection("on-buy");
            if (onBuy != null) {
                if (onBuy.isSet("commands")) {
                    buyCommands = onBuy.getStringList("commands");
                }
                String exec = onBuy.getString("execute-as", null);
                if (exec != null) {
                    buyCommandsRunAsConsole = !exec.equalsIgnoreCase("player");
                }
            }
        }
        if (section.isConfigurationSection("on-sell")) {
            ConfigurationSection onSell = section.getConfigurationSection("on-sell");
            if (onSell != null) {
                if (onSell.isSet("commands")) {
                    sellCommands = onSell.getStringList("commands");
                }
                String exec = onSell.getString("execute-as", null);
                if (exec != null) {
                    sellCommandsRunAsConsole = !exec.equalsIgnoreCase("player");
                }
            }
        }

        DeliveryType delivery = DeliveryType.fromConfig(section.getString("item-type"));
        return new ShopMenuLayout.Item(itemId, material, decoration, slot, page, amount, bulkAmount, price, type,
                spawnerEntity, enchantments, requiredIslandLevel, priceType, buyCommands, sellCommands,
                buyCommandsRunAsConsole, sellCommandsRunAsConsole, priceKey, delivery);
    }

    private void registerMenuItemType(Material material, ShopMenuLayout.ItemType type, String context) {
        ShopMenuLayout.ItemType previous = menuItemTypes.get(material);
        if (previous == null) {
            menuItemTypes.put(material, type);
            return;
        }

        if (previous == type) {
            return;
        }

        if (previous == ShopMenuLayout.ItemType.MATERIAL && type != ShopMenuLayout.ItemType.MATERIAL) {
            menuItemTypes.put(material, type);
            return;
        }

        if (type == ShopMenuLayout.ItemType.MATERIAL) {
            return;
        }

        logger.warning("Item '" + context + "' declares type '" + type + "' but material '" + material.name()
                + "' is already registered as '" + previous + "'.");
    }
}
