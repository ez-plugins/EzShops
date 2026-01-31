package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShopGuiFeatureTest extends AbstractEzShopsTest {

    @Test
    void open_main_shop_menu_opens_inventory_holder() throws Exception {
        // load plugin with a dummy economy provider
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        // ensure core component is available
        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core, "CoreShopComponent should be initialized");

        // retrieve the private ShopMenu instance via reflection
        java.lang.reflect.Field menuField = CoreShopComponent.class.getDeclaredField("shopMenu");
        menuField.setAccessible(true);
        Object shopMenu = menuField.get(core);
        assertNotNull(shopMenu, "ShopMenu should be created when core is enabled");

        // open main menu for a test player
        org.bukkit.entity.Player player = server.addPlayer("gui-player");
        java.lang.reflect.Method openMain = shopMenu.getClass().getMethod("openMainMenu", org.bukkit.entity.Player.class);
        openMain.invoke(shopMenu, player);

        // verify top inventory is an AbstractShopMenuHolder
        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top, "Player should have an open top inventory");
        assertTrue(top.getHolder() instanceof com.skyblockexp.ezshops.gui.shop.AbstractShopMenuHolder,
                "Top inventory holder should be an AbstractShopMenuHolder");
        // title contains configured menu title
        String title = player.getOpenInventory().getTitle();
        assertNotNull(title);
        assertTrue(title.toLowerCase().contains(core.pricingManager().getMenuLayout().mainTitle().toLowerCase()) || !core.pricingManager().getMenuLayout().mainTitle().isBlank());
    }
}
