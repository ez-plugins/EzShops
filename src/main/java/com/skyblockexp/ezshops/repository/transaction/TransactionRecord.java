package com.skyblockexp.ezshops.repository.transaction;

import java.util.UUID;

public final class TransactionRecord {
    public enum Type { SALE, PURCHASE }

    private final long occurredAt;
    private final Type type;
    private final UUID player;
    private final String itemYaml;
    private final int quantity;
    private final double total;

    public TransactionRecord(long occurredAt, Type type, UUID player, String itemYaml, int quantity, double total) {
        this.occurredAt = occurredAt;
        this.type = type;
        this.player = player;
        this.itemYaml = itemYaml;
        this.quantity = quantity;
        this.total = total;
    }

    public long getOccurredAt() { return occurredAt; }
    public Type getType() { return type; }
    public UUID getPlayer() { return player; }
    public String getItemYaml() { return itemYaml; }
    public int getQuantity() { return quantity; }
    public double getTotal() { return total; }
}
