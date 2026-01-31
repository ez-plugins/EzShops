package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.gui.ShopMenu;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShopConfigCategoriesTest extends AbstractEzShopsTest {

    @Test
    void categories_disabled_removes_shop_menu() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        // disable core and change config to disable categories entirely
        core.disable();
        plugin.getConfig().set("categories.enabled", false);
        plugin.getConfig().set("categories.single-list-when-disabled", false);

        // re-enable core to pick up new config
        core.enable(plugin);

        // reflectively inspect shopMenu field to ensure it is null
        java.lang.reflect.Field menuField = CoreShopComponent.class.getDeclaredField("shopMenu");
        menuField.setAccessible(true);
        Object shopMenu = menuField.get(core);
        assertNull(shopMenu, "shopMenu should be null when categories disabled and single-list not enabled");
    }

    @Test
    void single_list_when_disabled_shows_flat_list_menu() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        core.disable();
        plugin.getConfig().set("categories.enabled", false);
        plugin.getConfig().set("categories.single-list-when-disabled", true);
        core.enable(plugin);

        java.lang.reflect.Field menuField = CoreShopComponent.class.getDeclaredField("shopMenu");
        menuField.setAccessible(true);
        Object shopMenu = menuField.get(core);
        assertNotNull(shopMenu, "shopMenu should be present when single-list-when-disabled is true");

        // inspect displayMode on ShopMenu
        java.lang.reflect.Field displayField = shopMenu.getClass().getDeclaredField("displayMode");
        displayField.setAccessible(true);
        Object displayMode = displayField.get(shopMenu);
        assertEquals(ShopMenu.DisplayMode.FLAT_LIST, displayMode, "ShopMenu should be initialized in FLAT_LIST mode");
    }
}
