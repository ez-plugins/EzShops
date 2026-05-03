package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.gui.quicksell.QuickSellMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the {@code /sell} command, opening the Quick Sell GUI for the player.
 */
public class SellCommand implements CommandExecutor {

    private final QuickSellMenu quickSellMenu;
    private final ShopMessageConfiguration.CommandMessages.SellCommandMessages messages;
    private final boolean enabled;

    public SellCommand(QuickSellMenu quickSellMenu,
            ShopMessageConfiguration.CommandMessages.SellCommandMessages messages,
            boolean enabled) {
        this.quickSellMenu = quickSellMenu;
        this.messages = messages;
        this.enabled = enabled;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.playersOnly());
            return true;
        }

        if (!enabled) {
            player.sendMessage(messages.guiDisabled());
            return true;
        }

        quickSellMenu.open(player);
        return true;
    }
}
