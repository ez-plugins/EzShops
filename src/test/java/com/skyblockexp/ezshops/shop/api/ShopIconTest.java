package com.skyblockexp.ezshops.shop.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ShopIconTest {

    @Test
    public void toMapEmitsItemstackBase64() {
        ShopIcon icon = ShopIcon.builder().material("DIAMOND").amount(3).serializedItem("base64data").build();
        Map<String, Object> map = icon.toMap();
        assertEquals("base64data", map.get("itemstack-base64"));
        assertNull(map.get("item"));
    }

    @Test
    public void fromMapPrefersItemstackBase64AndFallsBackToItem() {
        Map<String, Object> both = Map.of("itemstack-base64", "b64", "item", "old");
        ShopIcon i1 = ShopIcon.fromMap(both);
        assertEquals("b64", i1.serializedItem());

        Map<String, Object> onlyOld = Map.of("item", "legacy");
        ShopIcon i2 = ShopIcon.fromMap(onlyOld);
        assertEquals("legacy", i2.serializedItem());
    }

    @Test
    public void roundtripPreservesFields() {
        ShopIcon icon = ShopIcon.builder()
                .material("STONE")
                .amount(5)
                .displayName("Nice")
                .lore(List.of("l1", "l2"))
                .serializedItem("dGVzdA==")
                .build();

        Map<String, Object> map = icon.toMap();
        ShopIcon parsed = ShopIcon.fromMap(map);

        assertEquals(icon.material(), parsed.material());
        assertEquals(icon.amount(), parsed.amount());
        assertEquals(icon.displayName(), parsed.displayName());
        assertEquals(icon.lore(), parsed.lore());
        assertEquals(icon.serializedItem(), parsed.serializedItem());
    }
}
 
