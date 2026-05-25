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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
