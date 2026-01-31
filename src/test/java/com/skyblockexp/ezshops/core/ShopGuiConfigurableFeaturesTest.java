package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.gui.shop.ShopInventoryComposer;
import com.skyblockexp.ezshops.common.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class ShopGuiConfigurableFeaturesTest extends AbstractEzShopsTest {

    @Test
    void category_command_closes_inventory_instead_of_opening_category_menu() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        // Build a simple menu layout with one category that has a command
        ShopMenuLayout.ItemDecoration icon = new ShopMenuLayout.ItemDecoration(Material.PAPER, 1, "Cat", List.of());
        ShopMenuLayout.Category category = new ShopMenuLayout.Category("testcat", "Test Cat", icon, 10,
                "Test Menu", 27, null, null, null, false, List.of(), null, "say hello {player}");
        ShopMenuLayout layout = new ShopMenuLayout("Test Shop", 27, null, null, 0, List.of(category));

        // inject layout into pricing manager
        java.lang.reflect.Field corePricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        corePricingField.setAccessible(true);
        Object pricingManager = corePricingField.get(core);
        java.lang.reflect.Field menuLayoutField = pricingManager.getClass().getDeclaredField("menuLayout");
        menuLayoutField.setAccessible(true);
        menuLayoutField.set(pricingManager, layout);

        // open main menu and invoke handleCategoryClick directly
        java.lang.reflect.Field menuField = CoreShopComponent.class.getDeclaredField("shopMenu");
        menuField.setAccessible(true);
        Object shopMenu = menuField.get(core);

        java.lang.reflect.Method openMain = shopMenu.getClass().getMethod("openMainMenu", org.bukkit.entity.Player.class);
        org.bukkit.entity.Player player = server.addPlayer("cmd-player");
        openMain.invoke(shopMenu, player);

        // find category slot and call handleCategoryClick
        java.lang.reflect.Method handleCategory = shopMenu.getClass().getDeclaredMethod("handleCategoryClick", org.bukkit.entity.Player.class, String.class);
        handleCategory.setAccessible(true);
        handleCategory.invoke(shopMenu, player, "testcat");

        // inventory should be closed after command dispatch
        assertNull(player.getOpenInventory().getTopInventory(), "Inventory should be closed after category command executes");
    }

    @Test
    void custom_back_button_slot_and_decoration_are_respected_in_category_menu() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        ShopMenuLayout.ItemDecoration icon = new ShopMenuLayout.ItemDecoration(Material.PAPER, 1, "Cat", List.of());
        ShopMenuLayout.ItemDecoration backDec = new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "Back", List.of("Go back"));

        ShopMenuLayout.Category category = new ShopMenuLayout.Category("testcat2", "Test Cat2", icon, 0,
                "Category Menu", 27, null, backDec, Integer.valueOf(5), false, List.of(), null, null);
        ShopMenuLayout layout = new ShopMenuLayout("Test Shop", 27, null, null, 0, List.of(category));

        java.lang.reflect.Field corePricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        corePricingField.setAccessible(true);
        Object pricingManager = corePricingField.get(core);
        java.lang.reflect.Field menuLayoutField = pricingManager.getClass().getDeclaredField("menuLayout");
        menuLayoutField.setAccessible(true);
        menuLayoutField.set(pricingManager, layout);

        java.lang.reflect.Field menuField = CoreShopComponent.class.getDeclaredField("shopMenu");
        menuField.setAccessible(true);
        Object shopMenu = menuField.get(core);

        // open category menu directly via ShopInventoryComposer
        java.lang.reflect.Field invCompField = shopMenu.getClass().getDeclaredField("inventoryComposer");
        invCompField.setAccessible(true);
        Object invComp = invCompField.get(shopMenu);
        java.lang.reflect.Method openCategory = invComp.getClass().getMethod("openCategoryMenu", org.bukkit.entity.Player.class, ShopMenuLayout.Category.class, int.class, boolean.class);
        org.bukkit.entity.Player player = server.addPlayer("backdec-player");
        openCategory.invoke(invComp, player, category, 0, true);

        var inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);
        var it = inv.getItem(5);
        assertNotNull(it, "Back button should be present at configured slot");
        assertEquals(Material.DIAMOND, it.getType());

        NamespacedKey actionKey = new NamespacedKey(plugin, "shop_action");
        var meta = it.getItemMeta();
        var container = CompatibilityUtil.getPersistentDataContainer(meta);
        assertTrue(CompatibilityUtil.hasKey(container, actionKey, PersistentDataType.STRING));
        String val = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
        assertEquals(ShopInventoryComposer.ACTION_BACK, val);
    }

    @Test
    void quantity_menu_includes_custom_action_button_with_persistent_key() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        // Build minimal category and item to open quantity menu
        ShopMenuLayout.ItemDecoration icon = new ShopMenuLayout.ItemDecoration(Material.DIAMOND, 1, "It", List.of());
        ShopMenuLayout.Item item = new ShopMenuLayout.Item("diamond_item", Material.DIAMOND, icon, 0, 0, 1, 1,
                new com.skyblockexp.ezshops.shop.ShopPrice(1.0, 1.0), ShopMenuLayout.ItemType.MATERIAL, null, java.util.Map.of(), 0);
        ShopMenuLayout.Category category = new ShopMenuLayout.Category("catq", "CatQ", icon, 0, "CatQ Menu", 27, null, null, null, false, List.of(item), null, null);
        ShopMenuLayout layout = new ShopMenuLayout("Test Shop", 27, null, null, 0, List.of(category));

        java.lang.reflect.Field corePricingField = CoreShopComponent.class.getDeclaredField("pricingManager");
        corePricingField.setAccessible(true);
        Object pricingManager = corePricingField.get(core);
        java.lang.reflect.Field menuLayoutField = pricingManager.getClass().getDeclaredField("menuLayout");
        menuLayoutField.setAccessible(true);
        menuLayoutField.set(pricingManager, layout);

        java.lang.reflect.Field menuField = CoreShopComponent.class.getDeclaredField("shopMenu");
        menuField.setAccessible(true);
        Object shopMenu = menuField.get(core);

        // open quantity menu via ShopInventoryComposer
        org.bukkit.entity.Player player = server.addPlayer("qty-player");
        java.lang.reflect.Field invCompField2 = shopMenu.getClass().getDeclaredField("inventoryComposer");
        invCompField2.setAccessible(true);
        Object invComp2 = invCompField2.get(shopMenu);
        java.lang.reflect.Method openQty = invComp2.getClass().getDeclaredMethod("openQuantityMenu", org.bukkit.entity.Player.class, ShopMenuLayout.Category.class, ShopMenuLayout.Item.class, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.class, int.class, boolean.class);
        openQty.setAccessible(true);
        openQty.invoke(invComp2, player, category, item, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY, 0, true);

        var inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);
        var customItem = inv.getItem(22);
        assertNotNull(customItem, "Custom amount item should be present at slot 22");
        NamespacedKey actionKey = new NamespacedKey(plugin, "shop_action");
        var meta = customItem.getItemMeta();
        var container = CompatibilityUtil.getPersistentDataContainer(meta);
        assertTrue(CompatibilityUtil.hasKey(container, actionKey, PersistentDataType.STRING));
        String val = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
        assertEquals(ShopInventoryComposer.ACTION_CUSTOM, val);
    }
}
