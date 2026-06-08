package com.skyblockexp.ezshops.database.jaloquent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public final class DefaultJaloquentClientAdapter implements JaloquentClientAdapter {

    private final JdbcJaloquentClientAdapter delegate = new JdbcJaloquentClientAdapter();

    @Override
    public void init(Map<String, String> config) throws Exception {
        delegate.init(config);
        try {
            // Apply bundled SQL migrations (if present)
            try {
                SqlMigrationRunner.applyMigrations(config);
            } catch (Exception ex) {
                // Non-fatal: migrations are best-effort and shouldn't block startup
            }
            Class<?> tr = Class.forName("com.github.ezframework.jaloquent.TableRegistry");
            Method register = tr.getMethod("register", String.class, String.class, Map.class);
            Map<String, String> cols = new HashMap<>();
            cols.put("sign_key", "VARCHAR(255) PRIMARY KEY");
            cols.put("owner_uuid", "VARCHAR(36)");
            cols.put("quantity", "INT");
            cols.put("price", "DOUBLE");
            cols.put("item_data", "MEDIUMTEXT");
            cols.put("chests", "TEXT");
            String tablePrefix = config.getOrDefault("table-prefix", "ez_");
            String resolved = tablePrefix + "player_shops";
            register.invoke(null, resolved, "player_shops", cols);
            Map<String, String> txCols = new HashMap<>();
            txCols.put("id", "INT AUTO_INCREMENT PRIMARY KEY");
            txCols.put("occurred_at", "BIGINT");
            txCols.put("type", "VARCHAR(10)");
            txCols.put("player_uuid", "VARCHAR(36)");
            txCols.put("item_yaml", "MEDIUMTEXT");
            txCols.put("quantity", "INT");
            txCols.put("total", "DOUBLE");
            String txResolved = tablePrefix + "shop_transactions";
            register.invoke(null, txResolved, "shop_transactions", txCols);
            Map<String, String> players = new HashMap<>();
            players.put("uuid", "VARCHAR(36) PRIMARY KEY");
            players.put("first_seen", "BIGINT");
            players.put("last_seen", "BIGINT");
            String playersResolved = tablePrefix + "players";
            register.invoke(null, playersResolved, "players", players);
        } catch (ClassNotFoundException ex) {
            // Jaloquent not present on classpath; nothing to do
        }
    }

    @Override
    public void createTableIfAbsent() throws Exception {
        delegate.createTableIfAbsent();
    }

    @Override
    public List<Map<String, String>> selectAllShops() throws Exception {
        return delegate.selectAllShops();
    }

    @Override
    public void transactional(JaloquentTransactionalCallback callback) throws Exception {
        delegate.transactional(callback);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
