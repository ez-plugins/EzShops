package com.skyblockexp.ezshops.database.jaloquent.model;

/**
 * Simple POJO representing a row in the `ez_shop_transactions` table.
 */
public final class JaloquentTransactionModel {
    private int id;
    private long occurredAt;
    private String type;
    private String playerUuid;
    private String itemYaml;
    private int quantity;
    private double total;

    public JaloquentTransactionModel() {}

    public JaloquentTransactionModel(int id, long occurredAt, String type, String playerUuid, String itemYaml, int quantity, double total) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.type = type;
        this.playerUuid = playerUuid;
        this.itemYaml = itemYaml;
        this.quantity = quantity;
        this.total = total;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getOccurredAt() { return occurredAt; }
    public void setOccurredAt(long occurredAt) { this.occurredAt = occurredAt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public String getItemYaml() { return itemYaml; }
    public void setItemYaml(String itemYaml) { this.itemYaml = itemYaml; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
