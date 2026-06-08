package com.skyblockexp.ezshops.database.jaloquent.model;

import com.github.ezframework.jaloquent.model.Model;

import java.util.Map;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.relation.HasMany;
import com.github.ezframework.jaloquent.relation.HasOne;
import com.github.ezframework.jaloquent.relation.BelongsTo;
import com.github.ezframework.jaloquent.relation.BelongsToMany;
import com.github.ezframework.jaloquent.model.BaseModel;

/**
 * Jaloquent-backed model for the `players` table.
 */
public final class PlayerModel extends Model {

    public PlayerModel() { super((String) null); }

    public PlayerModel(String uuid) { super(uuid); }

    public String uuid() { return getId(); }

    public Long firstSeen() { return getAs("first_seen", Long.class, null); }

    public Long lastSeen() { return getAs("last_seen", Long.class, null); }

    @Override
    public Map<String, Object> toMap() { return attributes(); }

    @Override
    public void fromMap(Map<String, Object> map) {
        if (map.containsKey("uuid") && map.get("uuid") != null) setId(String.valueOf(map.get("uuid")));
        fill(map);
    }

    public HasMany<TransactionModel> transactions(ModelRepository<TransactionModel> txRepo) {
        return hasMany(txRepo, "player_uuid");
    }

    public HasMany<PlayerShopModel> shops(ModelRepository<PlayerShopModel> shopRepo) {
        return hasMany(shopRepo, "owner_uuid");
    }
}
