package com.skyblockexp.ezshops.database.jaloquent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlMigrationRunnerTest {

    @Test
    void applyMigrationsCreatesPrefixedTablesOnH2() throws Exception {
        String url = "jdbc:h2:mem:migrations_test;MODE=MySQL;DB_CLOSE_DELAY=-1";
        Map<String, String> cfg = new HashMap<>();
        cfg.put("url", url);
        cfg.put("username", "sa");
        cfg.put("password", "");
        cfg.put("table-prefix", "ps_");

        SqlMigrationRunner.applyMigrations(cfg);

        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            assertEquals(1, tableExists(c, "ps_player_shops"));
            assertEquals(1, tableExists(c, "ps_shop_transactions"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void splitStatementsIgnoresCommentsAndSplitsOnSemicolonLines() throws Exception {
        Method m = SqlMigrationRunner.class.getDeclaredMethod("splitStatements", String.class);
        m.setAccessible(true);

        String sql = "-- comment\nCREATE TABLE a(id INT);\n\n-- comment 2\nCREATE TABLE b(id INT);\n";
        List<String> out = (List<String>) m.invoke(null, sql);

        assertEquals(2, out.size());
        assertTrue(out.get(0).contains("CREATE TABLE a"));
        assertTrue(out.get(1).contains("CREATE TABLE b"));
    }

    private static int tableExists(Connection c, String table) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?")) {
            ps.setString(1, table.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}

