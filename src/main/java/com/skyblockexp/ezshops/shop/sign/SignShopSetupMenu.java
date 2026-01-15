package com.skyblockexp.ezshops.shop.sign;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopSignListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Tag;

/**
 * Inventory-based GUI that lets administrators configure a batch of sign shops.
 */
public final class SignShopSetupMenu implements Listener {

    private static final int INVENTORY_SIZE = 45;

    private static final int SLOT_INFO = 4;
    private static final int SLOT_ACTION = 10;
    private static final int SLOT_BACKGROUND = 12;
    private static final int SLOT_DIRECTION = 14;
    private static final int SLOT_SIGN_MATERIAL = 16;
    private static final List<Integer> ITEM_SLOTS = List.of(19, 20, 21, 22, 23, 24, 25);
    private static final int SLOT_HORIZONTAL_SPACING_MINUS = 28;
    private static final int SLOT_HORIZONTAL_SPACING_DISPLAY = 31;
    private static final int SLOT_HORIZONTAL_SPACING_PLUS = 34;
    private static final int SLOT_ROW_COUNT_MINUS = 29;
    private static final int SLOT_ROW_COUNT_DISPLAY = 32;
    private static final int SLOT_ROW_COUNT_PLUS = 33;
    private static final int SLOT_ROW_SPACING_MINUS = 30;
    private static final int SLOT_ROW_SPACING_DISPLAY = 35;
    private static final int SLOT_ROW_SPACING_PLUS = 36;
    private static final int SLOT_CLEAR_ITEMS = 37;
    private static final int SLOT_CONFIRM = 40;

    private static final int CATALOG_INVENTORY_SIZE = 54;
    private static final int CATALOG_ITEMS_PER_PAGE = 45;
    private static final int CATALOG_SLOT_INSTRUCTIONS = 46;
    private static final int CATALOG_SLOT_PAGE_INFO = 48;
    private static final int CATALOG_SLOT_BACK = 49;
    private static final int CATALOG_SLOT_SOURCE_TOGGLE = 50;
    private static final int CATALOG_SLOT_SEARCH = 51;
    private static final int CATALOG_SLOT_PREVIOUS = 45;
    private static final int CATALOG_SLOT_NEXT = 53;

    private final JavaPlugin plugin;
    private final SignShopGenerator generator;
    private final ShopPricingManager pricingManager;
    private final Map<UUID, MenuState> openMenus;
    private static final List<Material> WALL_SIGN_MATERIALS;

    public SignShopSetupMenu(JavaPlugin plugin, SignShopGenerator generator, ShopPricingManager pricingManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.pricingManager = pricingManager;
        this.openMenus = new HashMap<>();
    }

    static {
        List<Material> materials = new ArrayList<>(Tag.WALL_SIGNS.getValues());
        materials.sort(Comparator.comparing(Material::name));
        WALL_SIGN_MATERIALS = List.copyOf(materials);
    }

    /**
     * Opens the sign shop setup menu for the given player.
     */
    public void open(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        MenuState state = openMenus.computeIfAbsent(id, ignored -> new MenuState());
        state.selectionInventory = null;
        state.selectionEntries = List.of();
        state.selectionPage = 0;
        state.reopenToPlanner = false;
        state.awaitingSearchInput = false;
        state.pendingItemSlot = state.pendingItemSlot < 0 ? -1 : Math.min(state.pendingItemSlot, state.items.size() - 1);
        state.inventory = Bukkit.createInventory(player, INVENTORY_SIZE, ChatColor.GREEN + "Sign Shop Setup");
        redraw(state);
        player.openInventory(state.inventory);
        if (!state.instructionsShown) {
            state.instructionsShown = true;
            player.sendMessage(ChatColor.AQUA
                    + "Left-click a slot to browse shop items or right-click to use one from your inventory.");
            player.sendMessage(ChatColor.AQUA
                    + "Look at the block that should sit behind the first sign before clicking Confirm.");
            player.sendMessage(ChatColor.AQUA
                    + "Use the direction toggle to choose whether signs extend left or right from that block.");
            player.sendMessage(ChatColor.AQUA
                    + "Adjust the row and spacing controls to stack signs vertically or add gaps between them.");
            player.sendMessage(ChatColor.AQUA + "Shift-click filled slots to fine tune their quantities.");
            player.sendMessage(ChatColor.GRAY
                    + "Close the menu to pause setup. Run /signshopsetup again to resume or use /signshopsetup cancel to discard.");
        }
    }

