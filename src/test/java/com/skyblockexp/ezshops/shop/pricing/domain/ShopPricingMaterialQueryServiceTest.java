package com.skyblockexp.ezshops.shop.pricing.domain;

import com.skyblockexp.ezshops.shop.ShopPrice;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPricingMaterialQueryServiceTest {

    @Test
    void filters_buyable_materials_and_sorts_results() {
        Map<String, ShopManagedPriceEntry> map = new LinkedHashMap<>();
        map.put("STONE", new ShopManagedPriceEntry(new ShopPrice(-1.0D, 2.0D), null, 1.0D));
        map.put("DIAMOND", new ShopManagedPriceEntry(new ShopPrice(10.0D, 5.0D), null, 1.0D));
        map.put("APPLE", new ShopManagedPriceEntry(new ShopPrice(2.0D, 1.0D), null, 1.0D));

        ShopPricingMaterialQueryService service = new ShopPricingMaterialQueryService(map);

        Collection<Material> buyable = service.filterMaterials(ShopPrice::canBuy);

        assertEquals(2, buyable.size());
        assertEquals(java.util.List.of(Material.APPLE, Material.DIAMOND), java.util.List.copyOf(buyable));
    }

    @Test
    void configured_materials_excludes_invalid_keys() {
        Map<String, ShopManagedPriceEntry> map = new LinkedHashMap<>();
        map.put("DIAMOND", new ShopManagedPriceEntry(new ShopPrice(10.0D, 5.0D), null, 1.0D));
        map.put("NOT_A_REAL_MATERIAL", new ShopManagedPriceEntry(new ShopPrice(1.0D, 1.0D), null, 1.0D));

        ShopPricingMaterialQueryService service = new ShopPricingMaterialQueryService(map);

        Set<Material> configured = service.getConfiguredMaterials();

        assertTrue(configured.contains(Material.DIAMOND));
        assertFalse(configured.contains(Material.STONE));
        assertEquals(1, configured.size());
    }
}
