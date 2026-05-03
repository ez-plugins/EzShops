package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.shop.api.ShopItem;
import com.skyblockexp.ezshops.shop.api.ShopTemplateCategory;
import com.skyblockexp.ezshops.shop.api.ShopTemplateBuilder;
import com.skyblockexp.ezshops.shop.api.ShopTemplateService;
import net.milkbowl.vault.economy.Economy;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShopTemplateFeatureTest extends AbstractEzShopsTest {

    @Test
    public void importTemplateWritesCategoriesAndGivesItems() throws Exception {
        // Register a dummy economy provider and load plugin
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertTrue(plugin.isEnabled());

        // Obtain the ShopTemplateService
        var reg = server.getServicesManager().getRegistration(ShopTemplateService.class);
        assertNotNull(reg, "ShopTemplateService should be registered");
        ShopTemplateService svc = reg.getProvider();
        assertNotNull(svc);

        // Build a template with one category and one top-level item (so import grants items)
        ShopItem item = ShopItem.builder("t1").material("DIAMOND").amount(2).buy(10).sell(5).build();
        java.util.List<java.util.Map<String, Object>> items = java.util.List.of(item.toMap());
        java.util.Map<String, ShopTemplateCategory> categories = java.util.Map.of(
            "kit", new ShopTemplateCategory("kit", java.util.Map.of("name", "Kit"), java.util.Map.of())
        );
        com.skyblockexp.ezshops.shop.template.ShopTemplate template =
            new com.skyblockexp.ezshops.shop.template.ShopTemplate("ftest", "Feature Test Template", items, java.util.Map.of(), categories);

        // Register template via service
        svc.registerTemplate(template);

        // Create a player and grant import permission
        org.bukkit.entity.Player player = server.addPlayer("importer");
        player.addAttachment(plugin, "ezshops.shop", true);
        player.addAttachment(plugin, "ezshops.import", true);

        // Dispatch the import command
        boolean dispatched = server.dispatchCommand(player, "shop import ftest");
        assertTrue(dispatched, "Command should be dispatched");

        // After import, player should have received the items
        boolean hasDiamond = false;
        for (org.bukkit.inventory.ItemStack is : player.getInventory().getContents()) {
            if (is == null) continue;
            if (is.getType() == org.bukkit.Material.DIAMOND) {
                hasDiamond = true;
                break;
            }
        }
        assertTrue(hasDiamond, "Player should receive imported diamond items");

        // Pricing manager should have reloaded categories including our 'kit' category
        var core = plugin.getCoreShopComponent();
        assertNotNull(core);
        var layout = core.pricingManager().getMenuLayout();
        boolean found = layout.categories().stream().anyMatch(c -> c.id().equalsIgnoreCase("kit"));
        assertTrue(found, "Pricing manager menu layout should include 'kit' category after import");
    }
}
