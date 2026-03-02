package com.skyblockexp.ezshops.shop.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ShopTemplateCategoryTest {

    @Test
    public void constructorCopiesAndIsImmutable() {
        Map<String, Object> props = new HashMap<>();
        props.put("name", "Building");

        Map<String, Map<String, Object>> items = new LinkedHashMap<>();
        Map<String, Object> itemProps = new HashMap<>();
        itemProps.put("material", "DIRT");
        items.put("dirt", itemProps);

        ShopTemplateCategory cat = new ShopTemplateCategory("building", props, items);

        // Mutate originals
        props.put("name", "Changed");
        itemProps.put("material", "STONE");

        // Category should keep original values (defensive copy)
        assertEquals("Building", cat.properties().get("name"));
        assertEquals("DIRT", cat.items().get("dirt").get("material"));

        // Returned property map should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> cat.properties().put("x", "y"));
        // Item inner-maps should be unmodifiable (values were copied with Map.copyOf)
        assertThrows(UnsupportedOperationException.class, () -> cat.items().get("dirt").put("x", "y"));
    }

    @Test
    public void toMapIncludesItemsAndProperties() {
        Map<String, Object> props = Map.of("name", "Tools");
        Map<String, Object> itemProps = Map.of("material", "IRON_INGOT", "amount", 5);
        Map<String, Map<String, Object>> items = Map.of("iron", itemProps);

        ShopTemplateCategory cat = new ShopTemplateCategory("tools", props, items);
        Map<String, Object> out = cat.toMap();

        assertEquals("Tools", out.get("name"));
        assertTrue(out.containsKey("items"));
        Object itemsObj = out.get("items");
        assertTrue(itemsObj instanceof Map<?, ?>);
        Map<?, ?> itemsMap = (Map<?, ?>) itemsObj;
        assertTrue(itemsMap.containsKey("iron"));
        Object ironObj = itemsMap.get("iron");
        assertTrue(ironObj instanceof Map<?, ?>);
        Map<?, ?> ironMap = (Map<?, ?>) ironObj;
        assertEquals("IRON_INGOT", String.valueOf(ironMap.get("material")));
        assertEquals(5, Integer.parseInt(String.valueOf(ironMap.get("amount"))));
    }

    @Test
    public void toMapOmitsEmptyItems() {
        ShopTemplateCategory cat = new ShopTemplateCategory("empty", Map.of("name", "Empty"), Map.of());
        Map<String, Object> out = cat.toMap();
        assertEquals("Empty", out.get("name"));
        assertFalse(out.containsKey("items"));
    }
}
