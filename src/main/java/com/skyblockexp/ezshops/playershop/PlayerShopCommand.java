package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.playershop.PlayerShopMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the player shop setup menu so players can configure their next sign.
 */
public final class PlayerShopCommand implements CommandExecutor {

    private final PlayerShopManager manager;
    private final PlayerShopSetupMenu menu;
    private final PlayerShopMessages messages;

    public PlayerShopCommand(PlayerShopManager manager, PlayerShopSetupMenu menu, PlayerShopMessages messages) {
        this.manager = manager;
        this.menu = menu;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.commandPlayersOnly());
            return true;
        }
        if (!player.hasPermission(PlayerShopManager.PERMISSION_CREATE)) {
            player.sendMessage(messages.noPermissionCreate());
            return true;
        }
        if (manager == null || menu == null) {
            player.sendMessage(messages.commandDisabled());
            return true;
        }
        menu.open(player);
        return true;
    }
}
