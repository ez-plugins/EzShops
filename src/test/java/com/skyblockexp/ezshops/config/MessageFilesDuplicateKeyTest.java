package com.skyblockexp.ezshops.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the bundled {@code messages/*.yml} files against duplicate YAML keys.
 *
 * <p>SnakeYAML (and therefore Bukkit's YamlConfiguration) silently keeps the last
 * duplicate mapping key and discards the first, which once silently dropped the
 * {@code shop.common} sub-keys in {@code messages_zh.yml} / {@code messages_nl.yml}
 * and logged {@code duplicate keys found : common} at startup.</p>
 *
 * <p>Parsing in strict mode ({@code allowDuplicateKeys = false}) makes any duplicate
 * key throw, so a regression re-introduced by an upstream merge fails this test.</p>
 */
class MessageFilesDuplicateKeyTest {

    private static final List<String> MESSAGE_FILES = List.of(
            "messages_en.yml", "messages_es.yml", "messages_nl.yml", "messages_zh.yml");

    @Test
    void messageFilesContainNoDuplicateKeys() throws IOException {
        for (String file : MESSAGE_FILES) {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            Yaml yaml = new Yaml(options);

            try (InputStream in = getClass().getResourceAsStream("/messages/" + file)) {
                if (in == null) {
                    fail("Missing bundled resource /messages/" + file);
                }
                assertDoesNotThrow(() -> yaml.load(in),
                        "Duplicate (or invalid) YAML keys in " + file);
            }
        }
    }
}
