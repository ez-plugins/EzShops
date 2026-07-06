package com.skyblockexp.ezshops.shop.pricing.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopConfigSourceLoaderFeatureTest {

    @Test
    void ensure_data_folder_and_merge_mode_specific_files_over_shop_yml() throws Exception {
        Path dataFolder = Files.createTempDirectory("shop-config-loader-feature");
        write(dataFolder.resolve("shop.yml"),
                "value: 1\n"
                        + "nested:\n"
                        + "  root: true\n");
        Files.createDirectories(dataFolder.resolve("shop/prison"));
        write(dataFolder.resolve("shop/prison/01-base.yml"),
                "value: 2\n"
                        + "nested:\n"
                        + "  mode: true\n");

        JavaPlugin plugin = mockPlugin(dataFolder, "prison");
        ShopConfigSourceLoader loader = new ShopConfigSourceLoader(plugin, Logger.getLogger("test"));

        loader.ensureDataFolder();
        YamlConfiguration loaded = loader.loadCombinedConfiguration();

        assertNotNull(loaded);
        assertEquals(2, loaded.getInt("value"));
        assertTrue(loaded.getBoolean("nested.root"));
        assertTrue(loaded.getBoolean("nested.mode"));

        String sourceInfo = loader.buildSourceInfo();
        assertTrue(sourceInfo.contains("shop/prison/"));
        assertTrue(sourceInfo.contains("shop.yml"));
    }

    @Test
    void falls_back_to_legacy_shop_directory_when_mode_directory_missing() throws Exception {
        Path dataFolder = Files.createTempDirectory("shop-config-loader-legacy-feature");
        Files.createDirectories(dataFolder.resolve("shop"));
        write(dataFolder.resolve("shop/legacy.yml"),
                "categories:\n"
                        + "  legacy:\n"
                        + "    slot: 0\n");

        JavaPlugin plugin = mockPlugin(dataFolder, "smp");
        ShopConfigSourceLoader loader = new ShopConfigSourceLoader(plugin, Logger.getLogger("test"));

        YamlConfiguration loaded = loader.loadCombinedConfiguration();
        assertNotNull(loaded);
        assertEquals(0, loaded.getInt("categories.legacy.slot"));

        String sourceInfo = loader.buildSourceInfo();
        assertTrue(sourceInfo.contains("shop/"));
    }

    @Test
    void returns_null_when_no_config_files_exist() throws Exception {
        Path dataFolder = Files.createTempDirectory("shop-config-loader-none-feature");
        JavaPlugin plugin = mockPlugin(dataFolder, "prison");
        ShopConfigSourceLoader loader = new ShopConfigSourceLoader(plugin, Logger.getLogger("test"));

        YamlConfiguration loaded = loader.loadCombinedConfiguration();
        assertNull(loaded);
    }

    private static JavaPlugin mockPlugin(Path dataFolder, String gameMode) {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("game-mode", gameMode);
        Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("ShopConfigSourceLoaderFeatureTest"));
        Mockito.when(plugin.getConfig()).thenReturn(config);
        return plugin;
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
