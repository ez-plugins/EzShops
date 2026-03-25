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

public class PricingAdminTabCompletionTest extends AbstractEzShopsTest {

    @Test
    void subcommand_and_item_completions_present() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("tester");
        player.addAttachment(plugin, "ezshops.pricing.admin", true);
        player.addAttachment(plugin, "ezshops.pricing.admin.set", true);
        player.addAttachment(plugin, "ezshops.pricing.admin.reset", true);
        player.addAttachment(plugin, "ezshops.pricing.admin.resetall", true);

        assertNotNull(plugin.getCommand("pricingadmin"));
        TabCompleter completer = plugin.getCommand("pricingadmin").getTabCompleter();
        assertNotNull(completer, "pricingadmin should have a tab completer");

        List<String> subs = completer.onTabComplete(player, plugin.getCommand("pricingadmin"), "pricingadmin", new String[]{""});
        assertTrue(subs.contains("set"));
        assertTrue(subs.contains("reset"));
        assertTrue(subs.contains("resetall"));

        // second-arg item completions should include known key from bundled resources
        List<String> items = completer.onTabComplete(player, plugin.getCommand("pricingadmin"), "pricingadmin", new String[]{"set", ""});
        assertNotNull(items);
        assertTrue(items.contains("wheat_seeds") || items.contains("carrot"), "Expected configured item keys to be suggested");
    }

    @Test
    void price_argument_completion_suggests_current_prices() {
        loadProviderPlugin(mock(Economy.class));
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        Player player = server.addPlayer("tester2");
        player.addAttachment(plugin, "ezshops.pricing.admin", true);
        player.addAttachment(plugin, "ezshops.pricing.admin.set", true);

        TabCompleter completer = plugin.getCommand("pricingadmin").getTabCompleter();
        assertNotNull(completer);

        // Use a known bundled key that has buy/sell prices in resources (farming.yml)
        List<String> prices = completer.onTabComplete(player, plugin.getCommand("pricingadmin"), "pricingadmin", new String[]{"set", "wheat_seeds", ""});
        assertNotNull(prices);
        // Expect buy and/or sell price suggestions (e.g., 15.0 and 3.8)
        boolean hasNumeric = prices.stream().anyMatch(s -> s.matches("\\d+(\\.\\d+)?"));
        assertTrue(hasNumeric, "Expected numeric price suggestions for wheat_seeds");
    }
}
