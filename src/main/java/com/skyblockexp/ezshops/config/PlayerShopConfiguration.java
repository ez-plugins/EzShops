package com.skyblockexp.ezshops.config;

import com.skyblockexp.ezshops.common.MessageUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.skyblockexp.ezshops.playershop.PlayerShopMessages;
import com.skyblockexp.ezshops.playershop.SignFormat;

/**
 * Configuration values that control how player-created shops behave.
 */
public final class PlayerShopConfiguration {

    private static final String DEFAULT_HEADER = "[playershop]";
    private static final String DEFAULT_AVAILABLE_HEADER = "&2[PlayerShop]";
    private static final String DEFAULT_OUT_OF_STOCK_HEADER = "&c[PlayerShop]";
    private static final String DEFAULT_OWNER_FORMAT = "&7{owner}";
    private static final String DEFAULT_UNKNOWN_OWNER = "Owner";
    private static final String DEFAULT_ITEM_FORMAT = "&b{amount}&7x &b{item}";
    private static final String DEFAULT_PRICE_FORMAT = "&6{price}";
    private static final String DEFAULT_OUT_OF_STOCK_LINE = "&cOut of Stock";

    private final boolean enabled;
    private final Set<String> headerTokens;
    private final boolean requireStockOnCreation;
    private final boolean protectionEnabled;
    private final int minQuantity;
    private final int maxQuantity;
    private final double minPrice;
    private final double maxPrice;
    private final SignFormat signFormat;
    private final PlayerShopMessages messages;

    private PlayerShopConfiguration(boolean enabled, Set<String> headerTokens, boolean requireStockOnCreation,
            boolean protectionEnabled, int minQuantity, int maxQuantity, double minPrice, double maxPrice, SignFormat signFormat,
            PlayerShopMessages messages) {
        this.enabled = enabled;
        this.headerTokens = headerTokens;
        this.requireStockOnCreation = requireStockOnCreation;
        this.protectionEnabled = protectionEnabled;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.signFormat = Objects.requireNonNull(signFormat, "signFormat");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public static PlayerShopConfiguration from(FileConfiguration configuration, Logger logger, ShopMessageConfiguration messages) {

        ConfigurationSection section = configuration.getConfigurationSection("player-shops");
        if (section == null) {
            return defaults();
        }

        boolean enabled = section.getBoolean("enabled", true);

        List<String> headers = section.getStringList("headers");
        if (headers.isEmpty()) {
            headers = List.of(DEFAULT_HEADER);
        }
        Set<String> normalizedHeaders = new HashSet<>();
        for (String header : headers) {
            if (header == null) {
                continue;
            }
            String normalized = ChatColor.stripColor(header).trim().toLowerCase(Locale.US);
            if (!normalized.isEmpty()) {
                normalizedHeaders.add(normalized);
            }
        }
        if (normalizedHeaders.isEmpty()) {
            normalizedHeaders.add(DEFAULT_HEADER);
        }
        normalizedHeaders = Collections.unmodifiableSet(normalizedHeaders);

        boolean requireStock = section.getBoolean("require-stock-on-creation", true);

        int minQuantity = Math.max(1, section.getInt("min-quantity", 1));
        int maxQuantity = Math.max(0, section.getInt("max-quantity", 0));
        if (maxQuantity > 0 && maxQuantity < minQuantity) {
            if (logger != null) {
                logger.log(Level.WARNING, "player-shops.max-quantity is less than min-quantity; using {0}", minQuantity);
            }
            maxQuantity = minQuantity;
        }

        double minPrice = Math.max(0.0d, section.getDouble("min-price", 0.0d));
        double maxPrice = Math.max(0.0d, section.getDouble("max-price", 0.0d));
        if (maxPrice > 0.0d && maxPrice < minPrice) {
            if (logger != null) {
                logger.log(Level.WARNING, "player-shops.max-price is less than min-price; using {0}", minPrice);
            }
            maxPrice = minPrice;
        }

        SignFormat signFormat = SignFormat.from(section.getConfigurationSection("sign-format"), messages);

        PlayerShopMessages playerMessages = PlayerShopMessages.from(section.getConfigurationSection("messages"));

        boolean protection = section.getBoolean("protection-enabled", true);

        return new PlayerShopConfiguration(enabled, normalizedHeaders, requireStock, protection, minQuantity, maxQuantity, minPrice,
            maxPrice, signFormat, playerMessages);
    }

    public static PlayerShopConfiguration defaults() {
        return new PlayerShopConfiguration(true, Collections.singleton(DEFAULT_HEADER), true, true, 1, 0, 0.0d, 0.0d,
            SignFormat.defaults(), PlayerShopMessages.defaults());
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean isHeaderToken(String text) {
        if (text == null) {
            return false;
        }
        String normalized = ChatColor.stripColor(text).trim().toLowerCase(Locale.US);
        return headerTokens.contains(normalized);
    }

    public boolean requireStockOnCreation() {
        return requireStockOnCreation;
    }

    public boolean protectionEnabled() {
        return protectionEnabled;
    }

    public int minQuantity() {
        return minQuantity;
    }

    public int maxQuantity() {
        return maxQuantity;
    }

    public double minPrice() {
        return minPrice;
    }

    public double maxPrice() {
        return maxPrice;
    }

    public SignFormat signFormat() {
        return signFormat;
    }

    public PlayerShopMessages messages() {
        return messages;
    }

    // SignFormat moved to com.skyblockexp.ezshops.playershop.SignFormat
}
