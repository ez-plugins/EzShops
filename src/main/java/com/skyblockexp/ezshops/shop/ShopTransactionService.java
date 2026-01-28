package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.common.EconomyUtils;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Provides shared logic for buying and selling items through the shop.
 */
public class ShopTransactionService {

    public static final String PERMISSION_BUY = "ezshops.shop.buy";
    public static final String PERMISSION_SELL = "ezshops.shop.sell";
    public static final String PERMISSION_ADMIN_MINION_HEAD = "ezshops.shop.admin.minionhead";

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private final ShopPricingManager pricingManager;
    private final Economy economy;
    private final ShopMessageConfiguration.TransactionMessages.ErrorMessages errorMessages;
    private final ShopMessageConfiguration.TransactionMessages.SuccessMessages successMessages;
    private final ShopMessageConfiguration.TransactionMessages.NotificationMessages notificationMessages;
    private final ShopMessageConfiguration.TransactionMessages.CustomItemMessages customItemMessages;
    private final Map<EntityType, ItemStack> spawnerCache = new EnumMap<>(EntityType.class);
    private com.skyblockexp.ezshops.hook.TransactionHookService hookService;

    public ShopTransactionService(ShopPricingManager pricingManager, Economy economy,
            ShopMessageConfiguration.TransactionMessages transactionMessages) {
        this.pricingManager = pricingManager;
        this.economy = economy;
        this.errorMessages = transactionMessages.errors();
        this.successMessages = transactionMessages.success();
        this.notificationMessages = transactionMessages.notifications();
        this.customItemMessages = transactionMessages.customItems();
    }

    public void setTransactionHookService(com.skyblockexp.ezshops.hook.TransactionHookService hookService) {
        this.hookService = hookService;
    }

