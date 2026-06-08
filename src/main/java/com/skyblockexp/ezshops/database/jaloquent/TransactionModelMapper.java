package com.skyblockexp.ezshops.database.jaloquent;

import com.skyblockexp.ezshops.database.jaloquent.model.TransactionModel;
import com.skyblockexp.ezshops.model.ShopTransaction;
import com.skyblockexp.ezshops.repository.transaction.TransactionRecord;

import java.time.Instant;

public final class TransactionModelMapper {

    private TransactionModelMapper() {}

    public static TransactionModel fromRecord(TransactionRecord r) {
        TransactionModel m = new TransactionModel();
        m.set("occurred_at", r.getOccurredAt());
        m.set("type", r.getType().name());
        m.set("player_uuid", r.getPlayer() != null ? r.getPlayer().toString() : null);
        m.set("item_yaml", r.getItemYaml());
        m.set("quantity", r.getQuantity());
        m.set("total", r.getTotal());
        return m;
    }

    public static TransactionModel fromShopTransaction(ShopTransaction s) {
        TransactionModel m = new TransactionModel();
        if (s.getOccurredAt() != null) m.set("occurred_at", s.getOccurredAt().toEpochMilli());
        m.set("type", s.getType().name());
        m.set("player_uuid", s.getPlayerUuid());
        m.set("item_yaml", s.getItemYaml());
        m.set("quantity", s.getQuantity());
        m.set("total", s.getTotal());
        return m;
    }

    public static ShopTransaction toShopTransaction(TransactionModel m) {
        Long id = m.idAsLong();
        Long occurred = m.occurredAt();
        ShopTransaction.Type type = ShopTransaction.Type.valueOf(m.type());
        String player = m.playerUuid();
        String yaml = m.itemYaml();
        int qty = m.quantity() == null ? 0 : m.quantity();
        double total = m.total() == null ? 0.0 : m.total();
        return new ShopTransaction(id, occurred == null ? Instant.EPOCH : Instant.ofEpochMilli(occurred), type, player, yaml, qty, total);
    }
}
