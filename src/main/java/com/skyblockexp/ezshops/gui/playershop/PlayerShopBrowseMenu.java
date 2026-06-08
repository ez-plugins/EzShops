package com.skyblockexp.ezshops.gui.playershop;

import com.skyblockexp.ezshops.playershop.PlayerShop;
import com.skyblockexp.ezshops.playershop.PlayerShopManager;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Listener that builds and manages the browse-all-player-shops GUI.
 *
 * <p>Call {@link #open(Player, int)} to open the GUI for a player.
 * The GUI uses {@link PlayerShopBrowseHolder} to identify its inventories
 * and handles pagination and purchase clicks.
 */
public final class PlayerShopBrowseMenu implements Listener {

    static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final Plugin plugin;
    private final PlayerShopManager manager;
    private final PlayerShopBrowseMessages messages;

    public PlayerShopBrowseMenu(Plugin plugin, PlayerShopManager manager, PlayerShopBrowseMessages messages) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.manager = Objects.requireNonNull(manager, "manager");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    /**
     * Opens the browse GUI on the specified page (1-indexed) for {@code player}.
     */
    public void open(Player player, int page) {
        List<PlayerShop> all = new ArrayList<>(manager.getShops());

        if (all.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.noShops()));
            return;
        }

        int totalPages = (int) Math.ceil((double) all.size() / PAGE_SIZE);
        page = Math.max(1, Math.min(page, totalPages));

        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, all.size());
        List<PlayerShop> pageShops = all.subList(from, to);

        PlayerShopBrowseHolder holder = new PlayerShopBrowseHolder(page, totalPages, pageShops);
        String title = ChatColor.translateAlternateColorCodes('&', messages.title(page, totalPages));
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        // Item slots
        for (int i = 0; i < pageShops.size(); i++) {
            inv.setItem(i, buildShopItem(pageShops.get(i), player));
        }

        // Filler for empty item slots and the entire bottom bar
        ItemStack filler = buildFiller();
        for (int i = pageShops.size(); i < PAGE_SIZE; i++) {
            inv.setItem(i, filler);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Prev button
        if (page > 1) {
            inv.setItem(PREV_SLOT, buildNavItem(Material.ARROW,
                    ChatColor.translateAlternateColorCodes('&', messages.prevPage())));
        }

        // Page info
        inv.setItem(INFO_SLOT, buildNavItem(Material.PAPER,
                ChatColor.translateAlternateColorCodes('&', messages.pageInfo(page, totalPages))));

        // Next button
        if (page < totalPages) {
            inv.setItem(NEXT_SLOT, buildNavItem(Material.ARROW,
                    ChatColor.translateAlternateColorCodes('&', messages.nextPage())));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PlayerShopBrowseHolder browse)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot == PREV_SLOT && browse.page() > 1) {
            Bukkit.getScheduler().runTask(plugin, () -> open(player, browse.page() - 1));
            return;
        }
        if (slot == NEXT_SLOT && browse.page() < browse.totalPages()) {
            Bukkit.getScheduler().runTask(plugin, () -> open(player, browse.page() + 1));
            return;
        }
        if (slot < PAGE_SIZE) {
            List<PlayerShop> shops = browse.pageShops();
            if (slot < shops.size()) {
                PlayerShop shop = shops.get(slot);
                ShopTransactionResult result = manager.purchase(shop, player);
                player.sendMessage(result.message());
                if (result.success()) {
                    // Reopen the same page so stock status updates
                    Bukkit.getScheduler().runTask(plugin, () -> open(player, browse.page()));
                }
            }
        }
    }

    // ---- private helpers ----

    private ItemStack buildShopItem(PlayerShop shop, Player viewer) {
        ItemStack display = shop.itemTemplate().clone();
        display.setAmount(1);
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(display.getType());
        }
        if (meta == null) return display;

        String ownerName = resolveOwnerName(shop.ownerId());
        String priceStr = manager.formatPrice(shop.price());
        boolean isOwn = viewer.getUniqueId().equals(shop.ownerId());
        boolean hasStock = manager.hasStock(shop);

        String rawItemName = rawName(shop.itemTemplate());
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                messages.shopName(shop.quantityPerSale(), rawItemName)));

        List<String> loreSource;
        if (isOwn) {
            loreSource = messages.ownShopLore(ownerName, priceStr);
        } else if (!hasStock) {
            loreSource = messages.outOfStockLore(ownerName, priceStr);
        } else {
            loreSource = messages.shopLore(ownerName, priceStr);
        }
        List<String> lore = new ArrayList<>();
        for (String line : loreSource) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private String resolveOwnerName(java.util.UUID ownerId) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(ownerId);
        String name = op.getName();
        return (name != null && !name.isEmpty()) ? name : "Unknown";
    }

    private String rawName(ItemStack item) {
        if (item == null) return "Item";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        // Convert MATERIAL_NAME -> Material Name
        String name = item.getType().name().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private static ItemStack buildFiller() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack buildNavItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            item.setItemMeta(meta);
        }
        return item;
    }
}
