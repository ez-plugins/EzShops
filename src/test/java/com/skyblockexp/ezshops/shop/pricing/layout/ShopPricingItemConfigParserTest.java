package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPriceType;
import com.skyblockexp.ezshops.shop.pricing.domain.ShopManagedPriceEntry;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicPricingService;
import com.skyblockexp.ezshops.shop.pricing.state.ShopDynamicStateStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPricingItemConfigParserTest {

    @Test
    void parse_item_returns_null_for_invalid_material_or_slot_or_minion_head() throws IOException {
        Fixture fixture = fixture();

        YamlConfiguration invalidMaterial = new YamlConfiguration();
        invalidMaterial.set("material", "NOT_A_MATERIAL");
        invalidMaterial.set("slot", 0);
        invalidMaterial.set("buy", 1.0D);
        assertNull(fixture.parser.parseItem("ctx", "bad", invalidMaterial, 54, fixture.parsers));

        YamlConfiguration invalidSlot = new YamlConfiguration();
        invalidSlot.set("material", "STONE");
        invalidSlot.set("slot", 99);
        invalidSlot.set("buy", 1.0D);
        assertNull(fixture.parser.parseItem("ctx", "bad-slot", invalidSlot, 54, fixture.parsers));

        YamlConfiguration minionHead = new YamlConfiguration();
        minionHead.set("type", "MINION_HEAD");
        minionHead.set("material", "STONE");
        minionHead.set("slot", 1);
        minionHead.set("buy", 1.0D);
        assertNull(fixture.parser.parseItem("ctx", "head", minionHead, 54, fixture.parsers));
    }

    @Test
    void parse_item_registers_price_and_respects_custom_price_id_and_price_type() throws IOException {
        Fixture fixture = fixture();

        YamlConfiguration section = new YamlConfiguration();
        section.set("material", "DIAMOND");
        section.set("slot", 5);
        section.set("buy", 20.0D);
        section.set("sell", 9.0D);
        section.set("price-id", "diamond_special");
        section.set("price-type", "STOCK_MARKET");
        section.set("icon", "EMERALD");
        section.set("icon-amount", 3);

        ShopMenuLayout.Item item = fixture.parser.parseItem("categories.test.items", "diamond", section, 54, fixture.parsers);

        assertNotNull(item);
        assertEquals("diamond_special", item.priceId());
        assertEquals(ShopPriceType.STOCK_MARKET, item.priceType());
        assertEquals(Material.EMERALD, item.display().material());
        assertTrue(fixture.priceMap.containsKey("diamond_special"));
    }

    @Test
    void parse_item_sets_menu_item_type_and_prefers_non_material_type_for_same_material() throws IOException {
        Fixture fixture = fixture();

        YamlConfiguration first = new YamlConfiguration();
        first.set("material", "TRIPWIRE_HOOK");
        first.set("type", "MATERIAL");
        first.set("slot", 1);
        first.set("buy", 2.0D);
        first.set("sell", 1.0D);

        YamlConfiguration second = new YamlConfiguration();
        second.set("material", "TRIPWIRE_HOOK");
        second.set("type", "VOTE_CRATE_KEY");
        second.set("slot", 2);
        second.set("buy", 3.0D);
        second.set("sell", 1.0D);

        fixture.parser.parseItem("ctx", "first", first, 54, fixture.parsers);
        fixture.parser.parseItem("ctx", "second", second, 54, fixture.parsers);

        assertEquals(ShopMenuLayout.ItemType.VOTE_CRATE_KEY, fixture.menuTypes.get(Material.TRIPWIRE_HOOK));
    }

    @Test
    void parse_item_spawner_requires_valid_entity() throws IOException {
        Fixture fixture = fixture();

        YamlConfiguration missingEntity = new YamlConfiguration();
        missingEntity.set("type", "SPAWNER");
        missingEntity.set("material", "SPAWNER");
        missingEntity.set("slot", 1);
        missingEntity.set("buy", 1.0D);
        assertNull(fixture.parser.parseItem("ctx", "spawner1", missingEntity, 54, fixture.parsers));

        YamlConfiguration invalidEntity = new YamlConfiguration();
        invalidEntity.set("type", "SPAWNER");
        invalidEntity.set("material", "SPAWNER");
        invalidEntity.set("slot", 1);
        invalidEntity.set("buy", 1.0D);
        invalidEntity.set("spawner-entity", "NOT_A_REAL_ENTITY");
        assertNull(fixture.parser.parseItem("ctx", "spawner2", invalidEntity, 54, fixture.parsers));

        YamlConfiguration valid = new YamlConfiguration();
        valid.set("type", "SPAWNER");
        valid.set("material", "SPAWNER");
        valid.set("slot", 1);
        valid.set("buy", 1.0D);
        valid.set("sell", 0.5D);
        valid.set("spawner-entity", "ZOMBIE");

        assertNotNull(fixture.parser.parseItem("ctx", "spawner3", valid, 54, fixture.parsers));
    }

    @Test
    void parse_item_supports_list_style_on_buy_commands() throws IOException {
        Fixture fixture = fixture();

        YamlConfiguration section = new YamlConfiguration();
        section.set("material", "PAPER");
        section.set("slot", 1);
        section.set("buy", 10.0D);
        section.set("item-type", "COMMAND");
        section.set("on-buy", java.util.List.of("say one", "say two"));

        ShopMenuLayout.Item item = fixture.parser.parseItem("categories.special.items", "paper", section, 54, fixture.parsers);

        assertNotNull(item);
        assertEquals(java.util.List.of("say one", "say two"), item.buyCommands());
    }

    private static Fixture fixture() throws IOException {
        Logger logger = Logger.getLogger("ShopPricingItemConfigParserTest");
        Path tempDir = Files.createTempDirectory("shop-pricing-item-parser-test");

        Map<String, ShopManagedPriceEntry> priceMap = new LinkedHashMap<>();
        ShopDynamicStateStore stateStore = new ShopDynamicStateStore(tempDir.resolve("shop-dynamic.yml").toFile(), logger);
        stateStore.load();
        ShopDynamicPricingService dynamicService = new ShopDynamicPricingService(
                priceMap,
                stateStore,
                DynamicPricingConfiguration.defaults(),
                logger);

        Map<Material, ShopMenuLayout.ItemType> menuTypes = new LinkedHashMap<>();
        ShopPricingItemConfigParser parser = new ShopPricingItemConfigParser(
                logger,
                new ShopPricingLayoutSupport(logger),
                dynamicService,
                menuTypes);

        return new Fixture(parser, new ShopPricingValueParsers(logger), priceMap, menuTypes);
    }

    private record Fixture(ShopPricingItemConfigParser parser,
                           ShopPricingValueParsers parsers,
                           Map<String, ShopManagedPriceEntry> priceMap,
                           Map<Material, ShopMenuLayout.ItemType> menuTypes) {
    }
}
