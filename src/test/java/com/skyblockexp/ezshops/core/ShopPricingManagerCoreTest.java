package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import org.bukkit.Material;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

@Disabled("Relies on MockBukkit/Paper runtime compatibility; covered by narrower unit tests in pricing packages")
public class ShopPricingManagerCoreTest extends AbstractEzShopsTest {

    @Test
    void pricing_manager_defaults_and_query_methods() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = com.skyblockexp.ezshops.bootstrap.EzShopsRegistry.current().getCoreShopComponent();
        assertNotNull(core);

        ShopPricingManager pricingManager = core.pricingManager();
        assertNotNull(pricingManager);

        // pricing data may be present in test resources; ensure method is callable
        assertNotNull(pricingManager.isEmpty());

        Collection<Material> materials = pricingManager.getConfiguredMaterials();
        assertNotNull(materials);

        Object layout = pricingManager.getMenuLayout();
        assertNotNull(layout);

        double res = pricingManager.estimateBulkTotal(Material.DIAMOND, 0, ShopTransactionType.BUY);
        assertEquals(-1.0D, res);

        assertFalse(pricingManager.setActiveRotationOption("no-such-rotation", "opt"));
    }
}

