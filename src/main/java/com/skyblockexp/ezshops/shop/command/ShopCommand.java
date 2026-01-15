package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.gui.ShopMenu;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private final ShopMessageConfiguration.CommandMessages.ShopCommandMessages messages;
    private final ShopMessageConfiguration.TransactionMessages.ErrorMessages errorMessages;
    private final ShopMessageConfiguration.TransactionMessages.RestrictionMessages restrictionMessages;

    public ShopCommand(ShopPricingManager pricingManager, ShopTransactionService transactionService,
            ShopMenu shopMenu, ShopMessageConfiguration.CommandMessages.ShopCommandMessages messages,
            ShopMessageConfiguration.TransactionMessages.ErrorMessages errorMessages,
            ShopMessageConfiguration.TransactionMessages.RestrictionMessages restrictionMessages) {
        this.pricingManager = pricingManager;
        this.transactionService = transactionService;
        this.shopMenu = shopMenu;
        this.messages = messages;
        this.errorMessages = errorMessages;
        this.restrictionMessages = restrictionMessages;
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

        if (args.length < 2) {
            sendUsage(player, label);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ENGLISH);
        String materialName = args[1];
        Material material = Material.matchMaterial(materialName, false);

        if (material == null) {
            player.sendMessage(messages.unknownItem(materialName));
            return true;
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
                result = handleBuy(player, material, amount);
                break;
            case "sell":
                result = transactionService.sell(player, material, amount);
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
            case MATERIAL -> transactionService.buy(player, material, amount);
            case MINION_CRATE_KEY -> executeCrateKeyPurchase(player, material, amount,
                    ShopMenuLayout.ItemType.MINION_CRATE_KEY);
            case VOTE_CRATE_KEY -> executeCrateKeyPurchase(player, material, amount,
                    ShopMenuLayout.ItemType.VOTE_CRATE_KEY);
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
            return filterCompletions(args[0], List.of("buy", "sell", "reload"));
        }

        if (args.length == 2) {
            String action = args[0].toLowerCase(Locale.ENGLISH);
            if (action.equals("buy")) {
                return filterBuyMaterials(args[1], pricingManager.getBuyableMaterials());
            }
            if (action.equals("sell")) {
                return filterMaterials(args[1], pricingManager.getSellableMaterials());
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
