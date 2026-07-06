package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopRotationDefinition;
import com.skyblockexp.ezshops.shop.ShopRotationOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Parses and rebuilds shop menu layouts, including rotation-aware categories.
 */
public final class ShopPricingLayoutAssembler {

    private final Logger logger;
    private final ShopPricingLayoutSupport layoutSupport;
    private final ShopPricingValueParsers valueParsers;

    private List<ShopPricingCategoryTemplate> categoryTemplates = List.of();
    private String mainMenuTitle = "Skyblock Shop";
    private int mainMenuSize = 27;
    private ShopMenuLayout.ItemDecoration mainMenuFillDecoration = null;
    private List<ShopMenuLayout.ConfigurableButton> defaultCategoryButtons = List.of();
    private List<ShopMenuLayout.ConfigurableButton> mainMenuButtons = List.of();

    public ShopPricingLayoutAssembler(Logger logger, ShopPricingLayoutSupport layoutSupport,
            ShopPricingValueParsers valueParsers) {
        this.logger = logger;
        this.layoutSupport = layoutSupport;
        this.valueParsers = valueParsers;
    }

    public void reset() {
        categoryTemplates = List.of();
        mainMenuTitle = "Skyblock Shop";
        mainMenuSize = 27;
        mainMenuFillDecoration = null;
        defaultCategoryButtons = List.of();
        mainMenuButtons = List.of();
    }

