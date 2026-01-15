package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.common.CompatibilityUtil;
import com.skyblockexp.ezshops.common.EconomyUtils;
import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.config.ShopSignConfiguration;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Adds support for shop signs that allow players to buy and sell items without using /shop.
 */
public class ShopSignListener implements Listener {

    public static final String PERMISSION_CREATE = "ezshops.shop.sign.create";

    private final JavaPlugin plugin;
    private final ShopPricingManager pricingManager;
    private final ShopTransactionService transactionService;
    private final NamespacedKey actionKey;
    private final NamespacedKey materialKey;
    private final NamespacedKey amountKey;
    private final ShopSignConfiguration signConfiguration;
    private final ShopMessageConfiguration.SignMessages signMessages;

    public ShopSignListener(JavaPlugin plugin, ShopPricingManager pricingManager,
            ShopTransactionService transactionService, ShopSignConfiguration signConfiguration,
            ShopMessageConfiguration.SignMessages signMessages) {
        this.plugin = plugin;
        this.pricingManager = pricingManager;
        this.transactionService = transactionService;
        this.actionKey = new NamespacedKey(plugin, "shop_sign_action");
        this.materialKey = new NamespacedKey(plugin, "shop_sign_material");
        this.amountKey = new NamespacedKey(plugin, "shop_sign_amount");
        this.signConfiguration = signConfiguration;
        this.signMessages = signMessages;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        String rawHeader = serializeLine(event.getLine(0));
        if (!signConfiguration.matchesHeader(rawHeader)) {
            return;
        }

        Player player = event.getPlayer();
        if (player != null && !player.hasPermission(PERMISSION_CREATE)) {
            player.sendMessage(signMessages.noPermission());
            clearShopHeader(event);
            return;
        }

        SignAction action = SignAction.from(serializeLine(event.getLine(1)));
        if (action == null) {
            if (player != null) {
                player.sendMessage(signMessages.invalidAction());
            }
            clearShopHeader(event);
            return;
        }

        String materialToken = serializeLine(event.getLine(2));
        Material material = materialToken == null ? null : Material.matchMaterial(materialToken, false);
        if (material == null) {
            if (player != null) {
                player.sendMessage(signMessages.unknownItem(materialToken));
            }
            clearShopHeader(event);
            return;
        }

        int amount = parseAmount(serializeLine(event.getLine(3)));
        if (amount <= 0) {
            if (player != null) {
                player.sendMessage(signMessages.invalidAmount());
            }
            clearShopHeader(event);
            return;
        }

        Optional<ShopPrice> priceLookup = pricingManager.getPrice(material);
        if (priceLookup.isEmpty()) {
            if (player != null) {
                player.sendMessage(signMessages.notConfigured());
            }
            clearShopHeader(event);
            return;
        }

        ShopPrice price = priceLookup.get();
        double unitPrice = action == SignAction.BUY ? price.buyPrice() : price.sellPrice();
        if (unitPrice < 0) {
            if (player != null) {
                String verb = action == SignAction.BUY ? signMessages.actionVerbBuy() : signMessages.actionVerbSell();
                player.sendMessage(signMessages.notAvailable(verb));
            }
            clearShopHeader(event);
            return;
        }

        double totalPrice = EconomyUtils.normalizeCurrency(unitPrice * amount);
        updateEventLines(event, action, material, amount, totalPrice);

        Block block = event.getBlock();
        if (block != null) {
            Bukkit.getScheduler().runTask(plugin, () -> storeSignData(block, action, material, amount));
        }

        if (player != null) {
            player.sendMessage(signMessages.ready(resolveActionLabel(action), amount,
                    ShopTransactionService.friendlyMaterialName(material)));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }

        if (!signConfiguration.matchesHeader(ChatColor.stripColor(sign.getLine(0)))) {
            return;
        }

        PersistentDataContainer container = CompatibilityUtil.getPersistentDataContainer(sign);
        SignAction action = readAction(container);
        Material material = readMaterial(container);
        int amount = readAmount(container);

        if (action == null || material == null || amount <= 0) {
            event.getPlayer().sendMessage(signMessages.malformed());
            return;
        }

        ShopTransactionResult result = action == SignAction.BUY
                ? transactionService.buy(event.getPlayer(), material, amount)
                : transactionService.sell(event.getPlayer(), material, amount);
        event.getPlayer().sendMessage(result.message());

        Optional<ShopPrice> priceLookup = pricingManager.getPrice(material);
        double totalPrice = -1.0D;
        if (priceLookup.isPresent()) {
            ShopPrice price = priceLookup.get();
            double unitPrice = action == SignAction.BUY ? price.buyPrice() : price.sellPrice();
            if (unitPrice >= 0) {
                totalPrice = EconomyUtils.normalizeCurrency(unitPrice * amount);
            }
        }
        updateSignDisplay(sign, action, material, amount, totalPrice);
        storeSignData(block, action, material, amount);
    }

