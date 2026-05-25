package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.config.ConfigTranslator;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

/**
 * Sign formatting for player shops (extracted from PlayerShopConfiguration).
 *
 * <p>Each of the four sign lines is a format string that may use any of these
 * placeholders:
 * <ul>
 *   <li>{@code {item}}  – full item name</li>
 *   <li>{@code {item1}} – first portion of the item name (up to {@code item-split-length} chars,
 *       split at the last word boundary)</li>
 *   <li>{@code {item2}} – remainder of the item name after the split (empty when the name fits)</li>
 *   <li>{@code {amount}} – quantity per sale</li>
 *   <li>{@code {price}}  – formatted price</li>
 *   <li>{@code {owner}}  – shop owner name</li>
 * </ul>
 */
public final class SignFormat {

    private static final String DEFAULT_AVAILABLE_HEADER = "&b{item1}";
    private static final String DEFAULT_OUT_OF_STOCK_HEADER = "&c{item1}";
    private static final String DEFAULT_OWNER_FORMAT = "&7{item2}";
    private static final String DEFAULT_UNKNOWN_OWNER = "Owner";
    private static final String DEFAULT_ITEM_FORMAT = "&b{amount}x";
    private static final String DEFAULT_PRICE_FORMAT = "&6{price}";
    private static final String DEFAULT_OUT_OF_STOCK_LINE = "&cOut of Stock";
    private static final int DEFAULT_ITEM_SPLIT_LENGTH = 14;

    private final String availableHeader;
    private final String outOfStockHeader;
    private final String ownerFormat;
    private final String unknownOwnerName;
    private final String itemFormat;
    private final String priceFormat;
    private final String outOfStockLine;
    private final int itemSplitLength;

    public SignFormat(String availableHeader, String outOfStockHeader, String ownerFormat,
                      String unknownOwnerName, String itemFormat, String priceFormat, String outOfStockLine,
                      int itemSplitLength) {
        this.availableHeader = availableHeader;
        this.outOfStockHeader = outOfStockHeader;
        this.ownerFormat = ownerFormat;
        this.unknownOwnerName = unknownOwnerName;
        this.itemFormat = itemFormat;
        this.priceFormat = priceFormat;
        this.outOfStockLine = outOfStockLine;
        this.itemSplitLength = itemSplitLength > 0 ? itemSplitLength : DEFAULT_ITEM_SPLIT_LENGTH;
    }

    public static SignFormat from(ConfigurationSection section, ShopMessageConfiguration messages) {
        if (section == null) {
            return defaults();
        }
        String availableHeader = ConfigTranslator.resolve(section.getString("available-header", DEFAULT_AVAILABLE_HEADER), messages);
        String outOfStockHeader = ConfigTranslator.resolve(section.getString("out-of-stock-header", DEFAULT_OUT_OF_STOCK_HEADER), messages);
        String ownerFormat = ConfigTranslator.resolve(section.getString("owner-format", DEFAULT_OWNER_FORMAT), messages);
        String unknownOwner = section.getString("unknown-owner-name", DEFAULT_UNKNOWN_OWNER);
        if (unknownOwner == null || unknownOwner.isBlank()) {
            unknownOwner = DEFAULT_UNKNOWN_OWNER;
        }
        String itemFormat = ConfigTranslator.resolve(section.getString("item-format", DEFAULT_ITEM_FORMAT), messages);
        String priceFormat = ConfigTranslator.resolve(section.getString("price-format", DEFAULT_PRICE_FORMAT), messages);
        String outOfStockLine = ConfigTranslator.resolve(section.getString("out-of-stock-line", DEFAULT_OUT_OF_STOCK_LINE), messages);
        int splitLength = section.getInt("item-split-length", DEFAULT_ITEM_SPLIT_LENGTH);
        return new SignFormat(availableHeader, outOfStockHeader, ownerFormat, unknownOwner, itemFormat, priceFormat,
                outOfStockLine, splitLength);
    }

    public static SignFormat defaults() {
        return new SignFormat(
                MessageUtil.translateColors(DEFAULT_AVAILABLE_HEADER),
                MessageUtil.translateColors(DEFAULT_OUT_OF_STOCK_HEADER),
                MessageUtil.translateColors(DEFAULT_OWNER_FORMAT),
                DEFAULT_UNKNOWN_OWNER,
                MessageUtil.translateColors(DEFAULT_ITEM_FORMAT),
                MessageUtil.translateColors(DEFAULT_PRICE_FORMAT),
                MessageUtil.translateColors(DEFAULT_OUT_OF_STOCK_LINE),
                DEFAULT_ITEM_SPLIT_LENGTH);
    }

    public String availableHeader() {
        return availableHeader;
    }

    public String outOfStockHeader() {
        return outOfStockHeader;
    }

    public String unknownOwnerName() {
        return unknownOwnerName;
    }

    /**
     * Splits {@code itemName} at the last word boundary at or before {@code maxLen} characters.
     * Returns a two-element array: {@code [first, rest]}.  {@code rest} is an empty string when
     * the name fits within {@code maxLen}.
     */
    static String[] splitItem(String itemName, int maxLen) {
        if (itemName == null) {
            return new String[] { "", "" };
        }
        if (itemName.length() <= maxLen) {
            return new String[] { itemName, "" };
        }
        int splitAt = itemName.lastIndexOf(' ', maxLen);
        if (splitAt <= 0) {
            // No word boundary found – hard-split at maxLen
            return new String[] { itemName.substring(0, maxLen), itemName.substring(maxLen).trim() };
        }
        return new String[] { itemName.substring(0, splitAt), itemName.substring(splitAt + 1) };
    }

    public String[] formatLines(String ownerName, int amount, String itemName, String priceText, boolean hasStock) {
        Objects.requireNonNull(ownerName, "ownerName");
        String[] split = splitItem(itemName, itemSplitLength);
        String item1 = split[0];
        String item2 = split[1];

        String[] lines = new String[4];
        lines[0] = replace(hasStock ? availableHeader : outOfStockHeader, ownerName, amount, itemName, item1, item2, priceText);
        lines[1] = replace(ownerFormat, ownerName, amount, itemName, item1, item2, priceText);
        lines[2] = replace(itemFormat, ownerName, amount, itemName, item1, item2, priceText);
        lines[3] = replace(hasStock ? priceFormat : outOfStockLine, ownerName, amount, itemName, item1, item2, priceText);
        return lines;
    }

    private static String replace(String template, String ownerName, int amount, String itemName,
                                   String item1, String item2, String priceText) {
        String result = template;
        result = result.replace("{owner}", ownerName);
        result = result.replace("{amount}", Integer.toString(amount));
        result = result.replace("{item}", itemName);
        result = result.replace("{item1}", item1);
        result = result.replace("{item2}", item2);
        result = result.replace("{price}", priceText);
        return result;
    }
}
