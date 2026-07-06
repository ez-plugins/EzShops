package com.skyblockexp.ezshops.shop.pricing.layout;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopItemDataMapperTest {

    @Test
    void read_item_data_ignores_non_sections() {
        YamlConfiguration section = new YamlConfiguration();
        section.set("valid.material", "DIAMOND");
        section.set("valid.amount", 2);
        section.set("bad", "not-a-section");

        Map<String, Map<String, Object>> data = ShopItemDataMapper.readItemData(section, Logger.getLogger("test"));

        assertEquals(1, data.size());
        assertTrue(data.containsKey("valid"));
        assertEquals("DIAMOND", data.get("valid").get("material"));
    }

    @Test
    void merge_item_maps_applies_nested_overrides_without_mutating_inputs() {
        Map<String, Object> defaultMeta = new LinkedHashMap<>();
        defaultMeta.put("name", "Default");
        defaultMeta.put("lore", List.of("a"));

        Map<String, Object> defaultItem = new LinkedHashMap<>();
        defaultItem.put("material", "STONE");
        defaultItem.put("meta", defaultMeta);

        Map<String, Map<String, Object>> defaults = new LinkedHashMap<>();
        defaults.put("item", defaultItem);

        Map<String, Object> overrideMeta = new LinkedHashMap<>();
        overrideMeta.put("name", "Override");

        Map<String, Object> overrideItem = new LinkedHashMap<>();
        overrideItem.put("meta", overrideMeta);
        overrideItem.put("amount", 3);

        Map<String, Map<String, Object>> overrides = new LinkedHashMap<>();
        overrides.put("item", overrideItem);

        Map<String, Map<String, Object>> merged = ShopItemDataMapper.mergeItemMaps(defaults, overrides);

        assertEquals("STONE", merged.get("item").get("material"));
        assertEquals(3, merged.get("item").get("amount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mergedMeta = (Map<String, Object>) merged.get("item").get("meta");
        assertEquals("Override", mergedMeta.get("name"));
        assertEquals(List.of("a"), mergedMeta.get("lore"));

        @SuppressWarnings("unchecked")
        Map<String, Object> originalDefaultMeta = (Map<String, Object>) defaults.get("item").get("meta");
        assertEquals("Default", originalDefaultMeta.get("name"));
    }

    @Test
    void create_section_from_map_builds_nested_configuration() {
        Map<String, Object> nestedMeta = new LinkedHashMap<>();
        nestedMeta.put("name", "Book");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("material", "BOOK");
        values.put("amount", 5);
        values.put("meta", nestedMeta);

        ConfigurationSection section = ShopItemDataMapper.createSectionFromMap(values);

        assertEquals("BOOK", section.getString("material"));
        assertEquals(5, section.getInt("amount"));
        ConfigurationSection meta = section.getConfigurationSection("meta");
        assertNotNull(meta);
        assertEquals("Book", meta.getString("name"));
    }
}
