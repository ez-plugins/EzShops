package com.skyblockexp.ezshops.gui.shop;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.UUID;

public final class QuantityShopMenuHolder extends AbstractShopMenuHolder {

    private final ShopMenuLayout.Category category;
    private final ShopMenuLayout.Item item;
    private final ShopTransactionType type;

    public QuantityShopMenuHolder(UUID owner, ShopMenuLayout.Category category, ShopMenuLayout.Item item,
            ShopTransactionType type) {
        super(owner);
        this.category = category;
        this.item = item;
        this.type = type;
    }

    public ShopMenuLayout.Category category() {
        return category;
    }

    public ShopMenuLayout.Item item() {
        return item;
    }

    public ShopTransactionType type() {
        return type;
    }
}
