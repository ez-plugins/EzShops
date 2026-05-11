package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamMarketManager;
import com.skyblockexp.ezshops.teams.TeamsIntegration;
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
 * <p>Slot layout (mirrored: qty row centred like price row):
 * <pre>
 *  Row 0:  [filler…] [item preview @ 4] [filler…]
 *  Row 1:  [MIN@9] [−8@10] [−1@11] [filler@12] [qty display@13] [filler@14] [+1@15] [+8@16] [MAX@17]
 *  Row 2:  [−L@18] [−M@19] [−S@20] [filler@21] [price display@22] [filler@23] [+S@24] [+M@25] [+L@26]
 *  Row 3:  [filler…] [instructions@31] [filler…]
 *  Row 4:  [filler…] [inv hint@40] [filler…]
 *  Row 5:  [back@45] [filler…] [cancel@49] [filler…] [confirm@53]
 * </pre>
 *
 * <p>The player selects the item by clicking it from their own inventory
 * (the lower half of the open inventory view). The clicked stack's amount is
 * used as the initial quantity.
 */
public final class TeamMarketListGui {

    public static final String TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "List Item for Sale";

    // Row 0 — item preview
    static final int SLOT_ITEM_PREVIEW = 4;

    // Row 1 — quantity (9 slots, 9-17, symmetric around slot 13)
    static final int SLOT_QTY_MIN     =  9;
    static final int SLOT_QTY_MINUS_8 = 10;
    static final int SLOT_QTY_MINUS_1 = 11;
    // 12 = filler
    static final int SLOT_QTY_DISPLAY = 13;
    // 14 = filler
    static final int SLOT_QTY_PLUS_1  = 15;
    static final int SLOT_QTY_PLUS_8  = 16;
    static final int SLOT_QTY_MAX     = 17;

    // Row 2 — price (9 slots, 18-26, symmetric around slot 22)
    static final int SLOT_PRICE_MINUS_LRG = 18;
    static final int SLOT_PRICE_MINUS_MED = 19;
    static final int SLOT_PRICE_MINUS_SML = 20;
    // 21 = filler
    static final int SLOT_PRICE_DISPLAY   = 22;
    // 23 = filler
    static final int SLOT_PRICE_PLUS_SML  = 24;
    static final int SLOT_PRICE_PLUS_MED  = 25;
    static final int SLOT_PRICE_PLUS_LRG  = 26;

    // Row 3 — instructions panel
    static final int SLOT_INSTRUCTIONS = 31;

    // Row 4 — inventory separator hint
    static final int SLOT_INV_HINT     = 40;

    // Row 5 — actions
    static final int SLOT_BACK    = 45;
    static final int SLOT_CANCEL  = 49;
    static final int SLOT_CONFIRM = 53;

    private static final Set<Integer> CONTROL_SLOTS = Set.of(
            SLOT_ITEM_PREVIEW,
            SLOT_QTY_MIN, SLOT_QTY_MINUS_8, SLOT_QTY_MINUS_1,
            SLOT_QTY_DISPLAY,
            SLOT_QTY_PLUS_1, SLOT_QTY_PLUS_8, SLOT_QTY_MAX,
            SLOT_PRICE_MINUS_LRG, SLOT_PRICE_MINUS_MED, SLOT_PRICE_MINUS_SML,
            SLOT_PRICE_DISPLAY,
            SLOT_PRICE_PLUS_SML, SLOT_PRICE_PLUS_MED, SLOT_PRICE_PLUS_LRG,
            SLOT_INSTRUCTIONS, SLOT_INV_HINT,
            SLOT_BACK, SLOT_CANCEL, SLOT_CONFIRM);

    /** Default price step sizes: small=1 000, medium=10 000, large=1 000 000. */
    public static final double[] DEFAULT_PRICE_STEPS = {1_000, 10_000, 1_000_000};

    // ── State ─────────────────────────────────────────────────────────────────

    public static final class State {
        public ItemStack selectedItem = null;
        public int quantity = 1;
        public double price = 1_000.0;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final TeamsIntegration teamsIntegration;
    private final TeamMarketManager marketManager;
    /** Three price step sizes: [0]=small, [1]=medium, [2]=large. */
    private final double[] priceSteps;

    /** Tracks open listing GUIs: playerUUID -> listing state. */
    private final Map<UUID, State> openStates = new HashMap<>();
    /**
     * Players whose inventory is currently being redrawn. Prevents the
     * InventoryCloseEvent (fired by openInventory) from wiping the state.
     */
    private final Set<UUID> redrawingPlayers = new HashSet<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    public TeamMarketListGui(TeamsIntegration teamsIntegration,
                             TeamMarketManager marketManager,
                             double[] priceSteps) {
        this.teamsIntegration = teamsIntegration;
        this.marketManager = marketManager;
        this.priceSteps = (priceSteps != null && priceSteps.length >= 3)
                ? priceSteps : DEFAULT_PRICE_STEPS;
    }

    // ── Public API ────────────────────────────────────────────────────────────

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

    /** Returns true while a {@code redraw} openInventory call is in-flight. */
    public boolean isRedrawing(Player player) {
        return redrawingPlayers.contains(player.getUniqueId());
    }

    /** Returns all open player UUIDs (for cleanup on quit). */
    public Set<UUID> openPlayers() {
        return openStates.keySet();
    }

    /**
     * Returns the price step amount for the given size index.
     * @param index 0=small, 1=medium, 2=large
     */
    public double priceStep(int index) {
        return priceSteps[index];
    }

