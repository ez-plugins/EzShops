package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the {@code /sellhand} command, selling the item held by the player.
 */
public class SellHandCommand implements CommandExecutor {

    private final ShopTransactionService transactionService;
    private final ShopMessageConfiguration.CommandMessages.SellHandCommandMessages messages;

    public SellHandCommand(ShopTransactionService transactionService,
            ShopMessageConfiguration.CommandMessages.SellHandCommandMessages messages) {
        this.transactionService = transactionService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.playersOnly());
            return true;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR || handItem.getAmount() <= 0) {
            player.sendMessage(messages.mustHoldItem());
            return true;
        }

        ShopTransactionResult result = transactionService.sell(player, handItem.getType(), handItem.getAmount());
        player.sendMessage(result.message());
        return true;
    }
}
