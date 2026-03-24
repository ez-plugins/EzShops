package com.skyblockexp.ezshops.shop.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class ShopTemplateCategoryBuilderTest {

    @Test
    public void buildCreatesCategoryWithPropertiesAndItems() {
        ShopTemplateCategoryBuilder builder = new ShopTemplateCategoryBuilder("building");
        builder.property("name", "Building");

        ShopItem item = ShopItem.builder("dirt").material("DIRT").amount(32).buy(64).sell(32).build();
        builder.addItem("dirt", item);

        ShopTemplateCategory cat = builder.build();

        assertEquals("building", cat.id());
        assertEquals("Building", cat.properties().get("name"));
        assertTrue(cat.items().containsKey("dirt"));
        Map<String, Object> dirtProps = cat.items().get("dirt");
        assertEquals("DIRT", dirtProps.get("material"));
        assertEquals(32, Integer.parseInt(String.valueOf(dirtProps.get("amount"))));
    }

    @Test
    public void buildProducesIndependentCopies() {
        ShopTemplateCategoryBuilder builder = new ShopTemplateCategoryBuilder("tools");
        builder.property("name", "Tools");
        ShopItem item = ShopItem.builder("iron").material("IRON_INGOT").amount(5).buy(10).sell(5).build();
        builder.addItem("iron", item);

        ShopTemplateCategory first = builder.build();
        // Modify builder after building
        builder.property("name", "Changed");
        ShopItem item2 = ShopItem.builder("stick").material("STICK").amount(1).buy(1).sell(0).build();
        builder.addItem("stick", item2);

        ShopTemplateCategory second = builder.build();

        // First should not contain the new stick item and should keep original name
        assertEquals("Tools", first.properties().get("name"));
        assertFalse(first.items().containsKey("stick"));

        // Second should reflect the changes
        assertEquals("Changed", second.properties().get("name"));
        assertTrue(second.items().containsKey("stick"));
    }
}
