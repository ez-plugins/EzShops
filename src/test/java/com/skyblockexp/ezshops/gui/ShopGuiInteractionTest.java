package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShopGuiInteractionTest extends AbstractEzShopsTest {

    @Test
    void clicking_category_opens_category_menu() throws Exception {
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

        org.bukkit.entity.Player player = server.addPlayer("interact-player");
        // open main menu
        java.lang.reflect.Method openMain = shopMenu.getClass().getMethod("openMainMenu", org.bukkit.entity.Player.class);
        openMain.invoke(shopMenu, player);

        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top);

        // find first non-empty slot (a category) and simulate left-click
        int slot = -1;
        for (int i = 0; i < top.getSize(); i++) {
            if (top.getItem(i) != null && top.getItem(i).getType() != org.bukkit.Material.AIR) { slot = i; break; }
        }
        assertTrue(slot >= 0, "Expected at least one category icon in main menu");

        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        org.bukkit.event.inventory.InventoryClickEvent click = new org.bukkit.event.inventory.InventoryClickEvent(view, org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, slot, org.bukkit.event.inventory.ClickType.LEFT, org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);
        plugin.getServer().getPluginManager().callEvent(click);

        // after click, top inventory should be replaced by a category or flat menu
        org.bukkit.inventory.Inventory newTop = player.getOpenInventory().getTopInventory();
        assertNotNull(newTop);
        // holder should be an AbstractShopMenuHolder (category/flat menu)
        assertTrue(newTop.getHolder() instanceof com.skyblockexp.ezshops.gui.shop.AbstractShopMenuHolder);
    }
}
