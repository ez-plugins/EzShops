package com.skyblockexp.ezshops.repository.transaction;

import com.skyblockexp.ezshops.repository.yml.YmlTransactionRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class YmlTransactionRepositoryTest {

    private File tempDir;

    @AfterEach
    public void cleanup() throws Exception {
        if (tempDir != null && tempDir.exists()) {
            Files.walk(tempDir.toPath())
                    .map(java.nio.file.Path::toFile)
                    .sorted((a,b) -> -a.compareTo(b))
                    .forEach(File::delete);
        }
    }

    @Test
    public void writesTransactionToYamlFile() throws Exception {
        tempDir = Files.createTempDirectory("ezshops-test").toFile();
        YmlTransactionRepository repo = new YmlTransactionRepository(tempDir);

        TransactionRecord rec = new TransactionRecord(System.currentTimeMillis(), TransactionRecord.Type.SALE, UUID.randomUUID(), "item-yaml", 5, 123.45);
        repo.record(rec);
        repo.close();

        File out = new File(tempDir, "transactions.yml");
        assertTrue(out.exists(), "transactions.yml should exist");

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(out);
        List<String> entries = cfg.getStringList("transactions");
        assertEquals(1, entries.size());
        String entry = entries.get(0);
        assertTrue(entry.toLowerCase().contains("sale") || entry.toLowerCase().contains("purchase"), "entry should mention sale/purchase type");
    }
}
