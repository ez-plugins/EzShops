package com.skyblockexp.ezshops.gui.playershop;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerShopBrowseMessagesTest {

    @Test
    void defaultsAreUsedWhenSectionIsNull() {
        PlayerShopBrowseMessages messages = PlayerShopBrowseMessages.from(null);

        assertEquals("&cOnly players can use this command.", messages.playersOnly());
        assertTrue(messages.title(2, 4).contains("2/4"));
        assertTrue(messages.pageInfo(3, 7).contains("3"));
    }

    @Test
    void customValuesAndLoreFormattingAreApplied() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("title", "Page {page} of {total}");
        cfg.set("shop-name", "{qty}x {item}");
        cfg.set("shop-lore", List.of("Seller={seller}", "Price={price}"));
        cfg.set("own-shop-lore", List.of("Mine {seller} {price}"));
        cfg.set("out-of-stock-lore", List.of("OOS {seller} {price}"));
        cfg.set("no-shops", "No shops");
        cfg.set("prev-page", "Prev");
        cfg.set("next-page", "Next");
        cfg.set("page-info", "P{page}/{total}");
        cfg.set("no-permission", "Denied");
        cfg.set("players-only", "Players only");

        PlayerShopBrowseMessages messages = PlayerShopBrowseMessages.from(cfg);

        assertEquals("Page 1 of 9", messages.title(1, 9));
        assertEquals("32x OAK_LOG", messages.shopName(32, "OAK_LOG"));
        assertEquals(List.of("Seller=Alex", "Price=$12"), messages.shopLore("Alex", "$12"));
        assertEquals(List.of("Mine Sam $44"), messages.ownShopLore("Sam", "$44"));
        assertEquals(List.of("OOS Pat $1"), messages.outOfStockLore("Pat", "$1"));
        assertEquals("No shops", messages.noShops());
        assertEquals("Prev", messages.prevPage());
        assertEquals("Next", messages.nextPage());
        assertEquals("P4/10", messages.pageInfo(4, 10));
        assertEquals("Denied", messages.noPermission());
        assertEquals("Players only", messages.playersOnly());
    }

    @Test
    void emptyConfiguredLoreFallsBackToDefaults() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("shop-lore", List.of());
        cfg.set("own-shop-lore", List.of());
        cfg.set("out-of-stock-lore", List.of());

        PlayerShopBrowseMessages messages = PlayerShopBrowseMessages.from(cfg);

        assertTrue(messages.shopLore("A", "$1").size() >= 3);
        assertTrue(messages.ownShopLore("A", "$1").size() >= 3);
        assertTrue(messages.outOfStockLore("A", "$1").size() >= 3);
    }
}

