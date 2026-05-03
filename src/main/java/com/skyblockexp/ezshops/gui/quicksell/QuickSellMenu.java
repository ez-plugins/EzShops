package com.skyblockexp.ezshops.gui.quicksell;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick Sell GUI — players drag items into a 36-slot area, see a live estimated
 * sell total, and confirm the sale with a single button click.
 *
 * <p>Layout (45 slots, 5 rows):
 * <pre>
 *  Rows 1-4  (slots  0-35): item drop area
 *  Row 5     (slots 36-44): control bar
 *    36 = Cancel (BARRIER)
 *    37,38,39,41,42,43 = Gray filler
 *    40 = Price display (GOLD_INGOT)
 *    44 = Confirm (LIME_STAINED_GLASS_PANE)
 * </pre>
 */
public class QuickSellMenu implements Listener {

    private static final int GUI_SIZE = 45;
    private static final int ITEM_SLOT_COUNT = 36; // 0-35
    private static final int SLOT_CANCEL = 36;
    private static final int SLOT_PRICE = 40;
    private static final int SLOT_CONFIRM = 44;

    private final ShopPricingManager pricingManager;
    private final ShopTransactionService transactionService;
    private final ShopMessageConfiguration.CommandMessages.SellCommandMessages messages;
    /** Bukkit sound key to play on confirm, or {@code null} / empty to disable. */
    private final String confirmSound;
    private final float confirmSoundVolume;
    private final float confirmSoundPitch;

    public QuickSellMenu(ShopPricingManager pricingManager,
            ShopTransactionService transactionService,
            ShopMessageConfiguration.CommandMessages.SellCommandMessages messages,
            String confirmSound,
            float confirmSoundVolume,
            float confirmSoundPitch) {
        this.pricingManager = pricingManager;
        this.transactionService = transactionService;
        this.messages = messages;
        this.confirmSound = confirmSound;
        this.confirmSoundVolume = confirmSoundVolume;
        this.confirmSoundPitch = confirmSoundPitch;
    }

