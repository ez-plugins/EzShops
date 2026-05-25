package com.skyblockexp.ezshops.database.jaloquent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Very small migration runner that applies SQL files bundled under
 * `db/migrations` on startup. It looks for known files and executes their
 * statements against the configured JDBC connection.
 */
public final class SqlMigrationRunner {

    private static final String[] SCRIPTS = new String[] {
            "db/migrations/V1__create_player_shops.sql",
            "db/migrations/V1__create_shop_transactions.sql"
    };

    private SqlMigrationRunner() {}

    public static void applyMigrations(Map<String, String> cfg) throws Exception {
        String url = cfg.get("url");
        if (url == null || url.isBlank()) {
            String host = cfg.getOrDefault("host", "localhost");
            String port = cfg.getOrDefault("port", "3306");
            String database = cfg.getOrDefault("database", "minecraft");
            url = "jdbc:mysql://" + host + ':' + port + '/' + database + "?useSSL=false&serverTimezone=UTC";
        }
        String user = cfg.getOrDefault("username", cfg.getOrDefault("user", "root"));
        String pass = cfg.getOrDefault("password", "");
        String tablePrefix = cfg.getOrDefault("table-prefix", "ez_");

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            for (String script : SCRIPTS) {
                String sql = loadResource(script);
                if (sql == null || sql.isBlank()) continue;
                // Replace default prefix with configured prefix
                if (!"ez_".equals(tablePrefix)) {
                    sql = sql.replace("`ez_", "`" + tablePrefix);
                    sql = sql.replace(" ez_", " " + tablePrefix);
                    sql = sql.replace("ez_", tablePrefix);
                }
                List<String> statements = splitStatements(sql);
                try (Statement st = conn.createStatement()) {
                    for (String stmt : statements) {
                        if (stmt == null || stmt.isBlank()) continue;
                        st.execute(stmt);
                    }
                }
            }
        }
    }

    private static String loadResource(String path) throws Exception {
        try (InputStream in = SqlMigrationRunner.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
    }

    private static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
            cur.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                out.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
