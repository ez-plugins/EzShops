package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.List;
import java.util.Objects;

/**
 * Parsed category template before active rotation option is applied.
 */
public final class ShopPricingCategoryTemplate {

    private final String id;
    private final String displayName;
    private final ShopMenuLayout.ItemDecoration icon;
    private final int slot;
    private final String menuTitle;
    private final int menuSize;
    private final ShopMenuLayout.ItemDecoration menuFill;
    private final List<ShopMenuLayout.ConfigurableButton> buttons;
    private final boolean preserveLastRow;
    private final List<ShopMenuLayout.Item> staticItems;
    private final ShopPricingRotationBinding rotation;
    private final String command;

    public ShopPricingCategoryTemplate(String id, String displayName, ShopMenuLayout.ItemDecoration icon, int slot,
            String menuTitle, int menuSize, ShopMenuLayout.ItemDecoration menuFill,
            List<ShopMenuLayout.ConfigurableButton> buttons, boolean preserveLastRow,
            List<ShopMenuLayout.Item> staticItems, ShopPricingRotationBinding rotation, String command) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.icon = icon;
        this.slot = slot;
        this.menuTitle = Objects.requireNonNull(menuTitle, "menuTitle");
        this.menuSize = menuSize;
        this.menuFill = menuFill;
        this.buttons = buttons == null ? List.of() : List.copyOf(buttons);
        this.preserveLastRow = preserveLastRow;
        this.staticItems = staticItems == null ? List.of() : List.copyOf(staticItems);
        this.rotation = rotation;
        this.command = command;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public ShopMenuLayout.ItemDecoration icon() {
        return icon;
    }

    public int slot() {
        return slot;
    }

    public String menuTitle() {
        return menuTitle;
    }

    public int menuSize() {
        return menuSize;
    }

    public ShopMenuLayout.ItemDecoration menuFill() {
        return menuFill;
    }

    public List<ShopMenuLayout.ConfigurableButton> buttons() {
        return buttons;
    }

    public boolean preserveLastRow() {
        return preserveLastRow;
    }

    public List<ShopMenuLayout.Item> staticItems() {
        return staticItems;
    }

    public ShopPricingRotationBinding rotation() {
        return rotation;
    }

    public String command() {
        return command;
    }

    public boolean isRotating() {
        return rotation != null;
    }
}
