package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.common.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Collection of configurable messages used by the player shop system.
 */
public final class PlayerShopMessages {

    private static final String DEFAULT_NO_PERMISSION_CREATE = "&cYou do not have permission to create shops.";
    private static final String DEFAULT_SIGN_REQUIRES_CHEST = "&cShop signs must be attached to a chest.";
    private static final String DEFAULT_ENTER_QUANTITY = "&cEnter the quantity to sell on the second line.";
    private static final String DEFAULT_QUANTITY_PROMPT = "&cQty?";
    private static final String DEFAULT_ENTER_PRICE = "&cEnter the price on the third line.";
    private static final String DEFAULT_PRICE_PROMPT = "&cPrice?";
    private static final String DEFAULT_USING_SAVED_SETTINGS = "&aUsing your saved /playershop settings.";
    private static final String DEFAULT_CHEST_ACCESS_DENIED = "&cYou cannot open another player's shop chest.";
    private static final String DEFAULT_BREAK_DENIED = "&cYou cannot break someone else's shop.";
    private static final String DEFAULT_SHOP_REMOVED = "&eShop removed.";
    private static final String DEFAULT_CREATION_INVALID_CONFIGURATION = "&cInvalid shop configuration.";
    private static final String DEFAULT_CREATION_QUANTITY_POSITIVE = "&cQuantity must be greater than zero.";
    private static final String DEFAULT_CREATION_QUANTITY_MIN = "&cQuantity must be at least &b{min}&c.";
    private static final String DEFAULT_CREATION_QUANTITY_MAX = "&cQuantity cannot exceed &b{max}&c.";
    private static final String DEFAULT_CREATION_PRICE_POSITIVE = "&cPrice must be greater than zero.";
    private static final String DEFAULT_CREATION_PRICE_MIN = "&cPrice must be at least &6{min}&c.";
    private static final String DEFAULT_CREATION_PRICE_MAX = "&cPrice cannot exceed &6{max}&c.";
    private static final String DEFAULT_CREATION_SIGN_IN_USE = "&cThere is already a shop on this sign.";
    private static final String DEFAULT_CREATION_CHEST_UNRESOLVED = "&cShop chests could not be resolved.";
    private static final String DEFAULT_CREATION_CHEST_IN_USE = "&cA shop already uses that chest.";
    private static final String DEFAULT_CREATION_CHEST_INACCESSIBLE = "&cUnable to access the chest inventory.";
    private static final String DEFAULT_CREATION_SELECTED_ITEM_MISSING =
            "&cThe chest does not contain the item you selected in /playershop.";
    private static final String DEFAULT_CREATION_ITEM_MISSING = "&cPlace the item you want to sell inside the chest.";
    private static final String DEFAULT_CREATION_INSUFFICIENT_STOCK =
            "&cThe chest does not contain enough items to start the shop.";
    private static final String DEFAULT_CREATION_SUCCESS = "&aShop created successfully!";
    private static final String DEFAULT_PURCHASE_INVALID = "&cInvalid shop purchase attempt.";
    private static final String DEFAULT_PURCHASE_NO_PERMISSION = "&cYou do not have permission to buy from shops.";
    private static final String DEFAULT_PURCHASE_NO_ECONOMY =
            "&cThe shop is currently unavailable because no economy provider is configured.";
    private static final String DEFAULT_PURCHASE_OWN_SHOP = "&cYou cannot buy from your own shop.";
    private static final String DEFAULT_PURCHASE_MISSING_CHEST =
            "&cThe shop chest is missing. The listing has been removed.";
    private static final String DEFAULT_PURCHASE_OUT_OF_STOCK = "&cThis shop is currently out of stock.";
    private static final String DEFAULT_PURCHASE_NO_SPACE = "&cYou do not have enough inventory space.";
    private static final String DEFAULT_PURCHASE_TRANSACTION_FAILED = "&cTransaction failed: {error}";
    private static final String DEFAULT_PURCHASE_OWNER_NOTIFY =
            "&b{buyer} &abought &b{item}&a for &6{price}&a.";
    private static final String DEFAULT_PURCHASE_BUYER_SUCCESS =
            "&aPurchased &b{item}&a from &b{seller}&a for &6{price}&a.";
    private static final String DEFAULT_UNKNOWN_SELLER_NAME = "the seller";
    private static final String DEFAULT_SETUP_OPEN =
            "&aConfigure your player shop settings, then confirm to save them.";
    private static final String DEFAULT_SETUP_ITEM_SELECTED = "&aSelected &b{item}&a to sell.";
    private static final String DEFAULT_SETUP_ITEM_CLEARED =
            "&eCleared the selected item. The chest contents will decide what the shop sells.";
    private static final String DEFAULT_SETUP_SELECT_ITEM = "&eClick an item in your inventory to choose what to sell.";
    private static final String DEFAULT_SETUP_SAVED =
            "&aSaved your player shop settings. Place a [playershop] sign on a chest to use them.";
    private static final String DEFAULT_SETUP_QUANTITY_CHAT_PROMPT =
            "&eType the quantity to sell in chat, or type cancel to abort.";
    private static final String DEFAULT_SETUP_PRICE_CHAT_PROMPT =
            "&eType the price in chat, or type cancel to abort.";
    private static final String DEFAULT_SETUP_CHAT_CANCELLED =
            "&eInput cancelled. Keeping the previous value.";
    private static final String DEFAULT_SETUP_CHAT_INVALID_NUMBER =
            "&cPlease enter a valid number.";
    private static final String DEFAULT_SETUP_QUANTITY_UPDATED = "&aQuantity set to &b{amount}&a.";
    private static final String DEFAULT_SETUP_PRICE_UPDATED = "&aPrice set to &6{price}&a.";
    private static final String DEFAULT_COMMAND_PLAYERS_ONLY = "&cOnly players can use this command.";
    private static final String DEFAULT_COMMAND_DISABLED = "&cPlayer shops are currently disabled.";

