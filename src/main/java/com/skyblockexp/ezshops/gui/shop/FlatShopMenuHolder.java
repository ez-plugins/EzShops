package com.skyblockexp.ezshops.gui.shop;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inventory holder used when the shop is displayed as a flat list of items.
 */
public final class FlatShopMenuHolder extends AbstractShopMenuHolder {

    private final List<FlatMenuEntry> entries;
    private final List<Integer> itemSlots;
    private final int previousSlot;
    private final int nextSlot;
    private final Map<Integer, FlatMenuEntry> slotEntries = new HashMap<>();
    private int page;

    public FlatShopMenuHolder(UUID owner, List<FlatMenuEntry> entries, int inventorySize) {
        super(owner);
        this.entries = List.copyOf(entries);
        if (inventorySize >= 18) {
            this.previousSlot = inventorySize - 9;
            this.nextSlot = inventorySize - 1;
        } else {
            this.previousSlot = -1;
            this.nextSlot = -1;
        }

        List<Integer> computedSlots = new ArrayList<>();
        for (int slot = 0; slot < inventorySize; slot++) {
            if (slot == previousSlot || slot == nextSlot) {
                continue;
            }
            computedSlots.add(slot);
        }
        if (computedSlots.isEmpty() && inventorySize > 0) {
            computedSlots.add(0);
        }
        this.itemSlots = List.copyOf(computedSlots);
    }

    public List<FlatMenuEntry> entries() {
        return entries;
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
        if (perPage <= 0 || entries.isEmpty()) {
            return 1;
        }
        return (entries.size() + perPage - 1) / perPage;
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

    public void setEntry(int slot, FlatMenuEntry entry) {
        if (entry == null) {
            slotEntries.remove(slot);
        } else {
            slotEntries.put(slot, entry);
        }
    }

    public FlatMenuEntry entryForSlot(int slot) {
        return slotEntries.get(slot);
    }

    public record FlatMenuEntry(ShopMenuLayout.Category category, ShopMenuLayout.Item item) {
    }
}
