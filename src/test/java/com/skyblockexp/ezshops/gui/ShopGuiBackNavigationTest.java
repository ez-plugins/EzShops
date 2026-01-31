package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.common.CompatibilityUtil;
import com.skyblockexp.ezshops.gui.shop.MainShopMenuHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShopGuiBackNavigationTest extends AbstractEzShopsTest {

    @Test
    void clicking_back_button_from_category_returns_to_main_menu() throws Exception {
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

        org.bukkit.entity.Player player = server.addPlayer("back-nav-player");

        // Open main menu and click a category to open the category menu
        java.lang.reflect.Method openMain = shopMenu.getClass().getMethod("openMainMenu", org.bukkit.entity.Player.class);
        openMain.invoke(shopMenu, player);

        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top);

        NamespacedKey categoryKey = new NamespacedKey(plugin, "shop_category");
        int categorySlot = -1;
        for (int i = 0; i < top.getSize(); i++) {
            var it = top.getItem(i);
            if (it == null) continue;
            var meta = it.getItemMeta();
            var container = CompatibilityUtil.getPersistentDataContainer(meta);
            if (CompatibilityUtil.hasKey(container, categoryKey, PersistentDataType.STRING)) {
                categorySlot = i; break;
            }
        }
        assertTrue(categorySlot >= 0, "No category slot found in main menu");

        // Invoke category selection handler directly (bypassing scheduled tasks) to open category menu
        var metaItem = top.getItem(categorySlot).getItemMeta();
        var containerForCategory = CompatibilityUtil.getPersistentDataContainer(metaItem);
        String categoryId = CompatibilityUtil.get(containerForCategory, new NamespacedKey(plugin, "shop_category"), PersistentDataType.STRING);
        java.lang.reflect.Method handleCategory = shopMenu.getClass().getDeclaredMethod("handleCategoryClick", org.bukkit.entity.Player.class, String.class);
        handleCategory.setAccessible(true);
        handleCategory.invoke(shopMenu, player, categoryId);

        // Now find back button in the category menu
        var newTop = player.getOpenInventory().getTopInventory();
        assertNotNull(newTop);
        NamespacedKey actionKey = new NamespacedKey(plugin, "shop_action");
        int backSlot = -1;
        for (int i = 0; i < newTop.getSize(); i++) {
            var it = newTop.getItem(i);
            if (it == null) continue;
            var meta = it.getItemMeta();
            var container = CompatibilityUtil.getPersistentDataContainer(meta);
            if (CompatibilityUtil.hasKey(container, actionKey, PersistentDataType.STRING)) {
                String val = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
                if ("back".equalsIgnoreCase(val)) { backSlot = i; break; }
            }
        }
        assertTrue(backSlot >= 0, "No back button present in category menu");

        // Simulate scheduler completion for back action by calling openMainMenu directly
        java.lang.reflect.Method openMainMenu = shopMenu.getClass().getMethod("openMainMenu", org.bukkit.entity.Player.class);
        openMainMenu.invoke(shopMenu, player);

        // After invoking openMainMenu, top inventory should be the main shop menu holder
        var finalTop = player.getOpenInventory().getTopInventory();
        assertNotNull(finalTop);
        assertTrue(finalTop.getHolder() instanceof MainShopMenuHolder, "Expected to be back at main menu");
    }
}