    private static final String DEFAULT_MENU_INVENTORY_TITLE = "&2Player Shop Setup";
    private static final String DEFAULT_MENU_CONFIRM_BUTTON_NAME = "&aSave Settings";
    private static final List<String> DEFAULT_MENU_CONFIRM_BUTTON_LORE =
            List.of("&7Apply these values to your next shop.");
    private static final String DEFAULT_MENU_SELECT_ITEM_NAME = "&eSelect Item";
    private static final List<String> DEFAULT_MENU_SELECT_ITEM_LORE = List.of(
            "&7Click an item in your inventory to choose it.",
            "&7Current: None",
            "&7Click this slot to clear the selection.");
    private static final String DEFAULT_MENU_ITEM_SELECTED_CLEAR_HINT = "&7Click to clear this selection.";
    private static final String DEFAULT_MENU_QUANTITY_NAME = "&bQuantity";
    private static final String DEFAULT_MENU_QUANTITY_VALUE_FORMAT = "&ex{amount}";
    private static final String DEFAULT_MENU_PRICE_NAME = "&6Price";
    private static final String DEFAULT_MENU_PRICE_VALUE_FORMAT = "&e{price}";
    private static final String DEFAULT_MENU_QUANTITY_MINUS_ONE = "&c-1";
    private static final String DEFAULT_MENU_QUANTITY_MINUS_SIXTEEN = "&c-16";
    private static final String DEFAULT_MENU_QUANTITY_PLUS_SIXTEEN = "&a+16";
    private static final String DEFAULT_MENU_QUANTITY_PLUS_ONE = "&a+1";
    private static final String DEFAULT_MENU_PRICE_MINUS_ONE = "&c-1";
    private static final String DEFAULT_MENU_PRICE_MINUS_TEN = "&c-10";
    private static final String DEFAULT_MENU_PRICE_PLUS_TEN = "&a+10";
    private static final String DEFAULT_MENU_PRICE_PLUS_ONE = "&a+1";
    private static final String DEFAULT_MENU_QUANTITY_TYPE_NAME = "&bType Quantity";
    private static final List<String> DEFAULT_MENU_QUANTITY_TYPE_LORE = List.of(
            "&7Enter an exact quantity in chat.",
            "&7Type &fcancel &7to abort.");
    private static final String DEFAULT_MENU_PRICE_TYPE_NAME = "&6Type Price";
    private static final List<String> DEFAULT_MENU_PRICE_TYPE_LORE = List.of(
            "&7Enter an exact price in chat.",
            "&7Type &fcancel &7to abort.");
    private static final String DEFAULT_MENU_ITEM_DESCRIPTION_FORMAT = "&b{amount}&7x {item}";
    private static final String DEFAULT_MENU_UNKNOWN_ITEM_SINGULAR = "&bItem";
    private static final String DEFAULT_MENU_UNKNOWN_ITEM_PLURAL = "&bItems";

    private final String noPermissionCreate;
    private final String signRequiresChest;
    private final String enterQuantity;
    private final String quantityPrompt;
    private final String enterPrice;
    private final String pricePrompt;
    private final String usingSavedSettings;
    private final String chestAccessDenied;
    private final String breakDenied;
    private final String shopRemoved;
    private final String creationInvalidConfiguration;
    private final String creationQuantityPositive;
    private final String creationQuantityMin;
    private final String creationQuantityMax;
    private final String creationPricePositive;
    private final String creationPriceMin;
    private final String creationPriceMax;
    private final String creationSignInUse;
    private final String creationChestUnresolved;
    private final String creationChestInUse;
    private final String creationChestInaccessible;
    private final String creationSelectedItemMissing;
    private final String creationItemMissing;
    private final String creationInsufficientStock;
    private final String creationSuccess;
    private final String purchaseInvalid;
    private final String purchaseNoPermission;
    private final String purchaseNoEconomy;
    private final String purchaseOwnShop;
    private final String purchaseMissingChest;
    private final String purchaseOutOfStock;
    private final String purchaseNoSpace;
    private final String purchaseTransactionFailed;
    private final String purchaseOwnerNotify;
    private final String purchaseBuyerSuccess;
    private final String unknownSellerName;
    private final String setupOpen;
    private final String setupItemSelected;
    private final String setupItemCleared;
    private final String setupSelectItem;
    private final String setupSaved;
    private final String setupQuantityChatPrompt;
    private final String setupPriceChatPrompt;
    private final String setupChatCancelled;
    private final String setupChatInvalidNumber;
    private final String setupQuantityUpdated;
    private final String setupPriceUpdated;
    private final String commandPlayersOnly;
    private final String commandDisabled;
    private final MenuMessages menu;

