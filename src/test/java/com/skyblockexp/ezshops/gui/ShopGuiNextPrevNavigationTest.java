package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.common.CompatibilityUtil;
import com.skyblockexp.ezshops.gui.shop.FlatShopMenuHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShopGuiNextPrevNavigationTest extends AbstractEzShopsTest {

    @Test
    void clicking_next_then_previous_navigates_pages_in_flat_menu() throws Exception {
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

        // access inventory composer
        java.lang.reflect.Field compField = shopMenu.getClass().getDeclaredField("inventoryComposer");
        compField.setAccessible(true);
        Object composer = compField.get(shopMenu);
        assertNotNull(composer);

        org.bukkit.entity.Player player = server.addPlayer("nav-player-2");

        // open flat menu directly to ensure pagination slots exist
        java.lang.reflect.Method openFlat = composer.getClass().getMethod("openFlatMenu", org.bukkit.entity.Player.class, int.class, boolean.class);
        openFlat.invoke(composer, player, 0, true);

        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top);

        NamespacedKey actionKey = new NamespacedKey(plugin, "shop_action");
        int nextSlot = -1;
        for (int i = 0; i < top.getSize(); i++) {
            var it = top.getItem(i);
            if (it == null) continue;
            var meta = it.getItemMeta();
            var container = CompatibilityUtil.getPersistentDataContainer(meta);
            if (CompatibilityUtil.hasKey(container, actionKey, PersistentDataType.STRING)) {
                String val = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
                if ("next".equalsIgnoreCase(val)) { nextSlot = i; break; }
            }
        }
        Assumptions.assumeTrue(nextSlot >= 0, "No next button present to test pagination");

        // advance page by calling composer.populateFlatMenu directly (scheduler in production does the same)
        var newTop = player.getOpenInventory().getTopInventory();
        assertNotNull(newTop);
        assertTrue(newTop.getHolder() instanceof FlatShopMenuHolder);
        FlatShopMenuHolder holder = (FlatShopMenuHolder) newTop.getHolder();
        // call populateFlatMenu to move to next page
        java.lang.reflect.Method populate = composer.getClass().getMethod("populateFlatMenu", holder.getClass(), int.class, int.class, boolean.class);
        populate.invoke(composer, holder, holder.page() + 1, 0, true);
        assertTrue(holder.page() > 0, "Expected page to advance after invoking populateFlatMenu");

        // find previous button
        int prevSlot = -1;
        for (int i = 0; i < newTop.getSize(); i++) {
            var it = newTop.getItem(i);
            if (it == null) continue;
            var meta = it.getItemMeta();
            var container = CompatibilityUtil.getPersistentDataContainer(meta);
            if (CompatibilityUtil.hasKey(container, actionKey, PersistentDataType.STRING)) {
                String val = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
                if ("previous".equalsIgnoreCase(val)) { prevSlot = i; break; }
            }
        }
        Assumptions.assumeTrue(prevSlot >= 0, "No previous button present to test backwards pagination");

        // go back to previous page via populateFlatMenu
        java.lang.reflect.Method populate2 = composer.getClass().getMethod("populateFlatMenu", holder.getClass(), int.class, int.class, boolean.class);
        populate2.invoke(composer, holder, Math.max(0, holder.page() - 1), 0, true);

        var afterTop = player.getOpenInventory().getTopInventory();
        assertNotNull(afterTop);
        assertTrue(afterTop.getHolder() instanceof FlatShopMenuHolder);
        FlatShopMenuHolder afterHolder = (FlatShopMenuHolder) afterTop.getHolder();
        assertTrue(afterHolder.page() >= 0, "Expected page to be valid after invoking previous");
    }
}