    private double getSellPriceMultiplier(Player player) {
        try {
            // Get the EzBoost plugin instance to access its class loader
            org.bukkit.plugin.Plugin ezBoostPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EzBoost");
            if (ezBoostPlugin == null) {
                return 1.0;
            }
            ClassLoader ezBoostClassLoader = ezBoostPlugin.getClass().getClassLoader();

            // Check if EzBoost classes are available using the plugin's class loader
            Class<?> ezBoostAPIClass = Class.forName("com.skyblockexp.ezboost.api.EzBoostAPI", true, ezBoostClassLoader);

            java.lang.reflect.Method getBoostManagerMethod = ezBoostAPIClass.getMethod("getBoostManager");
            Object boostManager = getBoostManagerMethod.invoke(null);
            if (boostManager == null) {
                return 1.0;
            }

            java.lang.reflect.Method getBoostsMethod = boostManager.getClass().getMethod("getBoosts", Player.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> boosts = (Map<String, Object>) getBoostsMethod.invoke(boostManager, player);
            double multiplier = 1.0;

            for (Object boost : boosts.values()) {
                java.lang.reflect.Method isActiveMethod = boostManager.getClass().getMethod("isActive", Player.class, String.class);
                java.lang.reflect.Method getKeyMethod = boost.getClass().getMethod("key");
                String key = (String) getKeyMethod.invoke(boost);

                Boolean isActive = (Boolean) isActiveMethod.invoke(boostManager, player, key);
                if (isActive) {
                    java.lang.reflect.Method getEffectsMethod = boost.getClass().getMethod("effects");
                    @SuppressWarnings("unchecked")
                    java.util.Collection<Object> effects = (java.util.Collection<Object>) getEffectsMethod.invoke(boost);

                    for (Object effect : effects) {
                        java.lang.reflect.Method getCustomNameMethod = effect.getClass().getMethod("customName");
                        String customName = (String) getCustomNameMethod.invoke(effect);
                        if ("ezshops_sellprice".equals(customName)) {
                            java.lang.reflect.Method getAmplifierMethod = effect.getClass().getMethod("amplifier");
                            Number amplifier = (Number) getAmplifierMethod.invoke(effect);
                            multiplier += amplifier.doubleValue() / 100.0;
                        }
                    }
                }
            }
            return multiplier;
        } catch (ClassNotFoundException e) {
            // EzBoost not available, return default multiplier
            return 1.0;
        } catch (Exception e) {
            // EzBoost integration failed, return default multiplier
            return 1.0;
        }
    }

    public ShopTransactionResult buy(Player player, Material material, int amount) {
        if (economy == null) {
            return ShopTransactionResult.failure(errorMessages.noEconomy());
        }

        if (!player.hasPermission(PERMISSION_BUY)) {
            return ShopTransactionResult.failure(errorMessages.noBuyPermission());
        }

        if (amount <= 0) {
            return ShopTransactionResult.failure(errorMessages.amountPositive());
        }

        ShopPrice price = pricingManager.getPrice(material).orElse(null);
        if (price == null) {
            return ShopTransactionResult.failure(errorMessages.notConfigured());
        }

        if (!price.canBuy()) {
            return ShopTransactionResult.failure(errorMessages.notBuyable());
        }

        double totalCost = pricingManager.estimateBulkTotal(material, amount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY);
        totalCost = EconomyUtils.normalizeCurrency(totalCost);
        if (totalCost <= 0) {
            return ShopTransactionResult.failure(errorMessages.invalidBuyPrice());
        }

        if (!hasInventorySpace(player, material, amount)) {
            return ShopTransactionResult.failure(errorMessages.noInventorySpace());
        }

        if (economy.getBalance(player) < totalCost) {
            return ShopTransactionResult.failure(errorMessages.cannotAfford());
        }

        EconomyResponse response = economy.withdrawPlayer(player, totalCost);
        if (!response.transactionSuccess()) {
            return ShopTransactionResult.failure(errorMessages.transactionFailed(response.errorMessage));
        }

        List<ItemStack> leftovers = giveItems(player, material, amount);
        handleLeftoverItems(player, leftovers);
        pricingManager.handlePurchase(material, amount);
        ShopTransactionResult result = ShopTransactionResult.success(successMessages.purchase(amount,
                ChatColor.AQUA + friendlyMaterialName(material), formatCurrency(totalCost)));
        // no item context available here; hooks are executed by overloads that have ShopMenuLayout.Item
        return result;
    }

    public ShopTransactionResult buy(Player player, com.skyblockexp.ezshops.shop.ShopMenuLayout.Item item, int amount) {
        ShopTransactionResult base = buy(player, item == null ? null : item.material(), amount);
        if (!base.success()) return base;
        if (hookService != null && item != null) {
            java.util.Map<String, String> tokens = new java.util.HashMap<>();
            tokens.put("amount", String.valueOf(amount));
            tokens.put("item", item.id());
            tokens.put("material", item.material().name());
            tokens.put("display", item.display() != null ? item.display().displayName() : "");
            tokens.put("price", item.price() != null ? formatCurrency(item.price().buyPrice()) : "");
            tokens.put("total", formatCurrency(pricingManager.estimateBulkTotal(item.material(), amount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY)));
            hookService.executeHooks(player, item.buyCommands(), item.commandsRunAsConsole() == null ? true : item.commandsRunAsConsole(), tokens);
            org.bukkit.Bukkit.getPluginManager().callEvent(new com.skyblockexp.ezshops.event.ShopPurchaseEvent(player, new ItemStack(item.material(), Math.max(1, amount)), amount, pricingManager.estimateBulkTotal(item.material(), amount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY)));
        }
        return base;
    }

    public ShopTransactionResult sell(Player player, Material material, int amount) {
        if (economy == null) {
            return ShopTransactionResult.failure(errorMessages.noEconomy());
        }

        if (!player.hasPermission(PERMISSION_SELL)) {
            return ShopTransactionResult.failure(errorMessages.noSellPermission());
        }

        if (amount <= 0) {
            return ShopTransactionResult.failure(errorMessages.amountPositive());
        }

        ShopPrice price = pricingManager.getPrice(material).orElse(null);
        if (price == null) {
            return ShopTransactionResult.failure(errorMessages.notConfigured());
        }

        if (!price.canSell()) {
            return ShopTransactionResult.failure(errorMessages.notSellable());
        }

        double totalGain = pricingManager.estimateBulkTotal(material, amount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.SELL);
        totalGain = EconomyUtils.normalizeCurrency(totalGain);
        totalGain *= getSellPriceMultiplier(player);
        if (totalGain <= 0) {
            return ShopTransactionResult.failure(errorMessages.invalidSellPrice());
        }

        int sellableAmount = countMaterial(player, material);
        if (sellableAmount < amount) {
            return ShopTransactionResult.failure(errorMessages.insufficientItems());
        }

        removeItems(player, material, amount);
        EconomyResponse response = economy.depositPlayer(player, totalGain);
        if (!response.transactionSuccess()) {
            List<ItemStack> leftovers = giveItems(player, material, amount);
            handleLeftoverItems(player, leftovers);
            return ShopTransactionResult.failure(errorMessages.transactionFailed(response.errorMessage));
        }

        pricingManager.handleSale(material, amount);
        ShopTransactionResult result = ShopTransactionResult.success(successMessages.sale(amount,
                ChatColor.AQUA + friendlyMaterialName(material), formatCurrency(totalGain)));
        // no item context available here; GUI callers may use the overload with item
        return result;
    }

    public ShopTransactionResult sell(Player player, com.skyblockexp.ezshops.shop.ShopMenuLayout.Item item, int amount) {
        ShopTransactionResult base = sell(player, item == null ? null : item.material(), amount);
        if (!base.success()) return base;
        if (hookService != null && item != null) {
            java.util.Map<String, String> tokens = new java.util.HashMap<>();
            tokens.put("amount", String.valueOf(amount));
            tokens.put("item", item.id());
            tokens.put("material", item.material().name());
            tokens.put("display", item.display() != null ? item.display().displayName() : "");
            tokens.put("price", item.price() != null ? formatCurrency(item.price().sellPrice()) : "");
            tokens.put("total", formatCurrency(pricingManager.estimateBulkTotal(item.material(), amount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.SELL)));
            hookService.executeHooks(player, item.sellCommands(), item.commandsRunAsConsole() == null ? true : item.commandsRunAsConsole(), tokens);
            org.bukkit.Bukkit.getPluginManager().callEvent(new com.skyblockexp.ezshops.event.ShopSaleEvent(player, new ItemStack(item.material(), Math.max(1, amount)), amount, pricingManager.estimateBulkTotal(item.material(), amount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.SELL)));
        }
        return base;
    }

    public ShopTransactionResult sellInventory(Player player) {
        if (economy == null) {
            return ShopTransactionResult.failure(errorMessages.noEconomy());
        }

        if (!player.hasPermission(PERMISSION_SELL)) {
            return ShopTransactionResult.failure(errorMessages.noSellPermission());
        }

        PlayerInventory inventory = player.getInventory();
        Map<Material, Integer> soldAmounts = new EnumMap<>(Material.class);
        double totalGain = 0.0D;

        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            Material material = stack.getType();
            ShopPrice price = pricingManager.getPrice(material).orElse(null);
            if (price == null || !price.canSell()) {
                continue;
            }

            double unitPrice = price.sellPrice();
            if (unitPrice <= 0) {
                continue;
            }

            int amount = stack.getAmount();
            if (amount <= 0) {
                continue;
            }

            soldAmounts.merge(material, amount, Integer::sum);
            // use estimator so progressive dynamic pricing is accounted for per material
            totalGain += pricingManager.estimateBulkTotal(material, amount, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.SELL);
        }

        if (soldAmounts.isEmpty()) {
            return ShopTransactionResult.failure(errorMessages.noSellableItems());
        }

        totalGain = EconomyUtils.normalizeCurrency(totalGain);
        totalGain *= getSellPriceMultiplier(player);
        if (totalGain <= 0) {
            return ShopTransactionResult.failure(errorMessages.noSellablePrices());
        }

        for (Map.Entry<Material, Integer> entry : soldAmounts.entrySet()) {
            removeItems(player, entry.getKey(), entry.getValue());
        }

        EconomyResponse response = economy.depositPlayer(player, totalGain);
        if (!response.transactionSuccess()) {
            List<ItemStack> leftovers = new ArrayList<>();
            for (Map.Entry<Material, Integer> entry : soldAmounts.entrySet()) {
                leftovers.addAll(giveItems(player, entry.getKey(), entry.getValue()));
            }
            handleLeftoverItems(player, leftovers);
            return ShopTransactionResult.failure(errorMessages.transactionFailed(response.errorMessage));
        }

        for (Map.Entry<Material, Integer> entry : soldAmounts.entrySet()) {
            pricingManager.handleSale(entry.getKey(), entry.getValue());
        }

        String soldItems = formatSoldInventorySummary(soldAmounts);
        return ShopTransactionResult.success(successMessages.sellInventory(soldItems, formatCurrency(totalGain)));
    }