    private PlayerShopMessages(String noPermissionCreate, String signRequiresChest, String enterQuantity,
            String quantityPrompt, String enterPrice, String pricePrompt, String usingSavedSettings,
            String chestAccessDenied, String breakDenied, String shopRemoved, String creationInvalidConfiguration,
            String creationQuantityPositive, String creationQuantityMin, String creationQuantityMax,
            String creationPricePositive, String creationPriceMin, String creationPriceMax, String creationSignInUse,
            String creationChestUnresolved, String creationChestInUse, String creationChestInaccessible,
            String creationSelectedItemMissing, String creationItemMissing, String creationInsufficientStock,
            String creationSuccess, String purchaseInvalid, String purchaseNoPermission, String purchaseNoEconomy,
            String purchaseOwnShop, String purchaseMissingChest, String purchaseOutOfStock, String purchaseNoSpace,
            String purchaseTransactionFailed, String purchaseOwnerNotify, String purchaseBuyerSuccess,
            String unknownSellerName, String setupOpen, String setupItemSelected, String setupItemCleared,
            String setupSelectItem, String setupSaved, String setupQuantityChatPrompt, String setupPriceChatPrompt,
            String setupChatCancelled, String setupChatInvalidNumber, String setupQuantityUpdated,
            String setupPriceUpdated, String commandPlayersOnly, String commandDisabled, MenuMessages menu) {
        this.noPermissionCreate = noPermissionCreate;
        this.signRequiresChest = signRequiresChest;
        this.enterQuantity = enterQuantity;
        this.quantityPrompt = quantityPrompt;
        this.enterPrice = enterPrice;
        this.pricePrompt = pricePrompt;
        this.usingSavedSettings = usingSavedSettings;
        this.chestAccessDenied = chestAccessDenied;
        this.breakDenied = breakDenied;
        this.shopRemoved = shopRemoved;
        this.creationInvalidConfiguration = creationInvalidConfiguration;
        this.creationQuantityPositive = creationQuantityPositive;
        this.creationQuantityMin = creationQuantityMin;
        this.creationQuantityMax = creationQuantityMax;
        this.creationPricePositive = creationPricePositive;
        this.creationPriceMin = creationPriceMin;
        this.creationPriceMax = creationPriceMax;
        this.creationSignInUse = creationSignInUse;
        this.creationChestUnresolved = creationChestUnresolved;
        this.creationChestInUse = creationChestInUse;
        this.creationChestInaccessible = creationChestInaccessible;
        this.creationSelectedItemMissing = creationSelectedItemMissing;
        this.creationItemMissing = creationItemMissing;
        this.creationInsufficientStock = creationInsufficientStock;
        this.creationSuccess = creationSuccess;
        this.purchaseInvalid = purchaseInvalid;
        this.purchaseNoPermission = purchaseNoPermission;
        this.purchaseNoEconomy = purchaseNoEconomy;
        this.purchaseOwnShop = purchaseOwnShop;
        this.purchaseMissingChest = purchaseMissingChest;
        this.purchaseOutOfStock = purchaseOutOfStock;
        this.purchaseNoSpace = purchaseNoSpace;
        this.purchaseTransactionFailed = purchaseTransactionFailed;
        this.purchaseOwnerNotify = purchaseOwnerNotify;
        this.purchaseBuyerSuccess = purchaseBuyerSuccess;
        this.unknownSellerName = unknownSellerName;
        this.setupOpen = setupOpen;
        this.setupItemSelected = setupItemSelected;
        this.setupItemCleared = setupItemCleared;
        this.setupSelectItem = setupSelectItem;
        this.setupSaved = setupSaved;
        this.setupQuantityChatPrompt = setupQuantityChatPrompt;
        this.setupPriceChatPrompt = setupPriceChatPrompt;
        this.setupChatCancelled = setupChatCancelled;
        this.setupChatInvalidNumber = setupChatInvalidNumber;
        this.setupQuantityUpdated = setupQuantityUpdated;
        this.setupPriceUpdated = setupPriceUpdated;
        this.commandPlayersOnly = commandPlayersOnly;
        this.commandDisabled = commandDisabled;
        this.menu = Objects.requireNonNull(menu, "menu");
    }

    public static PlayerShopMessages from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        MenuMessages menu = MenuMessages.from(section.getConfigurationSection("menu"));

