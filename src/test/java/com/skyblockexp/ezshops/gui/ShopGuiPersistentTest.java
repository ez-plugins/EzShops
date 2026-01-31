package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.common.CompatibilityUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShopGuiPersistentTest extends AbstractEzShopsTest {

    @Test
    void category_items_have_persistent_category_key_and_navigation_buttons_have_action_key() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        java.lang.reflect.Field menuField = CoreShopComponent.class.getDeclaredField("shopMenu");
        menuField.setAccessible(true);
        Object shopMenu = menuField.get(core);
        assertNotNull(shopMenu);

        // Access the inventory composer
        java.lang.reflect.Field compField = shopMenu.getClass().getDeclaredField("inventoryComposer");
        compField.setAccessible(true);
        Object composer = compField.get(shopMenu);
        assertNotNull(composer);

        org.bukkit.entity.Player player = server.addPlayer("persist-player");

        // open main menu
        java.lang.reflect.Method openMain = shopMenu.getClass().getMethod("openMainMenu", org.bukkit.entity.Player.class);
        openMain.invoke(shopMenu, player);

        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top);

        // Find a category item that has persistent category key
        NamespacedKey categoryKey = new NamespacedKey(plugin, "shop_category");
        NamespacedKey actionKey = new NamespacedKey(plugin, "shop_action");

        boolean foundCategory = false;
        for (int i = 0; i < top.getSize(); i++) {
            org.bukkit.inventory.ItemStack it = top.getItem(i);
            if (it == null) continue;
            var meta = it.getItemMeta();
            var container = CompatibilityUtil.getPersistentDataContainer(meta);
            if (CompatibilityUtil.hasKey(container, categoryKey, PersistentDataType.STRING)) {
                String value = CompatibilityUtil.get(container, categoryKey, PersistentDataType.STRING);
                assertNotNull(value);
                foundCategory = true;
            }
            if (CompatibilityUtil.hasKey(container, actionKey, PersistentDataType.STRING)) {
                String val = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
                assertTrue("previous".equalsIgnoreCase(val) || "next".equalsIgnoreCase(val) || "back".equalsIgnoreCase(val));
            }
        }

        assertTrue(foundCategory, "Expected at least one category with persistent category key");
    }
}
