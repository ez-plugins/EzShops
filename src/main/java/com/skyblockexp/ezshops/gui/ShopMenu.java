package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.common.CompatibilityUtil;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.gui.shop.AbstractShopMenuHolder;
import com.skyblockexp.ezshops.gui.shop.CategoryShopMenuHolder;
import com.skyblockexp.ezshops.gui.shop.FlatShopMenuHolder;
import com.skyblockexp.ezshops.gui.shop.FlatShopMenuHolder.FlatMenuEntry;
import com.skyblockexp.ezshops.gui.shop.MainShopMenuHolder;
import com.skyblockexp.ezshops.gui.shop.QuantityShopMenuHolder;
import com.skyblockexp.ezshops.gui.shop.ShopInventoryComposer;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles the inventory GUI for the {@code /shop} command.
 */
public class ShopMenu implements Listener {

    public enum DisplayMode {
        CATEGORIES,
        FLAT_LIST
    }

    private final JavaPlugin plugin;
    private final ShopPricingManager pricingManager;
    private final ShopTransactionService transactionService;
    private final IslandLevelProvider islandLevelProvider;
    private final boolean ignoreIslandRequirements;
    private final DisplayMode displayMode;
    private final NamespacedKey categoryKey;
    private final NamespacedKey itemKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey quantityKey;
    private final ShopInventoryComposer inventoryComposer;
    private final ConcurrentMap<UUID, PendingTransaction> pendingCustomInputs;
    private final ShopMessageConfiguration.GuiMessages guiMessages;
    private final ShopMessageConfiguration.TransactionMessages.RestrictionMessages restrictionMessages;

    public ShopMenu(JavaPlugin plugin, ShopPricingManager pricingManager, ShopTransactionService transactionService,
            IslandLevelProvider islandLevelProvider, boolean ignoreIslandRequirements,
            ShopMessageConfiguration.GuiMessages guiMessages,
            ShopMessageConfiguration.TransactionMessages.RestrictionMessages restrictionMessages) {
        this(plugin, pricingManager, transactionService, islandLevelProvider, ignoreIslandRequirements,
                DisplayMode.CATEGORIES, guiMessages, restrictionMessages);
    }

    public ShopMenu(JavaPlugin plugin, ShopPricingManager pricingManager, ShopTransactionService transactionService,
            IslandLevelProvider islandLevelProvider, boolean ignoreIslandRequirements, DisplayMode displayMode,
            ShopMessageConfiguration.GuiMessages guiMessages,
            ShopMessageConfiguration.TransactionMessages.RestrictionMessages restrictionMessages) {
        this.plugin = plugin;
        this.pricingManager = pricingManager;
        this.transactionService = transactionService;
        this.islandLevelProvider = islandLevelProvider != null ? islandLevelProvider : player -> 0;
        this.ignoreIslandRequirements = ignoreIslandRequirements;
        this.displayMode = displayMode == null ? DisplayMode.CATEGORIES : displayMode;
        this.guiMessages = guiMessages;
        this.restrictionMessages = restrictionMessages;
        this.categoryKey = new NamespacedKey(plugin, "shop_category");
        this.itemKey = new NamespacedKey(plugin, "shop_item");
        this.actionKey = new NamespacedKey(plugin, "shop_action");
        this.quantityKey = new NamespacedKey(plugin, "shop_quantity");
        this.inventoryComposer = new ShopInventoryComposer(plugin, pricingManager, transactionService, categoryKey,
                itemKey, actionKey, quantityKey, guiMessages);
        this.pendingCustomInputs = new ConcurrentHashMap<>();
    }

    public void openMainMenu(Player player) {
        if (displayMode == DisplayMode.FLAT_LIST) {
            int islandLevel = resolvePlayerIslandLevel(player);
            inventoryComposer.openFlatMenu(player, islandLevel, ignoreIslandRequirements);
        } else {
            inventoryComposer.openMainMenu(player);
        }
    }

