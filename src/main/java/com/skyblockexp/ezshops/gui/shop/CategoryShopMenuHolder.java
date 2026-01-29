package com.skyblockexp.ezshops.gui.shop;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CategoryShopMenuHolder extends AbstractShopMenuHolder {

    private final ShopMenuLayout.Category category;
    private final List<Integer> itemSlots;
    private final int previousSlot;
    private final int nextSlot;
    private final Map<Integer, ShopMenuLayout.Item> slotEntries = new HashMap<>();
    private int page;

    public CategoryShopMenuHolder(UUID owner, ShopMenuLayout.Category category) {
        super(owner);
        this.category = category;

        int inventorySize = Math.max(0, category.menuSize());
        boolean preserveLastRow = category.preserveLastRow();
        if (inventorySize >= 18) {
            this.previousSlot = inventorySize - 9;
            this.nextSlot = inventorySize - 1;
        } else {
            this.previousSlot = -1;
            this.nextSlot = -1;
        }

        List<Integer> computedSlots = new ArrayList<>();
        Integer backSlot = category.backButtonSlot();
        int back = backSlot != null ? backSlot : -9999;
        for (int slot = 0; slot < inventorySize; slot++) {
            if (slot == previousSlot || slot == nextSlot || slot == back) {
                continue;
            }
            if (preserveLastRow && inventorySize >= 9 && slot >= inventorySize - 9) {
                // preserve entire last row for navigation/back/etc.
                continue;
            }
            computedSlots.add(slot);
        }
        if (computedSlots.isEmpty() && inventorySize > 0) {
            computedSlots.add(0);
        }
        this.itemSlots = List.copyOf(computedSlots);
    }

    public ShopMenuLayout.Category category() {
        return category;
    }

    public List<Integer> itemSlots() {
        return itemSlots;
    }

    public int previousSlot() {
        return previousSlot;
    }

    public int nextSlot() {
        return nextSlot;
    }

    public int itemsPerPage() {
        return Math.max(1, itemSlots.size());
    }

    public int totalPages() {
        int perPage = itemsPerPage();
        List<ShopMenuLayout.Item> items = category.items();
        if (perPage <= 0 || items.isEmpty()) {
            return 1;
        }
        int autoCount = 0;
        int maxDeclared = 1;
        for (ShopMenuLayout.Item it : items) {
            if (it == null) continue;
            if (it.page() > 0) {
                maxDeclared = Math.max(maxDeclared, it.page());
            } else {
                autoCount++;
            }
        }
        int autoPages = (autoCount + perPage - 1) / perPage;
        return Math.max(Math.max(1, autoPages), maxDeclared);
    }

    public int page() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public boolean hasNextPage() {
        return page + 1 < totalPages();
    }

    public void clearEntries() {
        slotEntries.clear();
    }

    public void setEntry(int slot, ShopMenuLayout.Item entry) {
        if (entry == null) {
            slotEntries.remove(slot);
        } else {
            slotEntries.put(slot, entry);
        }
    }

    public ShopMenuLayout.Item entryForSlot(int slot) {
        return slotEntries.get(slot);
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
