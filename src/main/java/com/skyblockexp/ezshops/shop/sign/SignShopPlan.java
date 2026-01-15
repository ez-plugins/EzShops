package com.skyblockexp.ezshops.shop.sign;

import com.skyblockexp.ezshops.shop.ShopSignListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

/**
 * Represents the desired layout when automatically generating a collection of shop signs.
 */
public record SignShopPlan(List<ItemStack> items, Material backgroundBlock, Material signMaterial, int spacing,
        int rows, int rowSpacing, ShopSignListener.SignAction action, LayoutDirection direction) {

    public static final Material DEFAULT_BACKGROUND = Material.SMOOTH_STONE;
    private static final Material DEFAULT_SIGN_MATERIAL = Material.OAK_WALL_SIGN;

    public SignShopPlan {
        List<ItemStack> sanitizedItems = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                ItemStack clone = item.clone();
                clone.setAmount(Math.max(1, Math.min(64, clone.getAmount())));
                sanitizedItems.add(clone);
            }
        }
        items = Collections.unmodifiableList(sanitizedItems);
        backgroundBlock = sanitizeBackground(backgroundBlock);
        signMaterial = sanitizeSignMaterial(signMaterial);
        spacing = Math.max(0, spacing);
        rows = Math.max(1, rows);
        rowSpacing = Math.max(0, rowSpacing);
        action = action == null ? ShopSignListener.SignAction.BUY : action;
        direction = direction == null ? LayoutDirection.RIGHT : direction;
    }

    private Material sanitizeBackground(Material background) {
        if (background == null) {
            return null;
        }
        Material resolved = Objects.requireNonNull(background);
        return resolved.isBlock() ? resolved : DEFAULT_BACKGROUND;
    }

    private Material sanitizeSignMaterial(Material material) {
        Material resolved = material == null ? DEFAULT_SIGN_MATERIAL : material;
        if (!Tag.WALL_SIGNS.isTagged(resolved)) {
            return DEFAULT_SIGN_MATERIAL;
        }
        return resolved;
    }

    /**
     * Returns {@code true} when the plan does not contain any sign entries to generate.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Describes how the sign layout expands relative to the first backing block.
     */
    public enum LayoutDirection {
        LEFT,
        RIGHT
    }
}
