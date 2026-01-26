package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.config.ConfigTranslator;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

/**
 * Sign formatting for player shops (extracted from PlayerShopConfiguration).
 */
public final class SignFormat {

    private static final String DEFAULT_AVAILABLE_HEADER = "&2[PlayerShop]";
    private static final String DEFAULT_OUT_OF_STOCK_HEADER = "&c[PlayerShop]";
    private static final String DEFAULT_OWNER_FORMAT = "&7{owner}";
    private static final String DEFAULT_UNKNOWN_OWNER = "Owner";
    private static final String DEFAULT_ITEM_FORMAT = "&b{amount}&7x &b{item}";
    private static final String DEFAULT_PRICE_FORMAT = "&6{price}";
    private static final String DEFAULT_OUT_OF_STOCK_LINE = "&cOut of Stock";

    private final String availableHeader;
    private final String outOfStockHeader;
    private final String ownerFormat;
    private final String unknownOwnerName;
    private final String itemFormat;
    private final String priceFormat;
    private final String outOfStockLine;

    public SignFormat(String availableHeader, String outOfStockHeader, String ownerFormat,
                      String unknownOwnerName, String itemFormat, String priceFormat, String outOfStockLine) {
        this.availableHeader = availableHeader;
        this.outOfStockHeader = outOfStockHeader;
        this.ownerFormat = ownerFormat;
        this.unknownOwnerName = unknownOwnerName;
        this.itemFormat = itemFormat;
        this.priceFormat = priceFormat;
        this.outOfStockLine = outOfStockLine;
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
        return new SignFormat(availableHeader, outOfStockHeader, ownerFormat, unknownOwner, itemFormat, priceFormat,
                outOfStockLine);
    }

    public static SignFormat defaults() {
        return new SignFormat(MessageUtil.translateColors(DEFAULT_AVAILABLE_HEADER), MessageUtil.translateColors(DEFAULT_OUT_OF_STOCK_HEADER),
                MessageUtil.translateColors(DEFAULT_OWNER_FORMAT), DEFAULT_UNKNOWN_OWNER, MessageUtil.translateColors(DEFAULT_ITEM_FORMAT),
                MessageUtil.translateColors(DEFAULT_PRICE_FORMAT), MessageUtil.translateColors(DEFAULT_OUT_OF_STOCK_LINE));
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

    public String[] formatLines(String ownerName, int amount, String itemName, String priceText, boolean hasStock) {
        Objects.requireNonNull(ownerName, "ownerName");
        String resolvedOwner = replace(ownerFormat, ownerName, amount, itemName, priceText);
        String resolvedItem = replace(itemFormat, ownerName, amount, itemName, priceText);
        String resolvedPrice = replace(priceFormat, ownerName, amount, itemName, priceText);
        String resolvedOutOfStock = replace(outOfStockLine, ownerName, amount, itemName, priceText);

        String[] lines = new String[4];
        lines[0] = hasStock ? availableHeader : outOfStockHeader;
        lines[1] = resolvedOwner;
        lines[2] = resolvedItem;
        lines[3] = hasStock ? resolvedPrice : resolvedOutOfStock;
        return lines;
    }

    private static String replace(String template, String ownerName, int amount, String itemName, String priceText) {
        String result = template;
        result = result.replace("{owner}", ownerName);
        result = result.replace("{amount}", Integer.toString(amount));
        result = result.replace("{item}", itemName);
        result = result.replace("{price}", priceText);
        return result;
    }
}