    public void refreshViewers() {
        Runnable refreshTask = () -> {
            ShopMenuLayout layout = pricingManager.getMenuLayout();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                InventoryView view = player.getOpenInventory();
                if (view == null) {
                    continue;
                }
                Inventory topInventory = view.getTopInventory();
                if (topInventory == null) {
                    continue;
                }
                if (!(topInventory.getHolder() instanceof AbstractShopMenuHolder holder)) {
                    continue;
                }
                if (!holder.owner().equals(player.getUniqueId())) {
                    continue;
                }

                if (holder instanceof FlatShopMenuHolder) {
                    int islandLevel = resolvePlayerIslandLevel(player);
                    inventoryComposer.openFlatMenu(player, islandLevel, ignoreIslandRequirements);
                    continue;
                }

                if (holder instanceof MainShopMenuHolder) {
                    openMainMenu(player);
                    continue;
                }

                if (holder instanceof CategoryShopMenuHolder categoryHolder) {
                    ShopMenuLayout.Category category = findCategoryById(layout, categoryHolder.category().id());
                    if (category != null) {
                        int islandLevel = resolvePlayerIslandLevel(player);
                        inventoryComposer.openCategoryMenu(player, category, categoryHolder.page(), islandLevel,
                                ignoreIslandRequirements);
                    } else {
                        openMainMenu(player);
                    }
                    continue;
                }

                if (holder instanceof QuantityShopMenuHolder quantityHolder) {
                    ShopMenuLayout.Category category =
                            findCategoryById(layout, quantityHolder.category().id());
                    ShopMenuLayout.Item item = category != null
                            ? findItemById(category, quantityHolder.item().id())
                            : null;
                    if (category != null && item != null) {
                        int islandLevel = resolvePlayerIslandLevel(player);
                        inventoryComposer.openQuantityMenu(player, category, item, quantityHolder.type(),
                                islandLevel, ignoreIslandRequirements);
                    } else if (category != null) {
                        int islandLevel = resolvePlayerIslandLevel(player);
                        inventoryComposer.openCategoryMenu(player, category, islandLevel, ignoreIslandRequirements);
                    } else {
                        openMainMenu(player);
                    }
                }
            }
        };

