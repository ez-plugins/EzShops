package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopPricingManagerFeatureTest {

    @Test
    void loads_static_and_rotation_categories_and_exposes_visibility_features() throws Exception {
        Path dataFolder = Files.createTempDirectory("shop-pricing-manager-feature-layout");
        write(dataFolder.resolve("shop.yml"),
                "main-menu:\n"
                        + "  title: '&aTest Shop'\n"
                        + "  size: 27\n"
                        + "category-menu:\n"
                        + "  back:\n"
                        + "    action: BACK\n"
                        + "    slot: 8\n"
                        + "    material: ARROW\n"
                        + "rotations:\n"
                        + "  daily:\n"
                        + "    default-option: morning\n"
                        + "    options:\n"
                        + "      morning:\n"
                        + "        items:\n"
                        + "          rot_item:\n"
                        + "            buy: 12\n"
                        + "      evening:\n"
                        + "        menu-title: '&6Evening Deals'\n"
                        + "        items:\n"
                        + "          rot_item:\n"
                        + "            buy: 8\n"
                        + "categories:\n"
                        + "  static_cat:\n"
                        + "    slot: 0\n"
                        + "    menu:\n"
                        + "      size: 27\n"
                        + "    items:\n"
                        + "      APPLE:\n"
                        + "        material: APPLE\n"
                        + "        slot: 0\n"
                        + "        buy: 5\n"
                        + "        sell: 2\n"
                        + "  rotating_cat:\n"
                        + "    slot: 1\n"
                        + "    rotation-group: daily\n"
                        + "    menu:\n"
                        + "      title: '&bRotation Base'\n"
                        + "      size: 27\n"
                        + "    rotation-defaults:\n"
                        + "      items:\n"
                        + "        rot_item:\n"
                        + "          material: IRON_INGOT\n"
                        + "          slot: 1\n"
                        + "          buy: 10\n"
                        + "          sell: 4\n");

        JavaPlugin plugin = mockPlugin(dataFolder, "prison");
        ShopPricingManager manager = new ShopPricingManager(plugin, DynamicPricingConfiguration.defaults());

        assertFalse(manager.isEmpty());
        assertTrue(manager.isConfigured(Material.APPLE));
        assertTrue(manager.getPrice(Material.APPLE).isPresent());
        assertTrue(manager.getConfiguredPriceKeys().contains("rot_item"));
        assertTrue(manager.getConfiguredMaterials().contains(Material.APPLE));
        assertTrue(manager.getBuyableMaterials().contains(Material.APPLE));
        assertTrue(manager.getSellableMaterials().contains(Material.APPLE));
        assertTrue(manager.isVisibleInMenu(Material.APPLE));
        assertTrue(manager.isVisibleInMenu("rot_item"));
        assertTrue(manager.isPartOfRotation("rot_item"));
        assertTrue(manager.isPartOfRotation(Material.IRON_INGOT));

        assertTrue(manager.getRotationDefinitions().containsKey("daily"));
        assertEquals("morning", manager.getActiveRotationOptions().get("daily"));
        assertTrue(manager.setActiveRotationOption("daily", "evening"));
        assertEquals("evening", manager.getActiveRotationOptions().get("daily"));
        assertEquals(ShopMenuLayout.ItemType.MATERIAL, manager.getItemType(Material.APPLE));

        ShopMenuLayout layout = manager.getMenuLayout();
        assertNotNull(layout);
        assertEquals(2, layout.categories().size());
        Optional<ShopMenuLayout.Category> rotating = layout.categories().stream()
                .filter(category -> "rotating_cat".equals(category.id()))
                .findFirst();
        assertTrue(rotating.isPresent());
        assertNotNull(rotating.get().rotation());
        assertEquals("daily", rotating.get().rotation().groupId());
        assertEquals("evening", rotating.get().rotation().optionId());
        assertEquals("rot_item", rotating.get().items().get(0).id());

        assertFalse(manager.setActiveRotationOption("daily", "missing"));
        assertFalse(manager.setActiveRotationOption("unknown", "evening"));
    }

    @Test
    void facade_supports_dynamic_and_admin_operations_for_both_key_and_material_paths() throws Exception {
        Path dataFolder = Files.createTempDirectory("shop-pricing-manager-feature-dynamic");
        write(dataFolder.resolve("shop.yml"),
                "categories:\n"
                        + "  baseline:\n"
                        + "    slot: 0\n"
                        + "    menu:\n"
                        + "      size: 27\n"
                        + "    items:\n"
                        + "      STONE:\n"
                        + "        material: STONE\n"
                        + "        slot: 0\n"
                        + "        buy: 1\n"
                        + "        sell: 1\n");

        JavaPlugin plugin = mockPlugin(dataFolder, "prison");
        ShopPricingManager manager = new ShopPricingManager(plugin, DynamicPricingConfiguration.defaults());

        manager.putPriceEntryForTesting("DIAMOND", new ShopPrice(10.0D, 5.0D),
                1.0D, 0.5D, 2.0D, 0.10D, 0.10D, 1.0D);
        manager.putPriceEntryForTesting("EMERALD", new ShopPrice(8.0D, 4.0D),
                1.0D, 0.5D, 2.0D, 0.10D, 0.10D, 1.0D);

        assertTrue(manager.getPrice(Material.DIAMOND).isPresent());
        assertTrue(manager.getPrice("DIAMOND").isPresent());
        assertTrue(manager.getPrice((String) null).isEmpty());
        assertEquals(-1.0D, manager.estimateBulkTotal((Material) null, 2, ShopTransactionType.BUY), 1e-9);
        assertEquals(-1.0D, manager.estimateBulkTotal((String) null, 2, ShopTransactionType.BUY), 1e-9);
        assertTrue(manager.estimateBulkTotal(Material.DIAMOND, 2, ShopTransactionType.BUY) > 0.0D);
        assertTrue(manager.estimateBulkTotal("DIAMOND", 2, ShopTransactionType.SELL) > 0.0D);

        manager.handlePurchase(Material.DIAMOND, 2);
        manager.handleSale(Material.DIAMOND, 1);
        manager.handlePurchase("DIAMOND", 1);
        manager.handleSale("DIAMOND", 1);

        assertTrue(manager.setPriceMultiplierForTesting("DIAMOND", 1.7D));
        assertTrue(manager.setPrice("DIAMOND", 20.0D));
        assertTrue(manager.disableBuy("DIAMOND"));
        assertTrue(manager.disableSell("DIAMOND"));
        assertTrue(manager.resetDynamicPricing("DIAMOND"));
        assertTrue(manager.resetAllDynamicPricing() >= 0);

        ShopMenuLayout layoutOverride = new ShopMenuLayout("Override", 27, null, List.of(), List.of(), List.of());
        manager.setMenuLayoutForTesting(layoutOverride);
        assertEquals("Override", manager.getMenuLayout().mainTitle());

        assertEquals(ShopMenuLayout.ItemType.MATERIAL, manager.getItemType(null));
        assertTrue(manager.getConfiguredPriceKeys().contains("DIAMOND"));
        assertFalse(manager.disableBuy(""));
        assertFalse(manager.disableSell(null));
    }

    @Test
    void reload_merges_shop_yml_and_mode_files_and_switches_with_game_mode_changes() throws Exception {
        Path dataFolder = Files.createTempDirectory("shop-pricing-manager-feature-reload");
        write(dataFolder.resolve("shop.yml"),
                "categories:\n"
                        + "  merged:\n"
                        + "    slot: 0\n"
                        + "    menu:\n"
                        + "      size: 27\n"
                        + "    items:\n"
                        + "      APPLE:\n"
                        + "        material: APPLE\n"
                        + "        slot: 0\n"
                        + "        buy: 2\n"
                        + "        sell: 1\n");
        Files.createDirectories(dataFolder.resolve("shop/prison"));
        write(dataFolder.resolve("shop/prison/override.yml"),
                "categories:\n"
                        + "  merged:\n"
                        + "    items:\n"
                        + "      APPLE:\n"
                        + "        buy: 6\n"
                        + "        sell: 3\n");
        Files.createDirectories(dataFolder.resolve("shop/smp"));
        write(dataFolder.resolve("shop/smp/override.yml"),
                "categories:\n"
                        + "  merged:\n"
                        + "    items:\n"
                        + "      APPLE:\n"
                        + "        buy: 9\n"
                        + "        sell: 4\n");

        YamlConfiguration config = new YamlConfiguration();
        config.set("game-mode", "prison");

        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("ShopPricingManagerFeatureTest"));
        Mockito.when(plugin.getConfig()).thenReturn(config);

        ShopPricingManager manager = new ShopPricingManager(plugin, DynamicPricingConfiguration.defaults());

        ShopPrice prisonPrice = manager.getPrice(Material.APPLE).orElseThrow();
        assertEquals(6.0D, prisonPrice.buyPrice(), 1e-9);
        assertEquals(3.0D, prisonPrice.sellPrice(), 1e-9);

        config.set("game-mode", "smp");
        manager.reload();

        ShopPrice smpPrice = manager.getPrice(Material.APPLE).orElseThrow();
        assertEquals(9.0D, smpPrice.buyPrice(), 1e-9);
        assertEquals(4.0D, smpPrice.sellPrice(), 1e-9);

        config.set("game-mode", "missing");
        manager.reload();

        ShopPrice fallbackPrice = manager.getPrice(Material.APPLE).orElseThrow();
        assertEquals(2.0D, fallbackPrice.buyPrice(), 1e-9);
        assertEquals(1.0D, fallbackPrice.sellPrice(), 1e-9);
    }

    @Test
    void persists_dynamic_multiplier_and_rotation_option_across_manager_recreation() throws Exception {
        Path dataFolder = Files.createTempDirectory("shop-pricing-manager-feature-persist");
        write(dataFolder.resolve("shop.yml"),
                "rotations:\n"
                        + "  daily:\n"
                        + "    default-option: morning\n"
                        + "    options:\n"
                        + "      morning:\n"
                        + "        items:\n"
                        + "          DIAMOND:\n"
                        + "            buy: 10\n"
                        + "      evening:\n"
                        + "        items:\n"
                        + "          DIAMOND:\n"
                        + "            buy: 9\n"
                        + "categories:\n"
                        + "  rotating:\n"
                        + "    slot: 0\n"
                        + "    rotation-group: daily\n"
                        + "    menu:\n"
                        + "      size: 27\n"
                        + "    rotation-defaults:\n"
                        + "      items:\n"
                        + "        DIAMOND:\n"
                        + "          material: DIAMOND\n"
                        + "          slot: 0\n"
                        + "          buy: 10\n"
                        + "          sell: 5\n"
                        + "          dynamic-pricing:\n"
                        + "            enabled: true\n"
                        + "            starting-multiplier: 1.0\n"
                        + "            min-multiplier: 0.5\n"
                        + "            max-multiplier: 2.0\n"
                        + "            buy-change: 0.1\n"
                        + "            sell-change: 0.1\n");

        JavaPlugin plugin = mockPlugin(dataFolder, "prison");
        ShopPricingManager manager = new ShopPricingManager(plugin, DynamicPricingConfiguration.defaults());

        assertTrue(manager.setActiveRotationOption("daily", "evening"));
        manager.handlePurchase(Material.DIAMOND, 2);
        double boostedBuy = manager.getPrice(Material.DIAMOND).orElseThrow().buyPrice();
        assertTrue(boostedBuy > 10.0D);

        ShopPricingManager reloaded = new ShopPricingManager(mockPlugin(dataFolder, "prison"),
                DynamicPricingConfiguration.defaults());

        assertEquals("evening", reloaded.getActiveRotationOptions().get("daily"));
        double restoredBuy = reloaded.getPrice(Material.DIAMOND).orElseThrow().buyPrice();
        assertTrue(restoredBuy > 10.0D);

        assertTrue(reloaded.resetAllDynamicPricing() > 0);
        ShopPricingManager resetReloaded = new ShopPricingManager(mockPlugin(dataFolder, "prison"),
                DynamicPricingConfiguration.defaults());
        double resetBuy = resetReloaded.getPrice(Material.DIAMOND).orElseThrow().buyPrice();
        assertEquals(9.0D, resetBuy, 1e-9);
    }

    @Test
    void missing_configuration_results_in_empty_safe_manager_state() throws Exception {
        Path dataFolder = Files.createTempDirectory("shop-pricing-manager-feature-empty");
        ShopPricingManager manager = new ShopPricingManager(mockPlugin(dataFolder, "prison"),
                DynamicPricingConfiguration.defaults());

        assertTrue(manager.isEmpty());
        assertTrue(manager.getPrice(Material.APPLE).isEmpty());
        assertTrue(manager.getPrice("APPLE").isEmpty());
        assertFalse(manager.isConfigured(Material.APPLE));
        assertFalse(manager.isVisibleInMenu(Material.APPLE));
        assertFalse(manager.isVisibleInMenu("APPLE"));
        assertFalse(manager.isPartOfRotation(Material.APPLE));
        assertFalse(manager.isPartOfRotation("APPLE"));
        assertTrue(manager.getConfiguredMaterials().isEmpty());
        assertTrue(manager.getBuyableMaterials().isEmpty());
        assertTrue(manager.getSellableMaterials().isEmpty());
        assertEquals(-1.0D, manager.estimateBulkTotal(Material.APPLE, 1, ShopTransactionType.BUY), 1e-9);
        assertEquals(-1.0D, manager.estimateBulkTotal("APPLE", 1, ShopTransactionType.BUY), 1e-9);
        assertFalse(manager.setActiveRotationOption("daily", "morning"));
    }

    private static JavaPlugin mockPlugin(Path dataFolder, String gameMode) {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("game-mode", gameMode);
        Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("ShopPricingManagerFeatureTest"));
        Mockito.when(plugin.getConfig()).thenReturn(config);
        return plugin;
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
