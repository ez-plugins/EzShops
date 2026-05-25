package com.skyblockexp.ezshops.database.jaloquent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Simple JDBC-based adapter that implements {@link JaloquentClientAdapter}.
 *
 * <p>This adapter uses raw JDBC to provide the transactional semantics the
 * repository expects. It's intentionally lightweight so the project can
 * function without a full Jaloquent store implementation.
 */
public final class JdbcJaloquentClientAdapter implements JaloquentClientAdapter {

    private Connection connection;
    private String resolvedTable = "ez_player_shops";

    @Override
    public void init(Map<String, String> config) throws Exception {
        String url = config.get("url");
        if (url == null || url.isBlank()) {
            String host = config.getOrDefault("host", "localhost");
            String port = config.getOrDefault("port", "3306");
            String database = config.getOrDefault("database", "minecraft");
            url = "jdbc:mysql://" + host + ':' + port + '/' + database + "?useSSL=false&serverTimezone=UTC";
        }
        String user = config.getOrDefault("username", config.getOrDefault("user", "root"));
        String pass = config.getOrDefault("password", "");
        String tablePrefix = config.getOrDefault("table-prefix", "ez_");
        resolvedTable = tablePrefix + "player_shops";

        connection = DriverManager.getConnection(url, user, pass);
    }

    @Override
    public void createTableIfAbsent() throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS `" + resolvedTable + "` ("
                + "`sign_key` VARCHAR(255) PRIMARY KEY,"
                + "`owner_uuid` VARCHAR(36),"
                + "`quantity` INT,"
                + "`price` DOUBLE,"
                + "`item_data` MEDIUMTEXT,"
                + "`chests` TEXT"
                + ") ENGINE=InnoDB";
        try (Statement s = connection.createStatement()) {
            s.executeUpdate(sql);
        }
    }

    @Override
    public List<Map<String, String>> selectAllShops() throws Exception {
        String sql = "SELECT sign_key, owner_uuid, quantity, price, item_data, chests FROM `" + resolvedTable + "`";
        List<Map<String, String>> rows = new ArrayList<>();
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("sign_key", rs.getString("sign_key"));
                row.put("owner_uuid", rs.getString("owner_uuid"));
                row.put("quantity", Integer.toString(rs.getInt("quantity")));
                row.put("price", Double.toString(rs.getDouble("price")));
                row.put("item_data", rs.getString("item_data"));
                row.put("chests", rs.getString("chests"));
                rows.add(row);
            }
        }
        return rows;
    }

    @Override
    public void transactional(JaloquentTransactionalCallback callback) throws Exception {
        boolean previousAuto = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            JaloquentTransaction tx = new JaloquentTransaction() {
                @Override
                public void deleteAllShops() throws Exception {
                    try (Statement s = connection.createStatement()) {
                        s.executeUpdate("DELETE FROM `" + resolvedTable + "`");
                    }
                }

                @Override
                public void insertShop(Map<String, Object> values) throws Exception {
                    String sql = "INSERT INTO `" + resolvedTable + "` (sign_key, owner_uuid, quantity, price, item_data, chests) VALUES (?,?,?,?,?,?)";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, Objects.toString(values.getOrDefault("sign_key", ""), ""));
                        ps.setString(2, Objects.toString(values.getOrDefault("owner_uuid", ""), ""));
                        Object q = values.getOrDefault("quantity", 0);
                        ps.setInt(3, ((Number) q).intValue());
                        Object p = values.getOrDefault("price", 0.0);
                        ps.setDouble(4, ((Number) p).doubleValue());
                        ps.setString(5, Objects.toString(values.getOrDefault("item_data", ""), ""));
                        ps.setString(6, Objects.toString(values.getOrDefault("chests", ""), ""));
                        ps.executeUpdate();
                    }
                }
            };
            callback.execute(tx);
            connection.commit();
        } catch (Exception ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAuto);
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) connection.close();
    }
}
