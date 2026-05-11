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
 * Handles click events inside all team shop GUIs.
 */
public class TeamGuiListener implements Listener {

    private static final double DEFAULT_AMOUNT = 100.0;

    private final TeamsIntegration teamsIntegration;
    private final TeamTreasury teamTreasury;
    private final TeamDashboardGui dashboardGui;
    private final TeamTreasuryGui treasuryGui;
    private final Economy economy;

    public TeamGuiListener(TeamsIntegration teamsIntegration,
                           TeamTreasury teamTreasury,
                           TeamDashboardGui dashboardGui,
                           TeamTreasuryGui treasuryGui,
                           Economy economy) {
        this.teamsIntegration = teamsIntegration;
        this.teamTreasury = teamTreasury;
        this.dashboardGui = dashboardGui;
        this.treasuryGui = treasuryGui;
        this.economy = economy;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        String title = inv.getType().getDefaultTitle();
        if (event.getView() != null) {
            title = event.getView().getTitle();
        }

        if (title.startsWith(TeamDashboardGui.TITLE_PREFIX)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Material clicked = event.getCurrentItem().getType();
            switch (event.getRawSlot()) {
                case 11 -> treasuryGui.open(player); // Treasury
                case 22 -> player.closeInventory();   // Close
                default -> { /* no-op */ }
            }
            return;
        }

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
                if (!player.hasPermission("ezshops.teamshop.treasury.withdraw")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to deposit.");
                    return;
                }
                if (economy.getBalance(player) < DEFAULT_AMOUNT) {
                    player.sendMessage(ChatColor.RED + "You do not have enough money to deposit.");
                    return;
                }
                teamTreasury.deposit(team.getId(), player, DEFAULT_AMOUNT);
                player.sendMessage(ChatColor.GREEN + "Deposited $" + String.format("%.0f", DEFAULT_AMOUNT) + " into team treasury.");
                treasuryGui.open(player); // refresh
            } else if (slot == 15) { // Withdraw
                TeamRole role = teamsIntegration.getMemberRole(team.getId(), player.getUniqueId())
                        .orElse(TeamRole.MEMBER);
                if (!role.canManage(TeamRole.MEMBER)) {
                    player.sendMessage(ChatColor.RED + "Only admins and owners can withdraw.");
                    return;
                }
                if (!player.hasPermission("ezshops.teamshop.treasury.withdraw")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to withdraw.");
                    return;
                }
                double balance = teamTreasury.getBalance(team.getId());
                if (balance < DEFAULT_AMOUNT) {
                    player.sendMessage(ChatColor.RED + "Team treasury balance is too low.");
                    return;
                }
                teamTreasury.withdraw(team.getId(), player, DEFAULT_AMOUNT);
                player.sendMessage(ChatColor.GREEN + "Withdrew $" + String.format("%.0f", DEFAULT_AMOUNT) + " from team treasury.");
                treasuryGui.open(player); // refresh
            } else if (slot == 22) {
                player.closeInventory();
            }
        }
    }
}
