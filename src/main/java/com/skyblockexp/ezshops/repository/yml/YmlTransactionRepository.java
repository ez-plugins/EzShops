package com.skyblockexp.ezshops.repository.yml;

import com.skyblockexp.ezshops.repository.transaction.TransactionRecord;
import com.skyblockexp.ezshops.repository.transaction.TransactionRepository;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class YmlTransactionRepository implements TransactionRepository {

    private final File file;
    private final YamlConfiguration cfg;

    public YmlTransactionRepository(File pluginFolder) {
        this.file = new File(pluginFolder, "transactions.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); this.cfg.save(file); } catch (Exception ignored) {}
        }
    }

    @Override
    public synchronized void record(TransactionRecord record) throws Exception {
        List<String> entries = cfg.getStringList("transactions");
        if (entries == null) entries = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append(record.getOccurredAt()).append('|')
          .append(record.getType().name()).append('|')
          .append(record.getPlayer() != null ? record.getPlayer().toString() : "").append('|')
          .append(record.getQuantity()).append('|')
          .append(record.getTotal()).append('|')
          .append(record.getItemYaml() != null ? record.getItemYaml().replace("\n", "\\n") : "");
        entries.add(sb.toString());
        cfg.set("transactions", entries);
        cfg.save(file);
    }
}