    public ShopTransactionResult buyMinionCrateKey(Player player, double unitPrice, int quantity) {
        return purchaseCustomItem(player, unitPrice, quantity, Material.TRIPWIRE_HOOK,
                customItemMessages.minionCrateName(), customItemMessages.minionCrateLore());
    }

    public ShopTransactionResult buyVoteCrateKey(Player player, double unitPrice, int quantity) {
        return purchaseCustomItem(player, unitPrice, quantity, Material.TRIPWIRE_HOOK,
                customItemMessages.voteCrateName(), customItemMessages.voteCrateLore());
    }

    public ShopTransactionResult buySpawner(Player player, EntityType entityType, double unitPrice, int quantity) {
        if (economy == null) {
            return ShopTransactionResult.failure(errorMessages.noEconomy());
        }

        if (!player.hasPermission(PERMISSION_BUY)) {
            return ShopTransactionResult.failure(errorMessages.noBuyPermission());
        }

        if (entityType == null) {
            return ShopTransactionResult.failure(errorMessages.invalidSpawner());
        }

        if (quantity <= 0) {
            return ShopTransactionResult.failure(errorMessages.amountPositive());
        }

        double totalCost = EconomyUtils.normalizeCurrency(unitPrice * quantity);
        if (totalCost <= 0) {
            return ShopTransactionResult.failure(errorMessages.invalidCustomPrice());
        }

        if (economy.getBalance(player) < totalCost) {
            return ShopTransactionResult.failure(errorMessages.cannotAfford());
        }

        ItemStack template = spawnerCache.computeIfAbsent(entityType, this::createSpawnerItem);
        IntFunction<ItemStack> spawnerFactory = count -> {
            ItemStack stack = template.clone();
            stack.setAmount(count);
            return stack;
        };
        if (!hasInventorySpace(player, spawnerFactory, quantity)) {
            return ShopTransactionResult.failure(errorMessages.noInventorySpace());
        }

        EconomyResponse response = economy.withdrawPlayer(player, totalCost);
        if (!response.transactionSuccess()) {
            return ShopTransactionResult.failure(errorMessages.transactionFailed(response.errorMessage));
        }

        List<ItemStack> leftovers = giveSpawner(player, spawnerFactory, quantity);
        handleLeftoverItems(player, leftovers);
        String friendlyName = friendlyEntityName(entityType);
        return ShopTransactionResult.success(
                successMessages.spawnerPurchase(quantity, ChatColor.AQUA + friendlyName, formatCurrency(totalCost)));
    }

