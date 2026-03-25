package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import net.milkbowl.vault.economy.Economy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import java.util.List;

public class PricingAdminTabCompletionPermissionsTest extends AbstractEzShopsTest {

    @Test
    void completions_empty_without_base_permission() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("noperm");
        // no attachments/permissions

        assertNotNull(plugin.getCommand("pricingadmin"));
        TabCompleter completer = plugin.getCommand("pricingadmin").getTabCompleter();
        assertNotNull(completer);

        List<String> subs = completer.onTabComplete(player, plugin.getCommand("pricingadmin"), "pricingadmin", new String[]{""});
        assertTrue(subs.isEmpty(), "Expected no completions when player lacks base permission");
    }

    @Test
    void only_resetall_available_when_only_resetall_permission_granted() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("limited");
        player.addAttachment(plugin, "ezshops.pricing.admin", true);
        player.addAttachment(plugin, "ezshops.pricing.admin.resetall", true);

        TabCompleter completer = plugin.getCommand("pricingadmin").getTabCompleter();
        List<String> subs = completer.onTabComplete(player, plugin.getCommand("pricingadmin"), "pricingadmin", new String[]{""});
        assertTrue(subs.contains("resetall"));
        assertFalse(subs.contains("set"));
        assertFalse(subs.contains("reset"));
    }

    @Test
    void set_shown_when_set_permission_granted_but_reset_not_shown() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("setter");
        player.addAttachment(plugin, "ezshops.pricing.admin", true);
        player.addAttachment(plugin, "ezshops.pricing.admin.set", true);

        TabCompleter completer = plugin.getCommand("pricingadmin").getTabCompleter();
        List<String> subs = completer.onTabComplete(player, plugin.getCommand("pricingadmin"), "pricingadmin", new String[]{""});
        assertTrue(subs.contains("set"));
        assertFalse(subs.contains("reset"));
        assertFalse(subs.contains("resetall"));
    }
}
