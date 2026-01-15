package com.skyblockexp.ezshops.playershop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Stores the desired quantity and price a player wants to use for their next shop.
 */
public record PlayerShopSetup(int quantity, double price, ItemStack itemTemplate) {

    public PlayerShopSetup {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (price <= 0.0d) {
            throw new IllegalArgumentException("price must be greater than zero");
        }
        itemTemplate = sanitizeTemplate(itemTemplate);
    }

    @Override
    public ItemStack itemTemplate() {
        return itemTemplate == null ? null : itemTemplate.clone();
    }

    private ItemStack sanitizeTemplate(ItemStack template) {
        if (template == null || template.getType() == Material.AIR) {
            return null;
        }
        ItemStack clone = template.clone();
        clone.setAmount(Math.max(1, clone.getAmount()));
        return clone;
    }
}