    public ShopTransactionResult buySpawner(Player player, com.skyblockexp.ezshops.shop.ShopMenuLayout.Item item, int quantity) {
        if (economy == null) {
            return ShopTransactionResult.failure(errorMessages.noEconomy());
        }

        if (!player.hasPermission(PERMISSION_BUY)) {
            return ShopTransactionResult.failure(errorMessages.noBuyPermission());
        }

        if (item == null) {
            return ShopTransactionResult.failure(errorMessages.notConfigured());
        }

        if (item.type() != com.skyblockexp.ezshops.shop.ShopMenuLayout.ItemType.SPAWNER) {
            return ShopTransactionResult.failure(errorMessages.notConfigured());
        }

        EntityType entityType = item.spawnerEntity();
        if (entityType == null) {
            return ShopTransactionResult.failure(errorMessages.invalidSpawner());
        }

        if (quantity <= 0) {
            return ShopTransactionResult.failure(errorMessages.amountPositive());
        }

        ShopPrice price = item.price();
        if (price == null || !price.canBuy()) {
            return ShopTransactionResult.failure(errorMessages.notBuyable());
        }

        double unitPrice = price.buyPrice();
        if (unitPrice <= 0) {
            return ShopTransactionResult.failure(errorMessages.invalidCustomPrice());
        }

        // Delegate core purchase logic to existing method which handles economy and item delivery
        ShopTransactionResult base = buySpawner(player, entityType, unitPrice, quantity);
        if (!base.success()) return base;

        // Update dynamic pricing and run hooks/events
        pricingManager.handlePurchase(item.material(), quantity);
        if (hookService != null) {
            java.util.Map<String, String> tokens = new java.util.HashMap<>();
            tokens.put("amount", String.valueOf(quantity));
            tokens.put("item", item.id());
            tokens.put("material", item.material().name());
            tokens.put("display", item.display() != null ? item.display().displayName() : "");
            tokens.put("price", item.price() != null ? formatCurrency(item.price().buyPrice()) : "");
            tokens.put("total", formatCurrency(pricingManager.estimateBulkTotal(item.material(), quantity, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY)));
            hookService.executeHooks(player, item.buyCommands(), item.commandsRunAsConsole() == null ? true : item.commandsRunAsConsole(), tokens);
            ItemStack purchased = createSpawnerItem(entityType);
            purchased.setAmount(Math.max(1, quantity));
            org.bukkit.Bukkit.getPluginManager().callEvent(new com.skyblockexp.ezshops.event.ShopPurchaseEvent(player, purchased, quantity, pricingManager.estimateBulkTotal(item.material(), quantity, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY)));
        }
        return base;
    }

