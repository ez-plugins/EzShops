package com.skyblockexp.ezshops.database.jaloquent.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionModelTest {

    @Test
    void constructorPopulatesFieldsAndIdAsLong() {
        TransactionModel model = new TransactionModel(12, 123L, "SALE", "u", "yaml", 2, 3.5);
        assertEquals(12L, model.idAsLong());
        assertEquals(123L, model.occurredAt());
        assertEquals("SALE", model.type());
        assertEquals("u", model.playerUuid());
        assertEquals("yaml", model.itemYaml());
        assertEquals(2, model.quantity());
        assertEquals(3.5, model.total());
    }

    @Test
    void occurredAtFallsBackToNumericObjectWhenLongConversionIsNull() {
        TransactionModel model = new TransactionModel();
        model.set("occurred_at", Integer.valueOf(44));
        assertEquals(44L, model.occurredAt());
    }

    @Test
    void fromMapSetsIdAndFields() {
        TransactionModel model = new TransactionModel();
        Map<String, Object> map = new HashMap<>();
        map.put("id", 7);
        map.put("type", "PURCHASE");
        map.put("quantity", 9);
        model.fromMap(map);

        assertEquals(7L, model.idAsLong());
        assertEquals("PURCHASE", model.type());
        assertEquals(9, model.quantity());
    }

    @Test
    void nullValuesAreHandled() {
        TransactionModel model = new TransactionModel((String) null);
        assertNull(model.idAsLong());
        assertNull(model.occurredAt());
    }
}

