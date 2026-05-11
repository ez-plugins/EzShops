package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamStockManager;
import com.skyblockexp.ezshops.teams.TeamsIntegration;
import com.skyblockexp.teamsapi.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Team shared-stock overview GUI (54 slots).
 * Shows which product IDs the team currently holds stock in.
 */
public class TeamStockGui {

    private static final String TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Team Stocks";

    private final TeamsIntegration teamsIntegration;
    private final TeamStockManager teamStockManager;

    public TeamStockGui(TeamsIntegration teamsIntegration, TeamStockManager teamStockManager) {
        this.teamsIntegration = teamsIntegration;
        this.teamStockManager = teamStockManager;
    }

    public void open(Player player) {
        Optional<Team> teamOpt = teamsIntegration.getPlayerTeam(player.getUniqueId());
        if (teamOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }
        Team team = teamOpt.get();

        List<String> ownedStocks = teamStockManager.getTeamOwnedStocks(team.getId());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE + " - " + team.getName());

        int slot = 0;
        for (String productId : ownedStocks) {
            if (slot >= 45) break; // reserve last row for controls
            int amount = teamStockManager.getTeamStockAmount(team.getId(), productId);
            Material mat = parseMaterial(productId);
            inv.setItem(slot++, TeamDashboardGui.buildItem(mat,
                    ChatColor.YELLOW + productId,
                    ChatColor.GRAY + "Team stock: " + ChatColor.GOLD + amount));
        }

        if (ownedStocks.isEmpty()) {
            inv.setItem(22, TeamDashboardGui.buildItem(Material.BARRIER,
                    ChatColor.RED + "No team stocks yet.",
                    ChatColor.GRAY + "Team stocks are accumulated when members sell items."));
        }

        // Close button
        inv.setItem(49, TeamDashboardGui.buildItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(inv);
    }

    private Material parseMaterial(String productId) {
        try {
            return Material.valueOf(productId.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.CHEST;
        }
    }
}
