package com.skyblockexp.ezshops.database.jaloquent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JaloquentTransactionModelTest {

    @Test
    void constructorAndGettersExposeValues() {
        JaloquentTransactionModel model = new JaloquentTransactionModel(
                9, 1234L, "SALE", "uuid", "yaml", 8, 14.25
        );

        assertEquals(9, model.getId());
        assertEquals(1234L, model.getOccurredAt());
        assertEquals("SALE", model.getType());
        assertEquals("uuid", model.getPlayerUuid());
        assertEquals("yaml", model.getItemYaml());
        assertEquals(8, model.getQuantity());
        assertEquals(14.25, model.getTotal());
    }

    @Test
    void settersUpdateFields() {
        JaloquentTransactionModel model = new JaloquentTransactionModel();
        model.setId(4);
        model.setOccurredAt(777L);
        model.setType("PURCHASE");
        model.setPlayerUuid("abc");
        model.setItemYaml("serialized");
        model.setQuantity(16);
        model.setTotal(3.5);

        assertEquals(4, model.getId());
        assertEquals(777L, model.getOccurredAt());
        assertEquals("PURCHASE", model.getType());
        assertEquals("abc", model.getPlayerUuid());
        assertEquals("serialized", model.getItemYaml());
        assertEquals(16, model.getQuantity());
        assertEquals(3.5, model.getTotal());
    }
}

