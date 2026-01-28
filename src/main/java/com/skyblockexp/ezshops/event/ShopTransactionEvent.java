package com.skyblockexp.ezshops.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public abstract class ShopTransactionEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ItemStack item;
    private final int amount;
    private final double total;

    protected ShopTransactionEvent(Player player, ItemStack item, int amount, double total) {
        this.player = player;
        this.item = item;
        this.amount = amount;
        this.total = total;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    public double getTotal() {
        return total;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