    /** Re-renders the GUI into a new inventory and opens it for the player. */
    public void redraw(Player player) {
        State state = openStates.get(player.getUniqueId());
        if (state == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        ItemStack filler = buildPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (!CONTROL_SLOTS.contains(i)) inv.setItem(i, filler);
        }

        // ── Row 0: item preview ───────────────────────────────────────────────
        int heldCount = state.selectedItem != null ? countMatching(player, state.selectedItem) : 0;
        if (state.selectedItem != null) {
            ItemStack preview = state.selectedItem.clone();
            preview.setAmount(1);
            ItemMeta pm = preview.getItemMeta();
            if (pm != null) {
                pm.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD
                        + TeamMarketManager.friendlyName(state.selectedItem)
                        + ChatColor.RESET + ChatColor.YELLOW + " \u2714");
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
                    ChatColor.YELLOW + "\u25bc  Your inventory is at the bottom  \u25bc"));
        }

        // ── Row 1: quantity controls ──────────────────────────────────────────
        int maxQty = state.selectedItem != null ? Math.max(1, heldCount) : 64;
        inv.setItem(SLOT_QTY_MIN, buildPane(Material.MAGENTA_STAINED_GLASS_PANE,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MIN",
                ChatColor.GRAY + "Set quantity to 1"));
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
        inv.setItem(SLOT_QTY_MAX, buildPane(Material.MAGENTA_STAINED_GLASS_PANE,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "MAX",
                ChatColor.GRAY + "Set quantity to " + maxQty));

        // ── Row 2: price controls ─────────────────────────────────────────────
        inv.setItem(SLOT_PRICE_MINUS_LRG, buildPane(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "" + ChatColor.BOLD + "-" + formatPrice(priceSteps[2]),
                ChatColor.GRAY + "Decrease price by " + formatPrice(priceSteps[2])));
        inv.setItem(SLOT_PRICE_MINUS_MED, buildPane(Material.ORANGE_STAINED_GLASS_PANE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "-" + formatPrice(priceSteps[1]),
                ChatColor.GRAY + "Decrease price by " + formatPrice(priceSteps[1])));
        inv.setItem(SLOT_PRICE_MINUS_SML, buildPane(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "-" + formatPrice(priceSteps[0]),
                ChatColor.GRAY + "Decrease price by " + formatPrice(priceSteps[0])));
        inv.setItem(SLOT_PRICE_DISPLAY, buildPriceDisplay(state.price, state.quantity));
        inv.setItem(SLOT_PRICE_PLUS_SML, buildPane(Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "+" + formatPrice(priceSteps[0]),
                ChatColor.GRAY + "Increase price by " + formatPrice(priceSteps[0])));
        inv.setItem(SLOT_PRICE_PLUS_MED, buildPane(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "+" + formatPrice(priceSteps[1]),
                ChatColor.GRAY + "Increase price by " + formatPrice(priceSteps[1])));
        inv.setItem(SLOT_PRICE_PLUS_LRG, buildPane(Material.GREEN_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "+" + formatPrice(priceSteps[2]),
                ChatColor.GRAY + "Increase price by " + formatPrice(priceSteps[2])));

        // ── Row 3: instructions ───────────────────────────────────────────────
        inv.setItem(SLOT_INSTRUCTIONS, buildPane(Material.BOOK,
                ChatColor.AQUA + "" + ChatColor.BOLD + "How to Create a Listing",
                ChatColor.GRAY + "1. " + ChatColor.WHITE + "Click an item from your inventory below",
                ChatColor.GRAY + "2. " + ChatColor.WHITE + "Set " + ChatColor.YELLOW + "quantity"
                        + ChatColor.WHITE + " using the row above",
                ChatColor.GRAY + "3. " + ChatColor.WHITE + "Set " + ChatColor.GOLD + "price"
                        + ChatColor.WHITE + " using the row above that",
                ChatColor.GRAY + "4. " + ChatColor.WHITE + "Click "
                        + ChatColor.GREEN + "Confirm Listing" + ChatColor.WHITE + " when ready"));

        // ── Row 4: inventory separator hint ──────────────────────────────────
        inv.setItem(SLOT_INV_HINT, buildPane(Material.CYAN_STAINED_GLASS_PANE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "\u25bc  Your Inventory  \u25bc",
                ChatColor.GRAY + "Click any item below to select it for listing"));

        // ── Row 5: actions ────────────────────────────────────────────────────
        inv.setItem(SLOT_BACK, buildPane(Material.ARROW, ChatColor.YELLOW + "Back to Market"));
        inv.setItem(SLOT_CANCEL, buildPane(Material.BARRIER, ChatColor.RED + "Cancel"));

        if (state.selectedItem != null) {
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

    // ── Package-private helpers ───────────────────────────────────────────────

    /** Counts how many items matching {@code template} the player holds (ignores amount). */
    static int countMatching(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && s.isSimilar(template)) count += s.getAmount();
        }
        return count;
    }

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

    /**
     * Formats a price step value compactly for button labels.
     * Examples: 1000 → "$1k", 10000 → "$10k", 1500000 → "$1.5M"
     */
    static String formatPrice(double amount) {
        if (amount >= 1_000_000) {
            double m = amount / 1_000_000;
            return "$" + (m == (long) m
                    ? String.valueOf((long) m) : String.format("%.1f", m)) + "M";
        }
        if (amount >= 1_000) {
            double k = amount / 1_000;
            return "$" + (k == (long) k
                    ? String.valueOf((long) k) : String.format("%.1f", k)) + "k";
        }
        return "$" + String.format("%.2f", amount);
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
}
