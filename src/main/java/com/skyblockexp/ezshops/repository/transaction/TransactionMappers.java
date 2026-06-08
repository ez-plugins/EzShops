package com.skyblockexp.ezshops.repository.transaction;

import com.skyblockexp.ezshops.model.ShopTransaction;
import java.util.UUID;

/**
 * Simple mapping helpers between persistence model and API model.
 */
public final class TransactionMappers {

    private TransactionMappers() {}

    public static ShopTransaction toModel(TransactionRecord rec) {
        ShopTransaction.Type t = rec.getType() == TransactionRecord.Type.SALE ? ShopTransaction.Type.SALE : ShopTransaction.Type.PURCHASE;
        String player = rec.getPlayer() != null ? rec.getPlayer().toString() : null;
        java.time.Instant at = java.time.Instant.ofEpochMilli(rec.getOccurredAt());
        return new ShopTransaction(null, at, t, player, rec.getItemYaml(), rec.getQuantity(), rec.getTotal());
    }

    public static TransactionRecord fromModel(ShopTransaction model) {
        java.util.UUID player = null;
        if (model.getPlayerUuid() != null && !model.getPlayerUuid().isBlank()) {
            try { player = java.util.UUID.fromString(model.getPlayerUuid()); } catch (IllegalArgumentException ignored) {}
        }
        TransactionRecord.Type t = model.getType() == ShopTransaction.Type.SALE ? TransactionRecord.Type.SALE : TransactionRecord.Type.PURCHASE;
        long occurred = model.getOccurredAt() != null ? model.getOccurredAt().toEpochMilli() : System.currentTimeMillis();
        return new TransactionRecord(occurred, t, player, model.getItemYaml(), model.getQuantity(), model.getTotal());
    }
}
