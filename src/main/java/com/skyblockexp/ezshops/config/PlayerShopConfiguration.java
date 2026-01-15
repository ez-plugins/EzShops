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
    private final int minQuantity;
    private final int maxQuantity;
    private final double minPrice;
    private final double maxPrice;
    private final SignFormat signFormat;
    private final PlayerShopMessages messages;

    private PlayerShopConfiguration(boolean enabled, Set<String> headerTokens, boolean requireStockOnCreation,
            int minQuantity, int maxQuantity, double minPrice, double maxPrice, SignFormat signFormat,
            PlayerShopMessages messages) {
        this.enabled = enabled;
        this.headerTokens = headerTokens;
        this.requireStockOnCreation = requireStockOnCreation;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.signFormat = Objects.requireNonNull(signFormat, "signFormat");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public static PlayerShopConfiguration from(FileConfiguration configuration, Logger logger) {
        // Determine language from config
        String lang = configuration.getString("language", "en");
        String langFile = "messages/messages_" + lang + ".yml";
        org.bukkit.configuration.file.YamlConfiguration langYaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            new java.io.File(org.bukkit.Bukkit.getPluginManager().getPlugin("EzShops").getDataFolder(), langFile)
        );

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

        SignFormat signFormat = SignFormat.from(section.getConfigurationSection("sign-format"));

        // Use messages from the language YAML if present, else fallback to config
        org.bukkit.configuration.ConfigurationSection langMessages = langYaml.getConfigurationSection("player-shops.messages");
        PlayerShopMessages messages;
        if (langMessages != null) {
            messages = PlayerShopMessages.from(langMessages);
        } else {
            messages = PlayerShopMessages.from(section.getConfigurationSection("messages"));
        }

        return new PlayerShopConfiguration(enabled, normalizedHeaders, requireStock, minQuantity, maxQuantity, minPrice,
                maxPrice, signFormat, messages);
    }

    public static PlayerShopConfiguration defaults() {
        return new PlayerShopConfiguration(true, Collections.singleton(DEFAULT_HEADER), true, 1, 0, 0.0d, 0.0d,
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

    /**
     * Configurable text that controls how shop signs are displayed.
     */
    public static final class SignFormat {

        private final String availableHeader;
        private final String outOfStockHeader;
        private final String ownerFormat;
        private final String unknownOwnerName;
        private final String itemFormat;
        private final String priceFormat;
        private final String outOfStockLine;

        private SignFormat(String availableHeader, String outOfStockHeader, String ownerFormat,
                String unknownOwnerName, String itemFormat, String priceFormat, String outOfStockLine) {
            this.availableHeader = availableHeader;
            this.outOfStockHeader = outOfStockHeader;
            this.ownerFormat = ownerFormat;
            this.unknownOwnerName = unknownOwnerName;
            this.itemFormat = itemFormat;
            this.priceFormat = priceFormat;
            this.outOfStockLine = outOfStockLine;
        }

        public static SignFormat from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            String availableHeader = translate(section.getString("available-header", DEFAULT_AVAILABLE_HEADER));
            String outOfStockHeader = translate(section.getString("out-of-stock-header", DEFAULT_OUT_OF_STOCK_HEADER));
            String ownerFormat = translate(section.getString("owner-format", DEFAULT_OWNER_FORMAT));
            String unknownOwner = section.getString("unknown-owner-name", DEFAULT_UNKNOWN_OWNER);
            if (unknownOwner == null || unknownOwner.isBlank()) {
                unknownOwner = DEFAULT_UNKNOWN_OWNER;
            }
            String itemFormat = translate(section.getString("item-format", DEFAULT_ITEM_FORMAT));
            String priceFormat = translate(section.getString("price-format", DEFAULT_PRICE_FORMAT));
            String outOfStockLine = translate(section.getString("out-of-stock-line", DEFAULT_OUT_OF_STOCK_LINE));
            return new SignFormat(availableHeader, outOfStockHeader, ownerFormat, unknownOwner, itemFormat, priceFormat,
                    outOfStockLine);
        }

        public static SignFormat defaults() {
            return new SignFormat(translate(DEFAULT_AVAILABLE_HEADER), translate(DEFAULT_OUT_OF_STOCK_HEADER),
                    translate(DEFAULT_OWNER_FORMAT), DEFAULT_UNKNOWN_OWNER, translate(DEFAULT_ITEM_FORMAT),
                    translate(DEFAULT_PRICE_FORMAT), translate(DEFAULT_OUT_OF_STOCK_LINE));
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

        private static String translate(String value) {
            if (value == null) {
                return "";
            }
            return MessageUtil.translateColors(value);
        }
    }
}
