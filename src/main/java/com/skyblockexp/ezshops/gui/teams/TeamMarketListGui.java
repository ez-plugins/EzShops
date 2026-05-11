package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamMarketManager;
import com.skyblockexp.ezshops.teams.TeamsIntegration;
import com.skyblockexp.teamsapi.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 54-slot GUI for creating a new team market listing.
 *
 * <p>Slot layout:
 * <pre>
 *  Row 0:  [filler] ... [item preview @ 4] ...
 *  Row 1:  [qty -8 @ 9] [qty -1 @ 10] [qty display @ 11] [qty +1 @ 12] [qty +8 @ 13]
 *  Row 2:  [price -100 @ 18] [price -10 @ 19] [price display @ 22] [price +10 @ 25] [price +100 @ 26]
 *  Row 3:  (spare)
 *  Row 4:  (spare)
 *  Row 5:  [back @ 45] ... [cancel @ 49] ... [confirm @ 53]
 * </pre>
 *
 * <p>The player selects the item by clicking it from their own inventory
 * (the lower half of the open inventory view).
 */
public final class TeamMarketListGui {

    public static final String TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "List Item for Sale";

    // Row 0
    private static final int SLOT_ITEM_PREVIEW = 4;
    // Row 1 — quantity
    private static final int SLOT_QTY_MINUS_8  =  9;
    private static final int SLOT_QTY_MINUS_1  = 10;
    private static final int SLOT_QTY_DISPLAY  = 11;
    private static final int SLOT_QTY_PLUS_1   = 12;
    private static final int SLOT_QTY_PLUS_8   = 13;
    // Row 2 — price
    private static final int SLOT_PRICE_MINUS_100 = 18;
    private static final int SLOT_PRICE_MINUS_10  = 19;
    private static final int SLOT_PRICE_MINUS_1   = 20;
    private static final int SLOT_PRICE_DISPLAY   = 22;
    private static final int SLOT_PRICE_PLUS_1    = 24;
    private static final int SLOT_PRICE_PLUS_10   = 25;
    private static final int SLOT_PRICE_PLUS_100  = 26;
    // Row 3 — instructions panel (centre)
    private static final int SLOT_INSTRUCTIONS    = 31;
    // Row 4 — inventory separator hint (centre)
    private static final int SLOT_INV_HINT        = 40;
    // Row 5 — actions
    private static final int SLOT_BACK    = 45;
    private static final int SLOT_CANCEL  = 49;
    private static final int SLOT_CONFIRM = 53;

    private static final Set<Integer> CONTROL_SLOTS = Set.of(
            SLOT_ITEM_PREVIEW,
            SLOT_QTY_MINUS_8, SLOT_QTY_MINUS_1, SLOT_QTY_DISPLAY, SLOT_QTY_PLUS_1, SLOT_QTY_PLUS_8,
            SLOT_PRICE_MINUS_100, SLOT_PRICE_MINUS_10, SLOT_PRICE_MINUS_1,
            SLOT_PRICE_DISPLAY,
            SLOT_PRICE_PLUS_1, SLOT_PRICE_PLUS_10, SLOT_PRICE_PLUS_100,
            SLOT_INSTRUCTIONS, SLOT_INV_HINT,
            SLOT_BACK, SLOT_CANCEL, SLOT_CONFIRM);

    public static final class State {
        public ItemStack selectedItem = null;
        public int quantity = 1;
        public double price = 10.0;
    }

    private final TeamsIntegration teamsIntegration;
    private final TeamMarketManager marketManager;

    /** Tracks open listing GUIs: playerUUID -> listing state */
    private final Map<UUID, State> openStates = new HashMap<>();
    /**
     * Players whose inventory is currently being redrawn. Prevents the
     * InventoryCloseEvent (fired by openInventory) from wiping the state.
     */
    private final Set<UUID> redrawingPlayers = new HashSet<>();

    public TeamMarketListGui(TeamsIntegration teamsIntegration,
                             TeamMarketManager marketManager) {
        this.teamsIntegration = teamsIntegration;
        this.marketManager = marketManager;
    }

    public void open(Player player) {
        openStates.computeIfAbsent(player.getUniqueId(), k -> new State());
        redraw(player);
    }

    public void close(Player player) {
        openStates.remove(player.getUniqueId());
    }

    public State getState(Player player) {
        return openStates.get(player.getUniqueId());
    }

    public boolean isOpen(Player player) {
        return openStates.containsKey(player.getUniqueId());
    }

