package com.skyblockexp.ezshops.repository.transaction;

import com.skyblockexp.ezshops.database.jaloquent.JaloquentTransactionRepository;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JaloquentTransactionRepositoryIT {

    @Test
    public void recordsTransactionIntoH2() throws Exception {
        Map<String, String> config = new HashMap<>();
        String url = "jdbc:h2:mem:ezshops_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        config.put("url", url);
        config.put("username", "sa");
        config.put("password", "");
        config.put("table-prefix", "ez_");

        JaloquentTransactionRepository repo = new JaloquentTransactionRepository(config);

        TransactionRecord rec = new TransactionRecord(System.currentTimeMillis(), TransactionRecord.Type.PURCHASE, UUID.randomUUID(), "itmyaml", 2, 9.99);
        repo.record(rec);

        // verify via JDBC
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM ez_shop_transactions");
            ResultSet rs = ps.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            assertEquals(1, count);
        }

        repo.close();
    }

    @Test
    public void recordHandlesNullPlayer() throws Exception {
        Map<String, String> config = new HashMap<>();
        String url = "jdbc:h2:mem:ezshops_null_player;MODE=MySQL;DB_CLOSE_DELAY=-1";
        config.put("url", url);
        config.put("username", "sa");
        config.put("password", "");
        config.put("table-prefix", "ez_");

        JaloquentTransactionRepository repo = new JaloquentTransactionRepository(config);

        TransactionRecord rec = new TransactionRecord(System.currentTimeMillis(), TransactionRecord.Type.SALE, null, "yaml", 1, 5.0);
        repo.record(rec);

        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            PreparedStatement ps = c.prepareStatement("SELECT player_uuid FROM ez_shop_transactions");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertNull(rs.getString("player_uuid"));
        }

        repo.close();
    }

    @Test
    public void recordHandlesNegativePrice() throws Exception {
        Map<String, String> config = new HashMap<>();
        String url = "jdbc:h2:mem:ezshops_neg;MODE=MySQL;DB_CLOSE_DELAY=-1";
        config.put("url", url);
        config.put("username", "sa");
        config.put("password", "");
        config.put("table-prefix", "ez_");

        JaloquentTransactionRepository repo = new JaloquentTransactionRepository(config);

        TransactionRecord rec = new TransactionRecord(1000L, TransactionRecord.Type.PURCHASE, UUID.randomUUID(), "yaml", 3, -5.0);
        repo.record(rec);

        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            PreparedStatement ps = c.prepareStatement("SELECT total FROM ez_shop_transactions");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(-5.0, rs.getDouble("total"));
        }

        repo.close();
    }

    @Test
    public void constructorUsesDefaultValuesWhenConfigMissing() {
        Map<String, String> config = new HashMap<>();
        // Empty config - should use defaults
        assertDoesNotThrow(() -> new JaloquentTransactionRepository(config));
    }

    @Test
    public void constructorAcceptsHostPortDatabase() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("host", "localhost");
        config.put("port", "3306");
        config.put("database", "testdb");
        config.put("username", "sa");
        config.put("password", "");

        // Just verify construction - doesn't actually connect without valid DB
        assertDoesNotThrow(() -> new JaloquentTransactionRepository(config));
    }

    @Test
    public void constructorAcceptsUserAsUsername() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("url", "jdbc:h2:mem:user_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        config.put("user", "sa");
        config.put("password", "");

        assertDoesNotThrow(() -> new JaloquentTransactionRepository(config));
    }
}
