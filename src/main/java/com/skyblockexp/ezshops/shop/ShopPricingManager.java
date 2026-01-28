package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.common.EconomyUtils;
import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;

/**
 * Handles loading and exposing shop pricing and menu information from the plugin configuration.
 */
public class ShopPricingManager {

    private static final String MAIN_MENU_KEY = "main-menu";
    private static final String CATEGORY_MENU_KEY = "category-menu";
    private static final String CATEGORIES_KEY = "categories";
    private static final String ROTATIONS_KEY = "rotations";

    private final JavaPlugin plugin;
    private final Map<Material, PriceEntry> priceMap = new EnumMap<>(Material.class);
    private final Map<Material, ShopMenuLayout.ItemType> menuItemTypes = new EnumMap<>(Material.class);
    private final Logger logger;
    private final File dynamicStateFile;
    private final DynamicPricingConfiguration dynamicConfiguration;
    private YamlConfiguration dynamicStateConfiguration = new YamlConfiguration();
    private ShopMenuLayout menuLayout = ShopMenuLayout.empty();
    private final Map<String, ShopRotationDefinition> rotationDefinitions = new LinkedHashMap<>();
    private final Map<String, String> activeRotationOptions = new LinkedHashMap<>();
    private List<CategoryTemplate> categoryTemplates = List.of();
    private String mainMenuTitle = "Skyblock Shop";
    private int mainMenuSize = 27;
    private ShopMenuLayout.ItemDecoration mainMenuFillDecoration = null;
    private ShopMenuLayout.ItemDecoration defaultBackButtonDecoration = null;
    private int defaultBackButtonSlot = 0;

