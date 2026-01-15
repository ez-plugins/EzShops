package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the {@code /sellinventory} command, selling all sellable items in the player's inventory.
 */
public class SellInventoryCommand implements CommandExecutor {

    private final ShopTransactionService transactionService;
    private final ShopMessageConfiguration.CommandMessages.SellInventoryCommandMessages messages;

    public SellInventoryCommand(ShopTransactionService transactionService,
            ShopMessageConfiguration.CommandMessages.SellInventoryCommandMessages messages) {
        this.transactionService = transactionService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.playersOnly());
            return true;
        }

        ShopTransactionResult result = transactionService.sellInventory(player);
        player.sendMessage(result.message());
        return true;
    }
}
