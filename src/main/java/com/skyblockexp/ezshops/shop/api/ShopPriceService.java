package com.skyblockexp.ezshops.shop.api;

import java.util.OptionalDouble;
import org.bukkit.inventory.ItemStack;

/**
 * Exposes EzShops pricing information for other plugins.
 */
public interface ShopPriceService {

    /**
     * Looks up the total buy price for the provided item stack.
     *
     * @param itemStack the item stack to price
     * @return the total buy price for the stack, or {@link OptionalDouble#empty()} when unavailable
     */
    OptionalDouble findBuyPrice(ItemStack itemStack);

    /**
     * Looks up the total sell price for the provided item stack.
     *
     * @param itemStack the item stack to price
     * @return the total sell price for the stack, or {@link OptionalDouble#empty()} when unavailable
     */
    OptionalDouble findSellPrice(ItemStack itemStack);
}
