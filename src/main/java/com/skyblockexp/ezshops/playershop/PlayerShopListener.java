package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.config.PlayerShopConfiguration;
import com.skyblockexp.ezshops.playershop.PlayerShopMessages;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Handles Bukkit events that interact with player-created shops.
 */
public final class PlayerShopListener implements Listener {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private final PlayerShopManager manager;
    private final PlayerShopConfiguration configuration;
    private final PlayerShopMessages messages;

    public PlayerShopListener(PlayerShopManager manager, PlayerShopConfiguration configuration) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.messages = configuration.messages();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        String firstLine = event.getLine(0);
        if (firstLine == null || !configuration.isHeaderToken(firstLine)) {
            return;
        }

        if (!player.hasPermission(PlayerShopManager.PERMISSION_CREATE)) {
            player.sendMessage(messages.noPermissionCreate());
            event.setLine(0, failureHeader());
            return;
        }

        PlayerShopSetup pendingSetup = manager.normalizeSetup(manager.getPendingSetup(player.getUniqueId()));

        Block signBlock = event.getBlock();
        Block attachedBlock = getAttachedBlock(signBlock);
        if (attachedBlock == null || attachedBlock.getType() == Material.AIR) {
            player.sendMessage(messages.signRequiresChest());
            event.setLine(0, failureHeader());
            return;
        }

        int quantity = parseQuantity(event.getLine(1));
        double price = parsePrice(event.getLine(2));
        if (quantity <= 0 && pendingSetup != null) {
            quantity = pendingSetup.quantity();
        }
        if (price <= 0.0d && pendingSetup != null) {
            price = pendingSetup.price();
        }
        if (quantity <= 0) {
            player.sendMessage(messages.enterQuantity());
            event.setLine(1, messages.quantityPrompt());
            return;
        }
        if (price <= 0) {
            player.sendMessage(messages.enterPrice());
            event.setLine(2, messages.pricePrompt());
            return;
        }

        PlayerShopCreationResult result = manager.createShop(player, signBlock, attachedBlock, quantity, price);
        if (!result.success()) {
            player.sendMessage(result.message());
            event.setLine(0, failureHeader());
            return;
        }

        PlayerShop shop = result.shop();
        String[] lines = manager.formatSignLines(shop, true);
        for (int i = 0; i < lines.length && i < 4; i++) {
            event.setLine(i, lines[i]);
        }
        player.sendMessage(result.message());
        if (pendingSetup != null) {
            player.sendMessage(messages.usingSavedSettings());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock.getState() instanceof Sign) {
            PlayerShop shop = manager.getShopBySign(clickedBlock.getLocation());
            if (shop == null) {
                return;
            }
            event.setCancelled(true);
            ShopTransactionResult result = manager.purchase(shop, player);
            player.sendMessage(result.message());
            return;
        }

        PlayerShop shop = manager.getShopByChest(clickedBlock.getLocation());
        if (shop == null) {
            return;
        }
        if (configuration.protectionEnabled()) {
            if (shop.ownerId().equals(player.getUniqueId())) {
                return;
            }
            if (player.hasPermission(PlayerShopManager.PERMISSION_ADMIN)) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(messages.chestAccessDenied());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        PlayerShop shop = manager.getShopBySign(block.getLocation());
        if (shop == null) {
            shop = manager.getShopByChest(block.getLocation());
        }
        if (shop == null) {
            return;
        }
        Player player = event.getPlayer();
        boolean isOwner = shop.ownerId().equals(player.getUniqueId());
        if (configuration.protectionEnabled()) {
            if (!isOwner && !player.hasPermission(PlayerShopManager.PERMISSION_ADMIN)) {
                player.sendMessage(messages.breakDenied());
                event.setCancelled(true);
                return;
            }
        }
        manager.removeShop(shop);
        manager.saveShops();
        player.sendMessage(messages.shopRemoved());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof org.bukkit.block.DoubleChest doubleChest) {
            updateChestShop(doubleChest.getLeftSide());
            updateChestShop(doubleChest.getRightSide());
        } else if (holder instanceof org.bukkit.block.Chest chest) {
            updateChestShop(chest);
        }
    }

    private void updateChestShop(InventoryHolder holder) {
        if (holder instanceof org.bukkit.block.Chest chest) {
            PlayerShop shop = manager.getShopByChest(chest.getLocation());
            if (shop != null) {
                manager.refreshSign(shop);
            }
        }
    }

    private Block getAttachedBlock(Block signBlock) {
        if (signBlock == null) {
            return null;
        }
        BlockData data = signBlock.getBlockData();
        if (data instanceof WallSign wallSign) {
            BlockFace face = wallSign.getFacing().getOppositeFace();
            return signBlock.getRelative(face);
        }
        if (data instanceof Directional directional) {
            BlockFace face = directional.getFacing().getOppositeFace();
            return signBlock.getRelative(face);
        }
        return null;
    }

    private int parseQuantity(String text) {
        if (text == null) {
            return -1;
        }
        text = ChatColor.stripColor(text).trim();
        if (text.isEmpty()) {
            return -1;
        }
        if (text.startsWith("x") || text.startsWith("X")) {
            text = text.substring(1);
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private double parsePrice(String text) {
        if (text == null) {
            return -1;
        }
        text = ChatColor.stripColor(text).replaceAll("[^0-9.,]", "");
        if (text.isEmpty()) {
            return -1;
        }
        try {
            Number number = NUMBER_FORMAT.parse(text);
            return number != null ? number.doubleValue() : -1;
        } catch (Exception ex) {
            return -1;
        }
    }

    private String failureHeader() {
        String base = ChatColor.stripColor(configuration.signFormat().availableHeader());
        if (base == null || base.isEmpty()) {
            base = "[PlayerShop]";
        }
        return ChatColor.RED + base;
    }
}
