package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamsIntegration;
import com.skyblockexp.ezshops.teams.TeamTreasury;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamRole;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Optional;

/**
 * Handles click events inside all team shop GUIs (dashboard and treasury).
 * Market GUI clicks are delegated to {@link TeamMarketGuiListener}.
 */
public class TeamGuiListener implements Listener {

    private static final double DEFAULT_AMOUNT = 100.0;

    private final TeamsIntegration teamsIntegration;
    private final TeamTreasury teamTreasury;
    private final TeamDashboardGui dashboardGui;
    private final TeamTreasuryGui treasuryGui;
    private final TeamStockGui stockGui;
    private final TeamMarketGui marketGui;
    private final Economy economy;

    public TeamGuiListener(TeamsIntegration teamsIntegration,
                           TeamTreasury teamTreasury,
                           TeamDashboardGui dashboardGui,
                           TeamTreasuryGui treasuryGui,
                           TeamStockGui stockGui,
                           TeamMarketGui marketGui,
                           Economy economy) {
        this.teamsIntegration = teamsIntegration;
        this.teamTreasury = teamTreasury;
        this.dashboardGui = dashboardGui;
        this.treasuryGui = treasuryGui;
        this.stockGui = stockGui;
        this.marketGui = marketGui;
        this.economy = economy;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // ── Dashboard ──────────────────────────────────────────────────────
        if (title.startsWith(TeamDashboardGui.TITLE_PREFIX)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            switch (event.getRawSlot()) {
                case TeamDashboardGui.SLOT_TREASURY -> treasuryGui.open(player);
                case TeamDashboardGui.SLOT_MARKET   -> {
                    if (!player.hasPermission("ezshops.teamshop.market")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to access the team market.");
                        return;
                    }
                    marketGui.open(player);
                }
                case TeamDashboardGui.SLOT_STOCKS   -> stockGui.open(player);
                case TeamDashboardGui.SLOT_CLOSE    -> player.closeInventory();
                default -> { /* info/bonus slots — no action */ }
            }
            return;
        }

        // ── Treasury ───────────────────────────────────────────────────────
        String treasuryTitle = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Team Treasury";
        if (title.equals(treasuryTitle)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            int slot = event.getRawSlot();
            Optional<Team> teamOpt = teamsIntegration.getPlayerTeam(player.getUniqueId());
            if (teamOpt.isEmpty()) {
                player.closeInventory();
                return;
            }
            Team team = teamOpt.get();

            if (slot == 11) { // Deposit
                if (economy.getBalance(player) < DEFAULT_AMOUNT) {
                    player.sendMessage(ChatColor.RED + "You do not have enough money to deposit.");
                    return;
                }
                if (teamTreasury.deposit(team.getId(), player, DEFAULT_AMOUNT)) {
                    player.sendMessage(ChatColor.GREEN + "Deposited $" + String.format("%.0f", DEFAULT_AMOUNT) + " into team treasury.");
                } else {
                    player.sendMessage(ChatColor.RED + "Deposit failed.");
                }
                treasuryGui.open(player);
            } else if (slot == 15) { // Withdraw
                TeamRole role = teamsIntegration.getMemberRole(team.getId(), player.getUniqueId())
                        .orElse(TeamRole.MEMBER);
                if (!player.hasPermission("ezshops.teamshop.treasury.withdraw")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to withdraw.");
                    return;
                }
                if (teamTreasury.withdraw(team.getId(), player, DEFAULT_AMOUNT)) {
                    player.sendMessage(ChatColor.GREEN + "Withdrew $" + String.format("%.0f", DEFAULT_AMOUNT) + " from team treasury.");
                } else {
                    player.sendMessage(ChatColor.RED + "Treasury balance is too low.");
                }
                treasuryGui.open(player);
            } else if (slot == 22) {
                player.closeInventory();
            }
        }
    }
}
