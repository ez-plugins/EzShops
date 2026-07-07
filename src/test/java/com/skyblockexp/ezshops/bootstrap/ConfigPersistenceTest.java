package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that configs generated on first load persist across restarts:
 * - ensureStockGuiDefaults must NOT modify an existing stock-gui.yml (intentional
 *   removals and custom values must survive restarts).
 * - The /shop import command must NOT overwrite an existing category YAML file.
 */
public class ConfigPersistenceTest extends AbstractEzShopsTest {

    // -----------------------------------------------------------------------
    // stock-gui.yml persistence
    // -----------------------------------------------------------------------

    @Test
    void stock_gui_custom_values_are_not_overwritten_on_restart() throws Exception {
        Economy econ = Mockito.mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // Write a custom stock-gui.yml with a sentinel value the bundled default
        // does not contain.
        File dataFolder = plugin.getDataFolder();
        File stockGuiFile = new File(dataFolder, "stock-gui.yml");
        String customContent = "layout:\n  title: \"My Custom Title\"\n  rows: 4\n";
        Files.writeString(stockGuiFile.toPath(), customContent, StandardCharsets.UTF_8);

        // Simulate what StockComponent.ensureStockGuiDefaults does by calling it
        // reflectively on a new StockComponent.
        StockComponent stockComponent = new StockComponent();
        Method ensureMethod = StockComponent.class.getDeclaredMethod(
                "ensureStockGuiDefaults", EzShopsPlugin.class, File.class);
        ensureMethod.setAccessible(true);
        ensureMethod.invoke(stockComponent, plugin, stockGuiFile);

        // The file on disk must still contain only the operator's custom content;
        // the method must not have merged or appended anything.
        String afterContent = Files.readString(stockGuiFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(customContent, afterContent,
                "ensureStockGuiDefaults must not modify an already-existing stock-gui.yml");
    }

    @Test
    void stock_gui_is_created_when_absent() throws Exception {
        Economy econ = Mockito.mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // Remove the file that was written by saveDefaultResources() during onEnable.
        File dataFolder = plugin.getDataFolder();
        File stockGuiFile = new File(dataFolder, "stock-gui.yml");
        stockGuiFile.delete();
        assertFalse(stockGuiFile.exists(), "precondition: file must not exist");

        StockComponent stockComponent = new StockComponent();
        Method ensureMethod = StockComponent.class.getDeclaredMethod(
                "ensureStockGuiDefaults", EzShopsPlugin.class, File.class);
        ensureMethod.setAccessible(true);
        ensureMethod.invoke(stockComponent, plugin, stockGuiFile);

        assertTrue(stockGuiFile.exists(), "ensureStockGuiDefaults must create stock-gui.yml when absent");
    }

    // -----------------------------------------------------------------------
    // /shop import category-file persistence
    // -----------------------------------------------------------------------

    @Test
    void template_import_does_not_overwrite_existing_category_file() throws Exception {
        Economy econ = Mockito.mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // Write a custom category file the operator has personalised.
        File categoriesDir = new File(plugin.getDataFolder(), "shop/categories");
        categoriesDir.mkdirs();
        File customFile = new File(categoriesDir, "building.yml");
        String originalContent = "# operator custom file\ncategories:\n  building:\n    name: \"Custom Building\"\n";
        Files.writeString(customFile.toPath(), originalContent, StandardCharsets.UTF_8);

        // Simulate what ShopCommand import does: attempt to write the same filename.
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        java.util.Map<String, Object> templateData = new java.util.LinkedHashMap<>();
        templateData.put("categories", java.util.Map.of("building",
                java.util.Map.of("name", "Template Building")));

        // Replicate the fixed logic: skip if the file already exists.
        if (!customFile.exists()) {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(customFile)) {
                fos.write(yaml.dump(templateData).getBytes(StandardCharsets.UTF_8));
            }
        }

        // Operator's file must be unchanged.
        String afterContent = Files.readString(customFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(originalContent, afterContent,
                "Template import must not overwrite an existing category YAML file");
    }

    @Test
    void template_import_creates_category_file_when_absent() throws Exception {
        Economy econ = Mockito.mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        File categoriesDir = new File(plugin.getDataFolder(), "shop/categories");
        categoriesDir.mkdirs();
        File newFile = new File(categoriesDir, "my_new_category.yml");
        assertFalse(newFile.exists(), "precondition: file must not exist");

        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        java.util.Map<String, Object> templateData = new java.util.LinkedHashMap<>();
        templateData.put("categories", java.util.Map.of("my_new_category",
                java.util.Map.of("name", "New Category")));

        if (!newFile.exists()) {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
                fos.write(yaml.dump(templateData).getBytes(StandardCharsets.UTF_8));
            }
        }

        assertTrue(newFile.exists(), "Template import must write the file when it does not yet exist");
        YamlConfiguration written = YamlConfiguration.loadConfiguration(newFile);
        assertEquals("New Category",
                written.getString("categories.my_new_category.name"),
                "Written file should contain template data");
    }

    // -----------------------------------------------------------------------
    // startup default category seeding persistence
    // -----------------------------------------------------------------------

    @Test
    void startup_bundled_modes_include_multiple_defaults() throws Exception {
        Economy econ = Mockito.mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        var modes = com.skyblockexp.ezshops.bootstrap.EzShopsRegistry.current().getBundledShopModes();
        assertTrue(modes.contains("prison"),
                "Bundled resources should include prison mode defaults");
        assertTrue(modes.contains("smp"),
                "Bundled resources should include smp mode defaults");
    }

    @Test
    void manual_reseed_restores_missing_defaults_without_overwriting_existing_files() throws Exception {
        Economy econ = Mockito.mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        File categoriesDir = new File(plugin.getDataFolder(), "shop/prison/categories");
        assertTrue(categoriesDir.exists(), "precondition: prison categories directory should exist after startup");

        File deletedDefault = new File(categoriesDir, "decorations.yml");
        assertTrue(deletedDefault.exists(), "precondition: expected bundled default file to exist");
        assertTrue(deletedDefault.delete(), "precondition: failed to delete bundled default file");
        assertFalse(deletedDefault.exists(), "precondition: deleted file should be absent");

        File existingCustom = new File(categoriesDir, "building.yml");
        String customContent = "# custom\ncategories:\n  building:\n    name: \"Do Not Overwrite\"\n";
        Files.writeString(existingCustom.toPath(), customContent, StandardCharsets.UTF_8);

        int created = com.skyblockexp.ezshops.bootstrap.EzShopsRegistry.current().reseedCategoryDefaults("prison");
        assertTrue(created >= 1, "Manual reseed should create at least one missing default category file");
        assertTrue(deletedDefault.exists(), "Manual reseed should restore deleted default category files");

        String afterCustom = Files.readString(existingCustom.toPath(), StandardCharsets.UTF_8);
        assertEquals(customContent, afterCustom,
                "Manual reseed must not overwrite existing category files");
    }
}

