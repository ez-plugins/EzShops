package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPriceType;
import com.skyblockexp.ezshops.shop.ShopRotationDefinition;
import com.skyblockexp.ezshops.shop.ShopRotationOption;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPricingVisibilityServiceTest {

    @Test
    void detects_visible_material_and_price_key_in_layout() {
        ShopMenuLayout layout = new ShopMenuLayout(
                "Main",
                54,
                null,
                List.of(),
                List.of(),
                List.of(new ShopMenuLayout.Category(
                        "blocks",
                        "Blocks",
                        decoration(Material.CHEST, "Blocks"),
                        0,
                        "Blocks",
                        54,
                        null,
                        List.of(),
                        true,
                        List.of(itemWithPriceId("diamond-entry", Material.DIAMOND, "custom_diamond")),
                        null,
                        null)));

        ShopPricingVisibilityService service = new ShopPricingVisibilityService();

        assertTrue(service.isVisibleInMenu(layout, Material.DIAMOND));
        assertTrue(service.isVisibleInMenu(layout, "custom_diamond"));
        assertFalse(service.isVisibleInMenu(layout, Material.EMERALD));
        assertFalse(service.isVisibleInMenu(layout, "missing_key"));
    }

    @Test
    void detects_price_key_declared_in_rotation_options() {
        ShopMenuLayout.Item rotatingItem = itemWithPriceId("rot-diamond", Material.DIAMOND, "rot_key");
        ShopPricingRotationBinding binding = new ShopPricingRotationBinding(
                "daily",
                null,
                null,
                Map.of("option-a", List.of(rotatingItem)));

        ShopPricingCategoryTemplate template = new ShopPricingCategoryTemplate(
                "cat",
                "Category",
                decoration(Material.CHEST, "Cat"),
                0,
                "Category",
                54,
                null,
                List.of(),
                true,
                List.of(),
                binding,
                null);

        ShopRotationDefinition rotationDefinition = new ShopRotationDefinition(
                "daily",
                null,
                null,
                List.of(new ShopRotationOption("option-a", null, null, Map.of(), 1.0D)),
                "option-a");

        ShopPricingVisibilityService service = new ShopPricingVisibilityService();

        assertTrue(service.isPartOfRotation("rot_key", List.of(template), Map.of("daily", rotationDefinition)));
        assertFalse(service.isPartOfRotation("other_key", List.of(template), Map.of("daily", rotationDefinition)));
    }

    private static ShopMenuLayout.Item itemWithPriceId(String id, Material material, String priceId) {
        return new ShopMenuLayout.Item(
                id,
                material,
                decoration(material, id),
                0,
                0,
                1,
                64,
                new ShopPrice(10.0D, 5.0D),
                ShopMenuLayout.ItemType.MATERIAL,
                null,
                Map.of(),
                0,
                ShopPriceType.STATIC,
                List.of(),
                List.of(),
                Boolean.TRUE,
                priceId);
    }

    private static ShopMenuLayout.ItemDecoration decoration(Material material, String name) {
        return new ShopMenuLayout.ItemDecoration(material, 1, name, List.of());
    }
}
