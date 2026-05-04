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

        // find first CATEGORY ICON (item with shop_category PDC) and simulate left-click
        int slot = -1;
        org.bukkit.NamespacedKey catKey = new org.bukkit.NamespacedKey(plugin, "shop_category");
        for (int i = 0; i < top.getSize(); i++) {
            org.bukkit.inventory.ItemStack it = top.getItem(i);
            if (it == null || it.getType() == org.bukkit.Material.AIR) continue;
            org.bukkit.inventory.meta.ItemMeta im = it.getItemMeta();
            if (im == null) continue;
            if (im.getPersistentDataContainer().has(catKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                slot = i;
                break;
            }
        }
        assertTrue(slot >= 0, "Expected at least one category icon with shop_category PDC in main menu");

        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        org.bukkit.event.inventory.InventoryClickEvent click = new org.bukkit.event.inventory.InventoryClickEvent(view, org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, slot, org.bukkit.event.inventory.ClickType.LEFT, org.bukkit.event.inventory.InventoryAction.PICKUP_ALL);
        plugin.getServer().getPluginManager().callEvent(click);

        // flush the scheduled runTask so openCategory executes
        ((org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock) server.getScheduler()).performOneTick();

        // after click, top inventory should be replaced by a category menu
        org.bukkit.inventory.Inventory newTop = player.getOpenInventory().getTopInventory();
        assertNotNull(newTop);
        // holder should be a CategoryShopMenuHolder or FlatShopMenuHolder, NOT the main menu
        assertTrue(newTop.getHolder() instanceof com.skyblockexp.ezshops.gui.shop.CategoryShopMenuHolder
                || newTop.getHolder() instanceof com.skyblockexp.ezshops.gui.shop.FlatShopMenuHolder,
                "Expected CategoryShopMenuHolder or FlatShopMenuHolder but got: "
                        + (newTop.getHolder() == null ? "null" : newTop.getHolder().getClass().getName()));
    }
}
