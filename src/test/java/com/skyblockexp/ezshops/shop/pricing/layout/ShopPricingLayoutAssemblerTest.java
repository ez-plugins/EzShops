package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopRotationDefinition;
import com.skyblockexp.ezshops.shop.ShopRotationOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPricingLayoutAssemblerTest {

    @Test
    void load_menu_layout_builds_category_and_applies_default_back_button() {
        ShopPricingLayoutAssembler assembler = newAssembler();

        YamlConfiguration root = new YamlConfiguration();
        root.set("main-menu.title", "&aMain");
        root.set("main-menu.size", 54);
        root.set("categories.blocks.name", "&eBlocks");
        root.set("categories.blocks.slot", 3);
        root.set("categories.blocks.items.stone.slot", 10);
        root.set("categories.blocks.items.stone.material", "STONE");
        root.set("categories.blocks.items.stone.buy", 5.0D);
        root.set("categories.blocks.items.stone.sell", 2.0D);

        Map<String, ShopRotationDefinition> rotations = new LinkedHashMap<>();
        Map<String, String> active = new LinkedHashMap<>();

        ShopMenuLayout layout = assembler.loadMenuLayout(root, rotations, active, "test-source", simpleItemParser());

        assertEquals("Main", ChatColor.stripColor(layout.mainTitle()));
        assertEquals(1, layout.categories().size());
        assertEquals(1, layout.defaultCategoryButtons().size());
        assertEquals(1, layout.categories().get(0).items().size());
        assertEquals(Material.STONE, layout.categories().get(0).items().get(0).material());
    }

    @Test
    void rotation_category_uses_active_or_default_option_and_updates_invalid_active() {
        ShopPricingLayoutAssembler assembler = newAssembler();

        YamlConfiguration root = new YamlConfiguration();
        root.set("categories.rot.name", "Rot");
        root.set("categories.rot.rotation-group", "daily");
        root.set("categories.rot.rotation-defaults.items.base.slot", 5);
        root.set("categories.rot.rotation-defaults.items.base.material", "DIAMOND");
        root.set("categories.rot.rotation-defaults.items.base.buy", 10.0D);
        root.set("categories.rot.rotation-defaults.items.base.sell", 4.0D);

        ShopRotationDefinition def = new ShopRotationDefinition(
                "daily",
                null,
                null,
                List.of(
                        new ShopRotationOption("a", null, null, Map.of(), 1.0D),
                        new ShopRotationOption("b", null, null, Map.of(), 1.0D)),
                "a");

        Map<String, ShopRotationDefinition> rotations = new LinkedHashMap<>();
        rotations.put("daily", def);

        Map<String, String> active = new LinkedHashMap<>();
        active.put("daily", "missing-option");

        ShopMenuLayout layout = assembler.loadMenuLayout(root, rotations, active, "test-source", simpleItemParser());

        ShopMenuLayout.Category category = layout.categories().get(0);
        assertNotNull(category.rotation());
        assertEquals("a", category.rotation().optionId());
        assertEquals("a", active.get("daily"));
    }

    @Test
    void unknown_rotation_group_falls_back_to_static_items() {
        ShopPricingLayoutAssembler assembler = newAssembler();

        YamlConfiguration root = new YamlConfiguration();
        root.set("categories.rot.name", "Rot");
        root.set("categories.rot.rotation-group", "unknown");
        root.set("categories.rot.items.fallback.slot", 8);
        root.set("categories.rot.items.fallback.material", "STONE");
        root.set("categories.rot.items.fallback.buy", 2.0D);
        root.set("categories.rot.items.fallback.sell", 1.0D);

        ShopMenuLayout layout = assembler.loadMenuLayout(root, Map.of(), new LinkedHashMap<>(), "test-source", simpleItemParser());

        ShopMenuLayout.Category category = layout.categories().get(0);
        assertNotNull(category);
        assertFalse(category.items().isEmpty());
        assertEquals(Material.STONE, category.items().get(0).material());
    }

    private static ShopPricingLayoutAssembler newAssembler() {
        Logger logger = Logger.getLogger("ShopPricingLayoutAssemblerTest");
        return new ShopPricingLayoutAssembler(logger, new ShopPricingLayoutSupport(logger), new ShopPricingValueParsers(logger));
    }

    private static ShopPricingLayoutAssembler.ItemParser simpleItemParser() {
        return (contextPrefix, itemId, section, menuSize, parsers) -> {
            int slot = section.getInt("slot", 0);
            Material material = Material.matchMaterial(section.getString("material", "STONE"), false);
            if (material == null) {
                material = Material.STONE;
            }
            ShopPrice price = new ShopPrice(section.getDouble("buy", 1.0D), section.getDouble("sell", 1.0D));
            return new ShopMenuLayout.Item(
                    itemId,
                    material,
                    new ShopMenuLayout.ItemDecoration(material, 1, itemId, List.of()),
                    slot,
                    0,
                    1,
                    64,
                    price,
                    ShopMenuLayout.ItemType.MATERIAL,
                    null,
                    Map.of(),
                    0);
        };
    }
}
