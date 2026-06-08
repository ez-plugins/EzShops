package com.skyblockexp.ezshops.database.jaloquent;

import com.skyblockexp.ezshops.database.jaloquent.model.TransactionModel;
import com.skyblockexp.ezshops.model.ShopTransaction;
import com.skyblockexp.ezshops.repository.transaction.TransactionRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionModelMapperTest {

    @Test
    void fromRecordMapsAllFields() {
        UUID player = UUID.randomUUID();
        TransactionRecord record = new TransactionRecord(
                12345L,
                TransactionRecord.Type.SALE,
                player,
                "yaml",
                12,
                99.5
        );

        TransactionModel model = TransactionModelMapper.fromRecord(record);
        assertEquals(12345L, model.occurredAt());
        assertEquals("SALE", model.type());
        assertEquals(player.toString(), model.playerUuid());
        assertEquals("yaml", model.itemYaml());
        assertEquals(12, model.quantity());
        assertEquals(99.5, model.total());
    }

    @Test
    void fromRecordHandlesNullPlayer() {
        TransactionRecord record = new TransactionRecord(
                1L, TransactionRecord.Type.PURCHASE, null, "x", 1, 1.0
        );
        TransactionModel model = TransactionModelMapper.fromRecord(record);
        assertNull(model.playerUuid());
        assertEquals("PURCHASE", model.type());
    }

    @Test
    void fromShopTransactionAndBackRoundTripsCoreFields() {
        ShopTransaction input = new ShopTransaction(
                7L,
                Instant.ofEpochMilli(5000L),
                ShopTransaction.Type.SALE,
                "a-b-c",
                "item-yaml",
                3,
                45.0
        );

        TransactionModel model = TransactionModelMapper.fromShopTransaction(input);
        model.setId("7");
        ShopTransaction output = TransactionModelMapper.toShopTransaction(model);

        assertEquals(7L, output.getId());
        assertEquals(Instant.ofEpochMilli(5000L), output.getOccurredAt());
        assertEquals(ShopTransaction.Type.SALE, output.getType());
        assertEquals("a-b-c", output.getPlayerUuid());
        assertEquals("item-yaml", output.getItemYaml());
        assertEquals(3, output.getQuantity());
        assertEquals(45.0, output.getTotal());
    }

    @Test
    void toShopTransactionUsesSafeDefaultsForNullFields() {
        TransactionModel model = new TransactionModel();
        model.set("type", "PURCHASE");

        ShopTransaction output = TransactionModelMapper.toShopTransaction(model);
        assertEquals(Instant.EPOCH, output.getOccurredAt());
        assertEquals(0, output.getQuantity());
        assertEquals(0.0, output.getTotal());
        assertEquals(ShopTransaction.Type.PURCHASE, output.getType());
    }
}

