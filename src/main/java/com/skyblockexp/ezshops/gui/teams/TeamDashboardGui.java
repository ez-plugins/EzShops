package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamsIntegration;
import com.skyblockexp.ezshops.teams.TeamTreasury;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Optional;

/**
 * Main team shop dashboard GUI.
 * Slot layout (27 slots, 3 rows):
 *   4  = Team info
 *  11  = Treasury
 *  13  = Sell multiplier info
 *  15  = Buy discount info
 *  22  = Close
 */
public class TeamDashboardGui {

    static final String TITLE_PREFIX = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Team Shop";

    private final TeamsIntegration teamsIntegration;
    private final TeamTreasury teamTreasury;

    public TeamDashboardGui(TeamsIntegration teamsIntegration, TeamTreasury teamTreasury) {
        this.teamsIntegration = teamsIntegration;
        this.teamTreasury = teamTreasury;
    }

    public void open(Player player) {
        Optional<Team> teamOpt = teamsIntegration.getPlayerTeam(player.getUniqueId());
        if (teamOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }
        Team team = teamOpt.get();

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + " \u2014 " + team.getName());

        // Team info (slot 4)
        TeamRole role = teamsIntegration.getMemberRole(team.getId(), player.getUniqueId())
                .orElse(TeamRole.MEMBER);
        double balance = teamTreasury.getBalance(team.getId());
        inv.setItem(4, buildItem(Material.BEACON,
                ChatColor.GOLD + "" + ChatColor.BOLD + team.getName(),
                ChatColor.GRAY + "Your role: " + ChatColor.YELLOW + role.name(),
                ChatColor.GRAY + "Treasury: " + ChatColor.GREEN + String.format("$%.2f", balance)));

        // Treasury (slot 11)
        inv.setItem(11, buildItem(Material.CHEST,
                ChatColor.YELLOW + "Team Treasury",
                ChatColor.GRAY + "Balance: " + ChatColor.GREEN + String.format("$%.2f", balance),
                ChatColor.GRAY + "Click to manage treasury deposits/withdrawals."));

        // Sell multiplier (slot 13)
        double sellMult = teamsIntegration.getSellMultiplier(player);
        inv.setItem(13, buildItem(Material.GOLD_INGOT,
                ChatColor.GREEN + "Sell Bonus",
                ChatColor.GRAY + "Your current multiplier: " + ChatColor.YELLOW + String.format("x%.2f", sellMult),
                teamsIntegration.describePerk(player)));

        // Buy discount (slot 15)
        double buyMult = teamsIntegration.getBuyMultiplier(player);
        inv.setItem(15, buildItem(Material.EMERALD,
                ChatColor.AQUA + "Buy Discount",
                ChatColor.GRAY + "Your current multiplier: " + ChatColor.YELLOW + String.format("x%.2f", buyMult)));

        // Close (slot 22)
        inv.setItem(22, buildItem(Material.BARRIER, ChatColor.RED + "Close"));

        // Filler
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    static ItemStack buildItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
