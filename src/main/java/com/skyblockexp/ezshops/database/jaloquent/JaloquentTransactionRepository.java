package com.skyblockexp.ezshops.database.jaloquent;

import com.github.ezframework.jaloquent.model.ModelFactory;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.jaloquent.store.sql.DriverManagerDataSource;
import com.skyblockexp.ezshops.model.ShopTransaction;
import com.skyblockexp.ezshops.repository.transaction.TransactionRecord;
import com.skyblockexp.ezshops.repository.transaction.TransactionRepository;
import com.skyblockexp.ezshops.database.jaloquent.model.TransactionModel;
import com.skyblockexp.ezshops.database.jaloquent.model.PlayerModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Jaloquent-backed transaction repository using ModelRepository.
 */
public final class JaloquentTransactionRepository implements TransactionRepository {

    private final ModelRepository<TransactionModel> txRepo;
    private final ModelRepository<PlayerModel> playerRepo;
    private final String table;

    public JaloquentTransactionRepository(Map<String, String> config) throws Exception {
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
        this.table = tablePrefix + "shop_transactions";

        DriverManagerDataSource ds = new DriverManagerDataSource(url, user, pass);
        DataSourceJdbcStore store = new DataSourceJdbcStore(ds);

        ModelFactory<TransactionModel> txFactory = (id, map) -> {
            TransactionModel m = new TransactionModel();
            m.fromMap(map);
            return m;
        };

        ModelFactory<PlayerModel> playerFactory = (id, map) -> {
            PlayerModel p = new PlayerModel();
            p.fromMap(map);
            return p;
        };

        this.txRepo = new ModelRepository<>(store, this.table, txFactory);
        this.playerRepo = new ModelRepository<>(store, tablePrefix + "players", playerFactory);
    }

    @Override
    public synchronized void record(TransactionRecord record) throws Exception {
        txRepo.transaction(() -> {
            // ensure player exists
            if (record.getPlayer() != null) {
                String pu = record.getPlayer().toString();
                Optional<PlayerModel> existing = playerRepo.find(pu);
                if (existing.isEmpty()) {
                    PlayerModel p = new PlayerModel(pu);
                    p.set("first_seen", record.getOccurredAt());
                    p.set("last_seen", record.getOccurredAt());
                    playerRepo.save(p);
                } else {
                    PlayerModel p = existing.get();
                    p.set("last_seen", record.getOccurredAt());
                    playerRepo.save(p);
                }
            }

            TransactionModel tm = TransactionModelMapper.fromRecord(record);
            tm.save(txRepo);
        });
    }

    @Override
    public void close() throws Exception { /* DataSource/Repo cleanup not required here */ }
}
