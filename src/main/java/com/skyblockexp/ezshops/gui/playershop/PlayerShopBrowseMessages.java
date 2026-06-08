package com.skyblockexp.ezshops.gui.playershop;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Objects;

/**
 * Messages used by the browse-all-player-shops GUI.
 * Loaded from the {@code player-shops.browse-gui} messages section.
 */
public final class PlayerShopBrowseMessages {

    private static final String DEFAULT_TITLE = "&aPlayer Shops &7(Page {page}/{total})";
    private static final String DEFAULT_SHOP_NAME = "&b{qty}x {item}";
    private static final List<String> DEFAULT_SHOP_LORE = List.of(
            "&7Seller: &f{seller}",
            "&7Price: &6{price}",
            "",
            "&aClick to purchase");
    private static final List<String> DEFAULT_OWN_SHOP_LORE = List.of(
            "&7Seller: &f{seller}",
            "&7Price: &6{price}",
            "",
            "&eThis is your shop");
    private static final List<String> DEFAULT_OUT_OF_STOCK_LORE = List.of(
            "&7Seller: &f{seller}",
            "&7Price: &6{price}",
            "",
            "&cOut of stock");
    private static final String DEFAULT_NO_SHOPS = "&cThere are no player shops currently available.";
    private static final String DEFAULT_PREV_PAGE = "&7\u2190 Previous Page";
    private static final String DEFAULT_NEXT_PAGE = "&aNext Page \u2192";
    private static final String DEFAULT_PAGE_INFO = "&7Page {page} of {total}";
    private static final String DEFAULT_NO_PERMISSION = "&cYou do not have permission to browse player shops.";
    private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players can use this command.";

    private final String title;
    private final String shopName;
    private final List<String> shopLore;
    private final List<String> ownShopLore;
    private final List<String> outOfStockLore;
    private final String noShops;
    private final String prevPage;
    private final String nextPage;
    private final String pageInfo;
    private final String noPermission;
    private final String playersOnly;

    private PlayerShopBrowseMessages(String title, String shopName, List<String> shopLore,
            List<String> ownShopLore, List<String> outOfStockLore, String noShops,
            String prevPage, String nextPage, String pageInfo,
            String noPermission, String playersOnly) {
        this.title = Objects.requireNonNull(title);
        this.shopName = Objects.requireNonNull(shopName);
        this.shopLore = Objects.requireNonNull(shopLore);
        this.ownShopLore = Objects.requireNonNull(ownShopLore);
        this.outOfStockLore = Objects.requireNonNull(outOfStockLore);
        this.noShops = Objects.requireNonNull(noShops);
        this.prevPage = Objects.requireNonNull(prevPage);
        this.nextPage = Objects.requireNonNull(nextPage);
        this.pageInfo = Objects.requireNonNull(pageInfo);
        this.noPermission = Objects.requireNonNull(noPermission);
        this.playersOnly = Objects.requireNonNull(playersOnly);
    }

    public static PlayerShopBrowseMessages from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        List<String> rawShopLore = section.getStringList("shop-lore");
        List<String> rawOwnLore = section.getStringList("own-shop-lore");
        List<String> rawOosLore = section.getStringList("out-of-stock-lore");
        return new PlayerShopBrowseMessages(
                section.getString("title", DEFAULT_TITLE),
                section.getString("shop-name", DEFAULT_SHOP_NAME),
                rawShopLore.isEmpty() ? DEFAULT_SHOP_LORE : rawShopLore,
                rawOwnLore.isEmpty() ? DEFAULT_OWN_SHOP_LORE : rawOwnLore,
                rawOosLore.isEmpty() ? DEFAULT_OUT_OF_STOCK_LORE : rawOosLore,
                section.getString("no-shops", DEFAULT_NO_SHOPS),
                section.getString("prev-page", DEFAULT_PREV_PAGE),
                section.getString("next-page", DEFAULT_NEXT_PAGE),
                section.getString("page-info", DEFAULT_PAGE_INFO),
                section.getString("no-permission", DEFAULT_NO_PERMISSION),
                section.getString("players-only", DEFAULT_PLAYERS_ONLY));
    }

    public static PlayerShopBrowseMessages defaults() {
        return new PlayerShopBrowseMessages(
                DEFAULT_TITLE, DEFAULT_SHOP_NAME,
                DEFAULT_SHOP_LORE, DEFAULT_OWN_SHOP_LORE, DEFAULT_OUT_OF_STOCK_LORE,
                DEFAULT_NO_SHOPS, DEFAULT_PREV_PAGE, DEFAULT_NEXT_PAGE,
                DEFAULT_PAGE_INFO, DEFAULT_NO_PERMISSION, DEFAULT_PLAYERS_ONLY);
    }

    // --- formatted accessors ---

    public String title(int page, int total) {
        return format(title, "page", Integer.toString(page), "total", Integer.toString(total));
    }

    public String shopName(int qty, String item) {
        return format(shopName, "qty", Integer.toString(qty), "item", item);
    }

    public List<String> shopLore(String seller, String price) {
        return formatList(shopLore, "seller", seller, "price", price);
    }

    public List<String> ownShopLore(String seller, String price) {
        return formatList(ownShopLore, "seller", seller, "price", price);
    }

    public List<String> outOfStockLore(String seller, String price) {
        return formatList(outOfStockLore, "seller", seller, "price", price);
    }

    public String noShops() {
        return noShops;
    }

    public String prevPage() {
        return prevPage;
    }

    public String nextPage() {
        return nextPage;
    }

    public String pageInfo(int page, int total) {
        return format(pageInfo, "page", Integer.toString(page), "total", Integer.toString(total));
    }

    public String noPermission() {
        return noPermission;
    }

    public String playersOnly() {
        return playersOnly;
    }

    // --- helpers ---

    private static String format(String template, String... replacements) {
        String result = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return result;
    }

    private static List<String> formatList(List<String> lines, String... replacements) {
        return lines.stream()
                .map(line -> format(line, replacements))
                .toList();
    }
}
