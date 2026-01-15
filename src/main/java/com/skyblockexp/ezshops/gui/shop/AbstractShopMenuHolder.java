package com.skyblockexp.ezshops.gui.shop;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class AbstractShopMenuHolder implements InventoryHolder {

    private final UUID owner;
    private Inventory inventory;

    protected AbstractShopMenuHolder(UUID owner) {
        this.owner = owner;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID owner() {
        return owner;
    }
}
