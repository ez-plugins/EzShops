package com.skyblockexp.ezshops.config;

import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.shop.ShopSignListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Represents configurable formatting options for shop signs.
 */
public final class ShopSignConfiguration {

    private static final String DEFAULT_HEADER = "&2[Shop]";
    private static final String DEFAULT_BUY_FORMAT = "&aBuy &7x{amount}";
    private static final String DEFAULT_SELL_FORMAT = "&cSell &7x{amount}";
    private static final String DEFAULT_ITEM_FORMAT = "&b{item}";
    private static final String DEFAULT_PRICE_FORMAT = "&6{price}";
    private static final String DEFAULT_UNAVAILABLE_FORMAT = "&cUnavailable";
    private static final String DEFAULT_HEADER_TOKEN = "[shop]";

    private final Set<String> recognizedHeaders;
    private final String headerText;
    private final String buyFormat;
    private final String sellFormat;
    private final String itemFormat;
    private final String priceFormat;
    private final String unavailableFormat;

    private ShopSignConfiguration(Set<String> recognizedHeaders, String headerText, String buyFormat,
            String sellFormat, String itemFormat, String priceFormat, String unavailableFormat) {
        this.recognizedHeaders = Collections.unmodifiableSet(recognizedHeaders);
        this.headerText = headerText;
        this.buyFormat = buyFormat;
        this.sellFormat = sellFormat;
        this.itemFormat = itemFormat;
        this.priceFormat = priceFormat;
        this.unavailableFormat = unavailableFormat;
    }

    public static ShopSignConfiguration from(FileConfiguration configuration, Logger logger, ShopMessageConfiguration messages) {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(logger, "logger");

        ConfigurationSection signs = configuration.getConfigurationSection("signs");
        if (signs == null) {
            logger.warning("Missing 'signs' configuration section; using default shop sign formatting.");
            return defaults();
        }

        Set<String> headerTokens = new HashSet<>();
        List<String> configuredHeaders = new ArrayList<>(signs.getStringList("headers"));
        if (configuredHeaders.isEmpty()) {
            configuredHeaders.add(DEFAULT_HEADER_TOKEN);
        }
        for (String header : configuredHeaders) {
            String normalized = normalizeHeader(header);
            if (normalized != null) {
                headerTokens.add(normalized);
            }
        }

        ConfigurationSection display = signs.getConfigurationSection("display");
        if (display == null) {
            logger.warning("Missing 'signs.display' section; using default shop sign formatting.");
            return defaultsWithHeaders(headerTokens);
        }

        String headerText = ConfigTranslator.resolve(display.getString("header", DEFAULT_HEADER), messages);
        String buyFormat = ConfigTranslator.resolve(display.getString("buy-format", DEFAULT_BUY_FORMAT), messages);
        String sellFormat = ConfigTranslator.resolve(display.getString("sell-format", DEFAULT_SELL_FORMAT), messages);
        String itemFormat = ConfigTranslator.resolve(display.getString("item-format", DEFAULT_ITEM_FORMAT), messages);
        String priceFormat = ConfigTranslator.resolve(display.getString("price-format", DEFAULT_PRICE_FORMAT), messages);
        String unavailableFormat = ConfigTranslator.resolve(display.getString("unavailable-format", DEFAULT_UNAVAILABLE_FORMAT), messages);

        String normalizedHeader = normalizeHeader(headerText);
        if (normalizedHeader != null) {
            headerTokens.add(normalizedHeader);
        }

        if (headerTokens.isEmpty()) {
            String normalizedDefault = normalizeHeader(DEFAULT_HEADER_TOKEN);
            if (normalizedDefault != null) {
                headerTokens.add(normalizedDefault);
            }
        }

        return new ShopSignConfiguration(headerTokens, headerText, buyFormat, sellFormat, itemFormat, priceFormat,
                unavailableFormat);
    }

    public String headerText() {
        return headerText;
    }

    public boolean matchesHeader(String header) {
        String normalized = normalizeHeader(header);
        return normalized != null && recognizedHeaders.contains(normalized);
    }

    public String formatActionLine(ShopSignListener.SignAction action, int amount) {
        String format = action == ShopSignListener.SignAction.BUY ? buyFormat : sellFormat;
        return format.replace("{amount}", Integer.toString(Math.max(1, amount)));
    }

    public String formatItemLine(String itemName) {
        return itemFormat.replace("{item}", itemName == null ? "" : itemName);
    }

    public String formatPriceLine(String price) {
        return priceFormat.replace("{price}", price == null ? "" : price);
    }

    public String unavailableLine() {
        return unavailableFormat;
    }

    private static ShopSignConfiguration defaults() {
        Set<String> headers = new HashSet<>();
        String normalizedDefault = normalizeHeader(DEFAULT_HEADER_TOKEN);
        if (normalizedDefault != null) {
            headers.add(normalizedDefault);
        }
        return defaultsWithHeaders(headers);
    }

    private static ShopSignConfiguration defaultsWithHeaders(Collection<String> headers) {
        Set<String> normalizedHeaders = new HashSet<>();
        for (String header : headers) {
            if (header != null) {
                normalizedHeaders.add(header);
            }
        }
        String headerText = colorize(DEFAULT_HEADER);
        String normalizedHeader = normalizeHeader(headerText);
        if (normalizedHeader != null) {
            normalizedHeaders.add(normalizedHeader);
        }
        if (normalizedHeaders.isEmpty()) {
            String normalizedDefault = normalizeHeader(DEFAULT_HEADER_TOKEN);
            if (normalizedDefault != null) {
                normalizedHeaders.add(normalizedDefault);
            }
        }
        return new ShopSignConfiguration(normalizedHeaders, headerText, colorize(DEFAULT_BUY_FORMAT),
                colorize(DEFAULT_SELL_FORMAT), colorize(DEFAULT_ITEM_FORMAT), colorize(DEFAULT_PRICE_FORMAT),
                colorize(DEFAULT_UNAVAILABLE_FORMAT));
    }

    private static String colorize(String raw) {
        if (raw == null) {
            return "";
        }
        return MessageUtil.translateColors(raw);
    }

    private static String normalizeHeader(String raw) {
        if (raw == null) {
            return null;
        }
        String colorized = colorize(raw);
        String stripped = ChatColor.stripColor(colorized);
        if (stripped == null) {
            return null;
        }
        String trimmed = stripped.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ENGLISH);
    }
}
