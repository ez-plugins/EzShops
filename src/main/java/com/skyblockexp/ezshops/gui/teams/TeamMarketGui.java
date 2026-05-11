package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamMarketListing;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 54-slot paginated browse GUI for the team P2P market.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>0-44  — listing items (up to 45 per page)</li>
 *   <li>45    — previous page</li>
 *   <li>46    — page info</li>
 *   <li>47    — next page</li>
 *   <li>49    — "List an item" shortcut</li>
 *   <li>53    — Close</li>
 * </ul>
 */
public final class TeamMarketGui {

    public static final String TITLE_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Team Market";

    static final int ITEMS_PER_PAGE = 45;
    static final int SLOT_PREV      = 45;
    static final int SLOT_PAGE_INFO = 46;
    static final int SLOT_NEXT      = 47;
    static final int SLOT_LIST      = 49;
    static final int SLOT_CLOSE     = 53;

    private final TeamsIntegration teamsIntegration;
    private final TeamMarketManager marketManager;
    private final TeamMarketListGui listGui;

    public TeamMarketGui(TeamsIntegration teamsIntegration,
                         TeamMarketManager marketManager,
                         TeamMarketListGui listGui) {
        this.teamsIntegration = teamsIntegration;
        this.marketManager = marketManager;
        this.listGui = listGui;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        Optional<Team> teamOpt = teamsIntegration.getPlayerTeam(player.getUniqueId());
        if (teamOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }
        Team team = teamOpt.get();
        List<TeamMarketListing> allListings = marketManager.getTeamListings(team.getId());

        int maxPage = Math.max(0, (allListings.size() - 1) / ITEMS_PER_PAGE);
        int p = Math.min(Math.max(page, 0), maxPage);

        Inventory inv = Bukkit.createInventory(null, 54,
                TITLE_PREFIX + ChatColor.DARK_GRAY + " \u2014 " + ChatColor.RESET + team.getName()
                + ChatColor.DARK_GRAY + " [" + (p + 1) + "/" + (maxPage + 1) + "]");

        // Fill listing slots
        int start = p * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < allListings.size(); i++) {
            TeamMarketListing listing = allListings.get(start + i);
            inv.setItem(i, buildListingItem(listing, player.getUniqueId()));
        }

        // Navigation row
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        inv.setItem(SLOT_PREV, p > 0
                ? buildItem(Material.ARROW, ChatColor.YELLOW + "Previous Page")
                : filler);
        inv.setItem(SLOT_PAGE_INFO, buildItem(Material.PAPER,
                ChatColor.GRAY + "Page " + ChatColor.WHITE + (p + 1) + ChatColor.GRAY + " / " + ChatColor.WHITE + (maxPage + 1)));
        inv.setItem(SLOT_NEXT, p < maxPage
                ? buildItem(Material.ARROW, ChatColor.YELLOW + "Next Page")
                : filler);

        inv.setItem(SLOT_LIST, buildItem(Material.WRITABLE_BOOK,
                ChatColor.GREEN + "List an Item",
                ChatColor.GRAY + "Put an item up for sale",
                ChatColor.GRAY + "to your team members."));
        inv.setItem(SLOT_CLOSE, buildItem(Material.BARRIER, ChatColor.RED + "Close"));

        if (allListings.isEmpty()) {
            inv.setItem(22, buildItem(Material.COMPASS,
                    ChatColor.YELLOW + "No listings yet",
                    ChatColor.GRAY + "Click " + ChatColor.GREEN + "List an Item",
                    ChatColor.GRAY + "to put something up for sale."));
        }

        player.openInventory(inv);
    }

    private ItemStack buildListingItem(TeamMarketListing listing, UUID viewerUuid) {
        ItemStack display = listing.item().clone();
        display.setAmount(listing.quantity());
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String itemName = TeamMarketManager.friendlyName(listing.item());
        meta.setDisplayName(ChatColor.YELLOW + "" + listing.quantity() + "x " + itemName);

        List<String> lore = new ArrayList<>();
        String sellerName = Optional.ofNullable(Bukkit.getOfflinePlayer(listing.sellerUuid()).getName())
                .orElse("Unknown");
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.WHITE + sellerName);
        lore.add(ChatColor.GRAY + "Price:  " + ChatColor.GREEN + "$" + String.format("%.2f", listing.price()));
        lore.add("");
        boolean isOwn = listing.sellerUuid().equals(viewerUuid);
        if (isOwn) {
            lore.add(ChatColor.YELLOW + "Left-click " + ChatColor.GRAY + "to cancel this listing");
            lore.add(ChatColor.GRAY + "and reclaim your item.");
        } else {
            lore.add(ChatColor.AQUA + "Left-click " + ChatColor.GRAY + "to purchase.");
            lore.add(ChatColor.YELLOW + "Shift-click " + ChatColor.GRAY + "to purchase immediately.");
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    static ItemStack buildItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    // Package-private helpers for the listener

    TeamMarketListGui getListGui() { return listGui; }

    /**
     * Extracts the listing at a given GUI slot from the provided list.
     * Returns {@code null} if the slot is out of bounds.
     */
    static TeamMarketListing listingAt(List<TeamMarketListing> listings, int page, int slot) {
        if (slot >= ITEMS_PER_PAGE) return null;
        int index = page * ITEMS_PER_PAGE + slot;
        if (index < 0 || index >= listings.size()) return null;
        return listings.get(index);
    }

    /** Parses the page number out of a team market inventory title. */
    static int pageFromTitle(String title) {
        // Title ends with " [N/M]"
        int lb = title.lastIndexOf('[');
        int sl = title.lastIndexOf('/');
        if (lb < 0 || sl < 0 || sl <= lb) return 0;
        try {
            return Integer.parseInt(title.substring(lb + 1, sl).strip()) - 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