    public ShopPricingManager(JavaPlugin plugin, DynamicPricingConfiguration dynamicConfiguration) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dynamicConfiguration =
                dynamicConfiguration != null ? dynamicConfiguration : DynamicPricingConfiguration.defaults();
        this.dynamicStateFile = new File(plugin.getDataFolder(), "shop-dynamic.yml");
        reload();
    }

    /**
     * Reloads the pricing information from the configuration file.
     */
    public final void reload() {
        priceMap.clear();
        menuItemTypes.clear();
        menuLayout = ShopMenuLayout.empty();
        rotationDefinitions.clear();
        activeRotationOptions.clear();
        categoryTemplates = List.of();
        mainMenuTitle = "Skyblock Shop";
        mainMenuSize = 27;
        mainMenuFillDecoration = null;
        defaultBackButtonDecoration = null;
        defaultBackButtonSlot = 0;

        ensureDataFolder();
        if (dynamicStateFile.exists()) {
            dynamicStateConfiguration = YamlConfiguration.loadConfiguration(dynamicStateFile);
        } else {
            dynamicStateConfiguration = new YamlConfiguration();
        }

        YamlConfiguration root = loadCombinedConfiguration();
        if (root == null) {
            return;
        }
        loadLegacyEntries(root);
        parseRotations(root);
        menuLayout = loadMenuLayout(root);
        cleanupDynamicState();
    }

    public Optional<ShopPrice> getPrice(Material material) {
        PriceEntry entry = priceMap.get(material);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.priceType == ShopPriceType.STOCK_MARKET) {
            double stockPrice = com.skyblockexp.ezshops.stock.StockMarketManagerHolder.get().getPrice(material.name());
            return Optional.of(new ShopPrice(stockPrice, stockPrice));
        }
        return Optional.of(entry.currentPrice());
    }

    /**
     * Estimate the total price for purchasing/selling a given amount of a material taking
     * dynamic pricing into account. This does not mutate stored dynamic state.
     */
    public double estimateBulkTotal(Material material, int amount, ShopTransactionType type) {
        if (material == null || amount <= 0) {
            return -1.0D;
        }
        PriceEntry entry = priceMap.get(material);
        if (entry == null) {
            return -1.0D;
        }
        return entry.estimateBulkTotal(amount, type);
    }

    public boolean isConfigured(Material material) {
        return priceMap.containsKey(material);
    }

    public Collection<Material> getBuyableMaterials() {
        return filterMaterials(ShopPrice::canBuy);
    }

    public Collection<Material> getSellableMaterials() {
        return filterMaterials(ShopPrice::canSell);
    }

    public Set<Material> getConfiguredMaterials() {
        return Collections.unmodifiableSet(priceMap.keySet());
    }

    public boolean isEmpty() {
        return priceMap.isEmpty();
    }

    public ShopMenuLayout getMenuLayout() {
        return menuLayout;
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
        menuLayout = rebuildMenuLayoutFromTemplates();
        saveRotationState(rotationId, optionId);
        return true;
    }

    public ShopMenuLayout.ItemType getItemType(Material material) {
        if (material == null) {
            return ShopMenuLayout.ItemType.MATERIAL;
        }
        return menuItemTypes.getOrDefault(material, ShopMenuLayout.ItemType.MATERIAL);
    }

    private void loadLegacyEntries(ConfigurationSection root) {
        for (String key : root.getKeys(false)) {
            if (MAIN_MENU_KEY.equalsIgnoreCase(key) || CATEGORY_MENU_KEY.equalsIgnoreCase(key)
                    || CATEGORIES_KEY.equalsIgnoreCase(key) || ROTATIONS_KEY.equalsIgnoreCase(key)) {
                continue;
            }

            Material material = Material.matchMaterial(key, false);
            if (material == null) {
                logger.warning("Ignoring unknown material in shop pricing configuration: " + key);
                continue;
            }

            ConfigurationSection priceSection = root.getConfigurationSection(key);
            if (priceSection == null) {
                logger.warning("Ignoring price entry for material '" + key + "' because it is not a section.");
                continue;
            }

            double buyPrice = readPrice(priceSection, key, "buy");
            double sellPrice = readPrice(priceSection, key, "sell");

            if (Double.isNaN(buyPrice) && Double.isNaN(sellPrice)) {
                logger.warning("Ignoring price entry for material '" + key
                        + "' because no buy or sell price is defined.");
                continue;
            }

            ShopPrice price = new ShopPrice(Double.isNaN(buyPrice) ? -1.0D : buyPrice,
                    Double.isNaN(sellPrice) ? -1.0D : sellPrice);
            DynamicSettings dynamicSettings = parseDynamicSettings(priceSection, key);
            registerPrice(material, price, dynamicSettings);
        }
    }

    private void parseRotations(ConfigurationSection root) {
        ConfigurationSection rotationsSection = root.getConfigurationSection("rotations");
        if (rotationsSection == null) {
            return;
        }

        ConfigurationSection savedRotations =
                dynamicStateConfiguration != null ? dynamicStateConfiguration.getConfigurationSection("rotations") : null;

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
                        parseDecoration(optionSection.getConfigurationSection("icon"), null);
                String menuTitleOverride = optionSection.getString("menu-title");
                Map<String, Map<String, Object>> itemOverrides =
                        readItemData(optionSection.getConfigurationSection("items"));
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

    private ShopMenuLayout loadMenuLayout(ConfigurationSection root) {
        ConfigurationSection mainMenuSection = root.getConfigurationSection(MAIN_MENU_KEY);
        mainMenuTitle = colorize(mainMenuSection != null ? mainMenuSection.getString("title", "&aSkyblock Shop")
                : "&aSkyblock Shop");
        mainMenuSize = normalizeSize(mainMenuSection != null ? mainMenuSection.getInt("size", 54) : 54);
        mainMenuFillDecoration = parseDecoration(
                mainMenuSection != null ? mainMenuSection.getConfigurationSection("fill") : null,
                new ShopMenuLayout.ItemDecoration(Material.BLACK_STAINED_GLASS_PANE, 1, ChatColor.DARK_GRAY + " ",
                        List.of()));

        ConfigurationSection categoryMenuSection = root.getConfigurationSection(CATEGORY_MENU_KEY);
        defaultBackButtonDecoration = parseDecoration(
                categoryMenuSection != null ? categoryMenuSection.getConfigurationSection("back-button") : null,
                new ShopMenuLayout.ItemDecoration(Material.ARROW, 1,
                        ChatColor.YELLOW + "\u2190 Back to Shop",
                        List.of(ChatColor.GRAY + "Return to the main shop menu.")));
        defaultBackButtonSlot = clampSlot(
                categoryMenuSection != null ? categoryMenuSection.getInt("back-button-slot", 49) : 49, 54);

        List<CategoryTemplate> templates = new ArrayList<>();
        ConfigurationSection categoriesSection = root.getConfigurationSection(CATEGORIES_KEY);
        if (categoriesSection != null) {
            for (String categoryId : categoriesSection.getKeys(false)) {
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
                if (categorySection == null) {
                    logger.warning("Ignoring category '" + categoryId + "' because it is not a section.");
                    continue;
                }

                CategoryTemplate template = parseCategoryTemplate(categoryId, categorySection);
                if (template != null) {
                    templates.add(template);
                }
            }
        }

        categoryTemplates = List.copyOf(templates);
        return rebuildMenuLayoutFromTemplates();
    }

    private CategoryTemplate parseCategoryTemplate(String categoryId, ConfigurationSection section) {
        String displayName = colorize(section.getString("name", friendlyName(categoryId)));
        ShopMenuLayout.ItemDecoration icon = parseDecoration(section.getConfigurationSection("icon"),
                new ShopMenuLayout.ItemDecoration(Material.CHEST, 1, displayName, List.of()));

        int slot = clampSlot(section.getInt("slot", 0), mainMenuSize);
        ConfigurationSection menuSection = section.getConfigurationSection("menu");
        String menuTitle = colorize(menuSection != null ? menuSection.getString("title", displayName) : displayName);
        int menuSize = normalizeSize(menuSection != null ? menuSection.getInt("size", 54) : 54);
        ShopMenuLayout.ItemDecoration menuFill = parseDecoration(
                menuSection != null ? menuSection.getConfigurationSection("fill") : null, null);
        ShopMenuLayout.ItemDecoration backButton = parseDecoration(
                menuSection != null ? menuSection.getConfigurationSection("back-button") : null, null);

        Integer backButtonSlot = null;
        if (menuSection != null && menuSection.contains("back-button-slot")) {
            int slotValue = menuSection.getInt("back-button-slot");
            if (slotValue < 0 || slotValue >= menuSize) {
                logger.warning("Ignoring back button slot " + slotValue + " for category '" + categoryId
                        + "' because it is outside the menu bounds.");
            } else {
                backButtonSlot = slotValue;
            }
        }

        String rotationGroupId = section.getString("rotation-group");
        if (rotationGroupId != null && rotationGroupId.isBlank()) {
            rotationGroupId = null;
        }

        if (rotationGroupId == null) {
            List<ShopMenuLayout.Item> items = new ArrayList<>();
            ConfigurationSection itemsSection = section.getConfigurationSection("items");
            if (itemsSection == null) {
                logger.warning("Category '" + categoryId + "' does not define any items.");
            } else {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                    if (itemSection == null) {
                        logger.warning("Ignoring item '" + itemId + "' in category '" + categoryId
                                + "' because it is not a section.");
                        continue;
                    }

                    ShopMenuLayout.Item item = parseItem("categories." + categoryId + ".items", itemId, itemSection,
                            menuSize);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }

                String command = section.getString("command", null);
                return new CategoryTemplate(categoryId, displayName, icon, slot, menuTitle, menuSize, menuFill,
                    backButton, backButtonSlot, List.copyOf(items), null, command);
        }

        ShopRotationDefinition definition = rotationDefinitions.get(rotationGroupId);
        if (definition == null) {
            logger.warning("Category '" + categoryId + "' references unknown rotation-group '" + rotationGroupId + "'.");
            List<ShopMenuLayout.Item> items = new ArrayList<>();
            ConfigurationSection itemsSection = section.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                    if (itemSection == null) {
                        continue;
                    }
                    ShopMenuLayout.Item item = parseItem("categories." + categoryId + ".items", itemId, itemSection,
                            menuSize);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
                String command = section.getString("command", null);
                return new CategoryTemplate(categoryId, displayName, icon, slot, menuTitle, menuSize, menuFill,
                    backButton, backButtonSlot, List.copyOf(items), null, command);
        }

        ConfigurationSection rotationDefaultsSection = section.getConfigurationSection("rotation-defaults");
        ShopMenuLayout.ItemDecoration defaultIcon = parseDecoration(
                rotationDefaultsSection != null ? rotationDefaultsSection.getConfigurationSection("icon") : null, icon);
        String defaultMenuTitle = rotationDefaultsSection != null
                ? colorize(rotationDefaultsSection.getString("menu-title", menuTitle)) : menuTitle;

        Map<String, Map<String, Object>> defaultItemData = readItemData(
                rotationDefaultsSection != null ? rotationDefaultsSection.getConfigurationSection("items") : null);
        if (defaultItemData.isEmpty()) {
            defaultItemData = readItemData(section.getConfigurationSection("items"));
        }

        Map<String, List<ShopMenuLayout.Item>> optionItems = new LinkedHashMap<>();
        String optionContextPrefixBase = "rotations." + definition.id() + ".options";
        for (ShopRotationOption option : definition.options()) {
            Map<String, Map<String, Object>> mergedItems = mergeItemMaps(defaultItemData, option.itemOverrides());
            List<ShopMenuLayout.Item> parsedItems = new ArrayList<>();
            if (mergedItems.isEmpty()) {
                logger.warning("Rotation option '" + option.id() + "' in group '" + definition.id()
                        + "' for category '" + categoryId + "' does not define any items.");
            } else {
                for (Map.Entry<String, Map<String, Object>> entry : mergedItems.entrySet()) {
                    ConfigurationSection itemSection = createSectionFromMap(entry.getValue());
                    ShopMenuLayout.Item item = parseItem(optionContextPrefixBase + "." + option.id() + ".items",
                            entry.getKey(), itemSection, menuSize);
                    if (item != null) {
                        parsedItems.add(item);
                    }
                }
            }
            optionItems.put(option.id(), List.copyOf(parsedItems));
        }

        RotationBinding rotation = new RotationBinding(rotationGroupId, defaultIcon, defaultMenuTitle, optionItems);
        return new CategoryTemplate(categoryId, displayName, icon, slot, menuTitle, menuSize, menuFill, backButton,
                backButtonSlot, List.of(), rotation, section.getString("command", null));
    }

    private ShopMenuLayout rebuildMenuLayoutFromTemplates() {
        List<ShopMenuLayout.Category> categories = new ArrayList<>(categoryTemplates.size());
        for (CategoryTemplate template : categoryTemplates) {
            ShopMenuLayout.Category category = buildCategoryFromTemplate(template);
            if (category != null) {
                categories.add(category);
            }
        }
        return new ShopMenuLayout(mainMenuTitle, mainMenuSize, mainMenuFillDecoration, defaultBackButtonDecoration,
                defaultBackButtonSlot, categories);
    }

    private ShopMenuLayout.Category buildCategoryFromTemplate(CategoryTemplate template) {
        if (template == null) {
            return null;
        }
        if (!template.isRotating()) {
            return new ShopMenuLayout.Category(template.id, template.displayName, template.icon, template.slot,
                template.menuTitle, template.menuSize, template.menuFill, template.backButton,
                template.backButtonSlot, template.staticItems, null, template.command);
        }

        RotationBinding binding = template.rotation;
        ShopRotationDefinition definition = rotationDefinitions.get(binding.groupId());
        if (definition == null) {
            logger.warning("Rotation group '" + binding.groupId() + "' is no longer available for category '"
                    + template.id + "'.");
                return new ShopMenuLayout.Category(template.id, template.displayName, template.icon, template.slot,
                    template.menuTitle, template.menuSize, template.menuFill, template.backButton,
                    template.backButtonSlot, List.of(), null, template.command);
        }

        String optionId = activeRotationOptions.getOrDefault(definition.id(), definition.defaultOptionId());
        if (!definition.containsOption(optionId)) {
            optionId = definition.defaultOptionId();
            activeRotationOptions.put(definition.id(), optionId);
        }
        ShopRotationOption option = definition.option(optionId).orElse(null);

        ShopMenuLayout.ItemDecoration icon = template.icon;
        if (binding.defaultIcon() != null) {
            icon = binding.defaultIcon();
        }
        if (option != null && option.iconOverride() != null) {
            icon = option.iconOverride();
        }

        String menuTitle = template.menuTitle;
        if (binding.defaultMenuTitle() != null) {
            menuTitle = binding.defaultMenuTitle();
        }
        if (option != null && option.menuTitleOverride() != null) {
            menuTitle = colorize(option.menuTitleOverride());
        }

        List<ShopMenuLayout.Item> items = binding.itemsFor(optionId);
        if (items == null) {
            items = List.of();
        }

        ShopMenuLayout.CategoryRotation rotationState =
                new ShopMenuLayout.CategoryRotation(definition.id(), optionId);
        return new ShopMenuLayout.Category(template.id, template.displayName, icon, template.slot, menuTitle,
                template.menuSize, template.menuFill, template.backButton, template.backButtonSlot, items,
                rotationState, template.command);
    }

    private ShopMenuLayout.Item parseItem(String contextPrefix, String itemId, ConfigurationSection section,
            int menuSize) {
        String context = contextPrefix + "." + itemId;
        ShopMenuLayout.ItemType type = ShopMenuLayout.ItemType.fromConfig(section.getString("type"));
        ShopPriceType priceType = ShopPriceType.STATIC;
        String priceTypeStr = section.getString("price-type");
        if (priceTypeStr != null) {
            try {
                priceType = ShopPriceType.valueOf(priceTypeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
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

        double buyPrice = readPrice(section, context, "buy");
        double sellPrice = readPrice(section, context, "sell");
        if (Double.isNaN(buyPrice) && Double.isNaN(sellPrice)) {
            logger.warning("Ignoring item '" + context + "' because no buy or sell price is defined.");
            return null;
        }

        ShopPrice price = new ShopPrice(Double.isNaN(buyPrice) ? -1.0D : buyPrice,
                Double.isNaN(sellPrice) ? -1.0D : sellPrice);
        DynamicSettings dynamicSettings = parseDynamicSettings(section, context);

        if (type == ShopMenuLayout.ItemType.MATERIAL || type == ShopMenuLayout.ItemType.MINION_CRATE_KEY
                || type == ShopMenuLayout.ItemType.VOTE_CRATE_KEY) {
            registerPrice(material, price, dynamicSettings, priceType);
        }

        EntityType spawnerEntity = null;
        if (type == ShopMenuLayout.ItemType.SPAWNER) {
            String entityKey = section.getString("spawner-entity");
            if (entityKey == null || entityKey.isBlank()) {
                logger.warning("Ignoring item '" + context + "' because no spawner-entity is provided.");
                return null;
            }
            spawnerEntity = parseEntityType(entityKey);
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
            enchantments = parseEnchantments(context, section.getConfigurationSection("enchantments"));
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
                logger.warning("Unknown icon material '" + iconKey + "' for item '" + context + "'. Using actual material.");
            } else {
                iconMaterial = parsedIcon;
            }
        }

        int iconAmount = Math.max(1, section.getInt("icon-amount", Math.min(amount, 64)));
        iconAmount = Math.min(64, iconAmount);

        String displayName = colorize(section.getString("display-name", friendlyName(material.name())));
        List<String> lore = colorize(section.getStringList("lore"));
        ShopMenuLayout.ItemDecoration decoration = new ShopMenuLayout.ItemDecoration(iconMaterial, iconAmount, displayName,
                lore);

        int requiredIslandLevel = Math.max(0, section.getInt("required-island-level", 0));

        registerMenuItemType(material, type, context);

        // parse optional command hooks
        java.util.List<String> buyCommands = section.getStringList("buy-commands");
        java.util.List<String> sellCommands = section.getStringList("sell-commands");
        Boolean commandsRunAsConsole = null;
        // support 'on-buy'/'on-sell' blocks with execute-as and commands
        if (section.isConfigurationSection("on-buy")) {
            org.bukkit.configuration.ConfigurationSection onBuy = section.getConfigurationSection("on-buy");
            if (onBuy != null) {
                if (onBuy.isSet("commands")) {
                    buyCommands = onBuy.getStringList("commands");
                }
                String exec = onBuy.getString("execute-as", null);
                if (exec != null && exec.equalsIgnoreCase("player")) {
                    commandsRunAsConsole = Boolean.FALSE;
                }
            }
        }
        if (section.isConfigurationSection("on-sell")) {
            org.bukkit.configuration.ConfigurationSection onSell = section.getConfigurationSection("on-sell");
            if (onSell != null && onSell.isSet("commands")) {
                sellCommands = onSell.getStringList("commands");
                String exec = onSell.getString("execute-as", null);
                if (exec != null && exec.equalsIgnoreCase("player")) {
                    commandsRunAsConsole = Boolean.FALSE;
                }
            }
        }

        return new ShopMenuLayout.Item(itemId, material, decoration, slot, amount, bulkAmount, price, type,
                spawnerEntity, enchantments, requiredIslandLevel, priceType, buyCommands, sellCommands,
                commandsRunAsConsole);
    }

    private Map<String, Map<String, Object>> readItemData(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null) {
                logger.warning("Ignoring item template '" + key + "' because it is not a section.");
                continue;
            }
            values.put(key, deepCopyItemData(child));
        }
        return values;
    }

    private Map<String, Map<String, Object>> mergeItemMaps(Map<String, Map<String, Object>> defaults,
            Map<String, Map<String, Object>> overrides) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        if (defaults != null) {
            for (Map.Entry<String, Map<String, Object>> entry : defaults.entrySet()) {
                merged.put(entry.getKey(), deepCopyItemData(entry.getValue()));
            }
        }
        if (overrides != null) {
            for (Map.Entry<String, Map<String, Object>> entry : overrides.entrySet()) {
                Map<String, Object> overrideCopy = deepCopyItemData(entry.getValue());
                Map<String, Object> existing = merged.get(entry.getKey());
                if (existing == null) {
                    merged.put(entry.getKey(), overrideCopy);
                } else {
                    applyOverrides(existing, overrideCopy);
                }
            }
        }
        return merged;
    }

    private Map<String, Object> deepCopyItemData(ConfigurationSection section) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                copy.put(key, deepCopyItemData(child));
            } else {
                copy.put(key, value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopyItemData(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopyItemData((Map<String, Object>) nested));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private void applyOverrides(Map<String, Object> target, Map<String, Object> overrides) {
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                Object existing = target.get(entry.getKey());
                if (existing instanceof Map<?, ?> existingMap) {
                    applyOverrides((Map<String, Object>) existingMap, (Map<String, Object>) nested);
                } else {
                    target.put(entry.getKey(), deepCopyItemData((Map<String, Object>) nested));
                }
            } else {
                target.put(entry.getKey(), value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ConfigurationSection createSectionFromMap(Map<String, Object> values) {
        YamlConfiguration configuration = new YamlConfiguration();
        populateSection(configuration, values);
        return configuration;
    }

    @SuppressWarnings("unchecked")
    private void populateSection(ConfigurationSection target, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                ConfigurationSection child = target.createSection(entry.getKey());
                populateSection(child, (Map<String, Object>) nested);
            } else {
                target.set(entry.getKey(), value);
            }
        }
    }

    private Collection<Material> filterMaterials(PricePredicate predicate) {
        List<Material> materials = new ArrayList<>();
        for (Map.Entry<Material, PriceEntry> entry : priceMap.entrySet()) {
            if (predicate.test(entry.getValue().currentPrice())) {
                materials.add(entry.getKey());
            }
        }
        materials.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return Collections.unmodifiableList(materials);
    }

    private void registerPrice(Material material, ShopPrice price, DynamicSettings dynamicSettings) {
        PriceEntry entry = new PriceEntry(price, dynamicSettings,
                loadSavedMultiplier(material, dynamicSettings));
        PriceEntry previous = priceMap.put(material, entry);
        if (previous != null && !previous.basePrice.equals(price)) {
            logger.fine("Overriding shop price for material " + material.name() + " with new configuration values.");
        }
    }

    // Overload for price type
    private void registerPrice(Material material, ShopPrice price, DynamicSettings dynamicSettings, ShopPriceType priceType) {
        PriceEntry entry = new PriceEntry(price, dynamicSettings,
                loadSavedMultiplier(material, dynamicSettings), priceType);
        PriceEntry previous = priceMap.put(material, entry);
        if (previous != null && !previous.basePrice.equals(price)) {
            logger.fine("Overriding shop price for material " + material.name() + " with new configuration values.");
        }
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

    public void handlePurchase(Material material, int amount) {
        adjustDynamicMultiplier(material, amount, true);
    }

    public void handleSale(Material material, int amount) {
        adjustDynamicMultiplier(material, amount, false);
    }

    private void adjustDynamicMultiplier(Material material, int amount, boolean purchase) {
        if (amount <= 0) {
            return;
        }
        PriceEntry entry = priceMap.get(material);
        if (entry == null || !entry.hasDynamicPricing()) {
            return;
        }
        boolean changed = purchase ? entry.adjustAfterPurchase(amount) : entry.adjustAfterSale(amount);
        if (changed) {
            saveDynamicState(material, entry);
        }
    }

    private YamlConfiguration loadCombinedConfiguration() {
        File dataFolder = plugin.getDataFolder();
        YamlConfiguration combined = new YamlConfiguration();

        File primaryFile = new File(dataFolder, "shop.yml");
        boolean foundConfig = false;
        if (primaryFile.exists()) {
            mergeSections(combined, YamlConfiguration.loadConfiguration(primaryFile));
            foundConfig = true;
        }

        File directory = new File(dataFolder, "shop");
        if (mergeDirectory(combined, directory)) {
            foundConfig = true;
        }

        if (!foundConfig) {
            logger.warning("Shop pricing file not found: " + primaryFile.getName());
            return null;
        }

        return combined;
    }

    private boolean mergeDirectory(ConfigurationSection target, File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }

        Arrays.sort(files, Comparator.comparing(File::getName, String::compareToIgnoreCase));

        boolean merged = false;
        for (File file : files) {
            if (file.isDirectory()) {
                merged |= mergeDirectory(target, file);
                continue;
            }

            if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }

            mergeSections(target, YamlConfiguration.loadConfiguration(file));
            merged = true;
        }
        return merged;
    }

    private void mergeSections(ConfigurationSection target, ConfigurationSection source) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection section) {
                ConfigurationSection child = target.getConfigurationSection(key);
                if (child == null) {
                    child = target.createSection(key);
                }
                mergeSections(child, section);
            } else {
                target.set(key, value);
            }
        }
    }

    private void ensureDataFolder() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Unable to create plugin data folder for dynamic shop pricing state.");
        }
    }

    private double loadSavedMultiplier(Material material, DynamicSettings settings) {
        if (settings == null) {
            return 1.0D;
        }
        if (dynamicStateConfiguration == null) {
            dynamicStateConfiguration = new YamlConfiguration();
        }
        if (!dynamicStateConfiguration.isSet(material.name())) {
            return settings.clamp(settings.startingMultiplier());
        }
        return settings.clamp(dynamicStateConfiguration.getDouble(material.name(), settings.startingMultiplier()));
    }

    private void saveDynamicState(Material material, PriceEntry entry) {
        if (dynamicStateConfiguration == null || entry == null || !entry.hasDynamicPricing()) {
            return;
        }
        dynamicStateConfiguration.set(material.name(), entry.multiplier);
        try {
            dynamicStateConfiguration.save(dynamicStateFile);
        } catch (IOException ex) {
            logger.warning("Failed to save dynamic shop pricing data: " + ex.getMessage());
        }
    }

    private void saveRotationState(String rotationId, String optionId) {
        if (dynamicStateConfiguration == null) {
            dynamicStateConfiguration = new YamlConfiguration();
        }
        ConfigurationSection rotationSection = dynamicStateConfiguration.getConfigurationSection("rotations");
        if (rotationSection == null) {
            rotationSection = dynamicStateConfiguration.createSection("rotations");
        }
        rotationSection.set(rotationId, optionId);
        try {
            dynamicStateConfiguration.save(dynamicStateFile);
        } catch (IOException ex) {
            logger.warning("Failed to save rotation state: " + ex.getMessage());
        }
    }

    private void cleanupDynamicState() {
        if (dynamicStateConfiguration == null) {
            return;
        }
        boolean dirty = false;
        for (String key : new ArrayList<>(dynamicStateConfiguration.getKeys(false))) {
            if ("rotations".equalsIgnoreCase(key)) {
                continue;
            }
            Material material = Material.matchMaterial(key, false);
            PriceEntry entry = material != null ? priceMap.get(material) : null;
            if (material == null || entry == null || !entry.hasDynamicPricing()) {
                dynamicStateConfiguration.set(key, null);
                dirty = true;
            }
        }
        ConfigurationSection rotationSection = dynamicStateConfiguration.getConfigurationSection("rotations");
        if (rotationSection != null) {
            for (String key : new ArrayList<>(rotationSection.getKeys(false))) {
                ShopRotationDefinition definition = rotationDefinitions.get(key);
                String optionId = rotationSection.getString(key);
                if (definition == null || !definition.containsOption(optionId)) {
                    rotationSection.set(key, null);
                    dirty = true;
                }
            }
            if (rotationSection.getKeys(false).isEmpty()) {
                dynamicStateConfiguration.set("rotations", null);
                dirty = true;
            }
        }
        if (dirty) {
            try {
                dynamicStateConfiguration.save(dynamicStateFile);
            } catch (IOException ex) {
                logger.warning("Failed to clean up dynamic shop pricing data: " + ex.getMessage());
            }
        }
    }

    private DynamicSettings parseDynamicSettings(ConfigurationSection section, String materialKey) {
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

        double startingMultiplier =
                readDynamicValue(dynamicSection, materialKey, "starting-multiplier",
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
            logger.warning("Invalid starting-multiplier for dynamic pricing on material '" + materialKey + "'. Using "
                    + dynamicConfiguration.defaultStartingMultiplier() + '.');
            startingMultiplier = dynamicConfiguration.defaultStartingMultiplier();
        }
        if (Double.isNaN(minMultiplier) || minMultiplier <= 0.0D) {
            logger.warning("Invalid min-multiplier for dynamic pricing on material '" + materialKey + "'. Using "
                    + dynamicConfiguration.defaultMinMultiplier() + '.');
            minMultiplier = dynamicConfiguration.defaultMinMultiplier();
        }
        if (Double.isNaN(maxMultiplier) || maxMultiplier <= 0.0D) {
            logger.warning("Invalid max-multiplier for dynamic pricing on material '" + materialKey + "'. Using "
                    + dynamicConfiguration.defaultMaxMultiplier() + '.');
            maxMultiplier = dynamicConfiguration.defaultMaxMultiplier();
        }
        if (maxMultiplier < minMultiplier) {
            double temp = maxMultiplier;
            maxMultiplier = minMultiplier;
            minMultiplier = temp;
        }
        if (Double.isNaN(buyChange) || buyChange < 0.0D) {
            logger.warning("Invalid buy-change for dynamic pricing on material '" + materialKey + "'. Using "
                    + dynamicConfiguration.defaultBuyChange() + '.');
            buyChange = dynamicConfiguration.defaultBuyChange();
        }
        if (Double.isNaN(sellChange) || sellChange < 0.0D) {
            logger.warning("Invalid sell-change for dynamic pricing on material '" + materialKey + "'. Using "
                    + dynamicConfiguration.defaultSellChange() + '.');
            sellChange = dynamicConfiguration.defaultSellChange();
        }

        return new DynamicSettings(startingMultiplier, minMultiplier, maxMultiplier, buyChange, sellChange);
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

    private int normalizeSize(int requested) {
        return normalizeSize(requested, 54);
    }

    private int normalizeSize(int requested, int defaultSize) {
        int size = requested <= 0 ? defaultSize : requested;
        size = Math.min(54, Math.max(9, size));
        if (size % 9 != 0) {
            size += 9 - (size % 9);
        }
        return size;
    }

    private int clampSlot(int slot, int menuSize) {
        if (slot < 0) {
            return 0;
        }
        if (slot >= menuSize) {
            return menuSize - 1;
        }
        return slot;
    }

    private ShopMenuLayout.ItemDecoration parseDecoration(ConfigurationSection section,
            ShopMenuLayout.ItemDecoration fallback) {
        if (section == null) {
            return fallback;
        }

        String materialKey = section.getString("material");
        Material material = fallback != null ? fallback.material() : Material.AIR;
        if (materialKey != null) {
            Material parsed = Material.matchMaterial(materialKey, false);
            if (parsed == null) {
                logger.warning("Unknown material '" + materialKey + "' in menu decoration configuration.");
            } else {
                material = parsed;
            }
        }

        int amount = Math.max(1, section.getInt("amount", fallback != null ? fallback.amount() : 1));
        String displayName = colorize(section.getString("display-name", fallback != null ? fallback.displayName() : null));
        List<String> lore = colorize(section.getStringList("lore"));
        return new ShopMenuLayout.ItemDecoration(material, amount, displayName, lore);
    }

    private List<String> colorize(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> colored = new ArrayList<>(lines.size());
        for (String line : lines) {
            colored.add(colorize(line));
        }
        return colored;
    }

    private String colorize(String text) {
        if (text == null) {
            return null;
        }
        // Resolve {translate:...} tokens from message configuration, then apply color codes.
        try {
            return com.skyblockexp.ezshops.config.ConfigTranslator.resolve(text, null);
        } catch (Throwable t) {
            return MessageUtil.translateColors(text);
        }
    }

    private String friendlyName(String raw) {
        String lower = raw.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
            builder.append(' ');
        }
        if (builder.length() == 0) {
            return lower;
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private static final class CategoryTemplate {

        private final String id;
        private final String displayName;
        private final ShopMenuLayout.ItemDecoration icon;
        private final int slot;
        private final String menuTitle;
        private final int menuSize;
        private final ShopMenuLayout.ItemDecoration menuFill;
        private final ShopMenuLayout.ItemDecoration backButton;
        private final Integer backButtonSlot;
        private final List<ShopMenuLayout.Item> staticItems;
        private final RotationBinding rotation;
        private final String command;

        private CategoryTemplate(String id, String displayName, ShopMenuLayout.ItemDecoration icon, int slot,
                String menuTitle, int menuSize, ShopMenuLayout.ItemDecoration menuFill,
                ShopMenuLayout.ItemDecoration backButton, Integer backButtonSlot, List<ShopMenuLayout.Item> staticItems,
                RotationBinding rotation, String command) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.icon = icon;
            this.slot = slot;
            this.menuTitle = Objects.requireNonNull(menuTitle, "menuTitle");
            this.menuSize = menuSize;
            this.menuFill = menuFill;
            this.backButton = backButton;
            this.backButtonSlot = backButtonSlot;
            this.staticItems = staticItems == null ? List.of() : List.copyOf(staticItems);
            this.rotation = rotation;
            this.command = command;
        }

        private boolean isRotating() {
            return rotation != null;
        }
    }

    private static final class RotationBinding {

        private final String groupId;
        private final ShopMenuLayout.ItemDecoration defaultIcon;
        private final String defaultMenuTitle;
        private final Map<String, List<ShopMenuLayout.Item>> optionItems;

        private RotationBinding(String groupId, ShopMenuLayout.ItemDecoration defaultIcon, String defaultMenuTitle,
                Map<String, List<ShopMenuLayout.Item>> optionItems) {
            this.groupId = Objects.requireNonNull(groupId, "groupId");
            this.defaultIcon = defaultIcon;
            this.defaultMenuTitle = defaultMenuTitle;
            Map<String, List<ShopMenuLayout.Item>> items = new LinkedHashMap<>();
            if (optionItems != null) {
                for (Map.Entry<String, List<ShopMenuLayout.Item>> entry : optionItems.entrySet()) {
                    items.put(entry.getKey(), entry.getValue() == null ? List.of() : List.copyOf(entry.getValue()));
                }
            }
            this.optionItems = Collections.unmodifiableMap(items);
        }

        private String groupId() {
            return groupId;
        }

        private ShopMenuLayout.ItemDecoration defaultIcon() {
            return defaultIcon;
        }

        private String defaultMenuTitle() {
            return defaultMenuTitle;
        }

        private List<ShopMenuLayout.Item> itemsFor(String optionId) {
            return optionItems.get(optionId);
        }
    }

    private static final class PriceEntry {
        private final ShopPrice basePrice;
        private final DynamicSettings settings;
        private double multiplier;
        private final ShopPriceType priceType;

        private PriceEntry(ShopPrice basePrice, DynamicSettings settings, double initialMultiplier) {
            this(basePrice, settings, initialMultiplier, ShopPriceType.STATIC);
        }

        private PriceEntry(ShopPrice basePrice, DynamicSettings settings, double initialMultiplier, ShopPriceType priceType) {
            this.basePrice = basePrice;
            this.settings = settings;
            if (settings != null) {
                this.multiplier = settings.clamp(initialMultiplier);
            } else {
                this.multiplier = 1.0D;
            }
            this.priceType = priceType == null ? ShopPriceType.STATIC : priceType;
        }

        private ShopPrice currentPrice() {
            if (!hasDynamicPricing()) {
                return basePrice;
            }
            double buy = basePrice.buyPrice();
            double sell = basePrice.sellPrice();
            if (basePrice.canBuy()) {
                buy = EconomyUtils.normalizeCurrency(buy * multiplier);
            } else {
                buy = -1.0D;
            }
            if (basePrice.canSell()) {
                sell = EconomyUtils.normalizeCurrency(sell * multiplier);
            } else {
                sell = -1.0D;
            }
            return new ShopPrice(buy, sell);
        }

        private boolean hasDynamicPricing() {
            return settings != null && basePrice != null && (basePrice.canBuy() || basePrice.canSell());
        }

        private boolean adjustAfterPurchase(int amount) {
            if (!hasDynamicPricing() || amount <= 0) {
                return false;
            }
            // Apply multiplicative change per unit: multiplier *= (1 + buyChange) ^ amount
            double factor = Math.pow(1.0 + settings.buyChange(), amount);
            double previous = multiplier;
            multiplier = settings.clamp(multiplier * factor);
            return Double.compare(previous, multiplier) != 0;
        }

        private boolean adjustAfterSale(int amount) {
            if (!hasDynamicPricing() || amount <= 0) {
                return false;
            }
            // Apply multiplicative decrease per unit: multiplier *= (1 - sellChange) ^ amount
            double factor = Math.pow(1.0 - settings.sellChange(), amount);
            double previous = multiplier;
            multiplier = settings.clamp(multiplier * factor);
            return Double.compare(previous, multiplier) != 0;
        }

        private boolean applyChange(double delta) {
            if (delta == 0.0D) {
                return false;
            }
            double previous = multiplier;
            multiplier = settings.clamp(multiplier + delta);
            return Double.compare(previous, multiplier) != 0;
        }

        private double estimateBulkTotal(int amount, ShopTransactionType type) {
            if (!hasDynamicPricing() || amount <= 0) {
                // fall back to static per-unit pricing
                double unit = type == ShopTransactionType.BUY ? basePrice.buyPrice() : basePrice.sellPrice();
                return unit < 0.0D ? -1.0D : EconomyUtils.normalizeCurrency(unit * amount);
            }
            boolean isBuy = type == ShopTransactionType.BUY;
            double baseUnit = isBuy ? basePrice.buyPrice() : basePrice.sellPrice();
            if (baseUnit < 0.0D) {
                return -1.0D;
            }
            double simMultiplier = multiplier;
            double total = 0.0D;
            for (int i = 0; i < amount; i++) {
                double unitPrice = EconomyUtils.normalizeCurrency(baseUnit * simMultiplier);
                total += unitPrice;
                if (isBuy) {
                    simMultiplier = settings.clamp(simMultiplier * (1.0 + settings.buyChange()));
                } else {
                    simMultiplier = settings.clamp(simMultiplier * (1.0 - settings.sellChange()));
                }
            }
            return EconomyUtils.normalizeCurrency(total);
        }
    }

    private record DynamicSettings(double startingMultiplier, double minMultiplier, double maxMultiplier,
            double buyChange, double sellChange) {

        private double clamp(double value) {
            double clamped = Math.min(maxMultiplier, Math.max(minMultiplier, value));
            return clamped <= 0.0D ? minMultiplier : clamped;
        }
    }

    @FunctionalInterface
    private interface PricePredicate {
        boolean test(ShopPrice price);
    }

    private double readPrice(ConfigurationSection section, String materialKey, String path) {
        if (!section.contains(path)) {
            return Double.NaN;
        }

        Object value = section.get(path);
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue.trim());
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }

        logger.warning("Ignoring invalid " + path + " price for material '" + materialKey
                + "': expected a numeric value.");
        return Double.NaN;
    }

    private Map<Enchantment, Integer> parseEnchantments(String context, ConfigurationSection section) {
        if (section == null) {
            logger.warning("Ignoring item '" + context + "' because it does not define any enchantments.");
            return Map.of();
        }

        Map<Enchantment, Integer> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (key == null) {
                continue;
            }
            Enchantment enchantment = parseEnchantment(key);
            if (enchantment == null) {
                logger.warning("Ignoring enchantment '" + key + "' for item '" + context
                        + "' because it is not recognized.");
                continue;
            }
            int level = Math.max(1, section.getInt(key, 0));
            if (level <= 0) {
                logger.warning("Ignoring enchantment '" + key + "' for item '" + context
                        + "' because the configured level is not positive.");
                continue;
            }
            values.put(enchantment, Math.min(level, enchantment.getMaxLevel()));
        }

        return values;
    }

    private Enchantment parseEnchantment(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        String trimmed = key.trim();
        NamespacedKey namespacedKey;
        Enchantment enchantment;
        try {
            namespacedKey = NamespacedKey.fromString(trimmed);
        } catch (IllegalArgumentException ex) {
            namespacedKey = null;
        }
        if (namespacedKey != null) {
            enchantment = Enchantment.getByKey(namespacedKey);
            if (enchantment != null) {
                return enchantment;
            }
        }

        if (!trimmed.contains(":")) {
            enchantment = Enchantment.getByKey(NamespacedKey.minecraft(trimmed.toLowerCase(Locale.ROOT)));
            if (enchantment != null) {
                return enchantment;
            }
        }

        return Enchantment.getByName(trimmed.toUpperCase(Locale.ROOT));
    }

    private EntityType parseEntityType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return EntityType.fromName(value.trim());
        }
    }
}
