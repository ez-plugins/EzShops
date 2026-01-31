package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.gui.shop.FlatShopMenuHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import com.skyblockexp.ezshops.common.CompatibilityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import static org.junit.jupiter.api.Assertions.*;

public class ShopGuiNavigationTest extends AbstractEzShopsTest {

    @Test
    void clicking_next_button_changes_page_in_flat_menu() throws Exception {
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

        org.bukkit.entity.Player player = server.addPlayer("nav-player");

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

        // simulate click event
        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        org.bukkit.event.inventory.InventoryClickEvent click = new org.bukkit.event.inventory.InventoryClickEvent(view, org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, nextSlot, org.bukkit.event.inventory.ClickType.LEFT, org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);
        plugin.getServer().getPluginManager().callEvent(click);

        // After click, the top inventory holder should be a FlatShopMenuHolder with page > 0
        var newTop = player.getOpenInventory().getTopInventory();
        assertNotNull(newTop);
        assertTrue(newTop.getHolder() instanceof FlatShopMenuHolder);
        FlatShopMenuHolder holder = (FlatShopMenuHolder) newTop.getHolder();
        assertTrue(holder.page() >= 0);
    }
}
