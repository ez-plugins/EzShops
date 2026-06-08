package com.skyblockexp.ezshops.repository;

import com.skyblockexp.ezshops.database.jaloquent.DefaultJaloquentClientAdapter;
import com.skyblockexp.ezshops.database.jaloquent.JaloquentTransactionalCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JaloquentRealAdapterIntegrationTest {

    private DefaultJaloquentClientAdapter adapter;

    @AfterEach
    public void tearDown() throws Exception {
        if (adapter != null) adapter.close();
    }

    @Test
    public void realAdapterCreatesAndReadsRowsWithH2() throws Exception {
        adapter = new DefaultJaloquentClientAdapter();
        Map<String, String> config = new HashMap<>();
        // Use H2 in MySQL compatibility mode so the JDBC SQL used by the adapter is accepted
        config.put("url", "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.put("username", "sa");
        config.put("password", "");

        adapter.init(config);
        adapter.createTableIfAbsent();

        adapter.transactional((JaloquentTransactionalCallback) tx -> {
            tx.deleteAllShops();
            Map<String, Object> values = new HashMap<>();
            values.put("sign_key", "world:0:64:0");
            values.put("owner_uuid", "00000000-0000-0000-0000-000000000000");
            values.put("quantity", 1);
            values.put("price", 10.5);
            values.put("item_data", "dummy-data");
            values.put("chests", "");
            tx.insertShop(values);
        });

        List<Map<String, String>> rows = adapter.selectAllShops();
        assertNotNull(rows);
        assertEquals(1, rows.size());
        Map<String, String> row = rows.get(0);
        assertEquals("world:0:64:0", row.get("sign_key"));
        assertEquals("00000000-0000-0000-0000-000000000000", row.get("owner_uuid"));
        assertEquals("1", row.get("quantity"));
        // Price may be rendered as 10.5 or 10.500000; do a contains check
        assertTrue(row.get("price").startsWith("10.5"));
    }
}