    private void storeSignData(Block block, SignAction action, Material material, int amount) {
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }
        PersistentDataContainer container = CompatibilityUtil.getPersistentDataContainer(sign);
        CompatibilityUtil.set(container, actionKey, PersistentDataType.STRING, action.name());
        CompatibilityUtil.set(container, materialKey, PersistentDataType.STRING, material.name());
        CompatibilityUtil.set(container, amountKey, PersistentDataType.INTEGER, Math.max(1, amount));
        sign.update(true);
    }

    private void updateEventLines(SignChangeEvent event, SignAction action, Material material, int amount,
            double totalPrice) {
        event.setLine(0, signConfiguration.headerText());
        event.setLine(1, formatActionLine(action, amount));
        event.setLine(2, signConfiguration
                .formatItemLine(ShopTransactionService.friendlyMaterialName(material)));
        if (totalPrice < 0) {
            event.setLine(3, signConfiguration.unavailableLine());
        } else {
            event.setLine(3, signConfiguration
                    .formatPriceLine(transactionService.formatCurrency(totalPrice)));
        }
    }

    private void updateSignDisplay(Sign sign, SignAction action, Material material, int amount, double totalPrice) {
        sign.setLine(0, signConfiguration.headerText());
        sign.setLine(1, formatActionLine(action, amount));
        sign.setLine(2, signConfiguration.formatItemLine(ShopTransactionService.friendlyMaterialName(material)));
        if (totalPrice < 0) {
            sign.setLine(3, signConfiguration.unavailableLine());
        } else {
            sign.setLine(3, signConfiguration
                    .formatPriceLine(transactionService.formatCurrency(totalPrice)));
        }
        sign.update(true);
    }

    private void clearShopHeader(SignChangeEvent event) {
        event.setLine(0, "");
    }

    private String resolveActionLabel(SignAction action) {
        return action == SignAction.BUY ? signMessages.actionLabelBuy() : signMessages.actionLabelSell();
    }

    private String formatActionLine(SignAction action, int amount) {
        return signConfiguration.formatActionLine(action, amount);
    }

    private SignAction readAction(PersistentDataContainer container) {
        String stored = CompatibilityUtil.get(container, actionKey, PersistentDataType.STRING);
        if (stored == null) {
            return null;
        }
        try {
            return SignAction.valueOf(stored);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Material readMaterial(PersistentDataContainer container) {
        String stored = CompatibilityUtil.get(container, materialKey, PersistentDataType.STRING);
        if (stored == null) {
            return null;
        }
        return Material.matchMaterial(stored, false);
    }

    private int readAmount(PersistentDataContainer container) {
        Integer stored = CompatibilityUtil.get(container, amountKey, PersistentDataType.INTEGER);
        return stored == null ? -1 : stored;
    }

    private String serializeLine(String line) {
        if (line == null) {
            return null;
        }
        return MessageUtil.stripColors(MessageUtil.translateLegacyColors(line));
    }

    private int parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public enum SignAction {
        BUY,
        SELL;

        private static SignAction from(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String normalized = raw.trim().toLowerCase(Locale.ENGLISH);
            return switch (normalized) {
                case "buy" -> BUY;
                case "sell" -> SELL;
                default -> null;
            };
        }
    }
}