    public ShopMenuLayout loadMenuLayout(ConfigurationSection root,
            Map<String, ShopRotationDefinition> rotationDefinitions,
            Map<String, String> activeRotationOptions,
            String loadedSourceInfo,
            ItemParser itemParser) {
        ConfigurationSection mainMenuSection = root.getConfigurationSection("main-menu");
        mainMenuTitle = layoutSupport.colorize(mainMenuSection != null
                ? mainMenuSection.getString("title", "&aSkyblock Shop") : "&aSkyblock Shop");
        mainMenuSize = layoutSupport.normalizeSize(mainMenuSection != null ? mainMenuSection.getInt("size", 54) : 54);
        mainMenuFillDecoration = layoutSupport.parseDecoration(
                mainMenuSection != null ? mainMenuSection.getConfigurationSection("fill") : null,
                new ShopMenuLayout.ItemDecoration(Material.BLACK_STAINED_GLASS_PANE, 1, ChatColor.DARK_GRAY + " ",
                        List.of()),
                loadedSourceInfo);

        ConfigurationSection categoryMenuSection = root.getConfigurationSection("category-menu");
        List<ShopMenuLayout.ConfigurableButton> parsedCategoryButtons = layoutSupport.parseButtons(categoryMenuSection);
        if (parsedCategoryButtons.isEmpty()) {
            parsedCategoryButtons = List.of(new ShopMenuLayout.ConfigurableButton(
                    "back", 49,
                    new ShopMenuLayout.ItemDecoration(Material.ARROW, 1,
                            ChatColor.YELLOW + "\u2190 Back to Shop",
                            List.of(ChatColor.GRAY + "Return to the main shop menu.")),
                    ShopMenuLayout.ButtonAction.BACK, null, 1.0f, 1.0f, null));
        }
        defaultCategoryButtons = parsedCategoryButtons;
        mainMenuButtons = layoutSupport.parseButtons(mainMenuSection);

        List<ShopPricingCategoryTemplate> templates = new ArrayList<>();
        ConfigurationSection categoriesSection = root.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryId : categoriesSection.getKeys(false)) {
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
                if (categorySection == null) {
                    logger.warning("Ignoring category '" + categoryId + "' because it is not a section.");
                    continue;
                }

                try {
                    ShopPricingCategoryTemplate template = parseCategoryTemplate(categoryId, categorySection,
                            rotationDefinitions, loadedSourceInfo, itemParser);
                    if (template != null) {
                        templates.add(template);
                    }
                } catch (RuntimeException ex) {
                    logger.warning("Failed to parse category '" + categoryId + "': " + ex.getMessage());
                }
            }
        }

        categoryTemplates = List.copyOf(templates);
        return rebuildMenuLayoutFromTemplates(rotationDefinitions, activeRotationOptions);
    }

    public ShopMenuLayout rebuildMenuLayoutFromTemplates(Map<String, ShopRotationDefinition> rotationDefinitions,
            Map<String, String> activeRotationOptions) {
        List<ShopMenuLayout.Category> categories = new ArrayList<>(categoryTemplates.size());
        for (ShopPricingCategoryTemplate template : categoryTemplates) {
            ShopMenuLayout.Category category = buildCategoryFromTemplate(template, rotationDefinitions,
                    activeRotationOptions);
            if (category != null) {
                categories.add(category);
            }
        }
        return new ShopMenuLayout(mainMenuTitle, mainMenuSize, mainMenuFillDecoration, defaultCategoryButtons,
                mainMenuButtons, categories);
    }

    public List<ShopPricingCategoryTemplate> categoryTemplates() {
        return categoryTemplates;
    }

    private ShopPricingCategoryTemplate parseCategoryTemplate(String categoryId, ConfigurationSection section,
            Map<String, ShopRotationDefinition> rotationDefinitions,
            String loadedSourceInfo,
            ItemParser itemParser) {
        String displayName = layoutSupport.colorize(section.getString("name", layoutSupport.friendlyName(categoryId)));
        ShopMenuLayout.ItemDecoration icon = layoutSupport.parseDecoration(section.getConfigurationSection("icon"),
                new ShopMenuLayout.ItemDecoration(Material.CHEST, 1, displayName, List.of()), loadedSourceInfo);

        int slot = layoutSupport.clampSlot(section.getInt("slot", 0), mainMenuSize);
        ConfigurationSection menuSection = section.getConfigurationSection("menu");
        boolean preserveLastRow = menuSection != null ? menuSection.getBoolean("preserve-last-row", true) : true;
        String menuTitle = layoutSupport.colorize(menuSection != null ? menuSection.getString("title", displayName) : displayName);
        int menuSize = layoutSupport.normalizeSize(menuSection != null ? menuSection.getInt("size", 54) : 54);
        ShopMenuLayout.ItemDecoration menuFill = layoutSupport.parseDecoration(
                menuSection != null ? menuSection.getConfigurationSection("fill") : null, null, loadedSourceInfo);
        List<ShopMenuLayout.ConfigurableButton> categoryButtons = layoutSupport.parseButtons(menuSection);

        String rotationGroupId = section.getString("rotation-group");
        if (rotationGroupId != null && rotationGroupId.isBlank()) {
            rotationGroupId = null;
        }

        if (rotationGroupId == null) {
            List<ShopMenuLayout.Item> items = parseStaticItems(categoryId, section, menuSize, itemParser);
            String command = section.getString("command", null);
            return new ShopPricingCategoryTemplate(categoryId, displayName, icon, slot, menuTitle, menuSize, menuFill,
                    categoryButtons, preserveLastRow, items, null, command);
        }

        ShopRotationDefinition definition = rotationDefinitions.get(rotationGroupId);
        if (definition == null) {
            logger.warning("Category '" + categoryId + "' references unknown rotation-group '" + rotationGroupId + "'.");
            List<ShopMenuLayout.Item> items = parseStaticItems(categoryId, section, menuSize, itemParser);
            String command = section.getString("command", null);
            return new ShopPricingCategoryTemplate(categoryId, displayName, icon, slot, menuTitle, menuSize, menuFill,
                    categoryButtons, preserveLastRow, items, null, command);
        }

        ConfigurationSection rotationDefaultsSection = section.getConfigurationSection("rotation-defaults");
        ShopMenuLayout.ItemDecoration defaultIcon = layoutSupport.parseDecoration(
                rotationDefaultsSection != null ? rotationDefaultsSection.getConfigurationSection("icon") : null,
                icon, loadedSourceInfo);
        String defaultMenuTitle = rotationDefaultsSection != null
                ? layoutSupport.colorize(rotationDefaultsSection.getString("menu-title", menuTitle)) : menuTitle;

        Map<String, Map<String, Object>> defaultItemData = ShopItemDataMapper.readItemData(
                rotationDefaultsSection != null ? rotationDefaultsSection.getConfigurationSection("items") : null,
                logger);
        if (defaultItemData.isEmpty()) {
            defaultItemData = ShopItemDataMapper.readItemData(section.getConfigurationSection("items"), logger);
        }

        Map<String, List<ShopMenuLayout.Item>> optionItems = new LinkedHashMap<>();
        String optionContextPrefixBase = "rotations." + definition.id() + ".options";
        for (ShopRotationOption option : definition.options()) {
            Map<String, Map<String, Object>> mergedItems =
                    ShopItemDataMapper.mergeItemMaps(defaultItemData, option.itemOverrides());
            List<ShopMenuLayout.Item> parsedItems = new ArrayList<>();
            if (mergedItems.isEmpty()) {
                logger.warning("Rotation option '" + option.id() + "' in group '" + definition.id()
                        + "' for category '" + categoryId + "' does not define any items.");
            } else {
                for (Map.Entry<String, Map<String, Object>> entry : mergedItems.entrySet()) {
                    ConfigurationSection itemSection = ShopItemDataMapper.createSectionFromMap(entry.getValue());
                    ShopMenuLayout.Item item = itemParser.parseItem(
                            optionContextPrefixBase + "." + option.id() + ".items",
                            entry.getKey(),
                            itemSection,
                            menuSize,
                            valueParsers);
                    if (item != null) {
                        parsedItems.add(item);
                    }
                }
            }
            optionItems.put(option.id(), List.copyOf(parsedItems));
        }

        ShopPricingRotationBinding rotation =
                new ShopPricingRotationBinding(rotationGroupId, defaultIcon, defaultMenuTitle, optionItems);
        return new ShopPricingCategoryTemplate(categoryId, displayName, icon, slot, menuTitle, menuSize, menuFill,
                categoryButtons, preserveLastRow, List.of(), rotation, section.getString("command", null));
    }

    private List<ShopMenuLayout.Item> parseStaticItems(String categoryId, ConfigurationSection section, int menuSize,
            ItemParser itemParser) {
        List<ShopMenuLayout.Item> items = new ArrayList<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection == null) {
            logger.warning("Category '" + categoryId + "' does not define any items.");
            return List.copyOf(items);
        }
        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) {
                logger.warning("Ignoring item '" + itemId + "' in category '" + categoryId
                        + "' because it is not a section.");
                continue;
            }
            ShopMenuLayout.Item item = itemParser.parseItem("categories." + categoryId + ".items", itemId,
                    itemSection, menuSize, valueParsers);
            if (item != null) {
                items.add(item);
            }
        }
        return List.copyOf(items);
    }

    private ShopMenuLayout.Category buildCategoryFromTemplate(ShopPricingCategoryTemplate template,
            Map<String, ShopRotationDefinition> rotationDefinitions,
            Map<String, String> activeRotationOptions) {
        if (template == null) {
            return null;
        }
        if (!template.isRotating()) {
            return new ShopMenuLayout.Category(template.id(), template.displayName(), template.icon(), template.slot(),
                    template.menuTitle(), template.menuSize(), template.menuFill(), template.buttons(),
                    template.preserveLastRow(), template.staticItems(), null, template.command());
        }

        ShopPricingRotationBinding binding = template.rotation();
        ShopRotationDefinition definition = rotationDefinitions.get(binding.groupId());
        if (definition == null) {
            logger.warning("Rotation group '" + binding.groupId() + "' is no longer available for category '"
                    + template.id() + "'.");
            return new ShopMenuLayout.Category(template.id(), template.displayName(), template.icon(), template.slot(),
                    template.menuTitle(), template.menuSize(), template.menuFill(), template.buttons(),
                    template.preserveLastRow(), List.of(), null, template.command());
        }

        String optionId = activeRotationOptions.getOrDefault(definition.id(), definition.defaultOptionId());
        if (!definition.containsOption(optionId)) {
            optionId = definition.defaultOptionId();
            activeRotationOptions.put(definition.id(), optionId);
        }
        ShopRotationOption option = definition.option(optionId).orElse(null);

        ShopMenuLayout.ItemDecoration icon = template.icon();
        if (binding.defaultIcon() != null) {
            icon = binding.defaultIcon();
        }
        if (option != null && option.iconOverride() != null) {
            icon = option.iconOverride();
        }

        String menuTitle = template.menuTitle();
        if (binding.defaultMenuTitle() != null) {
            menuTitle = binding.defaultMenuTitle();
        }
        if (option != null && option.menuTitleOverride() != null) {
            menuTitle = layoutSupport.colorize(option.menuTitleOverride());
        }

        List<ShopMenuLayout.Item> items = binding.itemsFor(optionId);
        if (items == null) {
            items = List.of();
        }

        ShopMenuLayout.CategoryRotation rotationState =
                new ShopMenuLayout.CategoryRotation(definition.id(), optionId);
        return new ShopMenuLayout.Category(template.id(), template.displayName(), icon, template.slot(), menuTitle,
                template.menuSize(), template.menuFill(), template.buttons(), template.preserveLastRow(), items,
                rotationState, template.command());
    }

    @FunctionalInterface
    public interface ItemParser {
        ShopMenuLayout.Item parseItem(String contextPrefix, String itemId, ConfigurationSection section,
                int menuSize, ShopPricingValueParsers valueParsers);
    }
}
