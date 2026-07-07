package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.bootstrap.EzShopsRegistry;
import com.skyblockexp.ezshops.gui.admin.ShopAdminBrowseGui;
import com.skyblockexp.ezshops.gui.admin.ShopAdminBrowseGui.Mode;
import com.skyblockexp.ezshops.playershop.PlayerShop;
import com.skyblockexp.ezshops.playershop.PlayerShopManager;
import com.skyblockexp.ezshops.teams.TeamMarketListing;
import com.skyblockexp.ezshops.teams.TeamMarketManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

/**
 * Handles {@code /shopadmin [browse|market]} and the GUI click events it spawns.
 */
public final class ShopAdminCommand implements CommandExecutor, TabCompleter, Listener {

    public static final String PERMISSION = "ezshops.shop.admin";

    private final ShopAdminBrowseGui browseGui;

    public ShopAdminCommand(ShopAdminBrowseGui browseGui) {
        this.browseGui = browseGui;
    }

    // ── Command ───────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use /shopadmin.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reseed")) {
            return handleReseed(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the /shopadmin GUI.");
            return true;
        }

        Mode mode = Mode.PLAYER_SHOPS;
        if (args.length > 0 && args[0].equalsIgnoreCase("market")) {
            mode = Mode.TEAM_MARKET;
        }
        browseGui.open(player, mode);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return List.of("browse", "market", "reseed").stream()
                    .filter(s -> s.startsWith(partial))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reseed")) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            return EzShopsRegistry.current().getBundledShopModes().stream()
                    .filter(mode -> mode.toLowerCase(Locale.ROOT).startsWith(partial))
                    .toList();
        }
        return List.of();
    }

    private boolean handleReseed(CommandSender sender, String[] args) {
        String mode = args.length > 1 ? args[1] : null;
        if (mode != null && mode.isBlank()) {
            mode = null;
        }

        String targetMode = mode;
        Set<String> bundledModes = EzShopsRegistry.current().getBundledShopModes();
        if (targetMode != null && !bundledModes.stream().anyMatch(m -> m.equalsIgnoreCase(targetMode))) {
            sender.sendMessage(ChatColor.RED + "Unknown shop mode '" + mode + "'.");
            sender.sendMessage(ChatColor.YELLOW + "Available bundled modes: " + String.join(", ", bundledModes));
            return true;
        }

        int created = EzShopsRegistry.current().reseedCategoryDefaults(targetMode);
        String scope = targetMode == null ? "all modes" : ("mode '" + targetMode + "'");
        sender.sendMessage(ChatColor.GREEN + "Reseed complete for " + scope + ": created "
                + created + " missing category default file(s).");
        sender.sendMessage(ChatColor.GRAY + "Existing category files were left unchanged.");
        return true;
    }

    // ── GUI listener ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        boolean isPlayerShops = title.equals(ShopAdminBrowseGui.TITLE_PLAYER_SHOPS);
        boolean isTeamMarket  = title.equals(ShopAdminBrowseGui.TITLE_TEAM_MARKET);
        if (!isPlayerShops && !isTeamMarket) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        Mode currentMode = isPlayerShops ? Mode.PLAYER_SHOPS : Mode.TEAM_MARKET;
        int page = pageFromInventory(event.getInventory(), currentMode);

        if (slot == browseGui.getSlotClose()) {
            player.closeInventory();
            return;
        }
        if (slot == browseGui.getSlotPrev()) {
            if (page > 0) browseGui.open(player, currentMode, page - 1);
            return;
        }
        if (slot == browseGui.getSlotNext()) {
            browseGui.open(player, currentMode, page + 1);
            return;
        }
        if (slot == browseGui.getSlotToggle()) {
            browseGui.open(player, currentMode == Mode.PLAYER_SHOPS ? Mode.TEAM_MARKET : Mode.PLAYER_SHOPS, 0);
            return;
        }

        // Entry slots (0-44) — shift-click to remove/cancel
        if (slot < browseGui.getItemsPerPage() && event.isShiftClick()) {
            if (!player.hasPermission(PERMISSION)) {
                player.sendMessage(ChatColor.RED + "No permission.");
                return;
            }
            if (isPlayerShops) {
                List<PlayerShop> entries = browseGui.getPagedPlayerShops(page);
                if (slot < entries.size()) {
                    PlayerShop shop = entries.get(slot);
                    browseGui.getPlayerShopManager().removeShop(shop);
                    browseGui.getPlayerShopManager().saveShops();
                    player.sendMessage(ChatColor.YELLOW + "Removed player shop owned by "
                            + Optional.ofNullable(org.bukkit.Bukkit.getOfflinePlayer(shop.ownerId()).getName()).orElse("Unknown") + ".");
                    browseGui.open(player, Mode.PLAYER_SHOPS, page);
                }
            } else {
                List<TeamMarketListing> entries = browseGui.getPagedMarketListings(page);
                if (slot < entries.size()) {
                    TeamMarketManager manager = browseGui.getTeamMarketManager();
                    if (manager != null) {
                        String err = manager.adminCancelListing(entries.get(slot).listingId(), player);
                        if (err != null) player.sendMessage(ChatColor.RED + err);
                        else player.sendMessage(ChatColor.YELLOW + "Listing force-cancelled.");
                    }
                    browseGui.open(player, Mode.TEAM_MARKET, page);
                }
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static int pageFromInventory(org.bukkit.inventory.Inventory inv, Mode mode) {
        // We track page via a paper slot lore item — simplest: always default 0
        // The GUI stores page in the title info item lore; extract it
        var info = inv.getItem(46); // SLOT_INFO
        if (info != null && info.hasItemMeta() && info.getItemMeta().hasLore()) {
            List<String> lore = info.getItemMeta().getLore();
            if (lore != null) {
                for (String line : lore) {
                    String stripped = ChatColor.stripColor(line);
                    if (stripped.startsWith("Page ")) {
                        try {
                            String[] parts = stripped.split("/");
                            return Integer.parseInt(parts[0].replace("Page ", "").strip()) - 1;
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return 0;
    }
}
