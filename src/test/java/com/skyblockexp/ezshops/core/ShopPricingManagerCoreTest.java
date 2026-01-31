package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Collection;

public class ShopPricingManagerCoreTest extends AbstractEzShopsTest {

    @Test
    void pricing_manager_defaults_and_query_methods() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        java.lang.reflect.Field corePricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        corePricingField.setAccessible(true);
        Object pricingManager = corePricingField.get(core);
        assertNotNull(pricingManager);

        Class<?> pmClass = Class.forName("com.skyblockexp.ezshops.shop.ShopPricingManager");
        Method isEmpty = pmClass.getMethod("isEmpty");
        // pricing data may be present in test resources; ensure method is callable
        assertNotNull(isEmpty.invoke(pricingManager));

        Method getConfigured = pmClass.getMethod("getConfiguredMaterials");
        Collection<?> materials = (Collection<?>) getConfigured.invoke(pricingManager);
        assertNotNull(materials);

        Method getMenuLayout = pmClass.getMethod("getMenuLayout");
        Object layout = getMenuLayout.invoke(pricingManager);
        assertNotNull(layout);

        Method estimateBulk = pmClass.getMethod("estimateBulkTotal", Material.class, int.class, Class.forName("com.skyblockexp.ezshops.gui.shop.ShopTransactionType"));
        double res = (Double) estimateBulk.invoke(pricingManager, Material.DIAMOND, 0, Enum.valueOf((Class<Enum>) Class.forName("com.skyblockexp.ezshops.gui.shop.ShopTransactionType"), "BUY"));
        assertEquals(-1.0D, res);

        Method setActive = pmClass.getMethod("setActiveRotationOption", String.class, String.class);
        assertFalse((Boolean) setActive.invoke(pricingManager, "no-such-rotation", "opt"));
    }
}
