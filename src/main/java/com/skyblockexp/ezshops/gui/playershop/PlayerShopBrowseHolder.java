package com.skyblockexp.ezshops.gui.playershop;

import com.skyblockexp.ezshops.playershop.PlayerShop;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * InventoryHolder that tags the browse-all-player-shops GUI.
 */
public final class PlayerShopBrowseHolder implements InventoryHolder {

    private final int page;
    private final int totalPages;
    private final List<PlayerShop> pageShops;
    private Inventory inventory;

    public PlayerShopBrowseHolder(int page, int totalPages, List<PlayerShop> pageShops) {
        this.page = page;
        this.totalPages = totalPages;
        this.pageShops = List.copyOf(pageShops);
    }

    public int page() {
        return page;
    }

    public int totalPages() {
        return totalPages;
    }

    public List<PlayerShop> pageShops() {
        return pageShops;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
