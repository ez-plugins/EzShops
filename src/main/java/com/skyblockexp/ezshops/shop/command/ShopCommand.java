package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.gui.ShopMenu;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles the {@code /shop} command, allowing players to open the shop GUI or use text commands to trade.
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final ShopPricingManager pricingManager;
    private final ShopTransactionService transactionService;
    private final ShopMenu shopMenu;
    private final boolean debug;
    private final ShopMessageConfiguration.CommandMessages.ShopCommandMessages messages;
    private final ShopMessageConfiguration.TransactionMessages.ErrorMessages errorMessages;
    private final ShopMessageConfiguration.TransactionMessages.RestrictionMessages restrictionMessages;

    public ShopCommand(ShopPricingManager pricingManager, ShopTransactionService transactionService,
            ShopMenu shopMenu, ShopMessageConfiguration.CommandMessages.ShopCommandMessages messages,
            ShopMessageConfiguration.TransactionMessages.ErrorMessages errorMessages,
            ShopMessageConfiguration.TransactionMessages.RestrictionMessages restrictionMessages,
            boolean debug) {
        this.pricingManager = pricingManager;
        this.transactionService = transactionService;
        this.shopMenu = shopMenu;
        this.messages = messages;
        this.errorMessages = errorMessages;
        this.restrictionMessages = restrictionMessages;
        this.debug = debug;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("ezshops.reload")) {
                sender.sendMessage("§cYou do not have permission to reload EzShops.");
                return true;
            }
            try {
                pricingManager.reload();
                if (shopMenu != null) shopMenu.refreshViewers();
                sender.sendMessage("§aEzShops configuration reloaded successfully.");
            } catch (Exception ex) {
                sender.sendMessage("§cFailed to reload EzShops: " + ex.getMessage());
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.playersOnly());
            return true;
        }

        if (args.length == 0 || (args.length == 1 && args[0].trim().isEmpty())) {
            if (shopMenu != null) {
                shopMenu.openMainMenu(player);
            } else {
                player.sendMessage(messages.menuDisabled());
            }
            return true;
        }

        // If the first argument is not a known action, treat the entire argument list as a
        // category name (supports multi-word display names like "Building Blocks").
        String firstArg = args.length > 0 ? args[0] : "";
        String actionLower = firstArg.toLowerCase(Locale.ENGLISH);
        if (!actionLower.equals("buy") && !actionLower.equals("sell") && !actionLower.equals("reload") && !actionLower.equals("import")) {
            String catArg = String.join(" ", args).trim();
            String lowerArg = catArg == null ? "" : catArg.trim().toLowerCase(Locale.ENGLISH);
            ShopMenuLayout layout = pricingManager.getMenuLayout();
            if (layout != null) {
                for (ShopMenuLayout.Category category : layout.categories()) {
                    // match by id
                    if (category.id().equalsIgnoreCase(catArg)) {
                        if (shopMenu == null) {
                            player.sendMessage(messages.menuDisabled());
                            return true;
                        }
                        if (category.command() != null && !category.command().isBlank()) {
                            String commandToRun = category.command().replace("{player}", player.getName());
                            player.closeInventory();
                            org.bukkit.Bukkit.getServer().dispatchCommand(player, commandToRun);
                        } else {
                            shopMenu.openCategory(player, category);
                        }
                        return true;
                    }

                    // match by display name (strip color codes and compare)
                    String disp = category.displayName();
                    if (disp != null && !disp.isBlank()) {
                        String plain = MessageUtil.stripColors(MessageUtil.translateColors(disp)).trim().toLowerCase(Locale.ENGLISH);
                        if (plain.equals(lowerArg) || plain.startsWith(lowerArg)) {
                            if (shopMenu == null) {
                                player.sendMessage(messages.menuDisabled());
                                return true;
                            }
                            if (category.command() != null && !category.command().isBlank()) {
                                String commandToRun = category.command().replace("{player}", player.getName());
                                player.closeInventory();
                                org.bukkit.Bukkit.getServer().dispatchCommand(player, commandToRun);
                            } else {
                                shopMenu.openCategory(player, category);
                            }
                            return true;
                        }
                    }
                }
            }
            player.sendMessage(messages.unknownCategory(catArg));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ENGLISH);

        // Handle template import: /shop import <templateId>
        if (action.equals("import")) {
            if (!player.hasPermission("ezshops.import")) {
                player.sendMessage("§cYou do not have permission to import templates.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cUsage: /shop import <templateId>");
                return true;
            }
            String templateId = args[1];
            var api = com.skyblockexp.ezshops.api.EzShopsAPI.getInstance();
            var svc = api.getTemplateAPI();
            if (svc == null) {
                player.sendMessage("§cTemplate service unavailable.");
                return true;
            }
            var opt = svc.importTemplate(templateId);
            if (opt.isEmpty()) {
                player.sendMessage("§cTemplate not found: " + templateId);
                return true;
            }
            com.skyblockexp.ezshops.shop.template.ShopTemplate template = opt.get();

            // Persist any template-provided categories as shop/category YAML files so pricing manager can load them
            var plugin = api.getPlugin();
            java.io.File categoriesDir = new java.io.File(plugin.getDataFolder(), "shop/categories");
            if (!categoriesDir.exists()) categoriesDir.mkdirs();
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            for (var e : template.categories().entrySet()) {
                String catId = e.getKey();
                com.skyblockexp.ezshops.shop.api.ShopTemplateCategory cat = e.getValue();
                java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
                out.put("categories", java.util.Map.of(catId, cat.toMap()));
                java.io.File outFile = new java.io.File(categoriesDir, catId + ".yml");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
                    String dump = yaml.dump(out);
                    fos.write(dump.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception ex) {
                    player.sendMessage("§cFailed to write category file: " + ex.getMessage());
                }
            }

            // Give any top-level items to the player
            for (java.util.Map<String, Object> itemDef : template.items()) {
                try {
                    if (itemDef.containsKey("itemstack-base64")) {
                        Object b64Obj = itemDef.get("itemstack-base64");
                        if (b64Obj instanceof String b64) {
                            try {
                                org.bukkit.inventory.ItemStack is = com.skyblockexp.ezshops.util.ItemStackSerializers.fromBase64(b64);
                                player.getInventory().addItem(is);
                            } catch (Exception ignored) {}
                        }
                        continue;
                    }
                    String matKey = null;
                    if (itemDef.containsKey("item")) matKey = String.valueOf(itemDef.get("item"));
                    if (matKey == null && itemDef.containsKey("material")) matKey = String.valueOf(itemDef.get("material"));
                    int amount = 1;
                    if (itemDef.containsKey("amount")) {
                        try { amount = Integer.parseInt(String.valueOf(itemDef.get("amount"))); } catch (Exception ignored) {}
                    }
                    if (matKey != null) {
                        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(matKey, false);
                        if (mat != null) {
                            org.bukkit.inventory.ItemStack is = new org.bukkit.inventory.ItemStack(mat, Math.max(1, amount));
                            player.getInventory().addItem(is);
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // Reload pricing so new categories are visible
            try {
                pricingManager.reload();
                if (shopMenu != null) shopMenu.refreshViewers();
            } catch (Exception ex) {
                player.sendMessage("§cFailed to reload shop configuration: " + ex.getMessage());
            }

            player.sendMessage("§aTemplate imported: " + template.name());
            return true;
        }

        String materialName = args[1];
        Material material = Material.matchMaterial(materialName, false);
        ShopMenuLayout.Item explicitItem = null;

        if (material == null) {
            explicitItem = findItemByKey(materialName);
            if (explicitItem == null) {
                player.sendMessage(messages.unknownItem(materialName));
                return true;
            }
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                player.sendMessage(messages.invalidAmount(args[2]));
                return true;
            }
        }

        if (amount <= 0) {
            player.sendMessage(messages.amountMustBePositive());
            return true;
        }

        ShopTransactionResult result;
        switch (action) {
            case "buy":
                if (explicitItem != null) {
                    result = transactionService.buy(player, explicitItem, amount);
                } else {
                    result = handleBuy(player, material, amount);
                }
                break;
            case "sell":
                if (explicitItem != null) {
                    result = transactionService.sell(player, explicitItem, amount);
                } else {
                    result = handleSell(player, material, amount);
                }
                break;
            default:
                sendUsage(player, label);
                return true;
        }

        player.sendMessage(result.message());
        return true;
    }

    private void sendUsage(Player player, String label) {
        for (String line : messages.usage(label)) {
            player.sendMessage(line);
        }
    }

    private ShopTransactionResult handleBuy(Player player, Material material, int amount) {
        ShopMenuLayout.ItemType itemType = pricingManager.getItemType(material);
        return switch (itemType) {
            case MATERIAL -> {
                // Try to find a configured ShopMenuLayout.Item for this material so hooks (on-buy) execute.
                ShopMenuLayout.Item item = findItemForMaterial(material);
                if (item != null) {
                    yield transactionService.buy(player, item, amount);
                }
                // If the material is part of a rotation but not present in the active menu rotation, reject the command
                if (!pricingManager.isVisibleInMenu(material) && pricingManager.isPartOfRotation(material)) {
                    yield ShopTransactionResult.failure(messages.notInRotation());
                }
                yield transactionService.buy(player, material, amount);
            }
            case MINION_CRATE_KEY -> executeCrateKeyPurchase(player, material, amount,
                    ShopMenuLayout.ItemType.MINION_CRATE_KEY);
            case VOTE_CRATE_KEY -> executeCrateKeyPurchase(player, material, amount,
                    ShopMenuLayout.ItemType.VOTE_CRATE_KEY);
            case ENCHANTED_BOOK -> ShopTransactionResult.failure(restrictionMessages.enchantedBookMenuOnly());
            case MINION_HEAD -> ShopTransactionResult.failure(restrictionMessages.minionHeadCrateOnly());
            case SPAWNER -> ShopTransactionResult.failure(restrictionMessages.spawnerMenuOnly());
        };
    }

    private ShopMenuLayout.Item findItemForMaterial(Material material) {
        ShopMenuLayout layout = pricingManager.getMenuLayout();
        if (layout == null) return null;
        for (ShopMenuLayout.Category category : layout.categories()) {
            for (ShopMenuLayout.Item item : category.items()) {
                if (item != null && item.material() == material) {
                    return item;
                }
            }
        }
        return null;
    }

    private ShopTransactionResult handleSell(Player player, Material material, int amount) {
        ShopMenuLayout.ItemType itemType = pricingManager.getItemType(material);
        return switch (itemType) {
            case MATERIAL -> {
                ShopMenuLayout.Item item = findItemForMaterial(material);
                if (item != null) yield transactionService.sell(player, item, amount);
                // log fallback so admins can detect missing item-context for hooks (only when debug enabled)
                if (debug) {
                    org.bukkit.Bukkit.getLogger().info("ShopCommand: falling back to material-only sell for " + material.name());
                }
                // Prevent selling items via command that are part of a rotation but not visible in the active menu rotation
                if (!pricingManager.isVisibleInMenu(material) && pricingManager.isPartOfRotation(material)) {
                    yield ShopTransactionResult.failure(messages.notInRotation());
                }
                yield transactionService.sell(player, material, amount);
            }
            case MINION_CRATE_KEY, VOTE_CRATE_KEY -> ShopTransactionResult.failure(restrictionMessages.enchantedBookMenuOnly());
            case ENCHANTED_BOOK -> ShopTransactionResult.failure(restrictionMessages.enchantedBookMenuOnly());
            case MINION_HEAD -> ShopTransactionResult.failure(restrictionMessages.minionHeadCrateOnly());
            case SPAWNER -> ShopTransactionResult.failure(restrictionMessages.spawnerMenuOnly());
        };
    }

    private ShopTransactionResult executeCrateKeyPurchase(Player player, Material material, int amount,
            ShopMenuLayout.ItemType itemType) {
        return pricingManager.getPrice(material).map(price -> {
            if (!price.canBuy()) {
                return ShopTransactionResult.failure(errorMessages.notBuyable());
            }
            double unitPrice = price.buyPrice();
            return itemType == ShopMenuLayout.ItemType.MINION_CRATE_KEY
                    ? transactionService.buyMinionCrateKey(player, unitPrice, amount)
                    : transactionService.buyVoteCrateKey(player, unitPrice, amount);
        }).orElseGet(() -> ShopTransactionResult.failure(errorMessages.notConfigured()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("buy", "sell", "reload"));
            ShopMenuLayout layout = pricingManager.getMenuLayout();
            if (layout != null) {
                for (ShopMenuLayout.Category category : layout.categories()) {
                    completions.add(category.id());
                    String disp = category.displayName();
                    if (disp != null && !disp.isBlank()) {
                        String plain = MessageUtil.stripColors(MessageUtil.translateColors(disp)).trim();
                        if (!plain.isEmpty()) completions.add(plain);
                    }
                }
            }
            return filterCompletions(args[0], completions);
        }

        if (args.length == 2) {
            String action = args[0].toLowerCase(Locale.ENGLISH);
            if (action.equals("buy")) {
                return filterBuyCompletions(args[1]);
            }
            if (action.equals("sell")) {
                return filterSellCompletions(args[1]);
            }
        }

        if (args.length == 3) {
            return filterCompletions(args[2], List.of("1", "16", "32", "64"));
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(String current, List<String> completions) {
        if (current.isEmpty()) {
            return completions;
        }

        String lowerCurrent = current.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase(Locale.ENGLISH).startsWith(lowerCurrent)) {
                matches.add(completion);
            }
        }
        return matches;
    }

    private ShopMenuLayout.Item findItemByKey(String key) {
        if (key == null) return null;
        ShopMenuLayout layout = pricingManager.getMenuLayout();
        if (layout == null) return null;
        String lower = key.toLowerCase(Locale.ENGLISH);
        for (ShopMenuLayout.Category category : layout.categories()) {
            for (ShopMenuLayout.Item item : category.items()) {
                if (item == null) continue;
                if (item.id().equalsIgnoreCase(key)) return item;
                if (item.priceId() != null && item.priceId().equalsIgnoreCase(key)) return item;
                if (item.material() != null && item.material().name().equalsIgnoreCase(key)) return item;
                // also match display name (lowercased) if it begins with key
                if (item.display() != null && item.display().displayName() != null
                        && item.display().displayName().toLowerCase(Locale.ENGLISH).startsWith(lower)) {
                    return item;
                }
            }
        }
        return null;
    }

    private List<String> filterBuyCompletions(String current) {
        Set<String> completions = new HashSet<>();
        String lowerCurrent = current.toLowerCase(Locale.ENGLISH);
        // include material names
        for (Material material : pricingManager.getBuyableMaterials()) {
            if (!isDirectBuySupported(material)) continue;
            String name = material.name().toLowerCase(Locale.ENGLISH);
            if (name.startsWith(lowerCurrent)) completions.add(name);
        }
        // include configured item ids and price-ids from menu layout
        ShopMenuLayout layout = pricingManager.getMenuLayout();
        if (layout != null) {
            for (ShopMenuLayout.Category category : layout.categories()) {
                for (ShopMenuLayout.Item item : category.items()) {
                    if (item == null) continue;
                    // determine whether item is buyable
                    boolean buyable = false;
                    if (item.priceId() != null) {
                        buyable = pricingManager.getPrice(item.priceId()).map(p -> p.canBuy()).orElse(item.price().canBuy());
                    } else {
                        buyable = pricingManager.getPrice(item.material()).map(p -> p.canBuy()).orElse(item.price().canBuy());
                    }
                    if (!buyable) continue;
                    String id = item.id();
                    if (id != null && id.toLowerCase(Locale.ENGLISH).startsWith(lowerCurrent)) completions.add(id.toLowerCase(Locale.ENGLISH));
                    String pid = item.priceId();
                    if (pid != null && pid.toLowerCase(Locale.ENGLISH).startsWith(lowerCurrent)) completions.add(pid.toLowerCase(Locale.ENGLISH));
                }
            }
        }
        return new ArrayList<>(completions);
    }

    private List<String> filterSellCompletions(String current) {
        Set<String> completions = new HashSet<>();
        String lowerCurrent = current.toLowerCase(Locale.ENGLISH);
        // include material names
        for (Material material : pricingManager.getSellableMaterials()) {
            String name = material.name().toLowerCase(Locale.ENGLISH);
            if (name.startsWith(lowerCurrent)) completions.add(name);
        }
        // include configured item ids and price-ids from menu layout
        ShopMenuLayout layout = pricingManager.getMenuLayout();
        if (layout != null) {
            for (ShopMenuLayout.Category category : layout.categories()) {
                for (ShopMenuLayout.Item item : category.items()) {
                    if (item == null) continue;
                    // determine whether item is sellable
                    boolean sellable = false;
                    if (item.priceId() != null) {
                        sellable = pricingManager.getPrice(item.priceId()).map(p -> p.canSell()).orElse(item.price().canSell());
                    } else {
                        sellable = pricingManager.getPrice(item.material()).map(p -> p.canSell()).orElse(item.price().canSell());
                    }
                    if (!sellable) continue;
                    String id = item.id();
                    if (id != null && id.toLowerCase(Locale.ENGLISH).startsWith(lowerCurrent)) completions.add(id.toLowerCase(Locale.ENGLISH));
                    String pid = item.priceId();
                    if (pid != null && pid.toLowerCase(Locale.ENGLISH).startsWith(lowerCurrent)) completions.add(pid.toLowerCase(Locale.ENGLISH));
                }
            }
        }
        return new ArrayList<>(completions);
    }

    private List<String> filterBuyMaterials(String current, Collection<Material> materials) {
        List<String> completions = new ArrayList<>();
        String lowerCurrent = current.toLowerCase(Locale.ENGLISH);
        for (Material material : materials) {
            if (!isDirectBuySupported(material)) {
                continue;
            }
            String name = material.name().toLowerCase(Locale.ENGLISH);
            if (name.startsWith(lowerCurrent)) {
                completions.add(name);
            }
        }
        return completions;
    }

    private boolean isDirectBuySupported(Material material) {
        ShopMenuLayout.ItemType type = pricingManager.getItemType(material);
        return switch (type) {
            case MATERIAL, MINION_CRATE_KEY, VOTE_CRATE_KEY -> true;
            default -> false;
        };
    }

    private List<String> filterMaterials(String current, Collection<Material> materials) {
        List<String> completions = new ArrayList<>();
        String lowerCurrent = current.toLowerCase(Locale.ENGLISH);
        for (Material material : materials) {
            String name = material.name().toLowerCase(Locale.ENGLISH);
            if (name.startsWith(lowerCurrent)) {
                completions.add(name);
            }
        }
        return completions;
    }
}
