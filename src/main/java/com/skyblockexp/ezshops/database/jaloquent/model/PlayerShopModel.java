package com.skyblockexp.ezshops.database.jaloquent.model;

import com.github.ezframework.jaloquent.model.Model;

import java.util.Map;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.relation.BelongsTo;

/**
 * Jaloquent-backed model for the `player_shops` table.
 */
public final class PlayerShopModel extends Model {

    public PlayerShopModel() { super((String) null); }

    public PlayerShopModel(String signKey) { super(signKey); }

    public PlayerShopModel(String signKey, String ownerUuid, int quantity, double price, String itemData, String chests) {
        super(signKey);
        set("owner_uuid", ownerUuid);
        set("quantity", quantity);
        set("price", price);
        set("item_data", itemData);
        set("chests", chests);
    }

    public String signKey() { return getId(); }

    public String ownerUuid() { return getAs("owner_uuid", String.class, null); }

    public Integer quantity() { return getAs("quantity", Integer.class, null); }

    public Double price() { return getAs("price", Double.class, null); }

    public String itemData() { return getAs("item_data", String.class, null); }

    public String chests() { return getAs("chests", String.class, null); }

    @Override
    public Map<String, Object> toMap() { return attributes(); }

    @Override
    public void fromMap(Map<String, Object> map) {
        if (map.containsKey("sign_key") && map.get("sign_key") != null) setId(String.valueOf(map.get("sign_key")));
        fill(map);
    }

    public BelongsTo<PlayerModel> owner(ModelRepository<PlayerModel> playerRepo) {
        return belongsTo(playerRepo, "owner_uuid");
    }
}
