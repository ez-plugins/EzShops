package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopPricingLayoutSupportTest {

    @Test
    void normalize_and_clamp_enforce_inventory_bounds() {
        ShopPricingLayoutSupport support = new ShopPricingLayoutSupport(Logger.getLogger("test"));

        assertEquals(54, support.normalizeSize(0));
        assertEquals(9, support.normalizeSize(1, 9));
        assertEquals(54, support.normalizeSize(80, 27));
        assertEquals(27, support.normalizeSize(26, 27));

        assertEquals(0, support.clampSlot(-1, 54));
        assertEquals(53, support.clampSlot(99, 54));
        assertEquals(8, support.clampSlot(8, 54));
    }

    @Test
    void parse_buttons_accepts_valid_entries_and_skips_invalid() {
        ShopPricingLayoutSupport support = new ShopPricingLayoutSupport(Logger.getLogger("test"));
        YamlConfiguration buttons = new YamlConfiguration();

        buttons.set("next.action", "back");
        buttons.set("next.slot", 50);
        buttons.set("next.material", "ARROW");
        buttons.set("next.display-name", "&aNext");
        buttons.set("next.sound", "ui.button.click");
        buttons.set("next.volume", 0.8D);
        buttons.set("next.pitch", 1.2D);

        buttons.set("missingAction.slot", 10);
        buttons.set("missingAction.material", "PAPER");

        buttons.set("invalidAction.action", "does_not_exist");
        buttons.set("invalidAction.slot", 11);

        buttons.set("badSlot.action", "back");
        buttons.set("badSlot.slot", -1);

        List<ShopMenuLayout.ConfigurableButton> parsed = support.parseButtons(buttons);
        assertEquals(1, parsed.size());

        ShopMenuLayout.ConfigurableButton next = parsed.get(0);
        assertEquals("next", next.id());
        assertEquals(50, next.slot());
        assertEquals(ShopMenuLayout.ButtonAction.BACK, next.action());
        assertNotNull(next.display());
        assertEquals(Material.ARROW, next.display().material());
        assertEquals("§aNext", next.display().displayName());
        assertEquals("ui.button.click", next.sound());
        assertEquals(0.8f, next.soundVolume(), 1e-6f);
        assertEquals(1.2f, next.soundPitch(), 1e-6f);
    }

    @Test
    void parse_decoration_uses_fallback_when_material_unknown() {
        ShopPricingLayoutSupport support = new ShopPricingLayoutSupport(Logger.getLogger("test"));
        ShopMenuLayout.ItemDecoration fallback = new ShopMenuLayout.ItemDecoration(Material.STONE, 2, "Base", List.of("Lore"));

        YamlConfiguration section = new YamlConfiguration();
        section.set("material", "NOT_REAL");
        section.set("amount", 0);
        section.set("display-name", "&bDecor");
        section.set("lore", List.of("&7Line"));

        ShopMenuLayout.ItemDecoration parsed = support.parseDecoration(section, fallback, "unit-test");

        assertEquals(Material.STONE, parsed.material());
        assertEquals(1, parsed.amount());
        assertEquals("§bDecor", parsed.displayName());
        assertEquals(List.of("§7Line"), parsed.lore());

        assertNull(support.colorize((String) null));
        assertTrue(support.colorize(List.of()).isEmpty());
        assertEquals("Diamond Block", support.friendlyName("DIAMOND_BLOCK"));
    }
}
