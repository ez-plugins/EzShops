package com.skyblockexp.ezshops.shop.pricing.state;

import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopDynamicSettings;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopManagedPriceEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopDynamicPricingServiceTest {

    @Test
    void parse_dynamic_settings_returns_null_when_disabled_or_missing() throws IOException {
        Fixture fixture = fixture(DynamicPricingConfiguration.disabled());

        YamlConfiguration noDynamic = new YamlConfiguration();
        noDynamic.set("buy", 10.0D);

        assertNull(fixture.service.parseDynamicSettings(noDynamic, "DIAMOND"));

        Fixture enabledFixture = fixture(DynamicPricingConfiguration.defaults());
        assertNull(enabledFixture.service.parseDynamicSettings(noDynamic, "DIAMOND"));
    }

    @Test
    void parse_dynamic_settings_sanitizes_invalid_values() throws IOException {
        Fixture fixture = fixture(DynamicPricingConfiguration.defaults());

        YamlConfiguration section = new YamlConfiguration();
        section.set("dynamic-pricing.enabled", true);
        section.set("dynamic-pricing.starting-multiplier", "bad");
        section.set("dynamic-pricing.min-multiplier", -3.0D);
        section.set("dynamic-pricing.max-multiplier", 0.0D);
        section.set("dynamic-pricing.buy-change", -1.0D);
        section.set("dynamic-pricing.sell-change", "invalid");

        ShopDynamicSettings settings = fixture.service.parseDynamicSettings(section, "DIAMOND");
        assertNotNull(settings);
        assertEquals(1.0D, settings.startingMultiplier(), 1e-9);
        assertEquals(0.5D, settings.minMultiplier(), 1e-9);
        assertEquals(3.0D, settings.maxMultiplier(), 1e-9);
        assertEquals(0.0D, settings.buyChange(), 1e-9);
        assertEquals(0.0D, settings.sellChange(), 1e-9);
    }

    @Test
    void register_price_uses_saved_multiplier_and_supports_reset_and_cleanup() throws IOException {
        Fixture fixture = fixture(DynamicPricingConfiguration.defaults());
        ShopDynamicSettings dynamic = new ShopDynamicSettings(1.0D, 0.5D, 3.0D, 0.1D, 0.1D);

        fixture.store.set("DIAMOND", 1.6D);
        fixture.service.registerPrice("DIAMOND", new ShopPrice(10.0D, 5.0D), dynamic);

        ShopManagedPriceEntry entry = fixture.map.get("DIAMOND");
        assertNotNull(entry);
        assertEquals(1.6D, entry.multiplier(), 1e-9);

        assertTrue(fixture.service.setPriceMultiplierForTesting("DIAMOND", 2.0D));
        assertTrue(entry.multiplier() > 1.6D);

        assertTrue(fixture.service.resetDynamicPricing("DIAMOND"));
        assertEquals(1.0D, entry.multiplier(), 1e-9);

        fixture.store.set("unknown", 2.2D);
        fixture.store.set("rotations.daily", "a");
        fixture.store.set("rotations.weekly", "b");
        fixture.service.cleanupDynamicState(Map.of("daily", Set.of("a")));

        assertFalse(fixture.store.isSet("unknown"));
        assertTrue(fixture.store.isSet("rotations.daily"));
        assertFalse(fixture.store.isSet("rotations.weekly"));
    }

    private static Fixture fixture(DynamicPricingConfiguration configuration) throws IOException {
        Path tempDir = Files.createTempDirectory("dynamic-pricing-service-test");
        ShopDynamicStateStore store = new ShopDynamicStateStore(tempDir.resolve("shop-dynamic.yml").toFile(),
                Logger.getLogger("ShopDynamicPricingServiceTest"));
        store.load();

        Map<String, ShopManagedPriceEntry> map = new LinkedHashMap<>();
        ShopDynamicPricingService service = new ShopDynamicPricingService(
                map,
                store,
                configuration,
                Logger.getLogger("ShopDynamicPricingServiceTest"));
        return new Fixture(map, store, service);
    }

    private record Fixture(Map<String, ShopManagedPriceEntry> map,
                           ShopDynamicStateStore store,
                           ShopDynamicPricingService service) {
    }
}
