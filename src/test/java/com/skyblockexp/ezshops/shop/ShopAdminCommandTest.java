package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ShopAdminCommandTest extends AbstractEzShopsTest {

    @Test
    void resetdynamic_without_permission_does_not_reset() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertTrue(plugin.isEnabled());

        CoreShopComponent core = plugin.getCoreShopComponent();
        ShopPricingManager pm = core.pricingManager();

        Material m = Material.WHEAT_SEEDS;
        assertTrue(pm.getPrice(m).isPresent());
        double before = pm.getPrice(m).get().buyPrice();

        // cause dynamic multiplier to change
        pm.handlePurchase(m, 5);
        double changed = pm.getPrice(m).get().buyPrice();
        assertNotEquals(before, changed);

        Player player = server.addPlayer("noperm");
        // dispatch without granting permission
        boolean dispatched = server.dispatchCommand(player, "shop admin resetdynamic wheat_seeds");
        assertTrue(dispatched);

        double after = pm.getPrice(m).get().buyPrice();
        assertEquals(changed, after, 0.000001, "Price should not have been reset without permission");
    }

    @Test
    void resetdynamic_with_permission_resets_single_item() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertTrue(plugin.isEnabled());

        CoreShopComponent core = plugin.getCoreShopComponent();
        ShopPricingManager pm = core.pricingManager();

        Material m = Material.WHEAT_SEEDS;
        assertTrue(pm.getPrice(m).isPresent());
        double before = pm.getPrice(m).get().buyPrice();

        pm.handlePurchase(m, 5);
        double changed = pm.getPrice(m).get().buyPrice();
        assertNotEquals(before, changed);

        Player player = server.addPlayer("admin");
        player.addAttachment(plugin, "ezshops.shop.admin.resetdynamic", true);

        boolean dispatched = server.dispatchCommand(player, "shop admin resetdynamic wheat_seeds");
        assertTrue(dispatched);

        double after = pm.getPrice(m).get().buyPrice();
        assertEquals(before, after, 0.000001, "Price should be reset to original after admin resetdynamic");
    }

    @Test
    void resetdynamic_all_resets_multiple_items() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertTrue(plugin.isEnabled());

        CoreShopComponent core = plugin.getCoreShopComponent();
        ShopPricingManager pm = core.pricingManager();

        Material a = Material.WHEAT_SEEDS;
        Material b = Material.CARROT;
        assertTrue(pm.getPrice(a).isPresent());
        assertTrue(pm.getPrice(b).isPresent());

        double beforeA = pm.getPrice(a).get().buyPrice();
        double beforeB = pm.getPrice(b).get().buyPrice();

        pm.handlePurchase(a, 3);
        pm.handlePurchase(b, 8);

        double changedA = pm.getPrice(a).get().buyPrice();
        double changedB = pm.getPrice(b).get().buyPrice();
        boolean anyChanged = Double.compare(beforeA, changedA) != 0 || Double.compare(beforeB, changedB) != 0;
        assertTrue(anyChanged, "At least one price should have changed after simulated purchases");

        Player player = server.addPlayer("superadmin");
        player.addAttachment(plugin, "ezshops.shop.admin.resetdynamic", true);

        boolean dispatched = server.dispatchCommand(player, "shop admin resetdynamic all");
        assertTrue(dispatched);

        double afterA = pm.getPrice(a).get().buyPrice();
        double afterB = pm.getPrice(b).get().buyPrice();
        assertEquals(beforeA, afterA, 0.000001);
        assertEquals(beforeB, afterB, 0.000001);
    }
}