    public ShopTransactionResult buyEnchantedBook(Player player, ShopMenuLayout.Item item, int quantity) {
        if (economy == null) {
            return ShopTransactionResult.failure(errorMessages.noEconomy());
        }

        if (!player.hasPermission(PERMISSION_BUY)) {
            return ShopTransactionResult.failure(errorMessages.noBuyPermission());
        }

        if (item == null) {
            return ShopTransactionResult.failure(errorMessages.notConfigured());
        }

        if (item.material() != Material.ENCHANTED_BOOK) {
            return ShopTransactionResult.failure(errorMessages.notConfigured());
        }

        if (quantity <= 0) {
            return ShopTransactionResult.failure(errorMessages.amountPositive());
        }

        Map<Enchantment, Integer> enchantments = item.enchantments();
        if (enchantments == null || enchantments.isEmpty()) {
            return ShopTransactionResult.failure(errorMessages.notConfigured());
        }

        ShopPrice price = item.price();
        if (price == null || !price.canBuy()) {
            return ShopTransactionResult.failure(errorMessages.notBuyable());
        }

        double unitPrice = price.buyPrice();
        if (unitPrice <= 0) {
            return ShopTransactionResult.failure(errorMessages.invalidCustomPrice());
        }

        double totalCost = EconomyUtils.normalizeCurrency(unitPrice * quantity);
        if (totalCost <= 0) {
            return ShopTransactionResult.failure(errorMessages.invalidCustomPrice());
        }

        String bookName = formatEnchantedBookName(enchantments);
        String displayName = bookName.isEmpty() ? null : ChatColor.LIGHT_PURPLE + bookName;

        IntFunction<ItemStack> bookFactory = count -> {
            ItemStack book = createEnchantedBook(enchantments, displayName);
            book.setAmount(Math.max(1, Math.min(book.getMaxStackSize(), count)));
            return book;
        };

        if (!hasInventorySpace(player, bookFactory, quantity)) {
            return ShopTransactionResult.failure(errorMessages.noInventorySpace());
        }

        if (economy.getBalance(player) < totalCost) {
            return ShopTransactionResult.failure(errorMessages.cannotAfford());
        }

        EconomyResponse response = economy.withdrawPlayer(player, totalCost);
        if (!response.transactionSuccess()) {
            return ShopTransactionResult.failure(errorMessages.transactionFailed(response.errorMessage));
        }

        List<ItemStack> leftovers = giveItems(player, bookFactory, quantity);
        handleLeftoverItems(player, leftovers);

        String friendlyName = bookName.isEmpty() ? friendlyMaterialName(item.material()) : bookName;
        // adjust dynamic pricing and execute hooks/events like other buy paths
        pricingManager.handlePurchase(item.material(), quantity);
        ShopTransactionResult result = ShopTransactionResult.success(successMessages.purchase(quantity,
            ChatColor.AQUA + friendlyName, formatCurrency(totalCost)));
        if (hookService != null) {
            java.util.Map<String, String> tokens = new java.util.HashMap<>();
            tokens.put("amount", String.valueOf(quantity));
            tokens.put("item", item.id());
            tokens.put("material", item.material().name());
            tokens.put("display", item.display() != null ? item.display().displayName() : "");
            tokens.put("price", item.price() != null ? formatCurrency(item.price().buyPrice()) : "");
            tokens.put("total", formatCurrency(pricingManager.estimateBulkTotal(item.material(), quantity, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY)));
            hookService.executeHooks(player, item.buyCommands(), item.commandsRunAsConsole() == null ? true : item.commandsRunAsConsole(), tokens);
            org.bukkit.Bukkit.getPluginManager().callEvent(new com.skyblockexp.ezshops.event.ShopPurchaseEvent(player, new ItemStack(item.material(), Math.max(1, quantity)), quantity, pricingManager.estimateBulkTotal(item.material(), quantity, com.skyblockexp.ezshops.gui.shop.ShopTransactionType.BUY)));
        }
        return result;
    }