    /** Returns true while a redraw {@code openInventory} call is in-flight. */
    public boolean isRedrawing(Player player) {
        return redrawingPlayers.contains(player.getUniqueId());
    }

    /** Returns all open player UUIDs (for cleanup on quit). */
    public Set<UUID> openPlayers() {
        return openStates.keySet();
    }

    /** Re-renders the GUI into a new inventory and opens it. */
    public void redraw(Player player) {
        State state = openStates.get(player.getUniqueId());
        if (state == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack filler = buildPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (!CONTROL_SLOTS.contains(i)) inv.setItem(i, filler);
        }

        // ── Item preview (row 0, centre) ─────────────────────────────────────
        int heldCount = state.selectedItem != null ? countMatching(player, state.selectedItem) : 0;
        if (state.selectedItem != null) {
            ItemStack preview = state.selectedItem.clone();
            preview.setAmount(1);
            ItemMeta pm = preview.getItemMeta();
            if (pm != null) {
                pm.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD
                        + TeamMarketManager.friendlyName(state.selectedItem)
                        + ChatColor.RESET + ChatColor.YELLOW + " ✔");
                pm.setLore(List.of(
                        ChatColor.GRAY + "You have: " + ChatColor.WHITE + heldCount + " in your inventory",
                        ChatColor.GRAY + "Click a different item below to change"));
                preview.setItemMeta(pm);
            }
            inv.setItem(SLOT_ITEM_PREVIEW, preview);
        } else {
            inv.setItem(SLOT_ITEM_PREVIEW, buildPane(Material.LIME_STAINED_GLASS_PANE,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "No Item Selected",
                    ChatColor.WHITE + "Click any item from your inventory",
                    ChatColor.WHITE + "below this window to select it.",
                    "",
                    ChatColor.YELLOW + "▼  Your inventory is at the bottom  ▼"));
        }

        // ── Quantity controls (row 1, slots 9-13) ────────────────────────────
        int maxQty = state.selectedItem != null ? Math.max(1, heldCount) : 64;
        inv.setItem(SLOT_QTY_MINUS_8, buildPane(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "" + ChatColor.BOLD + "-8",
                ChatColor.GRAY + "Decrease quantity by 8"));
        inv.setItem(SLOT_QTY_MINUS_1, buildPane(Material.ORANGE_STAINED_GLASS_PANE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "-1",
                ChatColor.GRAY + "Decrease quantity by 1"));
        inv.setItem(SLOT_QTY_DISPLAY, buildQtyDisplay(state.quantity, maxQty));
        inv.setItem(SLOT_QTY_PLUS_1, buildPane(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "+1",
                ChatColor.GRAY + "Increase quantity by 1"));
        inv.setItem(SLOT_QTY_PLUS_8, buildPane(Material.GREEN_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "+8",
                ChatColor.GRAY + "Increase quantity by 8"));

        // ── Price controls (row 2, slots 18-26) ─────────────────────────────
        inv.setItem(SLOT_PRICE_MINUS_100, buildPane(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "" + ChatColor.BOLD + "-$100",
                ChatColor.GRAY + "Decrease price by $100"));
        inv.setItem(SLOT_PRICE_MINUS_10, buildPane(Material.ORANGE_STAINED_GLASS_PANE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "-$10",
                ChatColor.GRAY + "Decrease price by $10"));
        inv.setItem(SLOT_PRICE_MINUS_1, buildPane(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "-$1",
                ChatColor.GRAY + "Decrease price by $1"));
        inv.setItem(SLOT_PRICE_DISPLAY, buildPriceDisplay(state.price, state.quantity));
        inv.setItem(SLOT_PRICE_PLUS_1, buildPane(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "+$1",
                ChatColor.GRAY + "Increase price by $1"));
        inv.setItem(SLOT_PRICE_PLUS_10, buildPane(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "+$10",
                ChatColor.GRAY + "Increase price by $10"));
        inv.setItem(SLOT_PRICE_PLUS_100, buildPane(Material.GREEN_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "+$100",
                ChatColor.GRAY + "Increase price by $100"));

