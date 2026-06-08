package com.skyblockexp.ezshops.database.jaloquent.model;

import com.github.ezframework.jaloquent.model.Model;

import java.util.Map;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.relation.BelongsTo;

/**
 * Jaloquent-backed model for the `shop_transactions` table.
 */
public final class TransactionModel extends Model {

    public TransactionModel() { super((String) null); }

    public TransactionModel(String id) { super(id); }

    public TransactionModel(Integer id, long occurredAt, String type, String playerUuid, String itemYaml, int quantity, double total) {
        super(id == null ? null : String.valueOf(id));
        set("occurred_at", occurredAt);
        set("type", type);
        set("player_uuid", playerUuid);
        set("item_yaml", itemYaml);
        set("quantity", quantity);
        set("total", total);
    }

    public Long idAsLong() {
        String id = getId();
        return id == null ? null : Long.valueOf(id);
    }

    public Long occurredAt() {
        Long v = getAs("occurred_at", Long.class, null);
        if (v == null) {
            Object o = get("occurred_at");
            if (o instanceof Number) return ((Number) o).longValue();
            return null;
        }
        return v;
    }

    public String type() { return getAs("type", String.class, null); }

    public String playerUuid() { return getAs("player_uuid", String.class, null); }

    public String itemYaml() { return getAs("item_yaml", String.class, null); }

    public Integer quantity() { return getAs("quantity", Integer.class, null); }

    public Double total() { return getAs("total", Double.class, null); }

    @Override
    public Map<String, Object> toMap() { return attributes(); }

    @Override
    public void fromMap(Map<String, Object> map) { 
        if (map.containsKey("id") && map.get("id") != null) setId(String.valueOf(map.get("id")));
        fill(map);
    }

    public BelongsTo<PlayerModel> player(ModelRepository<PlayerModel> playerRepo) {
        return belongsTo(playerRepo, "player_uuid");
    }
}
