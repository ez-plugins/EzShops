package com.skyblockexp.ezshops.teams.command;

import com.skyblockexp.ezshops.gui.teams.TeamDashboardGui;
import com.skyblockexp.ezshops.gui.teams.TeamMarketGui;
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
 *
 * <p>Subcommands:
 * <ul>
 *   <li>(none)     – opens the team shop dashboard</li>
 *   <li>treasury   – opens the treasury GUI</li>
 *   <li>stocks     – opens the shared-stock GUI</li>
 *   <li>market     – opens the team P2P market GUI (core feature)</li>
 * </ul>
 */
public class TeamShopCommand implements CommandExecutor, TabCompleter {

    private final TeamDashboardGui dashboardGui;
    private final TeamTreasuryGui  treasuryGui;
    private final TeamStockGui     stockGui;
    private final TeamMarketGui    marketGui;

    public TeamShopCommand(TeamDashboardGui dashboardGui,
                           TeamTreasuryGui  treasuryGui,
                           TeamStockGui     stockGui,
                           TeamMarketGui    marketGui) {
        this.dashboardGui = dashboardGui;
        this.treasuryGui  = treasuryGui;
        this.stockGui     = stockGui;
        this.marketGui    = marketGui;
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
            case "treasury" -> treasuryGui.open(player);
            case "stocks"   -> stockGui.open(player);
            case "market"   -> {
                if (!player.hasPermission("ezshops.teamshop.market")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to access the team market.");
                    return true;
                }
                marketGui.open(player);
            }
            default -> player.sendMessage(ChatColor.RED + "Usage: /teamshop [treasury|stocks|market]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String current = args[0].toLowerCase();
            return Arrays.asList("treasury", "stocks", "market").stream()
                    .filter(s -> s.startsWith(current))
                    .toList();
        }
        return Collections.emptyList();
    }
}
