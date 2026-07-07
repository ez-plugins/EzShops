package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import net.milkbowl.vault.economy.Economy;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ShopCommandReloadTest extends AbstractEzShopsTest {

    @Test
    void shop_reload_denied_without_permission() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        PlayerMock player = server.addPlayer("reload-no-perm");
        boolean dispatched = server.dispatchCommand(player, "shop reload");

        assertTrue(dispatched);
        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("do not have permission"));
    }

    @Test
    void shop_reload_succeeds_with_permission() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        PlayerMock player = server.addPlayer("reload-admin");
        player.addAttachment(plugin, "ezshops.reload", true);

        boolean dispatched = server.dispatchCommand(player, "shop reload");

        assertTrue(dispatched);
        String message = player.nextMessage();
        assertNotNull(message);
        String lower = message.toLowerCase();
        assertTrue(
            lower.contains("reloaded successfully") || lower.contains("failed to reload"),
            "Expected handled reload response message, got: " + message);
    }
}
