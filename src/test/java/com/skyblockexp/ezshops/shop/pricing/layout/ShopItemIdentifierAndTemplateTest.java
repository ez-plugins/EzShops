package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPriceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopItemIdentifierAndTemplateTest {

    @Test
    void identifier_matcher_supports_id_price_id_material_and_display_prefix() {
        ShopMenuLayout.ItemDecoration display = new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "Diamond Deluxe", List.of());
        ShopMenuLayout.Item item = new ShopMenuLayout.Item(
                "diamond_item",
                Material.DIAMOND,
                display,
                0,
                0,
                1,
                1,
                new ShopPrice(10.0D, 5.0D),
                ShopMenuLayout.ItemType.MATERIAL,
                null,
                Map.of(),
                0,
                ShopPriceType.STATIC,
                List.of(),
                List.of(),
                Boolean.TRUE,
                null);

        ShopMenuLayout.Item withPriceId = new ShopMenuLayout.Item(
                "other",
                Material.STONE,
                new ShopMenuLayout.ItemDecoration(Material.STONE, 1, "Stone", List.of()),
                1,
                0,
                1,
                1,
                new ShopPrice(4.0D, 2.0D),
                ShopMenuLayout.ItemType.MATERIAL,
                null,
                Map.of(),
                0,
                ShopPriceType.STATIC,
                List.of(),
                List.of(),
                Boolean.TRUE,
                "special_price");

        assertTrue(ShopItemIdentifierMatcher.matchesKey(item, "diamond_item"));
        assertTrue(ShopItemIdentifierMatcher.matchesMaterial(item, "diamond"));
        assertTrue(ShopItemIdentifierMatcher.matchesKey(item, "diamond d"));
        assertTrue(ShopItemIdentifierMatcher.matchesKey(withPriceId, "special_price"));

        assertFalse(ShopItemIdentifierMatcher.matchesKey(item, "emerald"));
        assertFalse(ShopItemIdentifierMatcher.matchesKey(item, ""));
        assertFalse(ShopItemIdentifierMatcher.matchesMaterial(null, "diamond"));
    }

    @Test
    void rotation_binding_and_category_template_are_defensively_copied() {
        ShopMenuLayout.Item item = new ShopMenuLayout.Item(
                "a",
                Material.APPLE,
                new ShopMenuLayout.ItemDecoration(Material.APPLE, 1, "Apple", List.of()),
                2,
                0,
                1,
                1,
                new ShopPrice(2.0D, 1.0D),
                ShopMenuLayout.ItemType.MATERIAL,
                null,
                Map.of(),
                0,
                ShopPriceType.STATIC,
                List.of(),
                List.of(),
                Boolean.TRUE,
                null);

        Map<String, List<ShopMenuLayout.Item>> optionItems = new LinkedHashMap<>();
        List<ShopMenuLayout.Item> sourceList = new ArrayList<>();
        sourceList.add(item);
        optionItems.put("optA", sourceList);

        ShopPricingRotationBinding binding = new ShopPricingRotationBinding(
                "daily",
                new ShopMenuLayout.ItemDecoration(Material.CLOCK, 1, "Clock", List.of()),
                "Daily",
                optionItems);

        sourceList.clear();
        assertEquals(1, binding.itemsFor("optA").size());
        assertEquals("daily", binding.groupId());

        List<ShopMenuLayout.ConfigurableButton> buttons = new ArrayList<>();
        buttons.add(new ShopMenuLayout.ConfigurableButton(
                "back",
                0,
                null,
                ShopMenuLayout.ButtonAction.BACK,
                null,
                1.0f,
                1.0f,
                null));

        ShopPricingCategoryTemplate template = new ShopPricingCategoryTemplate(
                "cat",
                "Category",
                new ShopMenuLayout.ItemDecoration(Material.BOOK, 1, "Book", List.of()),
                0,
                "Title",
                27,
                null,
                buttons,
                false,
                List.of(item),
                binding,
                "say hi");

        buttons.clear();
        assertEquals(1, template.buttons().size());
        assertTrue(template.isRotating());
        assertEquals("say hi", template.command());
        assertNotNull(template.rotation());

        assertThrows(NullPointerException.class, () -> new ShopPricingRotationBinding(null, null, null, Map.of()));
        assertThrows(NullPointerException.class,
                () -> new ShopPricingCategoryTemplate(null, "d", null, 0, "m", 27, null, List.of(), false, List.of(), null, null));
    }
}
