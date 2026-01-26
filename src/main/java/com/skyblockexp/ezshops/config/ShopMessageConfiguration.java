package com.skyblockexp.ezshops.config;

import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.gui.shop.ShopTransactionType;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Provides strongly-typed access to localized EzShops messages.
 */
public final class ShopMessageConfiguration {

    private static final String BASE_PATH = "messages/messages";

    private final YamlConfiguration primary;
    private final YamlConfiguration fallback;

    private final CommandMessages commands;
    private final TransactionMessages transactions;
    private final GuiMessages gui;
    private final SignMessages signs;

    private ShopMessageConfiguration(YamlConfiguration primary, YamlConfiguration fallback) {
        this.primary = primary;
        this.fallback = fallback;
        this.commands = new CommandMessages();
        this.transactions = new TransactionMessages();
        this.gui = new GuiMessages();
        this.signs = new SignMessages();
    }

    public static ShopMessageConfiguration load(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        File dataFolder = plugin.getDataFolder();
        String language = normalizeLanguage(plugin.getConfig().getString("language"));

        YamlConfiguration fallback = loadConfiguration(dataFolder, languageFileName("en"));
        if (fallback == null) {
            plugin.getLogger()
                    .warning("Default English message file '" + languageFileName("en")
                            + "' is missing; using empty defaults.");
        }

        YamlConfiguration primary = fallback;
        if (!"en".equals(language)) {
            YamlConfiguration localized = loadConfiguration(dataFolder, languageFileName(language));
            if (localized != null) {
                primary = localized;
            } else {
                plugin.getLogger()
                        .warning("Configured language '" + language + "' is not available at '"
                                + languageFileName(language) + "'; falling back to English.");
            }
        }
        if (primary == null) {
            primary = new YamlConfiguration();
        }
        if (fallback == null) {
            fallback = new YamlConfiguration();
        }
        return new ShopMessageConfiguration(primary, fallback);
    }

    public CommandMessages commands() {
        return commands;
    }

    public TransactionMessages transactions() {
        return transactions;
    }

    public GuiMessages gui() {
        return gui;
    }

    public SignMessages signs() {
        return signs;
    }

    /**
     * Lookup a translation by path from the loaded primary messages, falling back to English.
     * Returns colorized text and falls back to the provided default when missing.
     */
    public String lookup(String path, String def) {
        return string(path, def);
    }

    private static String languageFileName(String language) {
        return BASE_PATH + '_' + language + ".yml";
    }

