package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.shop.api.ShopPriceService;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Default implementation of {@link ShopPriceService} backed by {@link ShopPricingManager} data.
 */
public final class ShopPriceLookupService implements ShopPriceService {

    private final ShopPricingManager pricingManager;
    private final Logger logger;

    public ShopPriceLookupService(ShopPricingManager pricingManager, Logger logger) {
        this.pricingManager = pricingManager;
        this.logger = logger;
    }

    @Override
    public OptionalDouble findBuyPrice(ItemStack itemStack) {
        return lookupPrice(itemStack, true);
    }

    @Override
    public OptionalDouble findSellPrice(ItemStack itemStack) {
        return lookupPrice(itemStack, false);
    }

    private OptionalDouble lookupPrice(ItemStack itemStack, boolean buyPrice) {
        if (pricingManager == null || itemStack == null) {
            return OptionalDouble.empty();
        }

        Material material = itemStack.getType();
        if (material == null || material == Material.AIR) {
            return OptionalDouble.empty();
        }

        Optional<ShopPrice> priceOptional = pricingManager.getPrice(material);
        if (priceOptional.isEmpty()) {
            return OptionalDouble.empty();
        }

        ShopPrice price = priceOptional.get();
        double unitValue;
        if (buyPrice) {
            if (!price.canBuy()) {
                return OptionalDouble.empty();
            }
            unitValue = price.buyPrice();
        } else {
            if (!price.canSell()) {
                return OptionalDouble.empty();
            }
            unitValue = price.sellPrice();
        }

        int amount = Math.max(1, itemStack.getAmount());
        double totalValue = unitValue * amount;
        if (Double.isNaN(totalValue) || Double.isInfinite(totalValue)) {
            if (logger != null) {
                logger.fine("Ignoring invalid shop price for " + material + ": " + totalValue);
            }
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.max(0.0D, totalValue));
    }
}
