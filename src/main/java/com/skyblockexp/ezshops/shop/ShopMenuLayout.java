package com.skyblockexp.ezshops.shop;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

/**
 * Represents the layout and configuration of the shop GUI.
 */
public final class ShopMenuLayout {

    private final String mainTitle;
    private final int mainSize;
    private final ItemDecoration mainFill;
    private final ItemDecoration defaultBackButton;
    private final int defaultBackButtonSlot;
    private final List<Category> categories;

    public ShopMenuLayout(String mainTitle, int mainSize, ItemDecoration mainFill,
            ItemDecoration defaultBackButton, int defaultBackButtonSlot, List<Category> categories) {
        this.mainTitle = Objects.requireNonNull(mainTitle, "mainTitle");
        this.mainSize = mainSize;
        this.mainFill = mainFill;
        this.defaultBackButton = defaultBackButton;
        this.defaultBackButtonSlot = defaultBackButtonSlot;
        this.categories = List.copyOf(categories);
    }

    public static ShopMenuLayout empty() {
        return new ShopMenuLayout("Skyblock Shop", 27, null, null, 0, List.of());
    }

    public String mainTitle() {
        return mainTitle;
    }

    public int mainSize() {
        return mainSize;
    }

    public ItemDecoration mainFill() {
        return mainFill;
    }

    public ItemDecoration defaultBackButton() {
        return defaultBackButton;
    }

    public int defaultBackButtonSlot() {
        return defaultBackButtonSlot;
    }

    public List<Category> categories() {
        return categories;
    }

    public static final class Category {

        private final String id;
        private final String displayName;
        private final ItemDecoration icon;
        private final int slot;
        private final String menuTitle;
        private final int menuSize;
        private final ItemDecoration menuFill;
        private final ItemDecoration backButton;
        private final Integer backButtonSlot;
        private final List<Item> items;
        private final CategoryRotation rotation;
        private final String command;

        public Category(String id, String displayName, ItemDecoration icon, int slot, String menuTitle, int menuSize,
                ItemDecoration menuFill, ItemDecoration backButton, Integer backButtonSlot, List<Item> items,
                CategoryRotation rotation, String command) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.icon = icon;
            this.slot = slot;
            this.menuTitle = Objects.requireNonNull(menuTitle, "menuTitle");
            this.menuSize = menuSize;
            this.menuFill = menuFill;
            this.backButton = backButton;
            this.backButtonSlot = backButtonSlot;
            this.items = List.copyOf(items);
            this.rotation = rotation;
            this.command = command;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public ItemDecoration icon() {
            return icon;
        }

        public int slot() {
            return slot;
        }

        public String menuTitle() {
            return menuTitle;
        }

        public int menuSize() {
            return menuSize;
        }

        public ItemDecoration menuFill() {
            return menuFill;
        }

        public ItemDecoration backButton() {
            return backButton;
        }

        public Integer backButtonSlot() {
            return backButtonSlot;
        }

        public List<Item> items() {
            return items;
        }

        public CategoryRotation rotation() {
            return rotation;
        }

        public String command() {
            return command;
        }
    }

    public static final class CategoryRotation {

        private final String groupId;
        private final String optionId;

        public CategoryRotation(String groupId, String optionId) {
            this.groupId = Objects.requireNonNull(groupId, "groupId");
            this.optionId = Objects.requireNonNull(optionId, "optionId");
        }

        public String groupId() {
            return groupId;
        }

        public String optionId() {
            return optionId;
        }
    }

    public static final class Item {

        private final String id;
        private final Material material;
        private final ItemDecoration display;
        private final int slot;
        private final int amount;
        private final int bulkAmount;
        private final ShopPrice price;
        private final ItemType type;
        private final EntityType spawnerEntity;
        private final Map<Enchantment, Integer> enchantments;
        private final int requiredIslandLevel;
        private final ShopPriceType priceType;
        private final String priceId;

