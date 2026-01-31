package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ShopGuiShiftClickPreventionTest extends AbstractEzShopsTest {

    @Test
    void shift_clicks_are_cancelled_in_all_shop_guis() throws Exception {
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

        org.bukkit.entity.Player player = server.addPlayer("shift-player");

        // --- Main menu ---
        java.lang.reflect.Method openMain = shopMenu.getClass().getMethod("openMainMenu", org.bukkit.entity.Player.class);
        openMain.invoke(shopMenu, player);
        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top);
        int slot = -1;
        for (int i = 0; i < top.getSize(); i++) {
            if (top.getItem(i) != null && top.getItem(i).getType() != org.bukkit.Material.AIR) { slot = i; break; }
        }
        assertTrue(slot >= 0, "Expected at least one category icon in main menu");
        var view = player.getOpenInventory();
        var click = new org.bukkit.event.inventory.InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, slot, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        plugin.getServer().getPluginManager().callEvent(click);
        assertTrue(click.isCancelled(), "Shift-click in main menu should be cancelled");

        // --- Category menu ---
        // open a category menu by invoking category click
        java.lang.reflect.Method handleCategory = shopMenu.getClass().getDeclaredMethod("handleCategoryClick", org.bukkit.entity.Player.class, String.class);
        handleCategory.setAccessible(true);
        // pick first category id from menu layout
        java.lang.reflect.Field pricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        pricingField.setAccessible(true);
        Object pricingManager = pricingField.get(core);
        java.lang.reflect.Field menuLayoutField = pricingManager.getClass().getDeclaredField("menuLayout");
        menuLayoutField.setAccessible(true);
        ShopMenuLayout layout = (ShopMenuLayout) menuLayoutField.get(pricingManager);
        assertNotNull(layout);
        assertFalse(layout.categories().isEmpty());
        String catId = layout.categories().get(0).id();
        handleCategory.invoke(shopMenu, player, catId);

        var catTop = player.getOpenInventory().getTopInventory();
        assertNotNull(catTop);
        int itemSlot = -1;
        for (int i = 0; i < catTop.getSize(); i++) {
            if (catTop.getItem(i) != null && catTop.getItem(i).getType() != org.bukkit.Material.AIR) { itemSlot = i; break; }
        }
        assertTrue(itemSlot >= 0, "Expected at least one item in category menu");
        var catView = player.getOpenInventory();
        var catClick = new org.bukkit.event.inventory.InventoryClickEvent(catView, InventoryType.SlotType.CONTAINER, itemSlot, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        plugin.getServer().getPluginManager().callEvent(catClick);
        assertTrue(catClick.isCancelled(), "Shift-click in category menu should be cancelled");

        // --- Flat menu ---
        java.lang.reflect.Field invCompFieldFlat = shopMenu.getClass().getDeclaredField("inventoryComposer");
        invCompFieldFlat.setAccessible(true);
        Object invCompFlat = invCompFieldFlat.get(shopMenu);
        java.lang.reflect.Method openFlat = invCompFlat.getClass().getMethod("openFlatMenu", org.bukkit.entity.Player.class, int.class, boolean.class);
        openFlat.invoke(invCompFlat, player, 0, true);
        var flatTop = player.getOpenInventory().getTopInventory();
        assertNotNull(flatTop);
        int flatSlot = -1;
        for (int i = 0; i < flatTop.getSize(); i++) {
            if (flatTop.getItem(i) != null && flatTop.getItem(i).getType() != org.bukkit.Material.AIR) { flatSlot = i; break; }
        }
        assertTrue(flatSlot >= 0, "Expected item in flat menu");
        var flatView = player.getOpenInventory();
        var flatClick = new org.bukkit.event.inventory.InventoryClickEvent(flatView, InventoryType.SlotType.CONTAINER, flatSlot, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        plugin.getServer().getPluginManager().callEvent(flatClick);
        assertTrue(flatClick.isCancelled(), "Shift-click in flat menu should be cancelled");

        // --- Quantity menu ---
        // open quantity menu for first category/item
        ShopMenuLayout.Category firstCat = layout.categories().get(0);
        ShopMenuLayout.Item firstItem = null;
        if (!firstCat.items().isEmpty()) firstItem = firstCat.items().get(0);
        if (firstItem != null) {
            // invoke inventoryComposer.openQuantityMenu
            java.lang.reflect.Field invCompField = shopMenu.getClass().getDeclaredField("inventoryComposer");
            invCompField.setAccessible(true);
            Object invComp = invCompField.get(shopMenu);
            java.lang.reflect.Method openQty = invComp.getClass().getDeclaredMethod("openQuantityMenu", org.bukkit.entity.Player.class, ShopMenuLayout.Category.class, ShopMenuLayout.Item.class, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.class, int.class, boolean.class);
            openQty.setAccessible(true);
            openQty.invoke(invComp, player, firstCat, firstItem, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY, 0, true);

            var qtyTop = player.getOpenInventory().getTopInventory();
            assertNotNull(qtyTop);
            int qtySlot = -1;
            for (int i = 0; i < qtyTop.getSize(); i++) {
                if (qtyTop.getItem(i) != null && qtyTop.getItem(i).getType() != org.bukkit.Material.AIR) { qtySlot = i; break; }
            }
            assertTrue(qtySlot >= 0, "Expected item in quantity menu");
            var qtyView = player.getOpenInventory();
            var qtyClick = new org.bukkit.event.inventory.InventoryClickEvent(qtyView, InventoryType.SlotType.CONTAINER, qtySlot, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
            plugin.getServer().getPluginManager().callEvent(qtyClick);
            assertTrue(qtyClick.isCancelled(), "Shift-click in quantity menu should be cancelled");
        }
    }
}