        return new PlayerShopMessages(
                translate(section.getString("no-permission-create", DEFAULT_NO_PERMISSION_CREATE)),
                translate(section.getString("sign-requires-chest", DEFAULT_SIGN_REQUIRES_CHEST)),
                translate(section.getString("enter-quantity", DEFAULT_ENTER_QUANTITY)),
                translate(section.getString("quantity-prompt", DEFAULT_QUANTITY_PROMPT)),
                translate(section.getString("enter-price", DEFAULT_ENTER_PRICE)),
                translate(section.getString("price-prompt", DEFAULT_PRICE_PROMPT)),
                translate(section.getString("using-saved-settings", DEFAULT_USING_SAVED_SETTINGS)),
                translate(section.getString("chest-access-denied", DEFAULT_CHEST_ACCESS_DENIED)),
                translate(section.getString("break-denied", DEFAULT_BREAK_DENIED)),
                translate(section.getString("shop-removed", DEFAULT_SHOP_REMOVED)),
                translate(section.getString("creation-invalid-configuration", DEFAULT_CREATION_INVALID_CONFIGURATION)),
                translate(section.getString("creation-quantity-positive", DEFAULT_CREATION_QUANTITY_POSITIVE)),
                translate(section.getString("creation-quantity-min", DEFAULT_CREATION_QUANTITY_MIN)),
                translate(section.getString("creation-quantity-max", DEFAULT_CREATION_QUANTITY_MAX)),
                translate(section.getString("creation-price-positive", DEFAULT_CREATION_PRICE_POSITIVE)),
                translate(section.getString("creation-price-min", DEFAULT_CREATION_PRICE_MIN)),
                translate(section.getString("creation-price-max", DEFAULT_CREATION_PRICE_MAX)),
                translate(section.getString("creation-sign-in-use", DEFAULT_CREATION_SIGN_IN_USE)),
                translate(section.getString("creation-chest-unresolved", DEFAULT_CREATION_CHEST_UNRESOLVED)),
                translate(section.getString("creation-chest-in-use", DEFAULT_CREATION_CHEST_IN_USE)),
                translate(section.getString("creation-chest-inaccessible", DEFAULT_CREATION_CHEST_INACCESSIBLE)),
                translate(section.getString("creation-selected-item-missing", DEFAULT_CREATION_SELECTED_ITEM_MISSING)),
                translate(section.getString("creation-item-missing", DEFAULT_CREATION_ITEM_MISSING)),
                translate(section.getString("creation-insufficient-stock", DEFAULT_CREATION_INSUFFICIENT_STOCK)),
                translate(section.getString("creation-success", DEFAULT_CREATION_SUCCESS)),
                translate(section.getString("purchase-invalid", DEFAULT_PURCHASE_INVALID)),
                translate(section.getString("purchase-no-permission", DEFAULT_PURCHASE_NO_PERMISSION)),
                translate(section.getString("purchase-no-economy", DEFAULT_PURCHASE_NO_ECONOMY)),
                translate(section.getString("purchase-own-shop", DEFAULT_PURCHASE_OWN_SHOP)),
                translate(section.getString("purchase-missing-chest", DEFAULT_PURCHASE_MISSING_CHEST)),
                translate(section.getString("purchase-out-of-stock", DEFAULT_PURCHASE_OUT_OF_STOCK)),
                translate(section.getString("purchase-no-space", DEFAULT_PURCHASE_NO_SPACE)),
                translate(section.getString("purchase-transaction-failed", DEFAULT_PURCHASE_TRANSACTION_FAILED)),
                translate(section.getString("purchase-owner-notify", DEFAULT_PURCHASE_OWNER_NOTIFY)),
                translate(section.getString("purchase-buyer-success", DEFAULT_PURCHASE_BUYER_SUCCESS)),
                section.getString("unknown-seller-name", DEFAULT_UNKNOWN_SELLER_NAME),
                translate(section.getString("setup-open", DEFAULT_SETUP_OPEN)),
                translate(section.getString("setup-item-selected", DEFAULT_SETUP_ITEM_SELECTED)),
                translate(section.getString("setup-item-cleared", DEFAULT_SETUP_ITEM_CLEARED)),
                translate(section.getString("setup-select-item", DEFAULT_SETUP_SELECT_ITEM)),
                translate(section.getString("setup-saved", DEFAULT_SETUP_SAVED)),
                translate(section.getString("setup-quantity-chat-prompt", DEFAULT_SETUP_QUANTITY_CHAT_PROMPT)),
                translate(section.getString("setup-price-chat-prompt", DEFAULT_SETUP_PRICE_CHAT_PROMPT)),
                translate(section.getString("setup-chat-cancelled", DEFAULT_SETUP_CHAT_CANCELLED)),
                translate(section.getString("setup-chat-invalid-number", DEFAULT_SETUP_CHAT_INVALID_NUMBER)),
                translate(section.getString("setup-quantity-updated", DEFAULT_SETUP_QUANTITY_UPDATED)),
                translate(section.getString("setup-price-updated", DEFAULT_SETUP_PRICE_UPDATED)),
                translate(section.getString("command-players-only", DEFAULT_COMMAND_PLAYERS_ONLY)),
                translate(section.getString("command-disabled", DEFAULT_COMMAND_DISABLED)),
                menu);
    }

    public static PlayerShopMessages defaults() {
        return new PlayerShopMessages(translate(DEFAULT_NO_PERMISSION_CREATE), translate(DEFAULT_SIGN_REQUIRES_CHEST),
                translate(DEFAULT_ENTER_QUANTITY), translate(DEFAULT_QUANTITY_PROMPT),
                translate(DEFAULT_ENTER_PRICE), translate(DEFAULT_PRICE_PROMPT),
                translate(DEFAULT_USING_SAVED_SETTINGS), translate(DEFAULT_CHEST_ACCESS_DENIED),
                translate(DEFAULT_BREAK_DENIED), translate(DEFAULT_SHOP_REMOVED),
                translate(DEFAULT_CREATION_INVALID_CONFIGURATION), translate(DEFAULT_CREATION_QUANTITY_POSITIVE),
                translate(DEFAULT_CREATION_QUANTITY_MIN), translate(DEFAULT_CREATION_QUANTITY_MAX),
                translate(DEFAULT_CREATION_PRICE_POSITIVE), translate(DEFAULT_CREATION_PRICE_MIN),
                translate(DEFAULT_CREATION_PRICE_MAX), translate(DEFAULT_CREATION_SIGN_IN_USE),
                translate(DEFAULT_CREATION_CHEST_UNRESOLVED), translate(DEFAULT_CREATION_CHEST_IN_USE),
                translate(DEFAULT_CREATION_CHEST_INACCESSIBLE), translate(DEFAULT_CREATION_SELECTED_ITEM_MISSING),
                translate(DEFAULT_CREATION_ITEM_MISSING), translate(DEFAULT_CREATION_INSUFFICIENT_STOCK),
                translate(DEFAULT_CREATION_SUCCESS), translate(DEFAULT_PURCHASE_INVALID),
                translate(DEFAULT_PURCHASE_NO_PERMISSION), translate(DEFAULT_PURCHASE_NO_ECONOMY),
                translate(DEFAULT_PURCHASE_OWN_SHOP), translate(DEFAULT_PURCHASE_MISSING_CHEST),
                translate(DEFAULT_PURCHASE_OUT_OF_STOCK), translate(DEFAULT_PURCHASE_NO_SPACE),
                translate(DEFAULT_PURCHASE_TRANSACTION_FAILED), translate(DEFAULT_PURCHASE_OWNER_NOTIFY),
                translate(DEFAULT_PURCHASE_BUYER_SUCCESS), DEFAULT_UNKNOWN_SELLER_NAME,
                translate(DEFAULT_SETUP_OPEN), translate(DEFAULT_SETUP_ITEM_SELECTED),
                translate(DEFAULT_SETUP_ITEM_CLEARED), translate(DEFAULT_SETUP_SELECT_ITEM),
                translate(DEFAULT_SETUP_SAVED), translate(DEFAULT_SETUP_QUANTITY_CHAT_PROMPT),
                translate(DEFAULT_SETUP_PRICE_CHAT_PROMPT), translate(DEFAULT_SETUP_CHAT_CANCELLED),
                translate(DEFAULT_SETUP_CHAT_INVALID_NUMBER), translate(DEFAULT_SETUP_QUANTITY_UPDATED),
                translate(DEFAULT_SETUP_PRICE_UPDATED), translate(DEFAULT_COMMAND_PLAYERS_ONLY),
                translate(DEFAULT_COMMAND_DISABLED), MenuMessages.defaults());
    }

    public String noPermissionCreate() {
        return noPermissionCreate;
    }

    public String signRequiresChest() {
        return signRequiresChest;
    }

    public String enterQuantity() {
        return enterQuantity;
    }

    public String quantityPrompt() {
        return quantityPrompt;
    }

    public String enterPrice() {
        return enterPrice;
    }

    public String pricePrompt() {
        return pricePrompt;
    }

    public String usingSavedSettings() {
        return usingSavedSettings;
    }

    public String chestAccessDenied() {
        return chestAccessDenied;
    }

    public String breakDenied() {
        return breakDenied;
    }

    public String shopRemoved() {
        return shopRemoved;
    }

    public String creationInvalidConfiguration() {
        return creationInvalidConfiguration;
    }

    public String creationQuantityPositive() {
        return creationQuantityPositive;
    }

    public String creationQuantityMin(int min) {
        return format(creationQuantityMin, "min", Integer.toString(min));
    }

    public String creationQuantityMax(int max) {
        return format(creationQuantityMax, "max", Integer.toString(max));
    }

    public String creationPricePositive() {
        return creationPricePositive;
    }

    public String creationPriceMin(String minPrice) {
        return format(creationPriceMin, "min", Objects.toString(minPrice, ""));
    }

    public String creationPriceMax(String maxPrice) {
        return format(creationPriceMax, "max", Objects.toString(maxPrice, ""));
    }

    public String creationSignInUse() {
        return creationSignInUse;
    }

    public String creationChestUnresolved() {
        return creationChestUnresolved;
    }

    public String creationChestInUse() {
        return creationChestInUse;
    }

    public String creationChestInaccessible() {
        return creationChestInaccessible;
    }

    public String creationSelectedItemMissing() {
        return creationSelectedItemMissing;
    }

    public String creationItemMissing() {
        return creationItemMissing;
    }

    public String creationInsufficientStock() {
        return creationInsufficientStock;
    }

    public String creationSuccess() {
        return creationSuccess;
    }

    public String purchaseInvalid() {
        return purchaseInvalid;
    }

    public String purchaseNoPermission() {
        return purchaseNoPermission;
    }

    public String purchaseNoEconomy() {
        return purchaseNoEconomy;
    }

    public String purchaseOwnShop() {
        return purchaseOwnShop;
    }

    public String purchaseMissingChest() {
        return purchaseMissingChest;
    }

    public String purchaseOutOfStock() {
        return purchaseOutOfStock;
    }

    public String purchaseNoSpace() {
        return purchaseNoSpace;
    }

    public String purchaseTransactionFailed(String error) {
        String sanitized = error;
        if (sanitized == null || sanitized.isBlank()) {
            sanitized = "unknown error";
        }
        return format(purchaseTransactionFailed, "error", sanitized);
    }

    public String purchaseOwnerNotify(String buyer, String item, String price) {
        return format(purchaseOwnerNotify, "buyer", Objects.toString(buyer, ""), "item", Objects.toString(item, ""),
                "price", Objects.toString(price, ""));
    }

    public String purchaseBuyerSuccess(String item, String seller, String price) {
        return format(purchaseBuyerSuccess, "item", Objects.toString(item, ""), "seller",
                Objects.toString(seller, ""), "price", Objects.toString(price, ""));
    }

    public String unknownSellerName() {
        return unknownSellerName;
    }

    public String setupOpen() {
        return setupOpen;
    }

    public String setupItemSelected(String itemName) {
        return format(setupItemSelected, "item", Objects.toString(itemName, ""));
    }

    public String setupItemCleared() {
        return setupItemCleared;
    }

    public String setupSelectItem() {
        return setupSelectItem;
    }

    public String setupSaved() {
        return setupSaved;
    }

    public String setupQuantityChatPrompt() {
        return setupQuantityChatPrompt;
    }

    public String setupPriceChatPrompt() {
        return setupPriceChatPrompt;
    }

    public String setupChatCancelled() {
        return setupChatCancelled;
    }

    public String setupChatInvalidNumber() {
        return setupChatInvalidNumber;
    }

    public String setupQuantityUpdated(int amount) {
        return format(setupQuantityUpdated, "amount", Integer.toString(amount));
    }

    public String setupPriceUpdated(String price) {
        return format(setupPriceUpdated, "price", Objects.toString(price, ""));
    }

    public String commandPlayersOnly() {
        return commandPlayersOnly;
    }

    public String commandDisabled() {
        return commandDisabled;
    }

    public MenuMessages menu() {
        return menu;
    }

    private static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return com.skyblockexp.ezshops.config.ConfigTranslator.resolve(text, null);
    }

    private static List<String> translateList(List<String> lines, List<String> defaults) {
        List<String> source = lines;
        if (source == null || source.isEmpty()) {
            source = defaults;
        }
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<String> translated = new ArrayList<>(source.size());
        for (String line : source) {
            translated.add(translate(line));
        }
        return List.copyOf(translated);
    }

    private static String format(String template, Object... replacements) {
        if (template == null || template.isEmpty() || replacements.length == 0) {
            return template;
        }
        String result = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = Objects.toString(replacements[i], "");
            String value = Objects.toString(replacements[i + 1], "");
            result = result.replace("{" + key + "}", value);
        }
        return result;
    }

    /**
     * Message bundle containing all configurable text for the player shop setup GUI.
     */
    public static final class MenuMessages {

        private final String inventoryTitle;
        private final String confirmButtonName;
        private final List<String> confirmButtonLore;
        private final String selectItemName;
        private final List<String> selectItemLore;
        private final String itemSelectedClearHint;
        private final String quantityName;
        private final String quantityValueFormat;
        private final String priceName;
        private final String priceValueFormat;
        private final String quantityTypeName;
        private final List<String> quantityTypeLore;
        private final String priceTypeName;
        private final List<String> priceTypeLore;
        private final String quantityMinusOneLabel;
        private final String quantityMinusSixteenLabel;
        private final String quantityPlusSixteenLabel;
        private final String quantityPlusOneLabel;
        private final String priceMinusOneLabel;
        private final String priceMinusTenLabel;
        private final String pricePlusTenLabel;
        private final String pricePlusOneLabel;
        private final String itemDescriptionFormat;
        private final String unknownItemSingular;
        private final String unknownItemPlural;

        private MenuMessages(String inventoryTitle, String confirmButtonName, List<String> confirmButtonLore,
                String selectItemName, List<String> selectItemLore, String itemSelectedClearHint, String quantityName,
                String quantityValueFormat, String priceName, String priceValueFormat, String quantityTypeName,
                List<String> quantityTypeLore, String priceTypeName, List<String> priceTypeLore,
                String quantityMinusOneLabel, String quantityMinusSixteenLabel, String quantityPlusSixteenLabel,
                String quantityPlusOneLabel, String priceMinusOneLabel, String priceMinusTenLabel, String pricePlusTenLabel,
                String pricePlusOneLabel, String itemDescriptionFormat, String unknownItemSingular,
                String unknownItemPlural) {
            this.inventoryTitle = inventoryTitle;
            this.confirmButtonName = confirmButtonName;
            this.confirmButtonLore = confirmButtonLore;
            this.selectItemName = selectItemName;
            this.selectItemLore = selectItemLore;
            this.itemSelectedClearHint = itemSelectedClearHint;
            this.quantityName = quantityName;
            this.quantityValueFormat = quantityValueFormat;
            this.priceName = priceName;
            this.priceValueFormat = priceValueFormat;
            this.quantityTypeName = quantityTypeName;
            this.quantityTypeLore = quantityTypeLore;
            this.priceTypeName = priceTypeName;
            this.priceTypeLore = priceTypeLore;
            this.quantityMinusOneLabel = quantityMinusOneLabel;
            this.quantityMinusSixteenLabel = quantityMinusSixteenLabel;
            this.quantityPlusSixteenLabel = quantityPlusSixteenLabel;
            this.quantityPlusOneLabel = quantityPlusOneLabel;
            this.priceMinusOneLabel = priceMinusOneLabel;
            this.priceMinusTenLabel = priceMinusTenLabel;
            this.pricePlusTenLabel = pricePlusTenLabel;
            this.pricePlusOneLabel = pricePlusOneLabel;
            this.itemDescriptionFormat = itemDescriptionFormat;
            this.unknownItemSingular = unknownItemSingular;
            this.unknownItemPlural = unknownItemPlural;
        }

        public static MenuMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            ConfigurationSection confirmSection = section.getConfigurationSection("confirm-button");
            ConfigurationSection selectSection = section.getConfigurationSection("select-item");
            ConfigurationSection quantityDisplay = section.getConfigurationSection("quantity-display");
            ConfigurationSection priceDisplay = section.getConfigurationSection("price-display");
            ConfigurationSection quantityInput = section.getConfigurationSection("quantity-input");
            ConfigurationSection priceInput = section.getConfigurationSection("price-input");
            ConfigurationSection quantityAdjust = section.getConfigurationSection("quantity-adjust");
            ConfigurationSection priceAdjust = section.getConfigurationSection("price-adjust");
            ConfigurationSection unknownItem = section.getConfigurationSection("unknown-item");

            List<String> confirmLore = confirmSection != null ? confirmSection.getStringList("lore") : null;
            List<String> selectLore = selectSection != null ? selectSection.getStringList("lore") : null;
            List<String> quantityInputLore = quantityInput != null ? quantityInput.getStringList("lore") : null;
            List<String> priceInputLore = priceInput != null ? priceInput.getStringList("lore") : null;

            return new MenuMessages(
                    translate(section.getString("inventory-title", DEFAULT_MENU_INVENTORY_TITLE)),
                    translate(confirmSection != null
                            ? confirmSection.getString("name", DEFAULT_MENU_CONFIRM_BUTTON_NAME)
                            : DEFAULT_MENU_CONFIRM_BUTTON_NAME),
                    translateList(confirmLore, DEFAULT_MENU_CONFIRM_BUTTON_LORE),
                    translate(selectSection != null
                            ? selectSection.getString("name", DEFAULT_MENU_SELECT_ITEM_NAME)
                            : DEFAULT_MENU_SELECT_ITEM_NAME),
                    translateList(selectLore, DEFAULT_MENU_SELECT_ITEM_LORE),
                    translate(section.getString("item-selected-clear-hint", DEFAULT_MENU_ITEM_SELECTED_CLEAR_HINT)),
                    translate(quantityDisplay != null
                            ? quantityDisplay.getString("name", DEFAULT_MENU_QUANTITY_NAME)
                            : DEFAULT_MENU_QUANTITY_NAME),
                    translate(quantityDisplay != null
                            ? quantityDisplay.getString("value-format", DEFAULT_MENU_QUANTITY_VALUE_FORMAT)
                            : DEFAULT_MENU_QUANTITY_VALUE_FORMAT),
                    translate(priceDisplay != null ? priceDisplay.getString("name", DEFAULT_MENU_PRICE_NAME)
                            : DEFAULT_MENU_PRICE_NAME),
                    translate(priceDisplay != null
                            ? priceDisplay.getString("value-format", DEFAULT_MENU_PRICE_VALUE_FORMAT)
                            : DEFAULT_MENU_PRICE_VALUE_FORMAT),
                    translate(quantityInput != null
                            ? quantityInput.getString("name", DEFAULT_MENU_QUANTITY_TYPE_NAME)
                            : DEFAULT_MENU_QUANTITY_TYPE_NAME),
                    translateList(quantityInputLore, DEFAULT_MENU_QUANTITY_TYPE_LORE),
                    translate(priceInput != null
                            ? priceInput.getString("name", DEFAULT_MENU_PRICE_TYPE_NAME)
                            : DEFAULT_MENU_PRICE_TYPE_NAME),
                    translateList(priceInputLore, DEFAULT_MENU_PRICE_TYPE_LORE),
                    translate(quantityAdjust != null
                            ? quantityAdjust.getString("minus-one", DEFAULT_MENU_QUANTITY_MINUS_ONE)
                            : DEFAULT_MENU_QUANTITY_MINUS_ONE),
                    translate(quantityAdjust != null
                            ? quantityAdjust.getString("minus-sixteen", DEFAULT_MENU_QUANTITY_MINUS_SIXTEEN)
                            : DEFAULT_MENU_QUANTITY_MINUS_SIXTEEN),
                    translate(quantityAdjust != null
                            ? quantityAdjust.getString("plus-sixteen", DEFAULT_MENU_QUANTITY_PLUS_SIXTEEN)
                            : DEFAULT_MENU_QUANTITY_PLUS_SIXTEEN),
                    translate(quantityAdjust != null
                            ? quantityAdjust.getString("plus-one", DEFAULT_MENU_QUANTITY_PLUS_ONE)
                            : DEFAULT_MENU_QUANTITY_PLUS_ONE),
                    translate(priceAdjust != null
                            ? priceAdjust.getString("minus-one", DEFAULT_MENU_PRICE_MINUS_ONE)
                            : DEFAULT_MENU_PRICE_MINUS_ONE),
                    translate(priceAdjust != null
                            ? priceAdjust.getString("minus-ten", DEFAULT_MENU_PRICE_MINUS_TEN)
                            : DEFAULT_MENU_PRICE_MINUS_TEN),
                    translate(priceAdjust != null
                            ? priceAdjust.getString("plus-ten", DEFAULT_MENU_PRICE_PLUS_TEN)
                            : DEFAULT_MENU_PRICE_PLUS_TEN),
                    translate(priceAdjust != null
                            ? priceAdjust.getString("plus-one", DEFAULT_MENU_PRICE_PLUS_ONE)
                            : DEFAULT_MENU_PRICE_PLUS_ONE),
                    translate(section.getString("item-description-format", DEFAULT_MENU_ITEM_DESCRIPTION_FORMAT)),
                    translate(unknownItem != null
                            ? unknownItem.getString("singular", DEFAULT_MENU_UNKNOWN_ITEM_SINGULAR)
                            : DEFAULT_MENU_UNKNOWN_ITEM_SINGULAR),
                    translate(unknownItem != null
                            ? unknownItem.getString("plural", DEFAULT_MENU_UNKNOWN_ITEM_PLURAL)
                            : DEFAULT_MENU_UNKNOWN_ITEM_PLURAL));
        }

        public static MenuMessages defaults() {
            return new MenuMessages(translate(DEFAULT_MENU_INVENTORY_TITLE),
                    translate(DEFAULT_MENU_CONFIRM_BUTTON_NAME),
                    translateList(DEFAULT_MENU_CONFIRM_BUTTON_LORE, DEFAULT_MENU_CONFIRM_BUTTON_LORE),
                    translate(DEFAULT_MENU_SELECT_ITEM_NAME),
                    translateList(DEFAULT_MENU_SELECT_ITEM_LORE, DEFAULT_MENU_SELECT_ITEM_LORE),
                    translate(DEFAULT_MENU_ITEM_SELECTED_CLEAR_HINT), translate(DEFAULT_MENU_QUANTITY_NAME),
                    translate(DEFAULT_MENU_QUANTITY_VALUE_FORMAT), translate(DEFAULT_MENU_PRICE_NAME),
                    translate(DEFAULT_MENU_PRICE_VALUE_FORMAT), translate(DEFAULT_MENU_QUANTITY_TYPE_NAME),
                    translateList(DEFAULT_MENU_QUANTITY_TYPE_LORE, DEFAULT_MENU_QUANTITY_TYPE_LORE),
                    translate(DEFAULT_MENU_PRICE_TYPE_NAME),
                    translateList(DEFAULT_MENU_PRICE_TYPE_LORE, DEFAULT_MENU_PRICE_TYPE_LORE),
                    translate(DEFAULT_MENU_QUANTITY_MINUS_ONE),
                    translate(DEFAULT_MENU_QUANTITY_MINUS_SIXTEEN), translate(DEFAULT_MENU_QUANTITY_PLUS_SIXTEEN),
                    translate(DEFAULT_MENU_QUANTITY_PLUS_ONE), translate(DEFAULT_MENU_PRICE_MINUS_ONE),
                    translate(DEFAULT_MENU_PRICE_MINUS_TEN), translate(DEFAULT_MENU_PRICE_PLUS_TEN),
                    translate(DEFAULT_MENU_PRICE_PLUS_ONE), translate(DEFAULT_MENU_ITEM_DESCRIPTION_FORMAT),
                    translate(DEFAULT_MENU_UNKNOWN_ITEM_SINGULAR),
                    translate(DEFAULT_MENU_UNKNOWN_ITEM_PLURAL));
        }

        public String inventoryTitle() {
            return inventoryTitle;
        }

        public String confirmButtonName() {
            return confirmButtonName;
        }

        public List<String> confirmButtonLore() {
            return confirmButtonLore;
        }

        public String selectItemName() {
            return selectItemName;
        }

        public List<String> selectItemLore() {
            return selectItemLore;
        }

        public String itemSelectedClearHint() {
            return itemSelectedClearHint;
        }

        public String quantityName() {
            return quantityName;
        }

        public String quantityValue(int amount) {
            return format(quantityValueFormat, "amount", Integer.toString(amount));
        }

        public String priceName() {
            return priceName;
        }

        public String priceValue(String price) {
            return format(priceValueFormat, "price", Objects.toString(price, ""));
        }

        public String quantityTypeName() {
            return quantityTypeName;
        }

        public List<String> quantityTypeLore() {
            return quantityTypeLore;
        }

        public String priceTypeName() {
            return priceTypeName;
        }

        public List<String> priceTypeLore() {
            return priceTypeLore;
        }

        public String quantityMinusOneLabel() {
            return quantityMinusOneLabel;
        }

        public String quantityMinusSixteenLabel() {
            return quantityMinusSixteenLabel;
        }

        public String quantityPlusSixteenLabel() {
            return quantityPlusSixteenLabel;
        }

        public String quantityPlusOneLabel() {
            return quantityPlusOneLabel;
        }

        public String priceMinusOneLabel() {
            return priceMinusOneLabel;
        }

        public String priceMinusTenLabel() {
            return priceMinusTenLabel;
        }

        public String pricePlusTenLabel() {
            return pricePlusTenLabel;
        }

        public String pricePlusOneLabel() {
            return pricePlusOneLabel;
        }

        public String itemDescription(int amount, String itemName) {
            return format(itemDescriptionFormat, "amount", Integer.toString(amount), "item",
                    Objects.toString(itemName, ""));
        }

        public String unknownItemDescription(int amount) {
            String itemName = amount == 1 ? unknownItemSingular : unknownItemPlural;
            return format(itemDescriptionFormat, "amount", Integer.toString(amount), "item",
                    Objects.toString(itemName, ""));
        }

        public String unknownItemSingularPlain() {
            return ChatColor.stripColor(Objects.toString(unknownItemSingular, ""));
        }
    }
}