        if (Bukkit.isPrimaryThread()) {
            refreshTask.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, refreshTask);
        }
    }

    private void openCategory(Player player, ShopMenuLayout.Category category) {
        int islandLevel = resolvePlayerIslandLevel(player);
        inventoryComposer.openCategoryMenu(player, category, islandLevel, ignoreIslandRequirements);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof AbstractShopMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }

        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != holder) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = CompatibilityUtil.getPersistentDataContainer(meta);
        if (holder instanceof MainShopMenuHolder) {
            String categoryId = CompatibilityUtil.get(container, categoryKey, PersistentDataType.STRING);
            if (categoryId != null) {
                handleCategoryClick((Player) event.getWhoClicked(), categoryId);
            }
            return;
        }

        if (holder instanceof QuantityShopMenuHolder quantityHolder) {
            handleQuantityMenuClick(player, quantityHolder, container);
            return;
        }

        if (holder instanceof FlatShopMenuHolder flatHolder) {
            handleFlatMenuClick(player, event, flatHolder, container);
            return;
        }

        if (!(holder instanceof CategoryShopMenuHolder categoryHolder)) {
            return;
        }

        String action = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
        if (ShopInventoryComposer.ACTION_BACK.equalsIgnoreCase(action)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> openMainMenu((Player) event.getWhoClicked()));
            return;
        }

        if (ShopInventoryComposer.ACTION_PREVIOUS.equalsIgnoreCase(action)) {
            if (categoryHolder.hasPreviousPage()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> inventoryComposer.openCategoryMenu(
                        player, categoryHolder.category(), categoryHolder.page() - 1, resolvePlayerIslandLevel(player),
                        ignoreIslandRequirements));
            }
            return;
        }

        if (ShopInventoryComposer.ACTION_NEXT.equalsIgnoreCase(action)) {
            if (categoryHolder.hasNextPage()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> inventoryComposer.openCategoryMenu(
                        player, categoryHolder.category(), categoryHolder.page() + 1, resolvePlayerIslandLevel(player),
                        ignoreIslandRequirements));
            }
            return;
        }

        String itemId = CompatibilityUtil.get(container, itemKey, PersistentDataType.STRING);
        if (itemId == null) {
            return;
        }

        ShopMenuLayout.Item item = categoryHolder.findItem(itemId);
        if (item == null) {
            ((Player) event.getWhoClicked()).sendMessage(guiMessages.common().entryUnavailable());
            return;
        }

        handleItemClick(player, categoryHolder.category(), item, event.getClick());
    }

    private void handleFlatMenuClick(Player player, InventoryClickEvent event, FlatShopMenuHolder holder,
            PersistentDataContainer container) {
        String action = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
        if (ShopInventoryComposer.ACTION_PREVIOUS.equalsIgnoreCase(action)) {
            if (holder.hasPreviousPage()) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> inventoryComposer.populateFlatMenu(holder, holder.page() - 1,
                                resolvePlayerIslandLevel(player), ignoreIslandRequirements));
            }
            return;
        }

        if (ShopInventoryComposer.ACTION_NEXT.equalsIgnoreCase(action)) {
            if (holder.hasNextPage()) {
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> inventoryComposer.populateFlatMenu(holder, holder.page() + 1,
                                resolvePlayerIslandLevel(player), ignoreIslandRequirements));
            }
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= holder.getInventory().getSize()) {
            return;
        }
        FlatMenuEntry entry = holder.entryForSlot(slot);
        if (entry == null) {
            return;
        }

        handleItemClick(player, entry.category(), entry.item(), event.getClick());
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PendingTransaction pending = pendingCustomInputs.get(playerId);
        if (pending == null) {
            return;
        }

        event.setCancelled(true);
        pendingCustomInputs.remove(playerId);

        plugin.getServer().getScheduler().runTask(plugin,
                () -> handleCustomTransactionInput(event.getPlayer(), pending, event.getMessage()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingCustomInputs.remove(event.getPlayer().getUniqueId());
    }

    private void handleCategoryClick(Player player, String categoryId) {
        ShopMenuLayout layout = pricingManager.getMenuLayout();
        for (ShopMenuLayout.Category category : layout.categories()) {
            if (category.id().equalsIgnoreCase(categoryId)) {
                if (category.command() != null && !category.command().isEmpty()) {
                    // Replace {player} placeholder with player name
                    String commandToRun = category.command().replace("{player}", player.getName());
                    player.closeInventory();
                    plugin.getServer().dispatchCommand(player, commandToRun);
                } else {
                    openCategory(player, category);
                }
                return;
            }
        }
        player.sendMessage(guiMessages.common().categoryUnavailable());
    }

    private void handleQuantityMenuClick(Player player, QuantityShopMenuHolder holder,
            PersistentDataContainer container) {
        String action = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
        if (ShopInventoryComposer.ACTION_BACK.equalsIgnoreCase(action)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> openCategory(player, holder.category()));
            return;
        }

        if (ShopInventoryComposer.ACTION_CUSTOM.equalsIgnoreCase(action)) {
            startCustomInput(player, holder);
            return;
        }

        Integer quantity = CompatibilityUtil.get(container, quantityKey, PersistentDataType.INTEGER);
        if (quantity == null) {
            return;
        }

        processTransaction(player, holder, quantity);
    }

    private void handleItemClick(Player player, ShopMenuLayout.Category category, ShopMenuLayout.Item item,
            ClickType click) {
        int playerIslandLevel = resolvePlayerIslandLevel(player);
        if (!hasRequiredIslandLevel(playerIslandLevel, item)) {
            int required = Math.max(1, item.requiredIslandLevel());
            player.sendMessage(guiMessages.common().islandLevelRequired(required));
            return;
        }

        boolean isBuyClick = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
        boolean isSellClick = click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT;

        if (!isBuyClick && !isSellClick) {
            return;
        }

        if (isBuyClick) {
            if (!item.price().canBuy()) {
                player.sendMessage(guiMessages.common().itemCannotBePurchased());
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> openQuantityMenu(player, category, item, ShopTransactionType.BUY));
            return;
        }

        if (item.type() != ShopMenuLayout.ItemType.MATERIAL || !item.price().canSell()) {
            player.sendMessage(guiMessages.common().itemCannotBeSold());
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin,
                () -> openQuantityMenu(player, category, item, ShopTransactionType.SELL));
    }

    private void openQuantityMenu(Player player, ShopMenuLayout.Category category, ShopMenuLayout.Item item,
            ShopTransactionType type) {
        if (player == null || category == null || item == null) {
            return;
        }

        int playerIslandLevel = resolvePlayerIslandLevel(player);
        openQuantityMenu(player, category, item, type, playerIslandLevel);
    }

    private void openQuantityMenu(Player player, ShopMenuLayout.Category category, ShopMenuLayout.Item item,
            ShopTransactionType type, int playerIslandLevel) {
        if (player == null || category == null || item == null) {
            return;
        }

        if (!hasRequiredIslandLevel(playerIslandLevel, item)) {
            int required = Math.max(1, item.requiredIslandLevel());
            player.sendMessage(guiMessages.common().islandLevelRequired(required));
            return;
        }

        if (type == ShopTransactionType.SELL
                && (item.type() != ShopMenuLayout.ItemType.MATERIAL || !item.price().canSell())) {
            player.sendMessage(guiMessages.common().itemCannotBeSold());
            return;
        }

        inventoryComposer.openQuantityMenu(player, category, item, type, playerIslandLevel,
                ignoreIslandRequirements);
    }

    private void startCustomInput(Player player, QuantityShopMenuHolder holder) {
        if (player == null || holder == null) {
            return;
        }

        pendingCustomInputs.put(player.getUniqueId(),
                new PendingTransaction(holder.category(), holder.item(), holder.type()));
        player.closeInventory();
        player.sendMessage(guiMessages.customInput().prompt(holder.type()));
    }

    private void handleCustomTransactionInput(Player player, PendingTransaction pending, String message) {
        if (player == null || pending == null) {
            return;
        }

        String trimmed = message == null ? "" : message.trim();
        if (trimmed.equalsIgnoreCase("cancel")) {
            player.sendMessage(guiMessages.customInput().cancelled());
            openQuantityMenu(player, pending.category(), pending.item(), pending.type());
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            player.sendMessage(guiMessages.customInput().invalidNumber());
            openQuantityMenu(player, pending.category(), pending.item(), pending.type());
            return;
        }

        if (amount <= 0) {
            player.sendMessage(guiMessages.customInput().amountPositive());
            openQuantityMenu(player, pending.category(), pending.item(), pending.type());
            return;
        }

        ShopTransactionResult result = processTransaction(player,
                new QuantityShopMenuHolder(player.getUniqueId(), pending.category(), pending.item(), pending.type()),
                amount);
        if (result == null || !result.success()) {
            openQuantityMenu(player, pending.category(), pending.item(), pending.type());
        }
    }

    private ShopMenuLayout.Category findCategoryById(ShopMenuLayout layout, String categoryId) {
        if (layout == null || categoryId == null) {
            return null;
        }
        for (ShopMenuLayout.Category category : layout.categories()) {
            if (category.id().equalsIgnoreCase(categoryId)) {
                return category;
            }
        }
        return null;
    }

    private ShopMenuLayout.Item findItemById(ShopMenuLayout.Category category, String itemId) {
        if (category == null || itemId == null) {
            return null;
        }
        for (ShopMenuLayout.Item item : category.items()) {
            if (item.id().equalsIgnoreCase(itemId)) {
                return item;
            }
        }
        return null;
    }

    private ShopTransactionResult processTransaction(Player player, QuantityShopMenuHolder holder, int amount) {
        if (holder == null || amount <= 0) {
            return null;
        }

        ShopMenuLayout.Item item = holder.item();
        int normalized = normalizeQuantity(item, holder.type(), amount);
        ShopTransactionResult result = executeTransaction(player, item, holder.type(), normalized);
        if (result == null) {
            return null;
        }

        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }

        if (result.success()) {
            refreshQuantityMenu(player, holder);
        }

        return result;
    }

    private void refreshQuantityMenu(Player player, QuantityShopMenuHolder holder) {
        if (player == null || holder == null) {
            return;
        }

        ShopMenuLayout.Category category = holder.category();
        ShopMenuLayout.Item item = holder.item();
        if (category == null || item == null) {
            return;
        }

        int islandLevel = resolvePlayerIslandLevel(player);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> openQuantityMenu(player, category, item, holder.type(), islandLevel));
    }

    private int normalizeQuantity(ShopMenuLayout.Item item, ShopTransactionType type, int amount) {
        if (amount <= 0) {
            return 0;
        }

        if (type == ShopTransactionType.SELL || item == null) {
            return amount;
        }

        return Math.max(1, amount);
    }

    private ShopTransactionResult executeTransaction(Player player, ShopMenuLayout.Item item, ShopTransactionType type,
            int amount) {
        if (item == null || player == null || amount <= 0) {
            return null;
        }

        if (type == ShopTransactionType.SELL) {
            if (item.type() != ShopMenuLayout.ItemType.MATERIAL) {
                return ShopTransactionResult.failure(guiMessages.common().itemCannotBeSold());
            }
            return transactionService.sell(player, item.material(), amount);
        }

        return switch (item.type()) {
            case MATERIAL -> transactionService.buy(player, item.material(), amount);
            case MINION_HEAD -> ShopTransactionResult.failure(restrictionMessages.minionHeadCrateOnly());
            case MINION_CRATE_KEY -> transactionService.buyMinionCrateKey(player, item.price().buyPrice(), amount);
            case VOTE_CRATE_KEY -> transactionService.buyVoteCrateKey(player, item.price().buyPrice(), amount);
            case SPAWNER -> transactionService.buySpawner(player, item.spawnerEntity(), item.price().buyPrice(), amount);
            case ENCHANTED_BOOK -> transactionService.buyEnchantedBook(player, item, amount);
        };
    }

    private int resolvePlayerIslandLevel(Player player) {
        if (player == null) {
            return 0;
        }
        try {
            return Math.max(0, islandLevelProvider.getIslandLevel(player));
        } catch (Exception ex) {
            plugin.getLogger().warning(
                    "Unable to determine island level for " + player.getName() + ": " + ex.getMessage());
            return 0;
        }
    }

    private boolean hasRequiredIslandLevel(int playerIslandLevel, ShopMenuLayout.Item item) {
        if (item == null) {
            return true;
        }
        if (ignoreIslandRequirements) {
            return true;
        }
        int required = Math.max(0, item.requiredIslandLevel());
        if (required <= 0) {
            return true;
        }
        return playerIslandLevel >= required;
    }

    private record PendingTransaction(ShopMenuLayout.Category category, ShopMenuLayout.Item item,
            ShopTransactionType type) {
    }
}
