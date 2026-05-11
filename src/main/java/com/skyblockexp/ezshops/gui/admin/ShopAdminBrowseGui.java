package com.skyblockexp.ezshops.gui.admin;

import com.skyblockexp.ezshops.playershop.PlayerShop;
import com.skyblockexp.ezshops.playershop.PlayerShopManager;
import com.skyblockexp.ezshops.teams.TeamMarketListing;
import com.skyblockexp.ezshops.teams.TeamMarketManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 54-slot paginated moderation GUI for server operators.
 *
 * <p>Shows two modes toggled by a button in the last row:
 * <ul>
 *   <li><b>PLAYER_SHOPS</b> — all active player chest shops</li>
 *   <li><b>TEAM_MARKET</b>  — all active team P2P market listings</li>
 * </ul>
 *
 * <p>Shift-clicking any entry removes/cancels it (requires
 * {@value PlayerShopManager#PERMISSION_ADMIN}).
 */
public final class ShopAdminBrowseGui {

    public enum Mode { PLAYER_SHOPS, TEAM_MARKET }

    public static final String TITLE_PLAYER_SHOPS = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Shop Admin - Player Shops";
    public static final String TITLE_TEAM_MARKET  = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Shop Admin - Team Market";

    private static final int ITEMS_PER_PAGE = 45;
    private static final int SLOT_PREV   = 45;
    private static final int SLOT_INFO   = 46;
    private static final int SLOT_NEXT   = 47;
    private static final int SLOT_TOGGLE = 49;
    private static final int SLOT_CLOSE  = 53;

    private final PlayerShopManager playerShopManager;
    private final TeamMarketManager teamMarketManager;

    public ShopAdminBrowseGui(PlayerShopManager playerShopManager,
                              TeamMarketManager teamMarketManager) {
        this.playerShopManager = playerShopManager;
        this.teamMarketManager = teamMarketManager;
    }

    public void open(Player player, Mode mode) {
        open(player, mode, 0);
    }

    public void open(Player player, Mode mode, int page) {
        if (mode == Mode.PLAYER_SHOPS) openPlayerShops(player, page);
        else openTeamMarket(player, page);
    }

    // ── Player shops mode ─────────────────────────────────────────────────────

    private void openPlayerShops(Player player, int page) {
        List<PlayerShop> shops = new ArrayList<>(playerShopManager.getShops());
        int maxPage = Math.max(0, (shops.size() - 1) / ITEMS_PER_PAGE);
        int p = clamp(page, 0, maxPage);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PLAYER_SHOPS);
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        int start = p * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < shops.size(); i++) {
            PlayerShop shop = shops.get(start + i);
            inv.setItem(i, buildShopEntry(shop));
        }

        if (shops.isEmpty()) {
            inv.setItem(22, buildItem(Material.COMPASS,
                    ChatColor.YELLOW + "No player shops active."));
        }

        buildNavRow(inv, filler, p, maxPage, Mode.PLAYER_SHOPS);
        player.openInventory(inv);
    }

    private ItemStack buildShopEntry(PlayerShop shop) {
        ItemStack display = shop.itemTemplate().clone();
        display.setAmount(shop.quantityPerSale());
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String ownerName = Optional.ofNullable(
                Bukkit.getOfflinePlayer(shop.ownerId()).getName()).orElse("Unknown");
        Location loc = shop.signLocation();

        meta.setDisplayName(ChatColor.YELLOW + ownerName + ChatColor.GRAY + "'s shop");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Item:  " + ChatColor.WHITE + friendlyMaterial(shop.itemTemplate().getType()));
        lore.add(ChatColor.GRAY + "Qty:   " + ChatColor.WHITE + shop.quantityPerSale());
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.GREEN + "$" + String.format("%.2f", shop.price()));
        lore.add(ChatColor.GRAY + "Sign:  " + ChatColor.WHITE
                + (loc.getWorld() != null ? loc.getWorld().getName() : "?")
                + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        lore.add("");
        lore.add(ChatColor.RED + "Shift-click " + ChatColor.GRAY + "to remove this shop.");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    // ── Team market mode ──────────────────────────────────────────────────────

    private void openTeamMarket(Player player, int page) {
        List<TeamMarketListing> listings;
        if (teamMarketManager != null) {
            listings = teamMarketManager.getAllListings();
        } else {
            listings = List.of();
        }
        int maxPage = Math.max(0, (listings.size() - 1) / ITEMS_PER_PAGE);
        int p = clamp(page, 0, maxPage);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_TEAM_MARKET);
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        int start = p * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < listings.size(); i++) {
            inv.setItem(i, buildMarketEntry(listings.get(start + i)));
        }

        if (listings.isEmpty()) {
            inv.setItem(22, buildItem(Material.COMPASS,
                    ChatColor.YELLOW + "No team market listings active."));
        }

        buildNavRow(inv, filler, p, maxPage, Mode.TEAM_MARKET);
        player.openInventory(inv);
    }

    private ItemStack buildMarketEntry(TeamMarketListing listing) {
        ItemStack display = listing.item().clone();
        display.setAmount(listing.quantity());
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String sellerName = Optional.ofNullable(
                Bukkit.getOfflinePlayer(listing.sellerUuid()).getName()).orElse("Unknown");
        long ageSeconds = (System.currentTimeMillis() - listing.listedAt()) / 1000;
        String age = ageSeconds < 60 ? ageSeconds + "s ago"
                : ageSeconds < 3600 ? (ageSeconds / 60) + "m ago"
                : (ageSeconds / 3600) + "h ago";

        meta.setDisplayName(ChatColor.YELLOW + "" + listing.quantity() + "x "
                + TeamMarketManager.friendlyName(listing.item()));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.WHITE + sellerName);
        lore.add(ChatColor.GRAY + "Team:   " + ChatColor.WHITE + listing.teamId());
        lore.add(ChatColor.GRAY + "Price:  " + ChatColor.GREEN + "$" + String.format("%.2f", listing.price()));
        lore.add(ChatColor.GRAY + "Listed: " + ChatColor.WHITE + age);
        lore.add("");
        lore.add(ChatColor.RED + "Shift-click " + ChatColor.GRAY + "to force-cancel this listing.");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void buildNavRow(Inventory inv, ItemStack filler, int page, int maxPage, Mode mode) {
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        inv.setItem(SLOT_PREV, page > 0
                ? buildItem(Material.ARROW, ChatColor.YELLOW + "Previous Page")
                : filler);
        inv.setItem(SLOT_INFO, buildItem(Material.PAPER,
                ChatColor.GRAY + "Page " + ChatColor.WHITE + (page + 1) + ChatColor.GRAY + "/" + ChatColor.WHITE + (maxPage + 1),
                ChatColor.GRAY + "Mode: " + ChatColor.WHITE + (mode == Mode.PLAYER_SHOPS ? "Player Shops" : "Team Market")));
        inv.setItem(SLOT_NEXT, page < maxPage
                ? buildItem(Material.ARROW, ChatColor.YELLOW + "Next Page")
                : filler);
        inv.setItem(SLOT_TOGGLE, mode == Mode.PLAYER_SHOPS
                ? buildItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "Switch to Team Market",
                        ChatColor.GRAY + "Click to view all team market listings.")
                : buildItem(Material.CHEST, ChatColor.AQUA + "Switch to Player Shops",
                        ChatColor.GRAY + "Click to view all player chest shops."));
        inv.setItem(SLOT_CLOSE, buildItem(Material.BARRIER, ChatColor.RED + "Close"));
    }

    // ── Public helpers for listener ───────────────────────────────────────────

    public int getItemsPerPage() { return ITEMS_PER_PAGE; }
    public int getSlotPrev()     { return SLOT_PREV; }
    public int getSlotNext()     { return SLOT_NEXT; }
    public int getSlotToggle()   { return SLOT_TOGGLE; }
    public int getSlotClose()    { return SLOT_CLOSE; }

    public List<PlayerShop> getPagedPlayerShops(int page) {
        List<PlayerShop> all = new ArrayList<>(playerShopManager.getShops());
        int start = page * ITEMS_PER_PAGE;
        if (start >= all.size()) return List.of();
        return all.subList(start, Math.min(start + ITEMS_PER_PAGE, all.size()));
    }

    public List<TeamMarketListing> getPagedMarketListings(int page) {
        if (teamMarketManager == null) return List.of();
        List<TeamMarketListing> all = teamMarketManager.getAllListings();
        int start = page * ITEMS_PER_PAGE;
        if (start >= all.size()) return List.of();
        return all.subList(start, Math.min(start + ITEMS_PER_PAGE, all.size()));
    }

    public PlayerShopManager getPlayerShopManager() { return playerShopManager; }
    public TeamMarketManager getTeamMarketManager()  { return teamMarketManager; }

    // ── Util ──────────────────────────────────────────────────────────────────

    private static ItemStack buildItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static String friendlyMaterial(Material mat) {
        String raw = mat.name().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
