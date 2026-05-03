package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.common.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ShopCommandCategoryTest extends AbstractEzShopsTest {

    @Test
    public void command_opens_category_by_display_name() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        var layout = core.pricingManager().getMenuLayout();
        assertNotNull(layout);
        assertFalse(layout.categories().isEmpty(), "Expected at least one configured category");

        ShopMenuLayout.Category category = layout.categories().get(0);
        // Use the display name (stripped of color codes) as the command argument
        String display = MessageUtil.stripColors(MessageUtil.translateColors(category.displayName()));

        org.bukkit.entity.Player player = server.addPlayer("cmd-player");
        player.addAttachment(plugin, "ezshops.shop", true);

        boolean dispatched = server.dispatchCommand(player, "shop " + display);
        assertTrue(dispatched, "Command should be dispatched");

        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top, "Player should have an open top inventory after the command");
        assertTrue(top.getHolder() instanceof com.skyblockexp.ezshops.gui.shop.CategoryShopMenuHolder,
                "Top inventory holder should be a CategoryShopMenuHolder");

        com.skyblockexp.ezshops.gui.shop.CategoryShopMenuHolder holder =
                (com.skyblockexp.ezshops.gui.shop.CategoryShopMenuHolder) top.getHolder();
        assertEquals(category.id().toLowerCase(), holder.category().id().toLowerCase());
    }

        @Test
        public void command_opens_category_by_display_name_multiword() throws Exception {
        net.milkbowl.vault.economy.Economy econ = mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        var layout = core.pricingManager().getMenuLayout();
        assertNotNull(layout);
        assertFalse(layout.categories().isEmpty(), "Expected at least one configured category");

        ShopMenuLayout.Category category = layout.categories().stream()
            .filter(c -> MessageUtil.stripColors(MessageUtil.translateColors(c.displayName())).contains(" "))
            .findFirst().orElse(layout.categories().get(0));

        String display = MessageUtil.stripColors(MessageUtil.translateColors(category.displayName()));

        org.bukkit.entity.Player player = server.addPlayer("cmd-player-2");
        player.addAttachment(plugin, "ezshops.shop", true);

        boolean dispatched = server.dispatchCommand(player, "shop " + display);
        assertTrue(dispatched, "Command should be dispatched");

        org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
        assertNotNull(top, "Player should have an open top inventory after the command");
        assertTrue(top.getHolder() instanceof com.skyblockexp.ezshops.gui.shop.CategoryShopMenuHolder,
            "Top inventory holder should be a CategoryShopMenuHolder");

        com.skyblockexp.ezshops.gui.shop.CategoryShopMenuHolder holder =
            (com.skyblockexp.ezshops.gui.shop.CategoryShopMenuHolder) top.getHolder();
        assertEquals(category.id().toLowerCase(), holder.category().id().toLowerCase());
        }
}
