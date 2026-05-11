package com.skyblockexp.ezshops.teams;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Immutable snapshot of a single team market listing.
 *
 * @param listingId  unique ID for this listing
 * @param teamId     the team this listing belongs to
 * @param sellerUuid UUID of the player who listed the item
 * @param item       the offered ItemStack (quantity = qty per purchase)
 * @param quantity   number of items per sale (mirrors item.getAmount())
 * @param price      price in Vault currency units
 * @param listedAt   System.currentTimeMillis() when the listing was created
 */
public record TeamMarketListing(
        UUID listingId,
        UUID teamId,
        UUID sellerUuid,
        ItemStack item,
        int quantity,
        double price,
        long listedAt
) {
    public TeamMarketListing {
        if (item == null) throw new IllegalArgumentException("item must not be null");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        if (price < 0) throw new IllegalArgumentException("price must be >= 0");
    }
}