    private static String normalizeLanguage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "en";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        StringBuilder sanitized = new StringBuilder(normalized.length());
        boolean lastUnderscore = false;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (c == '_') {
                if (!lastUnderscore && sanitized.length() > 0) {
                    sanitized.append('_');
                }
                lastUnderscore = true;
                continue;
            }
            if (valid) {
                sanitized.append(c);
                lastUnderscore = false;
            }
        }
        String result = sanitized.toString();
        while (result.endsWith("_")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.isEmpty()) {
            return "en";
        }
        if (result.length() > 32) {
            result = result.substring(0, 32);
        }
        return result;
    }

    private static YamlConfiguration loadConfiguration(File dataFolder, String relativePath) {
        if (dataFolder == null || relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        File file = new File(dataFolder, relativePath.replace('/', File.separatorChar));
        if (!file.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private String string(String path, String def) {
        String value = getString(primary, path);
        if (value == null) {
            value = getString(fallback, path);
        }
        if (value == null) {
            value = def;
        }
        return colorize(value);
    }

    private List<String> stringList(String path, List<String> def) {
        List<String> value = getStringList(primary, path);
        if (value == null) {
            value = getStringList(fallback, path);
        }
        if (value == null) {
            value = def == null ? Collections.emptyList() : def;
        }
        List<String> colored = new ArrayList<>(value.size());
        for (String line : value) {
            colored.add(colorize(line));
        }
        return List.copyOf(colored);
    }

    private static String getString(ConfigurationSection section, String path) {
        if (section == null || path == null || path.isEmpty()) {
            return null;
        }
        return section.getString(path);
    }

    private static List<String> getStringList(ConfigurationSection section, String path) {
        if (section == null || path == null || path.isEmpty()) {
            return null;
        }
        List<String> list = section.getStringList(path);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list;
    }

    private static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return MessageUtil.translateColors(input);
    }

    private static String format(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty() || placeholders == null || placeholders.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static List<String> formatList(List<String> input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return input == null ? List.of() : input;
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return input;
        }
        List<String> formatted = new ArrayList<>(input.size());
        for (String line : input) {
            formatted.add(format(line, placeholders));
        }
        return List.copyOf(formatted);
    }

    public final class CommandMessages {

        private final ShopCommandMessages shop = new ShopCommandMessages();
        private final SellHandCommandMessages sellHand = new SellHandCommandMessages();
        private final SellInventoryCommandMessages sellInventory = new SellInventoryCommandMessages();
        private final PriceCommandMessages price = new PriceCommandMessages();
        private final SignShopScanCommandMessages signShopScan = new SignShopScanCommandMessages();

        public ShopCommandMessages shop() {
            return shop;
        }

        public SellHandCommandMessages sellHand() {
            return sellHand;
        }

        public SellInventoryCommandMessages sellInventory() {
            return sellInventory;
        }

        public PriceCommandMessages price() {
            return price;
        }

        public SignShopScanCommandMessages signShopScan() {
            return signShopScan;
        }

        public final class ShopCommandMessages {

            public String playersOnly() {
                return string("commands.shop.players-only", "&cOnly players can use the shop.");
            }

            public String menuDisabled() {
                return string("commands.shop.menu-disabled", "&cThe shop menu is disabled on this server.");
            }

            public List<String> usage(String label) {
                Map<String, String> placeholders = Map.of("{label}", label == null ? "shop" : label);
                List<String> defaults = List.of(
                        "&eUsage: /{label} - Open the shop menu",
                        "&eUsage: /{label} buy <item> [amount]",
                        "&eUsage: /{label} sell <item> [amount]");
                return formatList(stringList("commands.shop.usage", defaults), placeholders);
            }

            public String unknownItem(String item) {
                return format(string("commands.shop.unknown-item", "&cUnknown item: {item}"),
                        Map.of("{item}", item == null ? "" : item));
            }

            public String invalidAmount(String amount) {
                return format(string("commands.shop.invalid-amount", "&cInvalid amount: {amount}"),
                        Map.of("{amount}", amount == null ? "" : amount));
            }

            public String amountMustBePositive() {
                return string("commands.shop.amount-positive", "&cAmount must be positive.");
            }
        }

        public final class SellHandCommandMessages {

            public String playersOnly() {
                return string("commands.sell-hand.players-only", "&cOnly players can sell items.");
            }

            public String mustHoldItem() {
                return string("commands.sell-hand.must-hold-item", "&cYou must hold an item to use this command.");
            }
        }

        public final class SellInventoryCommandMessages {

            public String playersOnly() {
                return string("commands.sell-inventory.players-only", "&cOnly players can sell items.");
            }
        }

        public final class PriceCommandMessages {

            public String usage(String label) {
                return format(string("commands.price.usage", "&eUsage: /{label} <item>"),
                        Map.of("{label}", label == null ? "price" : label));
            }

            public String unknownItem(String item) {
                return format(string("commands.price.unknown-item", "&cUnknown item: {item}"),
                        Map.of("{item}", item == null ? "" : item));
            }

            public String notConfigured() {
                return string("commands.price.not-configured", "&cThat item is not configured in the shop.");
            }

            public String header(String item) {
                return format(string("commands.price.header", "&6Prices for &b{item}&6:"),
                        Map.of("{item}", item == null ? "" : item));
            }

            public String buyLine(String priceValue) {
                return format(string("commands.price.buy-line", "&a Buy: &6{price}"),
                        Map.of("{price}", priceValue == null ? "" : priceValue));
            }

            public String buyUnavailable() {
                return string("commands.price.buy-unavailable", "&a Buy: &7Unavailable");
            }

            public String sellLine(String priceValue) {
                return format(string("commands.price.sell-line", "&c Sell: &6{price}"),
                        Map.of("{price}", priceValue == null ? "" : priceValue));
            }

            public String sellUnavailable() {
                return string("commands.price.sell-unavailable", "&c Sell: &7Unavailable");
            }
        }

        public final class SignShopScanCommandMessages {

            public String playersOnly() {
                return string("commands.sign-shop-scan.players-only",
                        "&cOnly players can scan for shop signs.");
            }

            public String noPermission() {
                return string("commands.sign-shop-scan.no-permission",
                        "&cYou do not have permission to scan for shop signs.");
            }

            public String scannerUnavailable() {
                return string("commands.sign-shop-scan.scanner-unavailable",
                        "&cSign scanning is currently unavailable.");
            }

            public String invalidRadius(int min, int max) {
                int minimum = Math.max(1, min);
                int maximum = Math.max(minimum, max);
                return format(string("commands.sign-shop-scan.invalid-radius",
                        "&cRadius must be between {min} and {max}."),
                        Map.of("{min}", Integer.toString(minimum),
                                "{max}", Integer.toString(maximum)));
            }

            public String noSigns(int radius) {
                return format(string("commands.sign-shop-scan.no-signs",
                        "&eNo legacy shop signs found within &b{radius}&e blocks."),
                        Map.of("{radius}", Integer.toString(Math.max(0, radius))));
            }

            public String pendingReplaced() {
                return string("commands.sign-shop-scan.pending-replaced",
                        "&eReplaced your previous pending sign conversion with the new scan.");
            }

            public String found(int count, int radius) {
                return format(string("commands.sign-shop-scan.found",
                        "&aFound &b{count}&a legacy shop sign(s) within &b{radius}&a blocks."),
                        Map.of("{count}", Integer.toString(Math.max(0, count)),
                                "{radius}", Integer.toString(Math.max(0, radius))));
            }

            public String entry(String action, int amount, String item, int x, int y, int z) {
                return format(string("commands.sign-shop-scan.entry",
                        "&7- {action} &b{amount}&7x &b{item}&7 at &f{x}&7, &f{y}&7, &f{z}"),
                        Map.of("{action}", action == null ? "" : action,
                                "{amount}", Integer.toString(Math.max(1, amount)),
                                "{item}", item == null ? "" : item,
                                "{x}", Integer.toString(x),
                                "{y}", Integer.toString(y),
                                "{z}", Integer.toString(z)));
            }

            public String more(int remaining) {
                return format(string("commands.sign-shop-scan.more",
                        "&7...and &b{remaining}&7 more sign(s)."),
                        Map.of("{remaining}", Integer.toString(Math.max(0, remaining))));
            }

            public String limit(int limit) {
                return format(string("commands.sign-shop-scan.limit",
                        "&eOnly the first &b{limit}&e signs are shown. Scan with a smaller radius to review the rest."),
                        Map.of("{limit}", Integer.toString(Math.max(1, limit))));
            }

            public String confirmHint(String label) {
                return format(string("commands.sign-shop-scan.confirm-hint",
                        "&eRun &b/{label} confirm &eto convert them or &b/{label} cancel &eto discard."),
                        Map.of("{label}", label == null ? "signshop" : label));
            }

            public String noPending() {
                return string("commands.sign-shop-scan.no-pending",
                        "&eYou do not have any pending sign conversions.");
            }

            public String expired() {
                return string("commands.sign-shop-scan.expired",
                        "&cYour pending sign conversion has expired. Run the scan again.");
            }

            public String converted(int success) {
                return format(string("commands.sign-shop-scan.converted",
                        "&aConverted &b{success}&a sign(s) successfully."),
                        Map.of("{success}", Integer.toString(Math.max(0, success))));
            }

            public String convertedPartial(int success, int failed) {
                return format(string("commands.sign-shop-scan.converted-partial",
                        "&eConverted &b{success}&e sign(s), but &c{failed}&e could not be converted."),
                        Map.of("{success}", Integer.toString(Math.max(0, success)),
                                "{failed}", Integer.toString(Math.max(0, failed))));
            }

            public String convertedNone() {
                return string("commands.sign-shop-scan.converted-none",
                        "&cNo signs were converted. They may have been removed or are no longer valid.");
            }

            public String cancelled() {
                return string("commands.sign-shop-scan.cancelled",
                        "&eCancelled the pending sign conversion.");
            }

            public String unknown(String label) {
                return format(string("commands.sign-shop-scan.unknown",
                        "&cUnknown subcommand. Use /{label} scan, /{label} confirm, or /{label} cancel."),
                        Map.of("{label}", label == null ? "signshop" : label));
            }
        }




    }

    public final class TransactionMessages {

        private final ErrorMessages errors = new ErrorMessages();
        private final SuccessMessages success = new SuccessMessages();
        private final NotificationMessages notifications = new NotificationMessages();
        private final CustomItemMessages customItems = new CustomItemMessages();
        private final RestrictionMessages restrictions = new RestrictionMessages();

        public ErrorMessages errors() {
            return errors;
        }

        public SuccessMessages success() {
            return success;
        }

        public NotificationMessages notifications() {
            return notifications;
        }

        public CustomItemMessages customItems() {
            return customItems;
        }

        public RestrictionMessages restrictions() {
            return restrictions;
        }

        public final class ErrorMessages {

            public String noEconomy() {
                return string("transactions.errors.no-economy",
                        "&cThe shop is currently unavailable because no economy provider is configured.");
            }

            public String noBuyPermission() {
                return string("transactions.errors.no-buy-permission", "&cYou do not have permission to buy items.");
            }

            public String noSellPermission() {
                return string("transactions.errors.no-sell-permission", "&cYou do not have permission to sell items.");
            }

            public String amountPositive() {
                return string("transactions.errors.amount-positive", "&cAmount must be positive.");
            }

            public String notConfigured() {
                return string("transactions.errors.not-configured", "&cThat item is not configured in the shop.");
            }

            public String notBuyable() {
                return string("transactions.errors.not-buyable", "&cThat item cannot be purchased from the shop.");
            }

            public String notSellable() {
                return string("transactions.errors.not-sellable", "&cThat item cannot be sold to the shop.");
            }

            public String invalidBuyPrice() {
                return string("transactions.errors.invalid-buy-price", "&cThis item does not have a valid buy price.");
            }

            public String invalidSellPrice() {
                return string("transactions.errors.invalid-sell-price", "&cThis item does not have a valid sell price.");
            }

            public String invalidSpawner() {
                return string("transactions.errors.invalid-spawner", "&cThis spawner is not configured correctly.");
            }

            public String invalidCustomPrice() {
                return string("transactions.errors.invalid-custom-price", "&cThis item does not have a valid price.");
            }

            public String noInventorySpace() {
                return string("transactions.errors.no-inventory-space", "&cYou do not have enough inventory space.");
            }

            public String cannotAfford() {
                return string("transactions.errors.cannot-afford", "&cYou cannot afford this purchase.");
            }

            public String insufficientItems() {
                return string("transactions.errors.insufficient-items",
                        "&cYou do not have enough of that item to sell.");
            }

            public String noSellableItems() {
                return string("transactions.errors.no-sellable-items",
                        "&cYou do not have any sellable items in your inventory.");
            }

            public String noSellablePrices() {
                return string("transactions.errors.no-sellable-prices",
                        "&cYour inventory does not contain items with valid sell prices.");
            }

            public String transactionFailed(String error) {
                return format(string("transactions.errors.transaction-failed", "&cTransaction failed: {error}"),
                        Map.of("{error}", error == null ? "" : error));
            }
        }

        public final class SuccessMessages {

            public String purchase(int amount, String item, String price) {
                return format(string("transactions.success.purchase",
                        "&aPurchased &b{amount}&ax &b{item}&a for &6{price}&a."),
                        Map.of("{amount}", Integer.toString(Math.max(0, amount)),
                                "{item}", item == null ? "" : item,
                                "{price}", price == null ? "" : price));
            }

            public String sale(int amount, String item, String price) {
                return format(string("transactions.success.sale",
                        "&aSold &b{amount}&ax &b{item}&a for &6{price}&a."),
                        Map.of("{amount}", Integer.toString(Math.max(0, amount)),
                                "{item}", item == null ? "" : item,
                                "{price}", price == null ? "" : price));
            }

            public String sellInventory(String soldItems, String price) {
                return format(string("transactions.success.sell-inventory",
                        "&aSold {items}&a for &6{price}&a."),
                        Map.of("{items}", soldItems == null ? "" : soldItems,
                                "{price}", price == null ? "" : price));
            }

            public String spawnerPurchase(int amount, String item, String price) {
                return format(string("transactions.success.spawner-purchase",
                        "&aPurchased &b{amount}&ax &b{item}&a spawner for &6{price}&a."),
                        Map.of("{amount}", Integer.toString(Math.max(0, amount)),
                                "{item}", item == null ? "" : item,
                                "{price}", price == null ? "" : price));
            }
        }

        public final class NotificationMessages {

            public String inventoryLeftovers() {
                return string("transactions.notifications.inventory-leftovers",
                        "&eSome items were dropped at your feet because your inventory is full.");
            }
        }

        public final class CustomItemMessages {

            public String minionCrateName() {
                return string("transactions.custom-items.minion-crate.name", "&dMinion Crate Key");
            }

            public String minionCrateLore() {
                return string("transactions.custom-items.minion-crate.lore",
                        "&7A mysterious key said to unlock minion technology.");
            }

            public String voteCrateName() {
                return string("transactions.custom-items.vote-crate.name", "&bVote Crate Key");
            }

            public String voteCrateLore() {
                return string("transactions.custom-items.vote-crate.lore",
                        "&7Redeemable at any vote crate station.");
            }

            public String spawnerDisplayName(String entityName) {
                return format(string("transactions.custom-items.spawner-display-name",
                        "&6{entity} &eSpawner"),
                        Map.of("{entity}", entityName == null ? "" : entityName));
            }
        }

        public final class RestrictionMessages {

            public String minionHeadCrateOnly() {
                return string("transactions.restrictions.minion-head", "&cMinion heads can only be obtained from crates.");
            }

            public String spawnerMenuOnly() {
                return string("transactions.restrictions.spawner-menu",
                        "&cSpawners can only be purchased through the shop menu.");
            }

            public String enchantedBookMenuOnly() {
                return string("transactions.restrictions.enchanted-book-menu",
                        "&cEnchanted books can only be purchased through the shop menu.");
            }
        }
    }

    public final class GuiMessages {

        private final CommonMessages common = new CommonMessages();
        private final CustomInputMessages customInput = new CustomInputMessages();
        private final MenuMessages menus = new MenuMessages();

        public CommonMessages common() {
            return common;
        }

        public CustomInputMessages customInput() {
            return customInput;
        }

        public MenuMessages menus() {
            return menus;
        }

        public final class CommonMessages {

            public String categoryUnavailable() {
                return string("gui.common.category-unavailable", "&cThat shop category is no longer available.");
            }

            public String entryUnavailable() {
                return string("gui.common.entry-unavailable",
                        "&cThat shop entry could not be found. Please reopen the menu.");
            }

            public String islandLevelRequired(int level) {
                return format(string("gui.common.island-level-required",
                        "&cYou must reach island level &e{level}&c to use this shop item."),
                        Map.of("{level}", Integer.toString(Math.max(1, level))));
            }

            public String itemCannotBePurchased() {
                return string("gui.common.cannot-purchase", "&cThis item cannot be purchased from the shop.");
            }

            public String itemCannotBeSold() {
                return string("gui.common.cannot-sell", "&cThis item cannot be sold to the shop.");
            }

            public String priceUnavailable() {
                return string("gui.common.price-unavailable", "&cN/A");
            }

            public String actionWord(ShopTransactionType type) {
                if (type == ShopTransactionType.SELL) {
                    return string("gui.common.actions.sell", "sell");
                }
                return string("gui.common.actions.buy", "buy");
            }

            public String actionLabel(ShopTransactionType type) {
                if (type == ShopTransactionType.SELL) {
                    return string("gui.common.actions.sell-label", "Sell");
                }
                return string("gui.common.actions.buy-label", "Buy");
            }
        }

        public final class CustomInputMessages {

            public String prompt(ShopTransactionType type) {
                String action = common.actionWord(type);
                return format(string("gui.custom-input.prompt",
                        "&eEnter the amount to {action} &ein chat.&7 (type 'cancel' to abort)"),
                        Map.of("{action}", action));
            }

            public String cancelled() {
                return string("gui.custom-input.cancelled", "&7Cancelled custom amount.");
            }

            public String invalidNumber() {
                return string("gui.custom-input.invalid-number", "&cPlease enter a valid number.");
            }

            public String amountPositive() {
                return string("gui.custom-input.amount-positive", "&cAmount must be positive.");
            }
        }

        public final class MenuMessages {

            private final MainMenuMessages main = new MainMenuMessages();
            private final FlatMenuMessages flat = new FlatMenuMessages();
            private final CategoryMenuMessages category = new CategoryMenuMessages();
            private final QuantityMenuMessages quantity = new QuantityMenuMessages();

            public MainMenuMessages main() {
                return main;
            }

            public FlatMenuMessages flat() {
                return flat;
            }

            public CategoryMenuMessages category() {
                return category;
            }

            public QuantityMenuMessages quantity() {
                return quantity;
            }

            public final class MainMenuMessages {

                public String emptyTitle() {
                    return string("gui.menus.main.empty-title", "&cShop Unavailable");
                }

                public List<String> emptyLore() {
                    return stringList("gui.menus.main.empty-lore", List.of("&7No shop categories are configured."));
                }
            }

            public final class FlatMenuMessages {

                public String emptyTitle() {
                    return string("gui.menus.flat.empty-title", "&cShop Unavailable");
                }

                public List<String> emptyLore() {
                    return stringList("gui.menus.flat.empty-lore", List.of("&7No shop items are configured."));
                }

                public String previousTitle() {
                    return string("gui.menus.flat.previous-title", "&e\u2190 Previous Page");
                }

                public List<String> previousLore(int page) {
                    return formatList(stringList("gui.menus.flat.previous-lore", List.of("&7Go to page {page}")),
                            Map.of("{page}", Integer.toString(Math.max(1, page))));
                }

                public String nextTitle() {
                    return string("gui.menus.flat.next-title", "&eNext Page \u2192");
                }

                public List<String> nextLore(int page) {
                    return formatList(stringList("gui.menus.flat.next-lore", List.of("&7Go to page {page}")),
                            Map.of("{page}", Integer.toString(Math.max(1, page))));
                }

                public String pageIndicatorTitle(int current, int total) {
                    return format(string("gui.menus.flat.page-indicator-title", "&bPage {current} / {total}"),
                            Map.of("{current}", Integer.toString(Math.max(1, current)),
                                    "{total}", Integer.toString(Math.max(1, total))));
                }

                public List<String> pageIndicatorLore() {
                    return stringList("gui.menus.flat.page-indicator-lore", List.of("&7Browse all shop items."));
                }
            }

            public final class CategoryMenuMessages {

                public String emptyTitle() {
                    return string("gui.menus.category.empty-title", "&cNo Items");
                }

                public List<String> emptyLore() {
                    return stringList("gui.menus.category.empty-lore",
                            List.of("&7This category has no items configured."));
                }

                public String defaultBackTitle() {
                    return string("gui.menus.category.back-title", "&e\u2190 Back to Shop");
                }

                public List<String> defaultBackLore(String categoryName) {
                    return formatList(stringList("gui.menus.category.back-lore",
                            List.of("&7Return to the main shop menu.")),
                            Map.of("{category}", categoryName == null ? "" : categoryName,
                                    "{name}", categoryName == null ? "" : categoryName));
                }
            }

            public final class QuantityMenuMessages {

                public String titlePrefixBuy() {
                    return string("gui.menus.quantity.title-buy", "&aBuy ");
                }

                public String titlePrefixSell() {
                    return string("gui.menus.quantity.title-sell", "&6Sell ");
                }

                public String instructions() {
                    return string("gui.menus.quantity.instructions",
                            "&eLeft-click to buy, right-click to sell.");
                }

                public String buyLine(String price) {
                    return format(string("gui.menus.quantity.buy-line", "&7Buy each: &a{price}"),
                            Map.of("{price}", price == null ? "" : price));
                }

                public String buyStackLine(int amount, String price) {
                    return format(string("gui.menus.quantity.buy-stack-line", "&7Buy {amount}: &a{price}"),
                            Map.of("{amount}", Integer.toString(Math.max(1, amount)),
                                    "{price}", price == null ? "" : price));
                }

                public String buyUnavailable() {
                    return string("gui.menus.quantity.buy-unavailable", "&cBuying unavailable.");
                }

                public String sellLine(String price) {
                    return format(string("gui.menus.quantity.sell-line", "&7Sell each: &c{price}"),
                            Map.of("{price}", price == null ? "" : price));
                }

                public String sellStackLine(int amount, String price) {
                    return format(string("gui.menus.quantity.sell-stack-line", "&7Sell {amount}: &c{price}"),
                            Map.of("{amount}", Integer.toString(Math.max(1, amount)),
                                    "{price}", price == null ? "" : price));
                }

                public String sellUnavailable() {
                    return string("gui.menus.quantity.sell-unavailable", "&cCannot sell to the shop.");
                }

                public String optionTitle(int amount) {
                    return format(string("gui.menus.quantity.option-title", "&e{amount}x"),
                            Map.of("{amount}", Integer.toString(Math.max(1, amount))));
                }

                public List<String> optionLore(ShopTransactionType type, String price, int amount) {
                    String action = common.actionLabel(type);
                    Map<String, String> placeholders = Map.of(
                            "{amount}", Integer.toString(Math.max(1, amount)),
                            "{price}", price == null ? "" : price,
                            "{action}", action);
                    List<String> defaults = type == ShopTransactionType.SELL
                            ? List.of("&7Sell {amount}: &c{price}", "&eClick to {action} this amount.")
                            : List.of("&7Buy {amount}: &a{price}", "&eClick to {action} this amount.");
                    String path = type == ShopTransactionType.SELL
                            ? "gui.menus.quantity.option-sell-lore"
                            : "gui.menus.quantity.option-buy-lore";
                    return formatList(stringList(path, defaults), placeholders);
                }

                public String customTitle() {
                    return string("gui.menus.quantity.custom-title", "&bCustom Amount");
                }

                public List<String> customLore(ShopTransactionType type) {
                    String action = common.actionWord(type);
                    Map<String, String> placeholders = Map.of("{action}", action);
                    List<String> defaults = List.of(
                            "&7Enter a custom amount to {action}.",
                            "&8Type the amount in chat.",
                            "&8Type 'cancel' to go back.");
                    return formatList(stringList("gui.menus.quantity.custom-lore", defaults), placeholders);
                }

                public String backTitle() {
                    return string("gui.menus.quantity.back-title", "&e\u2190 Back to Shop");
                }

                public List<String> backLore(String categoryName) {
                    return formatList(stringList("gui.menus.quantity.back-lore",
                            List.of("&7Return to the main shop menu.")),
                            Map.of("{category}", categoryName == null ? "" : categoryName,
                                    "{name}", categoryName == null ? "" : categoryName));
                }

                public String levelRequirement(int level) {
                    return format(string("gui.menus.quantity.level-requirement", "&cRequires Island Level {level}."),
                            Map.of("{level}", Integer.toString(Math.max(1, level))));
                }
            }
        }
    }

    public final class SignMessages {

        public String noPermission() {
            return string("signs.no-permission", "&cYou do not have permission to create shop signs.");
        }

        public String invalidAction() {
            return string("signs.invalid-action", "&cSecond line must be either 'buy' or 'sell'.");
        }

        public String unknownItem(String item) {
            return format(string("signs.unknown-item", "&cUnknown item: {item}"),
                    Map.of("{item}", item == null ? "" : item));
        }

        public String invalidAmount() {
            return string("signs.invalid-amount", "&cFourth line must be a positive amount.");
        }

        public String notConfigured() {
            return string("signs.not-configured", "&cThat item is not configured in the shop.");
        }

        public String notAvailable(String action) {
            return format(string("signs.not-available",
                    "&cThat item cannot be {action} through the shop."),
                    Map.of("{action}", action == null ? "" : action));
        }

        public String ready(String actionLabel, int amount, String item) {
            return format(string("signs.ready", "&aShop sign ready: {action} &b{amount}&ax &b{item}&a."),
                    Map.of("{action}", actionLabel == null ? "" : actionLabel,
                            "{amount}", Integer.toString(Math.max(1, amount)),
                            "{item}", item == null ? "" : item));
        }

        public String malformed() {
            return string("signs.malformed", "&cThis shop sign is not configured correctly.");
        }

        public String actionVerbBuy() {
            return string("signs.actions.buy-verb", "purchased");
        }

        public String actionVerbSell() {
            return string("signs.actions.sell-verb", "sold");
        }

        public String actionLabelBuy() {
            return string("signs.actions.buy-label", "&aBuy");
        }

        public String actionLabelSell() {
            return string("signs.actions.sell-label", "&cSell");
        }
    }
}

