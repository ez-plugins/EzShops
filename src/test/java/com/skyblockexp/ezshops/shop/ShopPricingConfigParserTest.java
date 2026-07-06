package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopManagedPriceEntry;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingLayoutSupport;
import com.skyblockexp.ezshops.shop.pricing.layout.ShopPricingValueParsers;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicPricingService;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicStateStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPricingConfigParserTest {

    @Test
    void load_legacy_entries_registers_valid_materials_only() throws IOException {
        Fixture fixture = fixture();

        YamlConfiguration root = new YamlConfiguration();
        root.set("main-menu.title", "Shop");
        root.set("DIAMOND.buy", 10.0D);
        root.set("DIAMOND.sell", 5.0D);
        root.set("NOT_A_MATERIAL.buy", 3.0D);

        fixture.parser.loadLegacyEntries(root, "test-source");

        assertTrue(fixture.priceMap.containsKey("DIAMOND"));
        assertFalse(fixture.priceMap.containsKey("NOT_A_MATERIAL"));
    }

    @Test
    void parse_rotations_loads_options_and_honors_saved_active_option() throws IOException {
        Fixture fixture = fixture();

        YamlConfiguration root = new YamlConfiguration();
        root.set("rotations.daily.interval", "2h");
        root.set("rotations.daily.mode", "random");
        root.set("rotations.daily.default-option", "a");
        root.createSection("rotations.daily.options.a");
        root.set("rotations.daily.options.a.weight", -5.0D);
        root.createSection("rotations.daily.options.b");
        root.set("rotations.daily.options.b.weight", 1.5D);

        fixture.store.set("rotations.daily", "b");

        Map<String, ShopRotationDefinition> definitions = new LinkedHashMap<>();
        Map<String, String> active = new LinkedHashMap<>();

        fixture.parser.parseRotations(root, definitions, active, "test-source");

        ShopRotationDefinition definition = definitions.get("daily");
        assertNotNull(definition);
        assertEquals(2, definition.options().size());
        assertEquals("b", active.get("daily"));
        assertEquals(0.0D, definition.option("a").orElseThrow().weight(), 1e-9);
        assertEquals(1.5D, definition.option("b").orElseThrow().weight(), 1e-9);
    }

    private static Fixture fixture() throws IOException {
        Logger logger = Logger.getLogger("ShopPricingConfigParserTest");
        Path tempDir = Files.createTempDirectory("shop-pricing-config-parser-test");

        ShopDynamicStateStore store = new ShopDynamicStateStore(tempDir.resolve("shop-dynamic.yml").toFile(), logger);
        store.load();

        Map<String, ShopManagedPriceEntry> priceMap = new LinkedHashMap<>();
        ShopDynamicPricingService dynamicService = new ShopDynamicPricingService(
                priceMap,
                store,
                DynamicPricingConfiguration.defaults(),
                logger);

        ShopPricingConfigParser parser = new ShopPricingConfigParser(
                logger,
                new ShopPricingLayoutSupport(logger),
                new ShopPricingValueParsers(logger),
                dynamicService,
                store);

        return new Fixture(priceMap, store, parser);
    }

    private record Fixture(Map<String, ShopManagedPriceEntry> priceMap,
                           ShopDynamicStateStore store,
                           ShopPricingConfigParser parser) {
    }
}
