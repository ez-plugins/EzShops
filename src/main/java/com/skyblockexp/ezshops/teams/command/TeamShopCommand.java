package com.skyblockexp.ezshops.teams.command;

import com.skyblockexp.ezshops.gui.teams.TeamDashboardGui;
import com.skyblockexp.ezshops.gui.teams.TeamStockGui;
import com.skyblockexp.ezshops.gui.teams.TeamTreasuryGui;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles the {@code /teamshop} command.
 * Subcommands: (none) = dashboard, treasury, stocks
 */
public class TeamShopCommand implements CommandExecutor, TabCompleter {

    private final TeamDashboardGui dashboardGui;
    private final TeamTreasuryGui treasuryGui;
    private final TeamStockGui stockGui;

    public TeamShopCommand(TeamDashboardGui dashboardGui, TeamTreasuryGui treasuryGui, TeamStockGui stockGui) {
        this.dashboardGui = dashboardGui;
        this.treasuryGui = treasuryGui;
        this.stockGui = stockGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("ezshops.teamshop")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use team shop features.");
            return true;
        }

        if (args.length == 0) {
            dashboardGui.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "treasury" -> {
                if (!player.hasPermission("ezshops.teamshop")) {
                    player.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                treasuryGui.open(player);
            }
            case "stocks" -> stockGui.open(player);
            default -> player.sendMessage(ChatColor.RED + "Usage: /teamshop [treasury|stocks]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            return Arrays.asList("treasury", "stocks").stream()
                    .filter(s -> s.startsWith(current))
                    .toList();
        }
        return Collections.emptyList();
    }
}
