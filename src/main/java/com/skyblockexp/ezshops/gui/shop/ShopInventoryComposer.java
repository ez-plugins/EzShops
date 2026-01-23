package com.skyblockexp.ezshops.gui.shop;

import com.skyblockexp.ezshops.common.CompatibilityUtil;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.gui.shop.FlatShopMenuHolder;
import com.skyblockexp.ezshops.gui.shop.FlatShopMenuHolder.FlatMenuEntry;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopInventoryComposer {

    public static final String ACTION_BACK = "back";
    public static final String ACTION_CUSTOM = "custom";
    public static final String ACTION_PREVIOUS = "previous";
    public static final String ACTION_NEXT = "next";

    private final JavaPlugin plugin;
    private final ShopPricingManager pricingManager;
    private final ShopTransactionService transactionService;
    private final NamespacedKey categoryKey;
    private final NamespacedKey itemKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey quantityKey;
    private final ShopMessageConfiguration.GuiMessages guiMessages;
    private final ShopMessageConfiguration.GuiMessages.CommonMessages commonMessages;
    private final ShopMessageConfiguration.GuiMessages.MenuMessages menuMessages;
    private final ShopMessageConfiguration.GuiMessages.MenuMessages.MainMenuMessages mainMenuMessages;
    private final ShopMessageConfiguration.GuiMessages.MenuMessages.FlatMenuMessages flatMenuMessages;
    private final ShopMessageConfiguration.GuiMessages.MenuMessages.CategoryMenuMessages categoryMenuMessages;
    private final ShopMessageConfiguration.GuiMessages.MenuMessages.QuantityMenuMessages quantityMenuMessages;

    public ShopInventoryComposer(JavaPlugin plugin, ShopPricingManager pricingManager,
            ShopTransactionService transactionService, NamespacedKey categoryKey, NamespacedKey itemKey,
            NamespacedKey actionKey, NamespacedKey quantityKey, ShopMessageConfiguration.GuiMessages guiMessages) {
        this.plugin = plugin;
        this.pricingManager = pricingManager;
        this.transactionService = transactionService;
        this.categoryKey = categoryKey;
        this.itemKey = itemKey;
        this.actionKey = actionKey;
        this.quantityKey = quantityKey;
        this.guiMessages = guiMessages;
        this.commonMessages = guiMessages.common();
        this.menuMessages = guiMessages.menus();
        this.mainMenuMessages = menuMessages.main();
        this.flatMenuMessages = menuMessages.flat();
        this.categoryMenuMessages = menuMessages.category();
        this.quantityMenuMessages = menuMessages.quantity();
    }

    public void openMainMenu(Player player) {
        if (player == null) {
            return;
        }

        ShopMenuLayout layout = pricingManager.getMenuLayout();
        MainShopMenuHolder holder = new MainShopMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, layout.mainSize(), layout.mainTitle());
        holder.setInventory(inventory);

        applyFill(inventory, layout.mainFill());

        List<ShopMenuLayout.Category> categories = layout.categories();
        if (categories.isEmpty()) {
            inventory.setItem(Math.min(13, inventory.getSize() - 1),
                    createPlaceholderItem(Material.BARRIER, mainMenuMessages.emptyTitle(),
                            mainMenuMessages.emptyLore()));
        } else {
            for (ShopMenuLayout.Category category : categories) {
                if (category.slot() < 0 || category.slot() >= inventory.getSize()) {
                    plugin.getLogger().warning("Category '" + category.id() + "' has an invalid slot " + category.slot()
                            + " for the main shop menu.");
                    continue;
                }

                ItemStack icon = createItem(category.icon(), createCategoryPlaceholders(category));
                if (icon == null) {
                    continue;
                }
                setPersistent(icon, categoryKey, category.id());
                inventory.setItem(category.slot(), icon);
            }
        }

        player.openInventory(inventory);
    }

    public void openFlatMenu(Player player, int islandLevel, boolean ignoreIslandRequirements) {
        if (player == null) {
            return;
        }

        ShopMenuLayout layout = pricingManager.getMenuLayout();
        List<FlatMenuEntry> entries = buildFlatMenuEntries(layout);
        FlatShopMenuHolder holder = new FlatShopMenuHolder(player.getUniqueId(), entries, layout.mainSize());
        Inventory inventory = Bukkit.createInventory(holder, layout.mainSize(), layout.mainTitle());
        holder.setInventory(inventory);

        populateFlatMenu(holder, 0, islandLevel, ignoreIslandRequirements);
        player.openInventory(inventory);
    }

    public void populateFlatMenu(FlatShopMenuHolder holder, int page, int islandLevel,
            boolean ignoreIslandRequirements) {
        if (holder == null) {
            return;
        }

        Inventory inventory = holder.getInventory();
        if (inventory == null) {
            return;
        }

        ShopMenuLayout layout = pricingManager.getMenuLayout();
        inventory.clear();
        applyFill(inventory, layout.mainFill());

        holder.clearEntries();

        int totalPages = Math.max(1, holder.totalPages());
        int normalizedPage = Math.max(0, Math.min(page, totalPages - 1));
        holder.setPage(normalizedPage);

        int perPage = holder.itemsPerPage();
        int startIndex = normalizedPage * perPage;
        List<Integer> itemSlots = holder.itemSlots();
        List<FlatMenuEntry> entries = holder.entries();

        if (entries.isEmpty()) {
            inventory.setItem(Math.min(13, inventory.getSize() - 1),
                    createPlaceholderItem(Material.BARRIER, flatMenuMessages.emptyTitle(),
                            flatMenuMessages.emptyLore()));
        } else {
            for (int i = 0; i < itemSlots.size(); i++) {
                int entryIndex = startIndex + i;
                if (entryIndex >= entries.size()) {
                    break;
                }

                int slot = itemSlots.get(i);
                FlatMenuEntry entry = entries.get(entryIndex);
                ItemStack stack = createShopMenuItem(entry.category(), entry.item(), islandLevel);
                if (stack == null) {
                    continue;
                }
                stack = applyLevelRequirement(stack, entry.item().requiredIslandLevel(), islandLevel,
                        ignoreIslandRequirements);
                setPersistent(stack, itemKey, entry.item().id());
                setPersistent(stack, categoryKey, entry.category().id());
                inventory.setItem(slot, stack);
                holder.setEntry(slot, entry);
            }
        }

        if (holder.previousSlot() >= 0 && holder.hasPreviousPage()) {
            ItemStack previous = createPlaceholderItem(Material.ARROW, flatMenuMessages.previousTitle(),
                    flatMenuMessages.previousLore(normalizedPage));
            if (previous != null) {
                setPersistent(previous, actionKey, ACTION_PREVIOUS);
                inventory.setItem(holder.previousSlot(), previous);
            }
        }

        if (holder.nextSlot() >= 0 && holder.hasNextPage()) {
            ItemStack next = createPlaceholderItem(Material.ARROW, flatMenuMessages.nextTitle(),
                    flatMenuMessages.nextLore(normalizedPage + 2));
            if (next != null) {
                setPersistent(next, actionKey, ACTION_NEXT);
                inventory.setItem(holder.nextSlot(), next);
            }
        }

        if (holder.nextSlot() >= 0 && holder.previousSlot() >= 0) {
            int indicatorSlot = holder.previousSlot() + (holder.nextSlot() - holder.previousSlot()) / 2;
            if (indicatorSlot >= 0 && indicatorSlot < inventory.getSize()) {
                ItemStack indicator = createPlaceholderItem(Material.PAPER,
                        flatMenuMessages.pageIndicatorTitle(normalizedPage + 1, totalPages),
                        flatMenuMessages.pageIndicatorLore());
                if (indicator != null) {
                    inventory.setItem(indicatorSlot, indicator);
                }
            }
        }
    }

    private List<FlatMenuEntry> buildFlatMenuEntries(ShopMenuLayout layout) {
        List<FlatMenuEntry> entries = new ArrayList<>();
        for (ShopMenuLayout.Category category : layout.categories()) {
            List<ShopMenuLayout.Item> sortedItems = new ArrayList<>(category.items());
            sortedItems.sort(Comparator.comparingInt(ShopMenuLayout.Item::slot));
            for (ShopMenuLayout.Item item : sortedItems) {
                entries.add(new FlatMenuEntry(category, item));
            }
        }
        return entries;
    }

    public void openCategoryMenu(Player player, ShopMenuLayout.Category category, int islandLevel,
            boolean ignoreIslandRequirements) {
        if (player == null || category == null) {
            return;
        }

        ShopMenuLayout layout = pricingManager.getMenuLayout();
        CategoryShopMenuHolder holder = new CategoryShopMenuHolder(player.getUniqueId(), category);
        Inventory inventory = Bukkit.createInventory(holder, category.menuSize(), category.menuTitle());
        holder.setInventory(inventory);

        ShopMenuLayout.ItemDecoration fill = category.menuFill() != null ? category.menuFill() : layout.mainFill();
        applyFill(inventory, fill);

        if (category.items().isEmpty()) {
            inventory.setItem(Math.min(13, inventory.getSize() - 1),
                    createPlaceholderItem(Material.BARRIER, categoryMenuMessages.emptyTitle(),
                            categoryMenuMessages.emptyLore()));
        } else {
            for (ShopMenuLayout.Item item : category.items()) {
                if (item.slot() < 0 || item.slot() >= inventory.getSize()) {
                    plugin.getLogger().warning("Item '" + item.id() + "' in category '" + category.id()
                            + "' has an invalid slot " + item.slot() + ".");
                    continue;
                }

                ItemStack stack = createShopMenuItem(category, item, islandLevel);
                if (stack == null) {
                    continue;
                }
                stack = applyLevelRequirement(stack, item.requiredIslandLevel(), islandLevel, ignoreIslandRequirements);
                setPersistent(stack, itemKey, item.id());
                inventory.setItem(item.slot(), stack);
            }
        }

        ShopMenuLayout.ItemDecoration backButtonDecoration = category.backButton() != null ? category.backButton()
                : layout.defaultBackButton();
        if (backButtonDecoration != null) {
            int backSlot = category.backButtonSlot() != null ? category.backButtonSlot()
                    : clampSlot(layout.defaultBackButtonSlot(), inventory.getSize());
            ItemStack backButton = createItem(backButtonDecoration, Map.of("{category}", category.displayName()));
            if (backButton != null) {
                setPersistent(backButton, actionKey, ACTION_BACK);
                inventory.setItem(backSlot, backButton);
            }
        } else {
            int backSlot = category.backButtonSlot() != null ? category.backButtonSlot()
                    : clampSlot(layout.defaultBackButtonSlot(), inventory.getSize());
            ItemStack backButton = createPlaceholderItem(Material.ARROW, categoryMenuMessages.defaultBackTitle(),
                    categoryMenuMessages.defaultBackLore(category.displayName()));
            if (backButton != null) {
                setPersistent(backButton, actionKey, ACTION_BACK);
                inventory.setItem(backSlot, backButton);
            }
        }

        player.openInventory(inventory);
    }

    public void openQuantityMenu(Player player, ShopMenuLayout.Category category, ShopMenuLayout.Item item,
            ShopTransactionType type, int playerIslandLevel, boolean ignoreIslandRequirements) {
        if (player == null || category == null || item == null || type == null) {
            return;
        }

        ShopMenuLayout layout = pricingManager.getMenuLayout();
        QuantityShopMenuHolder holder = new QuantityShopMenuHolder(player.getUniqueId(), category, item, type);
        String itemName = resolveItemName(category, item, playerIslandLevel);
        String titlePrefix = type == ShopTransactionType.BUY ? quantityMenuMessages.titlePrefixBuy()
                : quantityMenuMessages.titlePrefixSell();
        Inventory inventory = Bukkit.createInventory(holder, 27, titlePrefix + ChatColor.AQUA + itemName);
        holder.setInventory(inventory);

        applyFill(inventory, layout.mainFill());

        ItemStack preview = createItem(item.display(), createItemPlaceholders(category, item, playerIslandLevel));
        preview = applyLevelRequirement(preview, item.requiredIslandLevel(), playerIslandLevel,
                ignoreIslandRequirements);
        inventory.setItem(4, preview);

        int baseAmount = Math.max(1, item.amount());
        int[] multipliers = new int[] { 1, 2, 4, 8, 16, 32, 64 };
        int[] slots = new int[] { 9, 10, 11, 12, 13, 14, 15 };
        for (int i = 0; i < multipliers.length && i < slots.length; i++) {
            int multiplier = multipliers[i];
            int total = multiplyAmount(baseAmount, multiplier);
            ItemStack option = createQuantityOptionItem(item, type, total);
            if (option == null) {
                continue;
            }
            setPersistent(option, quantityKey, total);
            inventory.setItem(slots[i], option);
        }

        ItemStack custom = createCustomAmountItem(type);
        setPersistent(custom, actionKey, ACTION_CUSTOM);
        inventory.setItem(22, custom);

        ShopMenuLayout.ItemDecoration backDecoration = layout.defaultBackButton();
        ItemStack backItem;
        if (backDecoration != null) {
            backItem = createItem(backDecoration,
                    Map.of("{category}", category.displayName(), "{name}", category.displayName()));
        } else {
            backItem = createPlaceholderItem(Material.ARROW, quantityMenuMessages.backTitle(),
                    quantityMenuMessages.backLore(category.displayName()));
        }
        if (backItem != null) {
            setPersistent(backItem, actionKey, ACTION_BACK);
            inventory.setItem(18, backItem);
        }

        player.openInventory(inventory);
    }

    private void applyFill(Inventory inventory, ShopMenuLayout.ItemDecoration fill) {
        if (fill == null || fill.material() == null || fill.material() == Material.AIR) {
            return;
        }

        ItemStack item = createItem(fill, Map.of());
        if (item == null) {
            return;
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                inventory.setItem(slot, item.clone());
            }
        }
    }

    private ItemStack createShopMenuItem(ShopMenuLayout.Category category, ShopMenuLayout.Item item, int islandLevel) {
        Map<String, String> placeholders = createItemPlaceholders(category, item, islandLevel);
        ItemStack stack = createItem(item.display(), placeholders);
        if (stack == null) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta = decorateEnchantments(meta, item.enchantments());

        ShopPrice price = resolveDisplayPrice(item);
        double buyPrice = price.buyPrice();
        double sellPrice = price.sellPrice();
        int stackAmount = Math.max(1, Math.min(64, item.material().getMaxStackSize()));

        List<String> lore = new ArrayList<>();
        lore.add(quantityMenuMessages.instructions());

        if (buyPrice >= 0.0D) {
            lore.add(quantityMenuMessages.buyLine(formatPrice(buyPrice)));
            if (stackAmount > 1) {
                double buyStackTotal = item.type() == ShopMenuLayout.ItemType.MATERIAL && item.material() != null
                        ? pricingManager.estimateBulkTotal(item.material(), stackAmount, ShopTransactionType.BUY)
                        : totalPrice(buyPrice, stackAmount);
                lore.add(quantityMenuMessages.buyStackLine(stackAmount, formatPrice(buyStackTotal)));
            }
        } else {
            lore.add(quantityMenuMessages.buyUnavailable());
        }

        if (sellPrice >= 0.0D) {
            if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
                lore.add("");
            }
            lore.add(quantityMenuMessages.sellLine(formatPrice(sellPrice)));
            if (stackAmount > 1) {
                double sellStackTotal = item.type() == ShopMenuLayout.ItemType.MATERIAL && item.material() != null
                        ? pricingManager.estimateBulkTotal(item.material(), stackAmount, ShopTransactionType.SELL)
                        : totalPrice(sellPrice, stackAmount);
                lore.add(quantityMenuMessages.sellStackLine(stackAmount, formatPrice(sellStackTotal)));
            }
        } else {
            lore.add(quantityMenuMessages.sellUnavailable());
        }

        List<String> extraLore = item.display().lore().stream().map(line -> applyPlaceholders(line, placeholders))
                .filter(line -> line != null && !line.isEmpty()).toList();
        if (!extraLore.isEmpty()) {
            if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
                lore.add("");
            }
            lore.addAll(extraLore);
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createItem(ShopMenuLayout.ItemDecoration decoration, Map<String, String> placeholders) {
        if (decoration == null || decoration.material() == null || decoration.material() == Material.AIR) {
            return null;
        }

        ItemStack stack = new ItemStack(decoration.material(), Math.max(1, Math.min(64, decoration.amount())));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (decoration.displayName() != null && !decoration.displayName().isEmpty()) {
                meta.setDisplayName(applyPlaceholders(decoration.displayName(), placeholders));
            }
            if (!decoration.lore().isEmpty()) {
                List<String> lore = decoration.lore().stream().map(line -> applyPlaceholders(line, placeholders))
                        .toList();
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack applyLevelRequirement(ItemStack item, int requiredLevel, int playerLevel,
            boolean ignoreIslandRequirements) {
        if (ignoreIslandRequirements) {
            return item;
        }
        if (item == null || requiredLevel <= 0 || playerLevel >= requiredLevel) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<String> lore = meta.getLore();
        List<String> updated = new ArrayList<>();
        if (lore != null) {
            updated.addAll(lore);
        }
        if (!updated.isEmpty()) {
            String last = updated.get(updated.size() - 1);
            if (last != null && !last.isEmpty()) {
                updated.add("");
            }
        }
        updated.add(quantityMenuMessages.levelRequirement(requiredLevel));
        meta.setLore(updated);
        item.setItemMeta(meta);
        return item;
    }

    private Map<String, String> createCategoryPlaceholders(ShopMenuLayout.Category category) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{category}", stripColor(category.displayName()));
        placeholders.put("{name}", category.displayName());
        return placeholders;
    }

    private Map<String, String> createItemPlaceholders(ShopMenuLayout.Category category, ShopMenuLayout.Item item,
            int islandLevel) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{category}", stripColor(category.displayName()));
        placeholders.put("{category_name}", category.displayName());
        placeholders.put("{material}", friendlyMaterialName(item.material()));
        placeholders.put("{material_key}", item.material().name());
        placeholders.put("{amount}", Integer.toString(item.amount()));
        placeholders.put("{bulk_amount}", Integer.toString(item.bulkAmount()));
        int stackAmount = Math.max(1, Math.min(64, item.material().getMaxStackSize()));
        placeholders.put("{stack_amount}", Integer.toString(stackAmount));
        placeholders.put("{stack_size}", Integer.toString(stackAmount));
        placeholders.put("{island_level}", Integer.toString(Math.max(0, islandLevel)));
        placeholders.put("{required_island_level}",
                item.requiredIslandLevel() > 0 ? Integer.toString(item.requiredIslandLevel()) : "");

        EntityType spawnerEntity = item.spawnerEntity();
        if (spawnerEntity != null) {
            placeholders.put("{spawner_entity}", spawnerEntity.name());
            String friendlySpawner = friendlyEntityName(spawnerEntity);
            placeholders.put("{spawner_name}", friendlySpawner);
            placeholders.put("{spawner}", friendlySpawner + " Spawner");
        } else {
            placeholders.put("{spawner_entity}", "");
            placeholders.put("{spawner_name}", "");
            placeholders.put("{spawner}", "");
        }

        placeholders.put("{enchantments}", formatEnchantments(item.enchantments()));

        ShopPrice price = resolveDisplayPrice(item);
        placeholders.put("{buy_price}", formatPrice(price.buyPrice()));
        placeholders.put("{sell_price}", formatPrice(price.sellPrice()));
        placeholders.put("{buy}", formatPrice(price.buyPrice()));
        placeholders.put("{sell}", formatPrice(price.sellPrice()));
        // Use pricing manager estimator for material-backed items with dynamic pricing
        if (item.type() == ShopMenuLayout.ItemType.MATERIAL && item.material() != null) {
            double buyAmountTotal = pricingManager.estimateBulkTotal(item.material(), item.amount(), ShopTransactionType.BUY);
            double sellAmountTotal = pricingManager.estimateBulkTotal(item.material(), item.amount(), ShopTransactionType.SELL);
            double buyBulkTotal = pricingManager.estimateBulkTotal(item.material(), item.bulkAmount(), ShopTransactionType.BUY);
            double sellBulkTotal = pricingManager.estimateBulkTotal(item.material(), item.bulkAmount(), ShopTransactionType.SELL);
            double buyStackTotal = pricingManager.estimateBulkTotal(item.material(), stackAmount, ShopTransactionType.BUY);
            double sellStackTotal = pricingManager.estimateBulkTotal(item.material(), stackAmount, ShopTransactionType.SELL);
            placeholders.put("{buy_total}", formatPrice(buyAmountTotal));
            placeholders.put("{sell_total}", formatPrice(sellAmountTotal));
            placeholders.put("{buy_bulk_total}", formatPrice(buyBulkTotal));
            placeholders.put("{sell_bulk_total}", formatPrice(sellBulkTotal));
            placeholders.put("{buy_stack_total}", formatPrice(buyStackTotal));
            placeholders.put("{sell_stack_total}", formatPrice(sellStackTotal));
        } else {
            placeholders.put("{buy_total}", formatPrice(totalPrice(price.buyPrice(), item.amount())));
            placeholders.put("{sell_total}", formatPrice(totalPrice(price.sellPrice(), item.amount())));
            placeholders.put("{buy_bulk_total}", formatPrice(totalPrice(price.buyPrice(), item.bulkAmount())));
            placeholders.put("{sell_bulk_total}", formatPrice(totalPrice(price.sellPrice(), item.bulkAmount())));
            placeholders.put("{buy_stack_total}", formatPrice(totalPrice(price.buyPrice(), stackAmount)));
            placeholders.put("{sell_stack_total}", formatPrice(totalPrice(price.sellPrice(), stackAmount)));
        }
        return placeholders;
    }

    private ShopPrice resolveDisplayPrice(ShopMenuLayout.Item item) {
        if (item == null) {
            return new ShopPrice(-1.0D, -1.0D);
        }
        if (item.type() == ShopMenuLayout.ItemType.MATERIAL) {
            return pricingManager.getPrice(item.material()).orElse(item.price());
        }
        return item.price();
    }

    private String resolveItemName(ShopMenuLayout.Category category, ShopMenuLayout.Item item, int islandLevel) {
        if (item == null) {
            return "";
        }

        ShopMenuLayout.ItemDecoration display = item.display();
        String displayName = display != null ? display.displayName() : null;
        if (displayName != null && !displayName.isEmpty()) {
            Map<String, String> placeholders = createItemPlaceholders(category, item, islandLevel);
            return stripColor(applyPlaceholders(displayName, placeholders));
        }
        return friendlyMaterialName(item.material());
    }

    private ItemStack createPlaceholderItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private int clampSlot(int slot, int inventorySize) {
        if (slot < 0) {
            return 0;
        }
        if (slot >= inventorySize) {
            return inventorySize - 1;
        }
        return slot;
    }

    private ItemStack createQuantityOptionItem(ShopMenuLayout.Item item, ShopTransactionType type, int quantity) {
        if (quantity <= 0) {
            return null;
        }

        Material iconMaterial = item.material() != null && item.material() != Material.AIR ? item.material()
                : Material.PAPER;
        ItemStack option = new ItemStack(iconMaterial,
                Math.min(iconMaterial.getMaxStackSize(), Math.max(1, quantity)));
        ItemMeta meta = option.getItemMeta();
        if (meta == null) {
            return null;
        }

        meta = decorateEnchantments(meta, item.enchantments());

        meta.setDisplayName(quantityMenuMessages.optionTitle(quantity));

        List<String> lore = new ArrayList<>();
        ShopPrice currentPrice = resolveDisplayPrice(item);
        double totalValue;
        if (item.type() == ShopMenuLayout.ItemType.MATERIAL && item.material() != null) {
            totalValue = pricingManager.estimateBulkTotal(item.material(), quantity, type);
        } else {
            double unitPrice = type == ShopTransactionType.BUY ? currentPrice.buyPrice() : currentPrice.sellPrice();
            totalValue = unitPrice >= 0 ? totalPrice(unitPrice, quantity) : -1.0D;
        }
        String totalFormatted = totalValue >= 0 ? formatPrice(totalValue) : commonMessages.priceUnavailable();
        lore.addAll(quantityMenuMessages.optionLore(type, totalFormatted, quantity));
        meta.setLore(lore);
        option.setItemMeta(meta);
        return option;
    }

    private ItemStack createCustomAmountItem(ShopTransactionType type) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(quantityMenuMessages.customTitle());
            List<String> lore = new ArrayList<>(quantityMenuMessages.customLore(type));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemMeta decorateEnchantments(ItemMeta meta, Map<Enchantment, Integer> enchantments) {
        if (meta == null || enchantments == null || enchantments.isEmpty()) {
            return meta;
        }

        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                storageMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
            }
            return storageMeta;
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        return meta;
    }

    private String formatEnchantments(Map<Enchantment, Integer> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String name = friendlyEnchantmentName(entry.getKey());
            String level = toRomanNumeral(Math.max(1, entry.getValue()));
            parts.add(name + " " + level);
        }
        return String.join(", ", parts);
    }

    private String friendlyEnchantmentName(Enchantment enchantment) {
        if (enchantment == null) {
            return "";
        }
        String key = enchantment.getKey().getKey().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        String[] parts = key.split(" ");
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
            return key;
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private String toRomanNumeral(int number) {
        if (number <= 0) {
            return Integer.toString(number);
        }

        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int remaining = number;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length && remaining > 0; i++) {
            while (remaining >= values[i]) {
                builder.append(numerals[i]);
                remaining -= values[i];
            }
        }
        return builder.toString();
    }

    private void setPersistent(ItemStack item, NamespacedKey key, String value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = CompatibilityUtil.getPersistentDataContainer(meta);
        CompatibilityUtil.set(container, key, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    private void setPersistent(ItemStack item, NamespacedKey key, int value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = CompatibilityUtil.getPersistentDataContainer(meta);
        CompatibilityUtil.set(container, key, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
    }

    private int multiplyAmount(int base, int multiplier) {
        if (base <= 0 || multiplier <= 0) {
            return 0;
        }
        long result = (long) base * (long) multiplier;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) result;
    }

    private String formatPrice(double value) {
        if (value < 0) {
            return commonMessages.priceUnavailable();
        }
        return transactionService.formatCurrency(value);
    }

    private double totalPrice(double unitPrice, int amount) {
        if (unitPrice < 0) {
            return -1;
        }
        return unitPrice * amount;
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String stripColor(String input) {
        return input == null ? "" : ChatColor.stripColor(input);
    }

    private String friendlyMaterialName(Material material) {
        String lower = material.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
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

    private String friendlyEntityName(EntityType entityType) {
        if (entityType == null) {
            return "";
        }
        String lower = entityType.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
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
}