        // ── How-to instructions (row 3, slot 31) ─────────────────────────────
        inv.setItem(SLOT_INSTRUCTIONS, buildPane(Material.BOOK,
                ChatColor.AQUA + "" + ChatColor.BOLD + "How to Create a Listing",
                ChatColor.GRAY + "1. " + ChatColor.WHITE + "Click an item from your inventory below",
                ChatColor.GRAY + "2. " + ChatColor.WHITE + "Use " + ChatColor.RED + "-"
                        + ChatColor.WHITE + " / " + ChatColor.GREEN + "+"
                        + ChatColor.WHITE + " buttons in row 2 to set qty",
                ChatColor.GRAY + "3. " + ChatColor.WHITE + "Use " + ChatColor.RED + "-"
                        + ChatColor.WHITE + " / " + ChatColor.GREEN + "+"
                        + ChatColor.WHITE + " buttons in row 3 to set price",
                ChatColor.GRAY + "4. " + ChatColor.WHITE + "Click "
                        + ChatColor.GREEN + "Confirm Listing" + ChatColor.WHITE + " when ready"));

        // ── Inventory separator hint (row 4, slot 40) ────────────────────────
        inv.setItem(SLOT_INV_HINT, buildPane(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "▼  Your Inventory  ▼",
                ChatColor.GRAY + "Click any item below to select it for listing"));

        // ── Actions (row 5) ───────────────────────────────────────────────────
        inv.setItem(SLOT_BACK, buildPane(Material.ARROW, ChatColor.YELLOW + "Back to Market"));
        inv.setItem(SLOT_CANCEL, buildPane(Material.BARRIER, ChatColor.RED + "Cancel"));

        boolean canConfirm = state.selectedItem != null;
        if (canConfirm) {
            double total = state.price * state.quantity;
            inv.setItem(SLOT_CONFIRM, buildPane(Material.EMERALD,
                    ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm Listing",
                    ChatColor.GRAY + "Item: " + ChatColor.WHITE + state.quantity + "x "
                            + TeamMarketManager.friendlyName(state.selectedItem),
                    ChatColor.GRAY + "Price each: " + ChatColor.GREEN
                            + "$" + String.format("%.2f", state.price),
                    ChatColor.GRAY + "Total value: " + ChatColor.GREEN
                            + "$" + String.format("%.2f", total),
                    "",
                    ChatColor.GRAY + "The item will be taken from your",
                    ChatColor.GRAY + "inventory immediately upon listing."));
        } else {
            inv.setItem(SLOT_CONFIRM, buildPane(Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "" + ChatColor.BOLD + "Select an Item First",
                    ChatColor.GRAY + "Click any item from your inventory below"));
        }

        // Guard against InventoryCloseEvent clearing state during this call
        redrawingPlayers.add(player.getUniqueId());
        player.openInventory(inv);
        redrawingPlayers.remove(player.getUniqueId());
    }

    // ── Slot constants exposed for listener ───────────────────────────────────

    static int slotQtyMinus8()    { return SLOT_QTY_MINUS_8; }
    static int slotQtyMinus1()    { return SLOT_QTY_MINUS_1; }
    static int slotQtyPlus1()     { return SLOT_QTY_PLUS_1; }
    static int slotQtyPlus8()     { return SLOT_QTY_PLUS_8; }
    static int slotPriceMinus100(){ return SLOT_PRICE_MINUS_100; }
    static int slotPriceMinus10() { return SLOT_PRICE_MINUS_10; }
    static int slotPriceMinus1()  { return SLOT_PRICE_MINUS_1; }
    static int slotPricePlus1()   { return SLOT_PRICE_PLUS_1; }
    static int slotPricePlus10()  { return SLOT_PRICE_PLUS_10; }
    static int slotPricePlus100() { return SLOT_PRICE_PLUS_100; }
    static int slotBack()         { return SLOT_BACK; }
    static int slotCancel()       { return SLOT_CANCEL; }
    static int slotConfirm()      { return SLOT_CONFIRM; }
    static int slotItemPreview()  { return SLOT_ITEM_PREVIEW; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static ItemStack buildQtyDisplay(int qty, int maxQty) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Quantity");
        meta.setLore(List.of(
                ChatColor.WHITE + "Amount: " + ChatColor.YELLOW + "" + ChatColor.BOLD + qty,
                ChatColor.GRAY  + "Max available: " + ChatColor.WHITE + maxQty,
                "",
                ChatColor.GRAY + "Use the buttons on either side to adjust"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildPriceDisplay(double price, int qty) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Price per Item");
        meta.setLore(List.of(
                ChatColor.WHITE + "Price: " + ChatColor.GREEN + "" + ChatColor.BOLD
                        + "$" + String.format("%.2f", price),
                ChatColor.GRAY + "Total: " + ChatColor.GREEN
                        + "$" + String.format("%.2f", price * qty),
                "",
                ChatColor.GRAY + "Use the buttons on either side to adjust"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildPane(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    static int countMatching(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && s.isSimilar(template)) count += s.getAmount();
        }
        return count;
    }
}
