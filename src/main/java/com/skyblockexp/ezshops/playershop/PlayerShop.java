package com.skyblockexp.ezshops.playershop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a single player-created chest shop backed by a sign.
 */
public final class PlayerShop {

    private final UUID ownerId;
    private final Location signLocation;
    private final Location primaryChestLocation;
    private final List<Location> chestLocations;
    private final ItemStack itemTemplate;
    private final int quantityPerSale;
    private final double price;

    public PlayerShop(UUID ownerId, Location signLocation, Location primaryChestLocation,
            List<Location> chestLocations, ItemStack itemTemplate, int quantityPerSale, double price) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.signLocation = Objects.requireNonNull(signLocation, "signLocation").clone();
        this.primaryChestLocation = Objects.requireNonNull(primaryChestLocation, "primaryChestLocation").clone();
        this.chestLocations = Collections.unmodifiableList(new ArrayList<>(Objects
                .requireNonNull(chestLocations, "chestLocations")));
        this.itemTemplate = Objects.requireNonNull(itemTemplate, "itemTemplate").clone();
        this.itemTemplate.setAmount(Math.max(1, this.itemTemplate.getAmount()));
        this.quantityPerSale = quantityPerSale;
        this.price = price;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public Location signLocation() {
        return signLocation.clone();
    }

    public Location primaryChestLocation() {
        return primaryChestLocation.clone();
    }

    public List<Location> chestLocations() {
        List<Location> copies = new ArrayList<>(chestLocations.size());
        for (Location location : chestLocations) {
            copies.add(location.clone());
        }
        return Collections.unmodifiableList(copies);
    }

    public ItemStack itemTemplate() {
        return itemTemplate.clone();
    }

    public int quantityPerSale() {
        return quantityPerSale;
    }

    public double price() {
        return price;
    }
}