    public String formatCurrency(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        synchronized (CURRENCY_FORMAT) {
            return CURRENCY_FORMAT.format(amount);
        }
    }

    public static String friendlyMaterialName(Material material) {
        String name = material.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
            builder.append(' ');
        }
        if (builder.length() == 0) {
            return name;
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private ShopTransactionResult purchaseCustomItem(Player player, double unitPrice, int quantity, Material material,
            String displayName, String loreLine) {
        if (economy == null) {
            return ShopTransactionResult.failure(errorMessages.noEconomy());
        }
        if (!player.hasPermission(PERMISSION_BUY)) {
            return ShopTransactionResult.failure(errorMessages.noBuyPermission());
        }
        if (quantity <= 0) {
            return ShopTransactionResult.failure(errorMessages.amountPositive());
        }

        double totalCost = EconomyUtils.normalizeCurrency(unitPrice * quantity);
        if (totalCost <= 0) {
            return ShopTransactionResult.failure(errorMessages.invalidCustomPrice());
        }

        IntFunction<ItemStack> itemFactory = count -> {
            ItemStack custom = createCustomItem(material, displayName, loreLine);
            custom.setAmount(count);
            return custom;
        };
        if (!hasInventorySpace(player, itemFactory, quantity)) {
            return ShopTransactionResult.failure(errorMessages.noInventorySpace());
        }

        if (economy.getBalance(player) < totalCost) {
            return ShopTransactionResult.failure(errorMessages.cannotAfford());
        }

        EconomyResponse response = economy.withdrawPlayer(player, totalCost);
        if (!response.transactionSuccess()) {
            return ShopTransactionResult.failure(errorMessages.transactionFailed(response.errorMessage));
        }

        List<ItemStack> leftovers = giveItems(player, itemFactory, quantity);
        handleLeftoverItems(player, leftovers);
        return ShopTransactionResult.success(successMessages.purchase(quantity, displayName,
                formatCurrency(totalCost)));
    }

    private ItemStack createCustomItem(Material material, String displayName, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (loreLine != null && !loreLine.isEmpty()) {
                meta.setLore(java.util.List.of(loreLine));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean hasInventorySpace(Player player, Material material, int amount) {
        return hasInventorySpace(player, count -> new ItemStack(material, count), amount);
    }

    private boolean hasInventorySpace(Player player, IntFunction<ItemStack> itemFactory, int quantity) {
        if (quantity <= 0) {
            return true;
        }
        Inventory snapshot = cloneStorageInventory(player);
        int remaining = quantity;
        while (remaining > 0) {
            ItemStack stack = itemFactory.apply(remaining);
            if (stack == null || stack.getType() == Material.AIR) {
                return true;
            }
            int stackSize = Math.min(stack.getMaxStackSize(), remaining);
            stack.setAmount(stackSize);
            Map<Integer, ItemStack> leftovers = snapshot.addItem(stack);
            if (!leftovers.isEmpty()) {
                return false;
            }
            remaining -= stackSize;
        }
        return true;
    }

    private int countMaterial(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeItems(Player player, Material material, int amount) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        int remaining = amount;

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }

            int toRemove = Math.min(stack.getAmount(), remaining);
            remaining -= toRemove;
            int newAmount = stack.getAmount() - toRemove;
            if (newAmount <= 0) {
                contents[i] = null;
            } else {
                stack.setAmount(newAmount);
                contents[i] = stack;
            }
        }

        inventory.setStorageContents(contents);
    }

    private List<ItemStack> giveItems(Player player, Material material, int amount) {
        return giveItems(player, count -> new ItemStack(material, count), amount);
    }

    private List<ItemStack> giveItems(Player player, IntFunction<ItemStack> itemFactory, int quantity) {
        List<ItemStack> leftovers = new ArrayList<>();
        if (quantity <= 0) {
            return leftovers;
        }
        PlayerInventory inventory = player.getInventory();
        int remaining = quantity;
        while (remaining > 0) {
            ItemStack stack = itemFactory.apply(remaining);
            if (stack == null || stack.getType() == Material.AIR) {
                break;
            }
            int stackSize = Math.min(stack.getMaxStackSize(), remaining);
            stack.setAmount(stackSize);
            Map<Integer, ItemStack> result = inventory.addItem(stack);
            if (!result.isEmpty()) {
                leftovers.addAll(result.values());
            }
            remaining -= stackSize;
        }
        return leftovers;
    }

    private List<ItemStack> giveSpawner(Player player, IntFunction<ItemStack> spawnerFactory, int quantity) {
        return giveItems(player, spawnerFactory, quantity);
    }

    private String formatSoldInventorySummary(Map<Material, Integer> soldAmounts) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : soldAmounts.entrySet()) {
            parts.add(ChatColor.AQUA + String.valueOf(entry.getValue()) + ChatColor.GREEN + "x " + ChatColor.AQUA
                    + friendlyMaterialName(entry.getKey()));
        }
        return String.join(ChatColor.GREEN + ", ", parts);
    }

    private void handleLeftoverItems(Player player, List<ItemStack> leftovers) {
        if (leftovers == null || leftovers.isEmpty()) {
            return;
        }
        for (ItemStack leftover : leftovers) {
            if (leftover == null || leftover.getType() == Material.AIR) {
                continue;
            }
            player.getWorld().dropItemNaturally(player.getLocation(), leftover.clone());
        }
        player.sendMessage(notificationMessages.inventoryLeftovers());
    }

    private Inventory cloneStorageInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        Inventory snapshot = Bukkit.createInventory(null, contents.length);
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            snapshot.setItem(i, item == null ? null : item.clone());
        }
        return snapshot;
    }

    private ItemStack createEnchantedBook(Map<Enchantment, Integer> enchantments, String displayName) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                storageMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
            }
            if (displayName != null && !displayName.isEmpty()) {
                storageMeta.setDisplayName(displayName);
            }
            book.setItemMeta(storageMeta);
            return book;
        }

        if (meta != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(displayName);
            }
            book.setItemMeta(meta);
        }
        return book;
    }

    private String formatEnchantedBookName(Map<Enchantment, Integer> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String name = friendlyEnchantmentName(entry.getKey());
            String level = toRomanNumeral(Math.max(1, entry.getValue()));
            parts.add(name + " " + level);
        }
        return String.join(", ", parts) + " Book";
    }

    private String friendlyEnchantmentName(Enchantment enchantment) {
        if (enchantment == null) {
            return "";
        }
        String key = enchantment.getKey().getKey().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        String[] parts = key.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
            builder.append(' ');
        }
        if (builder.length() == 0) {
            return key;
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private String toRomanNumeral(int number) {
        if (number <= 0) {
            return Integer.toString(number);
        }

        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int remaining = number;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length && remaining > 0; i++) {
            while (remaining >= values[i]) {
                builder.append(numerals[i]);
                remaining -= values[i];
            }
        }
        return builder.toString();
    }

    private ItemStack createSpawnerItem(EntityType entityType) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta) {
            if (blockStateMeta.getBlockState() instanceof CreatureSpawner spawner) {
                spawner.setSpawnedType(entityType);
                blockStateMeta.setBlockState(spawner);
            }
            blockStateMeta.setDisplayName(customItemMessages
                    .spawnerDisplayName(friendlyEntityName(entityType)));
            item.setItemMeta(blockStateMeta);
        }
        return item;
    }

    private String friendlyEntityName(EntityType entityType) {
        if (entityType == null) {
            return "";
        }
        String name = entityType.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
            builder.append(' ');
        }
        if (builder.length() == 0) {
            return name;
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }
}
