package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamsIntegration;
import com.skyblockexp.ezshops.teams.TeamTreasury;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamRole;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Optional;

/**
 * Treasury management GUI (27 slots).
 * Slot 11 = Deposit 100 / Slot 13 = Balance info / Slot 15 = Withdraw 100 (admin+)
 */
public class TeamTreasuryGui {

    private static final String TITLE = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Team Treasury";
    private static final double DEFAULT_AMOUNT = 100.0;

    private final TeamsIntegration teamsIntegration;
    private final TeamTreasury teamTreasury;
    private final Economy economy;

    public TeamTreasuryGui(TeamsIntegration teamsIntegration, TeamTreasury teamTreasury, Economy economy) {
        this.teamsIntegration = teamsIntegration;
        this.teamTreasury = teamTreasury;
        this.economy = economy;
    }

    public void open(Player player) {
        Optional<Team> teamOpt = teamsIntegration.getPlayerTeam(player.getUniqueId());
        if (teamOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }
        Team team = teamOpt.get();
        double balance = teamTreasury.getBalance(team.getId());
        TeamRole role = teamsIntegration.getMemberRole(team.getId(), player.getUniqueId())
                .orElse(TeamRole.MEMBER);

        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Balance info
        inv.setItem(13, TeamDashboardGui.buildItem(Material.GOLD_BLOCK,
                ChatColor.YELLOW + "Treasury Balance",
                ChatColor.GRAY + "Balance: " + ChatColor.GREEN + String.format("$%.2f", balance),
                ChatColor.GRAY + "Your balance: " + ChatColor.YELLOW + String.format("$%.2f", economy.getBalance(player))));

        // Deposit
        inv.setItem(11, TeamDashboardGui.buildItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Deposit " + String.format("$%.0f", DEFAULT_AMOUNT),
                ChatColor.GRAY + "Click to deposit " + String.format("$%.0f", DEFAULT_AMOUNT) + " into the team treasury."));

        // Withdraw (requires ADMIN role or higher)
        if (role.canManage(TeamRole.MEMBER)) {
            inv.setItem(15, TeamDashboardGui.buildItem(Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "Withdraw " + String.format("$%.0f", DEFAULT_AMOUNT),
                    ChatColor.GRAY + "Click to withdraw " + String.format("$%.0f", DEFAULT_AMOUNT) + " from the treasury.",
                    ChatColor.GRAY + "(Requires ADMIN or OWNER role)"));
        }

        // Back / Close
        inv.setItem(22, TeamDashboardGui.buildItem(Material.BARRIER, ChatColor.RED + "Close"));

        // Filler
        org.bukkit.inventory.ItemStack filler = TeamDashboardGui.buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }
}
