package com.skyblockexp.ezshops.gui.shop;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.UUID;

public final class CategoryShopMenuHolder extends AbstractShopMenuHolder {

    private final ShopMenuLayout.Category category;

    public CategoryShopMenuHolder(UUID owner, ShopMenuLayout.Category category) {
        super(owner);
        this.category = category;
    }

    public ShopMenuLayout.Category category() {
        return category;
    }

    public ShopMenuLayout.Item findItem(String id) {
        for (ShopMenuLayout.Item item : category.items()) {
            if (item.id().equalsIgnoreCase(id)) {
                return item;
            }
        }
        return null;
    }
}