        public Item(String id, Material material, ItemDecoration display, int slot, int amount, int bulkAmount,
                ShopPrice price, ItemType type, EntityType spawnerEntity,
                Map<Enchantment, Integer> enchantments, int requiredIslandLevel) {
<<<<<<< Updated upstream
            this(id, material, display, slot, amount, bulkAmount, price, type, spawnerEntity, enchantments, requiredIslandLevel, ShopPriceType.STATIC);
=======
            this(id, material, display, slot, amount, bulkAmount, price, type, spawnerEntity, enchantments, requiredIslandLevel, ShopPriceType.STATIC, List.of(), List.of(), Boolean.TRUE, null);
>>>>>>> Stashed changes
        }

        public Item(String id, Material material, ItemDecoration display, int slot, int amount, int bulkAmount,
                ShopPrice price, ItemType type, EntityType spawnerEntity,
<<<<<<< Updated upstream
                Map<Enchantment, Integer> enchantments, int requiredIslandLevel, ShopPriceType priceType) {
=======
                Map<Enchantment, Integer> enchantments, int requiredIslandLevel, ShopPriceType priceType,
                java.util.List<String> buyCommands, java.util.List<String> sellCommands, Boolean commandsRunAsConsole, String priceId) {
>>>>>>> Stashed changes
            this.id = Objects.requireNonNull(id, "id");
            this.material = Objects.requireNonNull(material, "material");
            this.display = Objects.requireNonNull(display, "display");
            this.slot = slot;
            this.amount = amount;
            this.bulkAmount = bulkAmount;
            this.price = Objects.requireNonNull(price, "price");
            this.type = type == null ? ItemType.MATERIAL : type;
            this.spawnerEntity = spawnerEntity;
            this.enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
            this.requiredIslandLevel = Math.max(0, requiredIslandLevel);
            this.priceType = priceType == null ? ShopPriceType.STATIC : priceType;
            this.priceId = priceId == null ? id : priceId;
        }

        public String priceId() {
            return priceId;
        }

        public ShopPriceType priceType() {
            return priceType;
        }

        public String id() {
            return id;
        }

        public Material material() {
            return material;
        }

        public ItemDecoration display() {
            return display;
        }

        public int slot() {
            return slot;
        }

        public int amount() {
            return amount;
        }

        public int bulkAmount() {
            return bulkAmount;
        }

        public ShopPrice price() {
            return price;
        }

        public ItemType type() {
            return type;
        }

        public EntityType spawnerEntity() {
            return spawnerEntity;
        }

        public Map<Enchantment, Integer> enchantments() {
            return enchantments;
        }

        public int requiredIslandLevel() {
            return requiredIslandLevel;
        }
    }

    public enum ItemType {
        MATERIAL,
        MINION_HEAD,
        MINION_CRATE_KEY,
        VOTE_CRATE_KEY,
        SPAWNER,
        ENCHANTED_BOOK;

        public static ItemType fromConfig(String value) {
            if (value == null || value.isEmpty()) {
                return MATERIAL;
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "MINION_HEAD", "MINION_BLUEPRINT" -> MINION_HEAD;
                case "MINION_HEAD_CRATE", "MINION_BLUEPRINT_CRATE", "MINION_CRATE_KEY" -> MINION_CRATE_KEY;
                case "VOTE_CRATE", "VOTE_CRATE_KEY", "VOTE_KEY" -> VOTE_CRATE_KEY;
                case "ENCHANTED_BOOK", "ENCHANTMENT_BOOK", "ENCHANT_BOOK" -> ENCHANTED_BOOK;
                default -> {
                    for (ItemType type : values()) {
                        if (type.name().equals(normalized)) {
                            yield type;
                        }
                    }
                    yield MATERIAL;
                }
            };
        }
    }

    public static final class ItemDecoration {

        private final Material material;
        private final int amount;
        private final String displayName;
        private final List<String> lore;

        public ItemDecoration(Material material, int amount, String displayName, List<String> lore) {
            this.material = material;
            this.amount = amount;
            this.displayName = displayName;
            this.lore = lore == null ? List.of() : List.copyOf(lore);
        }

        public Material material() {
            return material;
        }

        public int amount() {
            return amount;
        }

        public String displayName() {
            return displayName;
        }

        public List<String> lore() {
            return lore;
        }
    }
}
