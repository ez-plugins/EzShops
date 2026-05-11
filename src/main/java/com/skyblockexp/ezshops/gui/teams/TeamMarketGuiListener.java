package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamMarketListing;
import com.skyblockexp.ezshops.teams.TeamMarketManager;
import com.skyblockexp.ezshops.teams.TeamsIntegration;
import com.skyblockexp.teamsapi.model.Team;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Handles all click events inside the team market GUIs.
 * Detected by inventory title prefixes:
 * <ul>
 *   <li>{@link TeamMarketGui#TITLE_PREFIX} – browse/buy GUI</li>
 *   <li>{@link TeamMarketListGui#TITLE} – listing creation GUI</li>
 * </ul>
 */
public final class TeamMarketGuiListener implements Listener {

    private final TeamMarketGui marketGui;
    private final TeamMarketListGui listGui;
    private final TeamMarketManager marketManager;
    private final TeamsIntegration teamsIntegration;

    public TeamMarketGuiListener(TeamMarketGui marketGui,
                                 TeamMarketListGui listGui,
                                 TeamMarketManager marketManager,
                                 TeamsIntegration teamsIntegration) {
        this.marketGui = marketGui;
        this.listGui = listGui;
        this.marketManager = marketManager;
        this.teamsIntegration = teamsIntegration;
    }

    // ── Browse GUI ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.stripColor(TeamMarketGui.TITLE_PREFIX))
                || title.startsWith(TeamMarketGui.TITLE_PREFIX)) {
            handleBrowseClick(event, player, title);
            return;
        }
        if (title.equals(TeamMarketListGui.TITLE)) {
            handleListGuiClick(event, player);
        }
    }

    private void handleBrowseClick(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        Optional<Team> teamOpt = teamsIntegration.getPlayerTeam(player.getUniqueId());
        if (teamOpt.isEmpty()) { player.closeInventory(); return; }
        Team team = teamOpt.get();

        int page = TeamMarketGui.pageFromTitle(title);
        List<TeamMarketListing> listings = marketManager.getTeamListings(team.getId());

        switch (slot) {
            case TeamMarketGui.SLOT_PREV -> {
                if (page > 0) marketGui.open(player, page - 1);
            }
            case TeamMarketGui.SLOT_NEXT -> {
                int max = Math.max(0, (listings.size() - 1) / TeamMarketGui.ITEMS_PER_PAGE);
                if (page < max) marketGui.open(player, page + 1);
            }
            case TeamMarketGui.SLOT_LIST -> {
                if (!player.hasPermission("ezshops.teamshop.market")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to list items.");
                    return;
                }
                listGui.open(player);
            }
            case TeamMarketGui.SLOT_CLOSE -> player.closeInventory();
            default -> {
                // Listing slot clicked
                TeamMarketListing listing = TeamMarketGui.listingAt(listings, page, slot);
                if (listing == null) return;

                boolean isOwn = listing.sellerUuid().equals(player.getUniqueId());
                if (isOwn) {
                    // Cancel own listing
                    String err = marketManager.cancelListing(listing.listingId(), player);
                    if (err != null) {
                        player.sendMessage(ChatColor.RED + err);
                    } else {
                        player.sendMessage(ChatColor.GREEN + "Listing cancelled. Item returned to your inventory.");
                    }
                    marketGui.open(player, page);
                } else {
                    // Purchase
                    String err = marketManager.purchaseListing(listing.listingId(), player);
                    if (err != null) {
                        player.sendMessage(ChatColor.RED + err);
                    } else {
                        player.sendMessage(ChatColor.GREEN + "Purchase complete!");
                    }
                    marketGui.open(player, page);
                }
            }
        }
    }

    // ── List GUI ──────────────────────────────────────────────────────────────

    private void handleListGuiClick(InventoryClickEvent event, Player player) {
        int rawSlot = event.getRawSlot();

        // Player clicking from their own inventory (lower half) → item selection
        if (rawSlot >= 54) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            TeamMarketListGui.State state = listGui.getState(player);
            if (state == null) return;

            // Reject air / meta-only items
            state.selectedItem = clicked.clone();
            state.selectedItem.setAmount(1);
            // Cap quantity to how many the player actually holds
            int held = countMatching(player, clicked);
            state.quantity = Math.min(state.quantity, Math.max(1, held));
            listGui.redraw(player);
            return;
        }

        event.setCancelled(true);
        if (rawSlot < 0 || rawSlot >= 54) return;

        TeamMarketListGui.State state = listGui.getState(player);
        if (state == null) return;

        if (rawSlot == TeamMarketListGui.slotQtyMinus8())    { adjustQty(player, state, -8); return; }
        if (rawSlot == TeamMarketListGui.slotQtyMinus1())    { adjustQty(player, state, -1); return; }
        if (rawSlot == TeamMarketListGui.slotQtyPlus1())     { adjustQty(player, state,  1); return; }
        if (rawSlot == TeamMarketListGui.slotQtyPlus8())     { adjustQty(player, state,  8); return; }
        if (rawSlot == TeamMarketListGui.slotPriceMinus100()){ adjustPrice(player, state, -100); return; }
        if (rawSlot == TeamMarketListGui.slotPriceMinus10()) { adjustPrice(player, state, -10);  return; }
        if (rawSlot == TeamMarketListGui.slotPriceMinus1())  { adjustPrice(player, state,  -1);  return; }
        if (rawSlot == TeamMarketListGui.slotPricePlus1())   { adjustPrice(player, state,   1);  return; }
        if (rawSlot == TeamMarketListGui.slotPricePlus10())  { adjustPrice(player, state,  10);  return; }
        if (rawSlot == TeamMarketListGui.slotPricePlus100()) { adjustPrice(player, state, 100);  return; }

        if (rawSlot == TeamMarketListGui.slotBack()) {
            listGui.close(player);
            marketGui.open(player);
            return;
        }
        if (rawSlot == TeamMarketListGui.slotCancel()) {
            listGui.close(player);
            player.closeInventory();
            return;
        }
        if (rawSlot == TeamMarketListGui.slotConfirm()) {
            if (state.selectedItem == null) {
                player.sendMessage(ChatColor.RED + "Please select an item first.");
                return;
            }
            Optional<Team> teamOpt = teamsIntegration.getPlayerTeam(player.getUniqueId());
            if (teamOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "You are not in a team.");
                listGui.close(player);
                player.closeInventory();
                return;
            }
            Team team = teamOpt.get();

            TeamMarketListing listing = marketManager.addListing(
                    team.getId(), player, state.selectedItem, state.quantity, state.price);
            if (listing == null) {
                player.sendMessage(ChatColor.RED + "You do not have enough of that item in your inventory.");
                listGui.redraw(player); // refresh in case qty is now wrong
                return;
            }
            player.sendMessage(ChatColor.GREEN + "Listed " + listing.quantity() + "x "
                    + TeamMarketManager.friendlyName(listing.item())
                    + ChatColor.GREEN + " for $" + String.format("%.2f", listing.price()) + ".");
            listGui.close(player);
            marketGui.open(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getView().getTitle().equals(TeamMarketListGui.TITLE)) {
            // Skip cleanup when the close was triggered by our own redraw call
            if (!listGui.isRedrawing(player)) {
                listGui.close(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        listGui.close(event.getPlayer());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void adjustQty(Player player, TeamMarketListGui.State state, int delta) {
        int held = state.selectedItem != null ? countMatching(player, state.selectedItem) : 64;
        state.quantity = Math.max(1, Math.min(state.quantity + delta, Math.max(1, held)));
        listGui.redraw(player);
    }

    private void adjustPrice(Player player, TeamMarketListGui.State state, double delta) {
        state.price = Math.max(0.01, state.price + delta);
        listGui.redraw(player);
    }

    private static int countMatching(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && s.isSimilar(template)) count += s.getAmount();
        }
        return count;
    }
}
