package com.skyblockexp.ezshops.model;

import java.time.Instant;

public class ShopTransaction {

    public enum Type { SALE, PURCHASE }

    private final Long id;
    private final Instant occurredAt;
    private final Type type;
    private final String playerUuid;
    private final String itemYaml;
    private final int quantity;
    private final double total;

    public ShopTransaction(Long id, Instant occurredAt, Type type, String playerUuid, String itemYaml, int quantity, double total) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.type = type;
        this.playerUuid = playerUuid;
        this.itemYaml = itemYaml;
        this.quantity = quantity;
        this.total = total;
    }

    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public Type getType() { return type; }
    public String getPlayerUuid() { return playerUuid; }
    public String getItemYaml() { return itemYaml; }
    public int getQuantity() { return quantity; }
    public double getTotal() { return total; }
}