    /**
     * Cancels any in-progress setup for the player and closes open inventories.
     */
    public boolean cancel(Player player) {
        if (player == null) {
            return false;
        }
        MenuState state = openMenus.remove(player.getUniqueId());
        if (state == null) {
            return false;
        }
        state.awaitingSearchInput = false;
        state.selectionInventory = null;
        state.inventory = null;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.closeInventory();
            }
        });
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        MenuState state = openMenus.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();
        if (state.selectionInventory != null && topInventory.equals(state.selectionInventory)) {
            if (clickedInventory != null && clickedInventory.equals(state.selectionInventory)) {
                handleItemCatalogClick(event, player, state);
            } else {
                event.setCancelled(true);
            }
            return;
        }
        if (state.inventory == null) {
            return;
        }
        if (clickedInventory == null || !clickedInventory.equals(state.inventory)) {
            if (topInventory.equals(state.inventory) && clickedInventory != null) {
                handlePlayerInventoryClick(event, player, state);
            }
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= state.inventory.getSize()) {
            return;
        }

        if (slot == SLOT_ACTION) {
            state.action = toggle(state.action);
            redraw(state);
            return;
        }
        if (slot == SLOT_BACKGROUND) {
            if (event.getClick().isRightClick()) {
                state.awaitingBackground = false;
                state.keepExistingBackground = !state.keepExistingBackground;
                if (!state.keepExistingBackground && state.background == null) {
                    state.background = SignShopPlan.DEFAULT_BACKGROUND;
                }
                if (state.keepExistingBackground) {
                    player.sendMessage(ChatColor.GREEN
                            + "Existing backing blocks will be kept when generating the sign shops.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Backing blocks will be replaced with "
                            + readable(state.background) + '.');
                }
                redraw(state);
            } else {
                state.awaitingBackground = true;
                state.keepExistingBackground = false;
                player.sendMessage(ChatColor.GREEN
                        + "Click a block in your inventory to use it as the background behind each sign.");
            }
            return;
        }
        if (slot == SLOT_DIRECTION) {
            state.direction = toggle(state.direction);
            player.sendMessage(ChatColor.GREEN + "Sign shops will extend to the "
                    + (state.direction == SignShopPlan.LayoutDirection.RIGHT ? "right" : "left") + '.');
            redraw(state);
            return;
        }
        if (slot == SLOT_SIGN_MATERIAL) {
            state.signMaterial = cycleSignMaterial(state.signMaterial, event.getClick() == ClickType.RIGHT ? -1 : 1);
            player.sendMessage(ChatColor.GREEN + "Wall sign material set to " + readable(state.signMaterial) + '.');
            redraw(state);
            return;
        }
        if (slot == SLOT_HORIZONTAL_SPACING_MINUS) {
            state.spacing = Math.max(0, state.spacing - 1);
            redraw(state);
            return;
        }
        if (slot == SLOT_HORIZONTAL_SPACING_PLUS) {
            state.spacing = Math.min(8, state.spacing + 1);
            redraw(state);
            return;
        }
        if (slot == SLOT_ROW_COUNT_MINUS) {
            state.rows = Math.max(1, state.rows - 1);
            redraw(state);
            return;
        }
        if (slot == SLOT_ROW_COUNT_PLUS) {
            state.rows = Math.min(ITEM_SLOTS.size(), state.rows + 1);
            redraw(state);
            return;
        }
        if (slot == SLOT_ROW_SPACING_MINUS) {
            state.rowSpacing = Math.max(0, state.rowSpacing - 1);
            redraw(state);
            return;
        }
        if (slot == SLOT_ROW_SPACING_PLUS) {
            state.rowSpacing = Math.min(8, state.rowSpacing + 1);
            redraw(state);
            return;
        }
        if (slot == SLOT_CLEAR_ITEMS) {
            for (int i = 0; i < state.items.size(); i++) {
                state.items.set(i, null);
            }
            state.pendingItemSlot = -1;
            player.sendMessage(ChatColor.YELLOW + "Cleared all planned sign items.");
            redraw(state);
            return;
        }
        if (slot == SLOT_HORIZONTAL_SPACING_DISPLAY || slot == SLOT_ROW_COUNT_DISPLAY
                || slot == SLOT_ROW_SPACING_DISPLAY) {
            return;
        }
        if (slot == SLOT_CONFIRM) {
            confirm(player, state);
            return;
        }
        int itemIndex = ITEM_SLOTS.indexOf(slot);
        if (itemIndex >= 0) {
            handlePlannerItemSlotClick(event, player, state, itemIndex);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        MenuState state = openMenus.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.selectionInventory != null && state.selectionInventory.equals(event.getInventory())) {
            state.selectionInventory = null;
            state.selectionEntries = List.of();
            state.pendingItemSlot = -1;
            state.awaitingSearchInput = false;
            if (state.reopenToPlanner) {
                state.reopenToPlanner = false;
                return;
            }
            if (state.inventory != null && openMenus.containsKey(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.getOpenInventory().getTopInventory().equals(state.inventory)
                            && player.isOnline()) {
                        player.openInventory(state.inventory);
                    }
                });
            }
            return;
        }
        if (state.inventory != null && state.inventory.equals(event.getInventory())) {
            if (state.selectionInventory != null) {
                return;
            }
            state.inventory = null;
            state.awaitingBackground = false;
            state.pendingItemSlot = -1;
            state.reopenToPlanner = false;
            state.awaitingSearchInput = false;
            state.selectionInventory = null;
            state.selectionEntries = List.of();
            state.selectionPage = 0;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        openMenus.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MenuState state = openMenus.get(player.getUniqueId());
        if (state == null || !state.awaitingSearchInput) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handleCatalogSearchInput(player, state, message));
    }

    private void handlePlayerInventoryClick(InventoryClickEvent event, Player player, MenuState state) {
        event.setCancelled(true);
        if (event.getRawSlot() < state.inventory.getSize()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (state.awaitingBackground) {
            if (!clicked.getType().isBlock()) {
                player.sendMessage(ChatColor.RED + "Please choose a block item for the background.");
                return;
            }
            state.background = clicked.getType();
            state.awaitingBackground = false;
            state.keepExistingBackground = false;
            player.sendMessage(ChatColor.GREEN + "Background block set to " + readable(clicked.getType())
                    + ". Existing backing blocks will be replaced.");
            redraw(state);
            return;
        }

        int targetIndex = state.pendingItemSlot;
        if (targetIndex < 0) {
            targetIndex = firstEmptySlot(state.items);
            if (targetIndex < 0) {
                player.sendMessage(ChatColor.RED + "All sign item slots are full. Remove one before adding another.");
                return;
            }
        }

        ItemStack sanitized = sanitizeItem(clicked);
        if (sanitized == null) {
            player.sendMessage(ChatColor.RED + "That item cannot be used for a shop sign.");
            return;
        }
        if (!generator.isActionSupported(state.action, sanitized.getType())) {
            player.sendMessage(ChatColor.YELLOW
                    + "Warning: the shop does not currently have a price configured for that item. The sign will show as unavailable.");
        }
        state.items.set(targetIndex, sanitized);
        state.pendingItemSlot = -1;
        player.sendMessage(ChatColor.GREEN + "Added " + sanitized.getAmount() + "x " + readable(sanitized.getType())
                + " to slot " + (targetIndex + 1) + '.');
        redraw(state);
    }

    private void handlePlannerItemSlotClick(InventoryClickEvent event, Player player, MenuState state, int itemIndex) {
        ItemStack existing = state.items.get(itemIndex);
        ClickType click = event.getClick();
        if (click == ClickType.SHIFT_LEFT) {
            adjustSlotQuantity(player, state, itemIndex, 1);
            return;
        }
        if (click == ClickType.SHIFT_RIGHT) {
            adjustSlotQuantity(player, state, itemIndex, -1);
            return;
        }
        if (event.isRightClick()) {
            if (existing != null) {
                state.items.set(itemIndex, null);
                if (state.pendingItemSlot == itemIndex) {
                    state.pendingItemSlot = -1;
                }
                player.sendMessage(ChatColor.YELLOW + "Removed the item from slot " + (itemIndex + 1) + '.');
                redraw(state);
            } else {
                if (state.pendingItemSlot == itemIndex) {
                    state.pendingItemSlot = -1;
                    player.sendMessage(ChatColor.YELLOW + "Cancelled inventory selection for slot " + (itemIndex + 1)
                            + '.');
                } else {
                    state.pendingItemSlot = itemIndex;
                    player.sendMessage(ChatColor.GREEN
                            + "Click an item in your inventory to assign it to slot " + (itemIndex + 1) + '.');
                }
                redraw(state);
            }
            return;
        }
        state.pendingItemSlot = itemIndex;
        openItemCatalog(player, state);
    }

    private void adjustSlotQuantity(Player player, MenuState state, int itemIndex, int delta) {
        ItemStack existing = state.items.get(itemIndex);
        if (existing == null) {
            return;
        }
        int current = existing.getAmount();
        int updated = Math.max(1, Math.min(64, current + delta));
        if (updated == current) {
            if (updated == 64 && delta > 0) {
                player.sendMessage(ChatColor.YELLOW + "Slot " + (itemIndex + 1) + " is already at the maximum quantity.");
            } else if (updated == 1 && delta < 0) {
                player.sendMessage(ChatColor.YELLOW + "Slot " + (itemIndex + 1) + " is already at the minimum quantity.");
            }
            return;
        }
        ItemStack replacement = existing.clone();
        replacement.setAmount(updated);
        state.items.set(itemIndex, replacement);
        player.sendMessage(ChatColor.GREEN + "Adjusted slot " + (itemIndex + 1) + " to " + updated + " items per sign.");
        redraw(state);
    }

    private void openItemCatalog(Player player, MenuState state) {
        ensureCatalogSource(state);
        state.selectionEntries = buildCatalogEntries(state);
        state.selectionPage = 0;
        state.awaitingSearchInput = false;
        state.selectionInventory = Bukkit.createInventory(player, CATALOG_INVENTORY_SIZE,
                ChatColor.GREEN + "Select Sign Item");
        state.reopenToPlanner = false;
        redrawItemCatalog(state);
        player.openInventory(state.selectionInventory);
    }

    private void handleItemCatalogClick(InventoryClickEvent event, Player player, MenuState state) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= state.selectionInventory.getSize()) {
            return;
        }
        if (slot != CATALOG_SLOT_SEARCH && state.awaitingSearchInput) {
            state.awaitingSearchInput = false;
            redrawItemCatalog(state);
        }
        if (slot == CATALOG_SLOT_PREVIOUS) {
            if (state.selectionPage > 0) {
                state.selectionPage--;
                redrawItemCatalog(state);
            }
            return;
        }
        if (slot == CATALOG_SLOT_NEXT) {
            int maxPage = Math.max(0, (state.selectionEntries.size() - 1) / CATALOG_ITEMS_PER_PAGE);
            if (state.selectionPage < maxPage) {
                state.selectionPage++;
                redrawItemCatalog(state);
            }
            return;
        }
        if (slot == CATALOG_SLOT_BACK) {
            state.pendingItemSlot = -1;
            state.reopenToPlanner = true;
            state.awaitingSearchInput = false;
            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(state.inventory));
            return;
        }
        if (slot == CATALOG_SLOT_SOURCE_TOGGLE) {
            ItemCatalogSource nextSource = nextCatalogSource(state);
            if (nextSource != state.selectionCatalog) {
                state.selectionCatalog = nextSource;
            }
            state.selectionEntries = buildCatalogEntries(state);
            state.selectionPage = 0;
            redrawItemCatalog(state);
            return;
        }
        if (slot == CATALOG_SLOT_SEARCH) {
            handleCatalogSearchClick(player, state, event.getClick());
            return;
        }
        if (slot == CATALOG_SLOT_INSTRUCTIONS || slot == CATALOG_SLOT_PAGE_INFO) {
            return;
        }
        if (slot >= CATALOG_ITEMS_PER_PAGE) {
            return;
        }
        int index = state.selectionPage * CATALOG_ITEMS_PER_PAGE + slot;
        if (index < 0 || index >= state.selectionEntries.size()) {
            return;
        }
        CatalogEntry entry = state.selectionEntries.get(index);
        if (entry.type() == CatalogEntryType.CATEGORY) {
            handleCategorySelection(player, state, entry);
            return;
        }
        Material material = entry.material();
        if (material == null || material == Material.AIR) {
            return;
        }
        int amount = entry.useConfiguredAmount() ? entry.amount() : resolveSelectionAmount(event);
        amount = Math.max(1, Math.min(64, amount));
        ItemStack stack = new ItemStack(material, amount);
        if (!generator.isActionSupported(state.action, material)) {
            player.sendMessage(ChatColor.YELLOW
                    + "Warning: the shop does not currently have a price configured for that item. The sign will show as unavailable.");
        }
        int targetIndex = state.pendingItemSlot >= 0 ? state.pendingItemSlot : firstEmptySlot(state.items);
        if (targetIndex < 0) {
            player.sendMessage(ChatColor.RED + "All sign item slots are full. Remove one before adding another.");
            return;
        }
        state.items.set(targetIndex, stack);
        player.sendMessage(ChatColor.GREEN + "Added " + amount + "x " + readable(material) + " to slot "
                + (targetIndex + 1) + '.');
        redraw(state);
        state.pendingItemSlot = -1;
        state.awaitingSearchInput = false;
        state.reopenToPlanner = true;
        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(state.inventory));
    }

    private void handleCategorySelection(Player player, MenuState state, CatalogEntry entry) {
        List<ItemStack> categoryItems = entry.categoryItems();
        if (categoryItems.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No items are configured for that category.");
            return;
        }

        int startIndex = state.pendingItemSlot >= 0 ? state.pendingItemSlot : firstEmptySlot(state.items);
        if (startIndex < 0 || startIndex >= state.items.size()) {
            player.sendMessage(ChatColor.RED + "All sign item slots are full. Remove one before adding another.");
            return;
        }

        int placed = 0;
        int target = startIndex;
        for (ItemStack stack : categoryItems) {
            if (target >= state.items.size()) {
                break;
            }
            state.items.set(target, stack.clone());
            target++;
            placed++;
        }

        if (placed <= 0) {
            player.sendMessage(ChatColor.RED + "No sign item slots were available for that category.");
            return;
        }

        state.pendingItemSlot = -1;
        redraw(state);

        String categoryName = entry.categoryName();
        if (categoryName == null || categoryName.isBlank()) {
            categoryName = "category";
        }
        int total = categoryItems.size();
        if (placed == total) {
            player.sendMessage(ChatColor.GREEN + "Added " + placed + " item" + (placed == 1 ? "" : "s")
                    + " from " + categoryName + " to the plan.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Added " + placed + " of " + total + " item"
                    + (total == 1 ? "" : "s") + " from " + categoryName
                    + ". Increase rows or clear slots to fit the rest.");
        }

        LinkedHashSet<String> unsupported = new LinkedHashSet<>();
        for (int i = 0; i < placed; i++) {
            ItemStack added = categoryItems.get(i);
            if (!generator.isActionSupported(state.action, added.getType())) {
                unsupported.add(readable(added.getType()));
            }
        }
        if (!unsupported.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Warning: the shop does not currently have prices for "
                    + String.join(", ", unsupported) + " when "
                    + (state.action == ShopSignListener.SignAction.BUY ? "buying" : "selling") + '.');
        }

        state.awaitingSearchInput = false;
        state.reopenToPlanner = true;
        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(state.inventory));
    }

    private void handleCatalogSearchClick(Player player, MenuState state, ClickType click) {
        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            if (state.selectionSearch == null || state.selectionSearch.isBlank()) {
                player.sendMessage(ChatColor.YELLOW + "Search filter is already cleared.");
            } else {
                state.selectionSearch = null;
                player.sendMessage(ChatColor.GREEN + "Cleared catalog search filter.");
            }
            state.awaitingSearchInput = false;
            state.selectionEntries = buildCatalogEntries(state);
            state.selectionPage = 0;
            redrawItemCatalog(state);
            return;
        }
        state.awaitingSearchInput = true;
        player.sendMessage(ChatColor.AQUA + "Type your search in chat to filter catalog items.");
        player.sendMessage(ChatColor.GRAY + "Type 'clear' to remove the filter or 'cancel' to keep it.");
        redrawItemCatalog(state);
    }

    private void handleCatalogSearchInput(Player player, MenuState state, String message) {
        state.awaitingSearchInput = false;
        String raw = message == null ? "" : message.trim();
        String stripped = ChatColor.stripColor(raw);
        if (stripped != null) {
            raw = stripped.trim();
        }
        if (raw.length() > 48) {
            raw = raw.substring(0, 48);
        }
        if (raw.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "Search unchanged.");
            redrawItemCatalog(state);
            if (state.selectionInventory != null && player.isOnline()) {
                player.openInventory(state.selectionInventory);
            }
            return;
        }
        if (raw.equalsIgnoreCase("clear") || raw.isEmpty()) {
            if (state.selectionSearch == null || state.selectionSearch.isBlank()) {
                player.sendMessage(ChatColor.YELLOW + "Search filter was already cleared.");
            } else {
                state.selectionSearch = null;
                player.sendMessage(ChatColor.GREEN + "Cleared catalog search filter.");
            }
        } else {
            state.selectionSearch = raw;
            player.sendMessage(ChatColor.GREEN + "Filtering catalog for \"" + raw + "\".");
        }
        state.selectionEntries = buildCatalogEntries(state);
        state.selectionPage = 0;
        if (state.selectionEntries.isEmpty() && state.selectionSearch != null && !state.selectionSearch.isBlank()) {
            player.sendMessage(ChatColor.YELLOW + "No items matched that search.");
        }
        redrawItemCatalog(state);
        if (state.selectionInventory != null && player.isOnline()) {
            player.openInventory(state.selectionInventory);
        }
    }

    private int resolveSelectionAmount(InventoryClickEvent event) {
        ClickType click = event.getClick();
        if (click == ClickType.RIGHT) {
            return 16;
        }
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            return 32;
        }
        if (click == ClickType.MIDDLE || click == ClickType.CONTROL_DROP) {
            return 64;
        }
        if (click == ClickType.DROP) {
            return 8;
        }
        if (click == ClickType.NUMBER_KEY) {
            return Math.min(64, Math.max(1, event.getHotbarButton() + 1));
        }
        return 1;
    }

    private void redrawItemCatalog(MenuState state) {
        Inventory catalog = state.selectionInventory;
        if (catalog == null) {
            return;
        }
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "", List.of());
        for (int i = 0; i < catalog.getSize(); i++) {
            catalog.setItem(i, filler);
        }
        int maxIndex = Math.max(0, state.selectionEntries.size() - 1);
        int maxPage = Math.max(0, maxIndex / CATALOG_ITEMS_PER_PAGE);
        if (state.selectionPage > maxPage) {
            state.selectionPage = maxPage;
        }
        int start = state.selectionPage * CATALOG_ITEMS_PER_PAGE;
        for (int i = 0; i < CATALOG_ITEMS_PER_PAGE; i++) {
            int index = start + i;
            if (index >= state.selectionEntries.size()) {
                break;
            }
            CatalogEntry entry = state.selectionEntries.get(index);
            if (entry == null) {
                continue;
            }
            catalog.setItem(i, createCatalogItem(entry, state));
        }
        if (state.selectionEntries.isEmpty()) {
            catalog.setItem(22,
                    createItem(Material.BARRIER, ChatColor.RED + "No matching items",
                            List.of(ChatColor.YELLOW + "Adjust your search filter or source.")));
        }
        boolean hasPrevious = state.selectionPage > 0;
        boolean hasNext = state.selectionPage < maxPage;
        catalog.setItem(CATALOG_SLOT_PREVIOUS,
                hasPrevious ? createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page",
                        List.of(ChatColor.GRAY + "Go to page " + state.selectionPage + " of " + (maxPage + 1)))
                        : createItem(Material.BARRIER, ChatColor.RED + "No previous page", List.of()));
        catalog.setItem(CATALOG_SLOT_NEXT,
                hasNext ? createItem(Material.ARROW, ChatColor.YELLOW + "Next Page",
                        List.of(ChatColor.GRAY + "Go to page " + (state.selectionPage + 2) + " of " + (maxPage + 1)))
                        : createItem(Material.BARRIER, ChatColor.RED + "No next page", List.of()));
        catalog.setItem(CATALOG_SLOT_BACK, createItem(Material.OAK_DOOR, ChatColor.GOLD + "Return to planner",
                List.of(ChatColor.YELLOW + "Leave the catalog without picking an item.")));
        catalog.setItem(CATALOG_SLOT_SOURCE_TOGGLE, createCatalogSourceItem(state));
        catalog.setItem(CATALOG_SLOT_SEARCH, createCatalogSearchItem(state));
        catalog.setItem(CATALOG_SLOT_INSTRUCTIONS,
                createItem(Material.BOOK, ChatColor.AQUA + "Selection Controls",
                        List.of(ChatColor.YELLOW + "Left-click: quantity 1", ChatColor.YELLOW + "Right-click: quantity 16",
                                ChatColor.YELLOW + "Shift-click: quantity 32", ChatColor.YELLOW + "Middle-click: quantity 64",
                                ChatColor.YELLOW + "Number keys: quantity 1-9", ChatColor.YELLOW
                                        + "Use the search icon to filter items.",
                                ChatColor.GRAY + "Adjust in planner with shift-clicks.")));
        catalog.setItem(CATALOG_SLOT_PAGE_INFO, createItem(Material.PAPER,
                ChatColor.GOLD + "Page " + (state.selectionPage + 1) + " of " + Math.max(1, maxPage + 1), List.of()));
    }

    private ItemStack createCatalogItem(CatalogEntry entry, MenuState state) {
        if (entry.type() == CatalogEntryType.CATEGORY) {
            Material icon = entry.material();
            if (icon == null || icon == Material.AIR) {
                icon = Material.CHEST;
            }
            ItemStack stack = new ItemStack(icon);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) {
                return stack;
            }
            String displayName = entry.displayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = ChatColor.AQUA + "Use Category";
            }
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>(entry.extraLore());
            String categoryName = entry.categoryName();
            if (categoryName != null && !categoryName.isBlank()) {
                lore.add(ChatColor.YELLOW + "Category: " + categoryName);
            }
            int size = entry.categoryItems().size();
            if (size > 0) {
                lore.add(ChatColor.GRAY + "Contains " + size + " item" + (size == 1 ? "" : "s") + '.');
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.values());
            stack.setItemMeta(meta);
            return stack;
        }

        Material material = entry.material();
        if (material == null || material == Material.AIR) {
            return new ItemStack(Material.BARRIER);
        }
        ItemStack stack = new ItemStack(material, Math.max(1, Math.min(64, entry.amount())));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        String displayName = entry.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = ChatColor.AQUA + readable(material);
        }
        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<>();
        if (!entry.extraLore().isEmpty()) {
            lore.addAll(entry.extraLore());
        }
        boolean supported = generator.isActionSupported(state.action, material);
        if (pricingManager != null) {
            pricingManager.getPrice(material).ifPresentOrElse(price -> {
                if (state.action == ShopSignListener.SignAction.BUY) {
                    if (price.buyPrice() >= 0) {
                        lore.add(ChatColor.GRAY + "Buy price configured.");
                    } else {
                        lore.add(ChatColor.RED + "Cannot buy from shop.");
                    }
                } else {
                    if (price.sellPrice() >= 0) {
                        lore.add(ChatColor.GRAY + "Sell price configured.");
                    } else {
                        lore.add(ChatColor.RED + "Cannot sell to shop.");
                    }
                }
            }, () -> lore.add(ChatColor.RED + "No price configured."));
        } else if (supported) {
            lore.add(ChatColor.GRAY + "Price configured for this action.");
        } else {
            lore.add(ChatColor.RED + "Price not configured for this action.");
        }
        if (entry.useConfiguredAmount()) {
            lore.add(ChatColor.YELLOW + "Uses shop amount: " + entry.amount());
            lore.add(ChatColor.YELLOW + "Click to add this option.");
            lore.add(ChatColor.GRAY + "Adjust quantity later with shift-clicks.");
        } else if (entry.showQuantityShortcuts()) {
            lore.add(ChatColor.YELLOW + "Left-click: quantity 1");
            lore.add(ChatColor.YELLOW + "Right-click: quantity 16");
            lore.add(ChatColor.YELLOW + "Shift-click: quantity 32");
            lore.add(ChatColor.YELLOW + "Middle-click: quantity 64");
            lore.add(ChatColor.YELLOW + "Number keys: quantity 1-9");
            lore.add(ChatColor.GRAY + "Adjust in planner with shift-clicks.");
        }
        if (!entry.useConfiguredAmount() && !entry.showQuantityShortcuts()) {
            lore.add(ChatColor.YELLOW + "Click to add this item.");
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createCatalogSearchItem(MenuState state) {
        boolean awaiting = state.awaitingSearchInput;
        String query = state.selectionSearch;
        boolean hasQuery = query != null && !query.isBlank();
        Material icon = awaiting ? Material.WRITABLE_BOOK : (hasQuery ? Material.NAME_TAG : Material.COMPASS);
        ItemStack stack = new ItemStack(icon);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        List<String> lore = new ArrayList<>();
        if (hasQuery) {
            lore.add(ChatColor.GRAY + "Filter: " + query);
        }
        if (awaiting) {
            meta.setDisplayName(ChatColor.GOLD + "Waiting for search text");
            lore.add(ChatColor.YELLOW + "Type your search in chat now.");
            lore.add(ChatColor.GRAY + "Type 'cancel' to keep the current filter.");
        } else if (hasQuery) {
            String preview = query;
            if (preview.length() > 24) {
                preview = preview.substring(0, 24) + "...";
            }
            meta.setDisplayName(ChatColor.AQUA + "Search: " + preview);
            lore.add(ChatColor.YELLOW + "Left-click: enter new search");
            lore.add(ChatColor.YELLOW + "Right-click: clear search");
            if (state.selectionEntries.isEmpty()) {
                lore.add(ChatColor.RED + "No matches currently shown.");
            }
        } else {
            meta.setDisplayName(ChatColor.AQUA + "Search Catalog");
            lore.add(ChatColor.YELLOW + "Left-click: enter search text");
            lore.add(ChatColor.YELLOW + "Right-click: clear search");
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createCatalogSourceItem(MenuState state) {
        ItemCatalogSource source = ensureCatalogSource(state);
        ShopSignListener.SignAction action = state.action;
        return switch (source) {
            case ALL -> createItem(Material.CHEST, ChatColor.AQUA + "Showing all materials",
                    List.of(ChatColor.YELLOW + "Click to show only items configured for the current action."));
            case SHOP -> createItem(Material.ITEM_FRAME, ChatColor.AQUA + "Showing shop options",
                    List.of(ChatColor.YELLOW + "Click to browse every material."));
            case ACTION -> {
                String mode = action == ShopSignListener.SignAction.BUY ? "buy" : "sell";
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Click to browse shop options.");
                lore.add(ChatColor.GRAY + "Only items priced for " + mode + " are shown.");
                yield createItem(Material.EMERALD, ChatColor.AQUA + "Showing " + mode + " items", lore);
            }
        };
    }

    private List<CatalogEntry> buildCatalogEntries(MenuState state) {
        if (ensureCatalogSource(state) == ItemCatalogSource.SHOP) {
            List<CatalogEntry> shopEntries = buildShopCatalogEntries();
            if (!shopEntries.isEmpty()) {
                return filterCatalogEntries(state, shopEntries);
            }
        }
        return filterCatalogEntries(state, buildMaterialCatalogEntries(state));
    }

    private List<CatalogEntry> buildShopCatalogEntries() {
        if (pricingManager == null) {
            return List.of();
        }
        ShopMenuLayout layout = pricingManager.getMenuLayout();
        if (layout == null || layout.categories().isEmpty()) {
            return List.of();
        }
        List<CatalogEntry> entries = new ArrayList<>();
        for (ShopMenuLayout.Category category : layout.categories()) {
            if (category == null) {
                continue;
            }
            List<ShopMenuLayout.Item> items = new ArrayList<>(category.items());
            items.sort(Comparator.comparingInt(ShopMenuLayout.Item::slot));
            if (items.isEmpty()) {
                continue;
            }
            String categoryName = stripColorOrFallback(category.displayName(), category.id());
            ShopMenuLayout.CategoryRotation rotation = category.rotation();
            String rotationInfo = rotation != null ? rotation.optionId() : null;

            List<ItemStack> categoryStacks = new ArrayList<>();
            List<CatalogEntry> categoryEntries = new ArrayList<>();
            for (ShopMenuLayout.Item item : items) {
                if (item == null) {
                    continue;
                }
                Material material = item.material();
                if (material == null || !material.isItem()) {
                    continue;
                }
                ItemStack stack = sanitizeItem(new ItemStack(material, Math.max(1, Math.min(64, item.amount()))));
                if (stack == null) {
                    continue;
                }
                categoryStacks.add(stack);
                String displayName = item.display() != null ? item.display().displayName() : null;
                if (displayName == null || displayName.isBlank()) {
                    displayName = ChatColor.AQUA + readable(material);
                }
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Category: " + categoryName);
                if (rotationInfo != null && !rotationInfo.isEmpty()) {
                    lore.add(ChatColor.GRAY + "Rotation option: " + rotationInfo);
                }
                categoryEntries.add(new CatalogEntry(CatalogEntryType.ITEM, material, stack.getAmount(), displayName, lore,
                        true, false, List.of(), null));
            }

            if (categoryEntries.isEmpty()) {
                continue;
            }

            CatalogEntry categoryEntry = createCategoryCatalogEntry(categoryName, rotationInfo, categoryStacks);
            entries.add(categoryEntries.get(0));
            if (categoryEntry != null) {
                entries.add(categoryEntry);
            }
            for (int i = 1; i < categoryEntries.size(); i++) {
                entries.add(categoryEntries.get(i));
            }
        }
        return List.copyOf(entries);
    }

    private List<CatalogEntry> buildMaterialCatalogEntries(MenuState state) {
        ItemCatalogSource catalogSource = ensureCatalogSource(state);
        LinkedHashSet<Material> materials = new LinkedHashSet<>();
        if (pricingManager != null) {
            Collection<Material> base;
            if (catalogSource == ItemCatalogSource.ACTION) {
                base = state.action == ShopSignListener.SignAction.BUY ? pricingManager.getBuyableMaterials()
                        : pricingManager.getSellableMaterials();
                if (base == null || base.isEmpty()) {
                    base = pricingManager.getConfiguredMaterials();
                }
            } else {
                base = pricingManager.getConfiguredMaterials();
            }
            if (base != null) {
                for (Material material : base) {
                    if (material != null && material.isItem()) {
                        materials.add(material);
                    }
                }
            }
        }
        if (catalogSource == ItemCatalogSource.ALL || materials.isEmpty()) {
            for (Material material : Material.values()) {
                if (material != null && material.isItem()) {
                    materials.add(material);
                }
            }
        }
        List<Material> sorted = new ArrayList<>(materials);
        sorted.sort(Comparator.comparing(Material::name));
        List<CatalogEntry> entries = new ArrayList<>(sorted.size());
        for (Material material : sorted) {
            entries.add(new CatalogEntry(CatalogEntryType.ITEM, material, 1, ChatColor.AQUA + readable(material), List.of(),
                    false, true, List.of(), null));
        }
        return List.copyOf(entries);
    }

    private CatalogEntry createCategoryCatalogEntry(String categoryName, String rotationInfo,
            List<ItemStack> categoryStacks) {
        if (categoryStacks == null || categoryStacks.isEmpty()) {
            return null;
        }
        String readableName = categoryName == null || categoryName.isBlank() ? "category" : categoryName;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Click to add the entire category.");
        lore.add(ChatColor.GRAY + "Items added: " + categoryStacks.size());
        if (rotationInfo != null && !rotationInfo.isEmpty()) {
            lore.add(ChatColor.GRAY + "Rotation option: " + rotationInfo);
        }
        lore.add(ChatColor.GRAY + "Replaces planner slots starting here.");
        return new CatalogEntry(CatalogEntryType.CATEGORY, Material.CHEST, 1,
                ChatColor.AQUA + "Use Category: " + readableName, lore, false, false, categoryStacks, readableName);
    }

    private List<CatalogEntry> filterCatalogEntries(MenuState state, List<CatalogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        String query = state.selectionSearch;
        if (query == null || query.isBlank()) {
            return entries;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        List<CatalogEntry> filtered = new ArrayList<>();
        for (CatalogEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (catalogEntryMatchesQuery(entry, normalized)) {
                filtered.add(entry);
            }
        }
        if (filtered.isEmpty()) {
            return List.of();
        }
        return List.copyOf(filtered);
    }

    private boolean catalogEntryMatchesQuery(CatalogEntry entry, String normalizedQuery) {
        Material material = entry.material();
        if (material != null) {
            String materialName = material.name().toLowerCase(Locale.ROOT);
            if (materialName.contains(normalizedQuery)) {
                return true;
            }
            String readable = readable(material).toLowerCase(Locale.ROOT);
            if (readable.contains(normalizedQuery)) {
                return true;
            }
        }
        String displayName = entry.displayName();
        if (displayName != null) {
            String stripped = ChatColor.stripColor(displayName);
            if (stripped != null && stripped.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return true;
            }
        }
        for (String line : entry.extraLore()) {
            if (line == null) {
                continue;
            }
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private ItemCatalogSource nextCatalogSource(MenuState state) {
        ItemCatalogSource current = ensureCatalogSource(state);
        for (int i = 0; i < ItemCatalogSource.values().length; i++) {
            current = current.next();
            if (isCatalogSourceAvailable(current)) {
                return current;
            }
        }
        return state.selectionCatalog;
    }

    private ItemCatalogSource ensureCatalogSource(MenuState state) {
        if (state.selectionCatalog == null) {
            state.selectionCatalog = determineInitialCatalogSource();
        }
        return state.selectionCatalog;
    }

    private ItemCatalogSource determineInitialCatalogSource() {
        if (pricingManager != null) {
            ShopMenuLayout layout = pricingManager.getMenuLayout();
            if (layout != null) {
                for (ShopMenuLayout.Category category : layout.categories()) {
                    if (category == null) {
                        continue;
                    }
                    List<ShopMenuLayout.Item> items = category.items();
                    if (items != null && !items.isEmpty()) {
                        return ItemCatalogSource.SHOP;
                    }
                }
            }
        }
        return ItemCatalogSource.ACTION;
    }

    private boolean isCatalogSourceAvailable(ItemCatalogSource source) {
        if (source != ItemCatalogSource.SHOP) {
            return true;
        }
        if (pricingManager == null) {
            return false;
        }
        ShopMenuLayout layout = pricingManager.getMenuLayout();
        if (layout == null) {
            return false;
        }
        for (ShopMenuLayout.Category category : layout.categories()) {
            if (category == null) {
                continue;
            }
            for (ShopMenuLayout.Item item : category.items()) {
                if (item == null) {
                    continue;
                }
                Material material = item.material();
                if (material != null && material.isItem()) {
                    return true;
                }
            }
        }
        return false;
    }

    private String stripColorOrFallback(String text, String fallback) {
        if (text != null && !text.isBlank()) {
            String stripped = ChatColor.stripColor(text);
            if (stripped != null && !stripped.isBlank()) {
                return stripped;
            }
        }
        return fallback == null ? "" : fallback;
    }

    private void confirm(Player player, MenuState state) {
        Material backgroundMaterial = state.keepExistingBackground ? null
                : (state.background == null ? SignShopPlan.DEFAULT_BACKGROUND : state.background);
        SignShopPlan plan = new SignShopPlan(state.items, backgroundMaterial, state.signMaterial, state.spacing,
                state.rows, state.rowSpacing, state.action, state.direction);
        if (plan.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Add at least one item before generating sign shops.");
            return;
        }
        SignShopGenerator.GenerationResult result = generator.generate(player, plan);
        player.sendMessage((result.success() ? ChatColor.GREEN : ChatColor.RED) + result.message());
        if (result.success()) {
            openMenus.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        }
    }

    private void redraw(MenuState state) {
        Inventory inventory = state.inventory;
        inventory.clear();
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(SLOT_INFO,
                createItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "Sign Shop Planner",
                        List.of(ChatColor.YELLOW + "Left-click slots to browse the catalog.", ChatColor.YELLOW
                                + "Right-click slots to use items from your inventory.", ChatColor.YELLOW
                                + "Shift-click filled slots to tweak quantities.", ChatColor.YELLOW
                                + "Adjust spacing and rows to tune the final layout.")));

        inventory.setItem(SLOT_ACTION, createActionItem(state.action));
        inventory.setItem(SLOT_BACKGROUND,
                createBackgroundItem(state.background, state.awaitingBackground, state.keepExistingBackground));
        inventory.setItem(SLOT_DIRECTION, createDirectionItem(state.direction));
        inventory.setItem(SLOT_SIGN_MATERIAL, createSignMaterialItem(state.signMaterial));

        for (int i = 0; i < ITEM_SLOTS.size(); i++) {
            ItemStack planned = state.items.get(i);
            int slot = ITEM_SLOTS.get(i);
            ItemStack display;
            if (planned == null) {
                if (state.pendingItemSlot == i) {
                    display = createItem(Material.GLOWSTONE_DUST, ChatColor.GOLD + "Awaiting inventory item",
                            List.of(ChatColor.YELLOW + "Click an item in your inventory.",
                                    ChatColor.YELLOW + "Left-click here to open the catalog instead.",
                                    ChatColor.YELLOW + "Right-click to cancel."));
                } else {
                    display = createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                            ChatColor.GRAY + "Empty Slot",
                            List.of(ChatColor.YELLOW + "Left-click: open catalog",
                                    ChatColor.YELLOW + "Right-click: use inventory item"));
                }
            } else {
                display = planned.clone();
                ItemMeta meta = display.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + readable(planned.getType()));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "Quantity: " + planned.getAmount());
                lore.add(ChatColor.YELLOW + "Left-click: change item");
                lore.add(ChatColor.YELLOW + "Right-click: clear slot");
                lore.add(ChatColor.YELLOW + "Shift-click: adjust quantity");
                if (!generator.isActionSupported(state.action, planned.getType())) {
                    lore.add(ChatColor.RED + "Price missing for this action.");
                }
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.values());
                display.setItemMeta(meta);
            }
            inventory.setItem(slot, display);
        }

        inventory.setItem(SLOT_HORIZONTAL_SPACING_MINUS,
                createItem(Material.REDSTONE_TORCH, ChatColor.RED + "Decrease horizontal gap",
                        List.of(ChatColor.YELLOW + "Current spacing: " + state.spacing)));
        inventory.setItem(SLOT_HORIZONTAL_SPACING_PLUS,
                createItem(Material.LIME_DYE, ChatColor.GREEN + "Increase horizontal gap",
                        List.of(ChatColor.YELLOW + "Current spacing: " + state.spacing)));
        inventory.setItem(SLOT_HORIZONTAL_SPACING_DISPLAY,
                createItem(Material.CLOCK, ChatColor.GOLD + "Horizontal Spacing",
                        List.of(ChatColor.YELLOW + "Gap between backing blocks: " + state.spacing + " block(s)")));

        inventory.setItem(SLOT_ROW_COUNT_MINUS,
                createItem(Material.RED_CARPET, ChatColor.RED + "Fewer rows",
                        List.of(ChatColor.YELLOW + "Rows planned: " + state.rows)));
        inventory.setItem(SLOT_ROW_COUNT_PLUS,
                createItem(Material.LIME_CARPET, ChatColor.GREEN + "More rows",
                        List.of(ChatColor.YELLOW + "Rows planned: " + state.rows)));
        inventory.setItem(SLOT_ROW_COUNT_DISPLAY,
                createItem(Material.OAK_SIGN, ChatColor.GOLD + "Row Count",
                        List.of(ChatColor.YELLOW + "Total rows generated: " + state.rows)));

        inventory.setItem(SLOT_ROW_SPACING_MINUS,
                createItem(Material.REPEATER, ChatColor.RED + "Lower vertical gap",
                        List.of(ChatColor.YELLOW + "Current vertical spacing: " + state.rowSpacing)));
        inventory.setItem(SLOT_ROW_SPACING_PLUS,
                createItem(Material.SLIME_BALL, ChatColor.GREEN + "Raise vertical gap",
                        List.of(ChatColor.YELLOW + "Current vertical spacing: " + state.rowSpacing)));
        inventory.setItem(SLOT_ROW_SPACING_DISPLAY,
                createItem(Material.SCAFFOLDING, ChatColor.GOLD + "Vertical Spacing",
                        List.of(ChatColor.YELLOW + "Blocks between rows: " + state.rowSpacing + " block(s)")));

        inventory.setItem(SLOT_CLEAR_ITEMS,
                createItem(Material.BARRIER, ChatColor.RED + "Clear planned items",
                        List.of(ChatColor.YELLOW + "Removes all configured sign entries")));

        inventory.setItem(SLOT_CONFIRM,
                createItem(Material.LIME_CONCRETE, ChatColor.GREEN + "Confirm & Generate",
                        List.of(ChatColor.YELLOW + "Uses the block you are looking at", ChatColor.YELLOW
                                + "for the first backing block.")));
    }

    private ItemStack createActionItem(ShopSignListener.SignAction action) {
        if (action == ShopSignListener.SignAction.SELL) {
            return createItem(Material.REDSTONE, ChatColor.RED + "Mode: Sell",
                    List.of(ChatColor.YELLOW + "Players sell items to the shop."));
        }
        return createItem(Material.EMERALD, ChatColor.GREEN + "Mode: Buy",
                List.of(ChatColor.YELLOW + "Players buy items from the shop."));
    }

    private ItemStack createDirectionItem(SignShopPlan.LayoutDirection direction) {
        boolean right = direction == SignShopPlan.LayoutDirection.RIGHT;
        return createItem(right ? Material.COMPASS : Material.ARROW,
                ChatColor.AQUA + "Direction: " + (right ? "Right" : "Left"),
                List.of(ChatColor.YELLOW + "Determines which side future signs use.",
                        ChatColor.YELLOW + "Click to toggle between left/right."));
    }

    private ItemStack createSignMaterialItem(Material signMaterial) {
        Material wallMaterial = signMaterial == null ? Material.OAK_WALL_SIGN : signMaterial;
        Material displayMaterial = resolveDisplaySignMaterial(wallMaterial);
        ItemStack stack = new ItemStack(displayMaterial);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Wall Sign: " + readable(wallMaterial));
        meta.setLore(List.of(ChatColor.YELLOW + "Left-click: next sign type", ChatColor.YELLOW
                + "Right-click: previous sign type"));
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    private Material resolveDisplaySignMaterial(Material wallMaterial) {
        if (!Tag.WALL_SIGNS.isTagged(wallMaterial)) {
            return Material.OAK_SIGN;
        }
        String name = wallMaterial.name();
        if (name.endsWith("_WALL_SIGN")) {
            String candidateName = name.substring(0, name.length() - "_WALL_SIGN".length()) + "_SIGN";
            Material candidate = Material.getMaterial(candidateName);
            if (candidate != null && candidate.isItem()) {
                return candidate;
            }
        }
        return Material.OAK_SIGN;
    }

    private ItemStack createBackgroundItem(Material background, boolean awaitingSelection, boolean keepExisting) {
        if (awaitingSelection) {
            return createItem(Material.GLOWSTONE_DUST, ChatColor.GOLD + "Select a background block",
                    List.of(ChatColor.YELLOW + "Click a block in your inventory"));
        }
        if (keepExisting) {
            return createItem(Material.LIME_DYE, ChatColor.AQUA + "Keep Existing Background",
                    List.of(ChatColor.YELLOW + "Right-click: toggle keep/replace",
                            ChatColor.YELLOW + "Left-click then pick a block",
                            ChatColor.GRAY + "Existing backing blocks will remain unchanged."));
        }
        Material displayMaterial = background == null ? SignShopPlan.DEFAULT_BACKGROUND : background;
        ItemStack stack = new ItemStack(displayMaterial);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Background: " + readable(displayMaterial));
        meta.setLore(List.of(ChatColor.YELLOW + "Right-click: toggle keep/replace",
                ChatColor.YELLOW + "Left-click then pick a block",
                ChatColor.GRAY + "Backing blocks will be replaced with this material."));
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    private int firstEmptySlot(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == null) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack sanitizeItem(ItemStack original) {
        if (original == null || original.getType() == Material.AIR) {
            return null;
        }
        ItemStack sanitized = original.clone();
        sanitized.setAmount(Math.max(1, Math.min(64, sanitized.getAmount())));
        return sanitized;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    private ShopSignListener.SignAction toggle(ShopSignListener.SignAction action) {
        return action == ShopSignListener.SignAction.BUY ? ShopSignListener.SignAction.SELL
                : ShopSignListener.SignAction.BUY;
    }

    private SignShopPlan.LayoutDirection toggle(SignShopPlan.LayoutDirection direction) {
        return direction == SignShopPlan.LayoutDirection.RIGHT ? SignShopPlan.LayoutDirection.LEFT
                : SignShopPlan.LayoutDirection.RIGHT;
    }

    private Material cycleSignMaterial(Material current, int delta) {
        if (WALL_SIGN_MATERIALS.isEmpty()) {
            return Material.OAK_WALL_SIGN;
        }
        int index = current == null ? -1 : WALL_SIGN_MATERIALS.indexOf(current);
        if (index < 0) {
            index = 0;
        }
        int next = (index + delta) % WALL_SIGN_MATERIALS.size();
        if (next < 0) {
            next += WALL_SIGN_MATERIALS.size();
        }
        return WALL_SIGN_MATERIALS.get(next);
    }

    private String readable(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        if (builder.length() == 0) {
            return name;
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private enum ItemCatalogSource {
        ACTION,
        SHOP,
        ALL;

        private ItemCatalogSource next() {
            return switch (this) {
                case ACTION -> SHOP;
                case SHOP -> ALL;
                case ALL -> ACTION;
            };
        }
    }

    private enum CatalogEntryType {
        ITEM,
        CATEGORY
    }

    private static final class CatalogEntry {

        private final CatalogEntryType type;
        private final Material material;
        private final int amount;
        private final String displayName;
        private final List<String> extraLore;
        private final boolean useConfiguredAmount;
        private final boolean showQuantityShortcuts;
        private final List<ItemStack> categoryItems;
        private final String categoryName;

        private CatalogEntry(CatalogEntryType type, Material material, int amount, String displayName,
                List<String> extraLore, boolean useConfiguredAmount, boolean showQuantityShortcuts,
                List<ItemStack> categoryItems, String categoryName) {
            this.type = Objects.requireNonNull(type, "type");
            this.material = material;
            this.amount = Math.max(1, Math.min(64, amount));
            this.displayName = displayName;
            this.extraLore = extraLore == null ? List.of() : List.copyOf(extraLore);
            this.useConfiguredAmount = useConfiguredAmount;
            this.showQuantityShortcuts = showQuantityShortcuts;
            this.categoryItems = sanitizeCategoryItems(categoryItems);
            this.categoryName = categoryName;
        }

        private CatalogEntryType type() {
            return type;
        }

        private Material material() {
            return material;
        }

        private int amount() {
            return amount;
        }

        private String displayName() {
            return displayName;
        }

        private List<String> extraLore() {
            return extraLore;
        }

        private boolean useConfiguredAmount() {
            return useConfiguredAmount;
        }

        private boolean showQuantityShortcuts() {
            return showQuantityShortcuts;
        }

        private List<ItemStack> categoryItems() {
            return categoryItems;
        }

        private String categoryName() {
            return categoryName;
        }

        private static List<ItemStack> sanitizeCategoryItems(List<ItemStack> items) {
            if (items == null || items.isEmpty()) {
                return List.of();
            }
            List<ItemStack> sanitized = new ArrayList<>();
            for (ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                ItemStack clone = item.clone();
                clone.setAmount(Math.max(1, Math.min(64, clone.getAmount())));
                sanitized.add(clone);
            }
            return List.copyOf(sanitized);
        }
    }

    private static final class MenuState {

        private Inventory inventory;
        private final List<ItemStack> items;
        private Material background;
        private boolean keepExistingBackground;
        private Material signMaterial;
        private int spacing;
        private int rows;
        private int rowSpacing;
        private ShopSignListener.SignAction action;
        private SignShopPlan.LayoutDirection direction;
        private boolean awaitingBackground;
        private int pendingItemSlot;
        private Inventory selectionInventory;
        private List<CatalogEntry> selectionEntries;
        private int selectionPage;
        private ItemCatalogSource selectionCatalog;
        private boolean reopenToPlanner;
        private boolean awaitingSearchInput;
        private String selectionSearch;
        private boolean instructionsShown;

        private MenuState() {
            this.items = new ArrayList<>();
            for (int i = 0; i < ITEM_SLOTS.size(); i++) {
                items.add(null);
            }
            this.background = SignShopPlan.DEFAULT_BACKGROUND;
            this.keepExistingBackground = false;
            this.signMaterial = Material.OAK_WALL_SIGN;
            this.spacing = 1;
            this.rows = 1;
            this.rowSpacing = 1;
            this.action = ShopSignListener.SignAction.BUY;
            this.direction = SignShopPlan.LayoutDirection.RIGHT;
            this.awaitingBackground = false;
            this.pendingItemSlot = -1;
            this.selectionInventory = null;
            this.selectionEntries = List.of();
            this.selectionPage = 0;
            this.selectionCatalog = null;
            this.reopenToPlanner = false;
            this.awaitingSearchInput = false;
            this.selectionSearch = null;
            this.instructionsShown = false;
        }
    }
}
