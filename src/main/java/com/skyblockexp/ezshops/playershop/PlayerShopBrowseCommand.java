package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.gui.playershop.PlayerShopBrowseMenu;
import com.skyblockexp.ezshops.gui.playershop.PlayerShopBrowseMessages;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the {@code /playershops} command, opening the browse-all GUI.
 */
public final class PlayerShopBrowseCommand implements CommandExecutor {

    public static final String PERMISSION = "ezshops.playershop.browse";

    private final PlayerShopBrowseMenu menu;
    private final PlayerShopBrowseMessages messages;

    public PlayerShopBrowseCommand(PlayerShopBrowseMenu menu, PlayerShopBrowseMessages messages) {
        this.menu = menu;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.playersOnly()));
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.noPermission()));
            return true;
        }
        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        menu.open(player, page);
        return true;
    }
}
