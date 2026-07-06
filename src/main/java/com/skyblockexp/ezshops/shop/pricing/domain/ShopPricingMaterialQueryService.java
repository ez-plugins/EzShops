package com.skyblockexp.ezshops.shop.pricing.domain;

import com.skyblockexp.ezshops.shop.ShopPrice;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.bukkit.Material;

/**
 * Read-only queries over configured material price entries.
 */
public final class ShopPricingMaterialQueryService {

    private final Map<String, ShopManagedPriceEntry> priceMap;

    public ShopPricingMaterialQueryService(Map<String, ShopManagedPriceEntry> priceMap) {
        this.priceMap = priceMap;
    }

    public Collection<Material> filterMaterials(Predicate<ShopPrice> predicate) {
        List<Material> materials = new ArrayList<>();
        for (Map.Entry<String, ShopManagedPriceEntry> entry : priceMap.entrySet()) {
            String key = entry.getKey();
            Material material = Material.matchMaterial(key, false);
            if (material != null && predicate.test(entry.getValue().currentPrice())) {
                materials.add(material);
            }
        }
        materials.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return Collections.unmodifiableList(materials);
    }

    public Set<Material> getConfiguredMaterials() {
        List<Material> materials = new ArrayList<>();
        for (String key : priceMap.keySet()) {
            try {
                Material material = Material.matchMaterial(key, false);
                if (material != null) {
                    materials.add(material);
                }
            } catch (Throwable ignored) {
            }
        }
        return Collections.unmodifiableSet(Set.copyOf(materials));
    }
}
