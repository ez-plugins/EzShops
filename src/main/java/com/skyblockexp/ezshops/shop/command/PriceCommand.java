package com.skyblockexp.ezshops.shop.command;

import com.skyblockexp.ezshops.common.EconomyUtils;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * Handles the {@code /price} command, allowing players to check buy and sell prices for materials.
 */
public class PriceCommand implements CommandExecutor, TabCompleter {

    private final ShopPricingManager pricingManager;
    private final ShopTransactionService transactionService;
    private final ShopMessageConfiguration.CommandMessages.PriceCommandMessages messages;

    public PriceCommand(ShopPricingManager pricingManager, ShopTransactionService transactionService,
            ShopMessageConfiguration.CommandMessages.PriceCommandMessages messages) {
        this.pricingManager = pricingManager;
        this.transactionService = transactionService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || args[0].trim().isEmpty()) {
            sender.sendMessage(messages.usage(label));
            return true;
        }

        String materialName = args[0];
        Material material = Material.matchMaterial(materialName, false);
        if (material == null) {
            sender.sendMessage(messages.unknownItem(materialName));
            return true;
        }

        Optional<ShopPrice> priceLookup = pricingManager.getPrice(material);
        if (priceLookup.isEmpty()) {
            sender.sendMessage(messages.notConfigured());
            return true;
        }

        ShopPrice price = priceLookup.get();
        String displayName = ShopTransactionService.friendlyMaterialName(material);
        sender.sendMessage(messages.header(displayName));

        sender.sendMessage(resolveBuyLine(price));
        sender.sendMessage(resolveSellLine(price));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String current = args[0].toLowerCase(Locale.ENGLISH);
        Collection<Material> materials = pricingManager.getConfiguredMaterials();
        List<String> completions = new ArrayList<>();
        for (Material material : materials) {
            String name = material.name().toLowerCase(Locale.ENGLISH);
            if (current.isEmpty() || name.startsWith(current)) {
                completions.add(name);
            }
        }
        return completions;
    }

    private String resolveBuyLine(ShopPrice price) {
        double normalized = EconomyUtils.normalizeCurrency(price.buyPrice());
        if (!price.canBuy() || normalized <= 0.0D) {
            return messages.buyUnavailable();
        }
        return messages.buyLine(transactionService.formatCurrency(normalized));
    }

    private String resolveSellLine(ShopPrice price) {
        double normalized = EconomyUtils.normalizeCurrency(price.sellPrice());
        if (!price.canSell() || normalized <= 0.0D) {
            return messages.sellUnavailable();
        }
        return messages.sellLine(transactionService.formatCurrency(normalized));
    }
}
