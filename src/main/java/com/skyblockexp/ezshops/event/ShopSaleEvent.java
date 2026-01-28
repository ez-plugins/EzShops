package com.skyblockexp.ezshops.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopSaleEvent extends ShopTransactionEvent {
    public ShopSaleEvent(Player player, ItemStack item, int amount, double total) {
        super(player, item, amount, total);
    }
}