    /**
     * Opens the Quick Sell GUI for {@code player}.
     */
    public void open(Player player) {
        QuickSellMenuHolder holder = new QuickSellMenuHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE,
                ChatColor.translateAlternateColorCodes('&', messages.guiTitle()));
        holder.setInventory(inv);
        populateControls(inv, 0.0, player);
        player.openInventory(inv);
    }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof QuickSellMenuHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }

        // Always cancel — we handle inventory manipulation manually
        event.setCancelled(true);

        Inventory topInv = event.getView().getTopInventory();
        int rawSlot = event.getRawSlot();
        InventoryAction action = event.getAction();

        // ----- Control row -----
        if (rawSlot == SLOT_CANCEL) {
            player.closeInventory(); // onInventoryClose returns items
            return;
        }
        if (rawSlot == SLOT_CONFIRM) {
            handleConfirm(player, topInv);
            player.closeInventory();
            return;
        }
        if (rawSlot > ITEM_SLOT_COUNT - 1 && rawSlot < GUI_SIZE) {
            // Other filler slots in the control row — do nothing
            return;
        }

        // ----- Item area (slots 0-35) -----
        if (rawSlot >= 0 && rawSlot < ITEM_SLOT_COUNT) {
            handleItemAreaClick(event, player, topInv, rawSlot, action);
            return;
        }

        // ----- Player inventory area (rawSlot >= GUI_SIZE) -----
        if (rawSlot >= GUI_SIZE) {
            // Shift-click from player inventory into the GUI
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR) {
                    return;
                }
                if (!isSellable(clicked.getType())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.unsellableItem()));
                    return;
                }
                // Find first empty slot in item area
                int emptySlot = findEmptyItemSlot(topInv);
                if (emptySlot == -1) {
                    // GUI full — leave item in player inventory
                    return;
                }
                topInv.setItem(emptySlot, clicked.clone());
                event.setCurrentItem(null);
                refreshPriceDisplay(topInv, player);
            }
            // All other actions in player inventory are harmless (already cancelled)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof QuickSellMenuHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }

        Inventory topInv = event.getView().getTopInventory();
        boolean touchesControlRow = event.getRawSlots().stream()
                .anyMatch(s -> s >= ITEM_SLOT_COUNT && s < GUI_SIZE);
        if (touchesControlRow) {
            event.setCancelled(true);
            return;
        }

        boolean touchesItemArea = event.getRawSlots().stream().anyMatch(s -> s < ITEM_SLOT_COUNT);
        if (!touchesItemArea) {
            return;
        }

        ItemStack dragged = event.getOldCursor();
        if (dragged == null || dragged.getType() == Material.AIR) {
            return;
        }
        if (!isSellable(dragged.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.unsellableItem()));
            return;
        }

        // Allow the drag; schedule a price refresh for the next tick
        Bukkit.getScheduler().runTask(
                Bukkit.getPluginManager().getPlugin("EzShops"),
                () -> refreshPriceDisplay(topInv, player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuickSellMenuHolder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }

        // Return all items in the item area to the player
        Inventory inv = event.getInventory();
        for (int slot = 0; slot < ITEM_SLOT_COUNT; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                inv.setItem(slot, null);
                giveOrDrop(player, item);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Handles a click inside the item drop area (slots 0-35).
     */
    private void handleItemAreaClick(InventoryClickEvent event, Player player,
            Inventory topInv, int rawSlot, InventoryAction action) {

        ItemStack cursor = player.getItemOnCursor();
        ItemStack slotItem = topInv.getItem(rawSlot);
        boolean cursorHasItem = cursor != null && cursor.getType() != Material.AIR;
        boolean slotHasItem = slotItem != null && slotItem.getType() != Material.AIR;

        switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> {
                if (!cursorHasItem) return;
                if (!isSellable(cursor.getType())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.unsellableItem()));
                    return;
                }
                // Let the server handle the actual placement by un-cancelling would be complex.
                // We manually replicate the placement instead.
                if (action == InventoryAction.PLACE_ALL) {
                    if (!slotHasItem) {
                        topInv.setItem(rawSlot, cursor.clone());
                        player.setItemOnCursor(null);
                    } else if (slotItem.isSimilar(cursor)) {
                        // Stack onto existing
                        int maxStack = slotItem.getMaxStackSize();
                        int combined = slotItem.getAmount() + cursor.getAmount();
                        if (combined <= maxStack) {
                            slotItem.setAmount(combined);
                            player.setItemOnCursor(null);
                        } else {
                            cursor.setAmount(combined - maxStack);
                            slotItem.setAmount(maxStack);
                        }
                        topInv.setItem(rawSlot, slotItem);
                    } else {
                        // Swap
                        topInv.setItem(rawSlot, cursor.clone());
                        player.setItemOnCursor(slotItem.clone());
                    }
                } else if (action == InventoryAction.PLACE_ONE) {
                    if (!slotHasItem) {
                        ItemStack single = cursor.clone();
                        single.setAmount(1);
                        topInv.setItem(rawSlot, single);
                        if (cursor.getAmount() <= 1) {
                            player.setItemOnCursor(null);
                        } else {
                            cursor.setAmount(cursor.getAmount() - 1);
                        }
                    } else if (slotItem.isSimilar(cursor) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                        slotItem.setAmount(slotItem.getAmount() + 1);
                        topInv.setItem(rawSlot, slotItem);
                        if (cursor.getAmount() <= 1) {
                            player.setItemOnCursor(null);
                        } else {
                            cursor.setAmount(cursor.getAmount() - 1);
                        }
                    }
                } else if (action == InventoryAction.SWAP_WITH_CURSOR) {
                    if (slotHasItem) {
                        topInv.setItem(rawSlot, cursor.clone());
                        player.setItemOnCursor(slotItem.clone());
                    }
                }
                refreshPriceDisplay(topInv, player);
            }
            case PICKUP_ALL, PICKUP_ONE, PICKUP_HALF, PICKUP_SOME -> {
                if (!slotHasItem) return;
                // Move item back to cursor / player inventory
                if (action == InventoryAction.PICKUP_ALL) {
                    player.setItemOnCursor(slotItem.clone());
                    topInv.setItem(rawSlot, null);
                } else if (action == InventoryAction.PICKUP_HALF || action == InventoryAction.PICKUP_SOME) {
                    int half = (int) Math.ceil(slotItem.getAmount() / 2.0);
                    int remaining = slotItem.getAmount() - half;
                    ItemStack taken = slotItem.clone();
                    taken.setAmount(half);
                    player.setItemOnCursor(taken);
                    if (remaining > 0) {
                        slotItem.setAmount(remaining);
                        topInv.setItem(rawSlot, slotItem);
                    } else {
                        topInv.setItem(rawSlot, null);
                    }
                } else if (action == InventoryAction.PICKUP_ONE) {
                    ItemStack one = slotItem.clone();
                    one.setAmount(1);
                    player.setItemOnCursor(one);
                    if (slotItem.getAmount() <= 1) {
                        topInv.setItem(rawSlot, null);
                    } else {
                        slotItem.setAmount(slotItem.getAmount() - 1);
                        topInv.setItem(rawSlot, slotItem);
                    }
                }
                refreshPriceDisplay(topInv, player);
            }
            case DROP_ONE_SLOT, DROP_ALL_SLOT -> {
                if (!slotHasItem) return;
                int dropAmount = action == InventoryAction.DROP_ONE_SLOT ? 1 : slotItem.getAmount();
                ItemStack drop = slotItem.clone();
                drop.setAmount(dropAmount);
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                if (dropAmount >= slotItem.getAmount()) {
                    topInv.setItem(rawSlot, null);
                } else {
                    slotItem.setAmount(slotItem.getAmount() - dropAmount);
                    topInv.setItem(rawSlot, slotItem);
                }
                refreshPriceDisplay(topInv, player);
            }
            case COLLECT_TO_CURSOR -> {
                // Pull matching items from the item area into cursor
                ItemStack cur = player.getItemOnCursor();
                if (cur == null || cur.getType() == Material.AIR) return;
                int needed = cur.getMaxStackSize() - cur.getAmount();
                for (int s = 0; s < ITEM_SLOT_COUNT && needed > 0; s++) {
                    ItemStack candidate = topInv.getItem(s);
                    if (candidate == null || !candidate.isSimilar(cur)) continue;
                    int take = Math.min(candidate.getAmount(), needed);
                    cur.setAmount(cur.getAmount() + take);
                    needed -= take;
                    if (take >= candidate.getAmount()) {
                        topInv.setItem(s, null);
                    } else {
                        candidate.setAmount(candidate.getAmount() - take);
                        topInv.setItem(s, candidate);
                    }
                }
                refreshPriceDisplay(topInv, player);
            }
            default -> { /* ignore hotbar number, clone, etc. */ }
        }
    }

    /**
     * Sells all items currently in the item area and closes the GUI.
     */
    private void handleConfirm(Player player, Inventory topInv) {
        double total = 0.0;
        boolean soldAnything = false;

        for (int slot = 0; slot < ITEM_SLOT_COUNT; slot++) {
            ItemStack item = topInv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            ShopTransactionResult result = transactionService.sell(player, item.getType(), item.getAmount());
            if (result.success()) {
                topInv.setItem(slot, null);
                // Accumulate the sell value from the pricing manager for the summary
                total += pricingManager.estimateBulkTotal(
                        item.getType().name(), item.getAmount(), ShopTransactionType.SELL);
                soldAnything = true;
            }
            // Failed items remain in the slot; onInventoryClose will return them
        }

        if (!soldAnything) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.nothingToSell()));
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                messages.soldSummary(transactionService.formatCurrency(total))));

        if (confirmSound != null && !confirmSound.isEmpty()) {
            player.playSound(player.getLocation(), confirmSound, confirmSoundVolume, confirmSoundPitch);
        }
    }

    /**
     * Recalculates the estimated sell total and updates the price display slot.
     */
    private void refreshPriceDisplay(Inventory topInv, Player player) {
        double total = 0.0;
        for (int slot = 0; slot < ITEM_SLOT_COUNT; slot++) {
            ItemStack item = topInv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            total += pricingManager.estimateBulkTotal(
                    item.getType().name(), item.getAmount(), ShopTransactionType.SELL);
        }
        updatePriceItem(topInv, total, player);
    }

    /**
     * Builds and places the control row into the given inventory.
     */
    private void populateControls(Inventory inv, double currentTotal, Player player) {
        // Gray glass filler
        ItemStack filler = buildNamedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = ITEM_SLOT_COUNT; slot < GUI_SIZE; slot++) {
            inv.setItem(slot, filler);
        }

        // Cancel button
        ItemStack cancel = buildNamedItem(Material.BARRIER,
                ChatColor.RED + "Cancel",
                ChatColor.GRAY + "Return all items to your inventory");
        inv.setItem(SLOT_CANCEL, cancel);

        // Price display
        updatePriceItem(inv, currentTotal, player);

        // Confirm button
        ItemStack confirm = buildNamedItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Confirm Sale",
                ChatColor.GRAY + "Click to sell all items above");
        inv.setItem(SLOT_CONFIRM, confirm);
    }

    /**
     * Rebuilds the price display item at {@value #SLOT_PRICE}.
     */
    private void updatePriceItem(Inventory inv, double total, Player player) {
        String formatted = transactionService.formatCurrency(total);
        ItemStack priceItem = buildNamedItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "Estimated Total: " + ChatColor.YELLOW + "~" + formatted,
                ChatColor.GRAY + "Place items above to sell them",
                ChatColor.GRAY + "The actual payout may differ slightly");
        inv.setItem(SLOT_PRICE, priceItem);
    }

    /**
     * Returns {@code true} if the material has a configured sell price and is
     * currently available (visible / not rotation-hidden).
     */
    private boolean isSellable(Material material) {
        if (material == null || material == Material.AIR) return false;
        // Rotation check: if the item is part of a rotation but not currently visible, reject it
        if (!pricingManager.isVisibleInMenu(material) && pricingManager.isPartOfRotation(material)) {
            return false;
        }
        return pricingManager.getPrice(material)
                .map(ShopPrice::canSell)
                .orElse(false);
    }

    /**
     * Finds the first empty slot in the item area (0-35), or -1 if full.
     */
    private int findEmptyItemSlot(Inventory inv) {
        for (int slot = 0; slot < ITEM_SLOT_COUNT; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) return slot;
        }
        return -1;
    }

    /**
     * Gives {@code item} to the player's inventory, dropping overflow at their feet.
     */
    private void giveOrDrop(Player player, ItemStack item) {
        var leftovers = player.getInventory().addItem(item);
        for (ItemStack overflow : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
    }

    // -----------------------------------------------------------------------
    // Item builder helpers
    // -----------------------------------------------------------------------

    private static ItemStack buildNamedItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>(lore.length);
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
