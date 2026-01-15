package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.config.PlayerShopConfiguration;
import com.skyblockexp.ezshops.playershop.PlayerShopMessages;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple inventory-based GUI that lets players configure their next shop sign.
 */
public final class PlayerShopSetupMenu implements Listener {

    private static final int INVENTORY_SIZE = 27;

    private static final int SLOT_CONFIRM = 4;
    private static final int SLOT_ITEM_DISPLAY = 12;
    private static final int SLOT_QUANTITY_DISPLAY = 13;
    private static final int SLOT_QUANTITY_INPUT = 14;
    private static final int SLOT_PRICE_DISPLAY = 22;
    private static final int SLOT_PRICE_INPUT = 23;

    private static final int SLOT_QUANTITY_MINUS_ONE = 9;
    private static final int SLOT_QUANTITY_MINUS_SIXTEEN = 10;
    private static final int SLOT_QUANTITY_PLUS_SIXTEEN = 16;
    private static final int SLOT_QUANTITY_PLUS_ONE = 17;

    private static final int SLOT_PRICE_MINUS_ONE = 18;
    private static final int SLOT_PRICE_MINUS_TEN = 19;
    private static final int SLOT_PRICE_PLUS_TEN = 25;
    private static final int SLOT_PRICE_PLUS_ONE = 26;

    private final JavaPlugin plugin;
    private final PlayerShopManager manager;
    private final PlayerShopConfiguration configuration;
    private final PlayerShopMessages messages;
    private final Map<UUID, MenuState> openMenus;
    private final Map<UUID, PendingChatInput> pendingInputs;

    public PlayerShopSetupMenu(JavaPlugin plugin, PlayerShopManager manager, PlayerShopConfiguration configuration) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.manager = Objects.requireNonNull(manager, "manager");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.messages = configuration.messages();
        this.openMenus = new HashMap<>();
        this.pendingInputs = new ConcurrentHashMap<>();
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        pendingInputs.remove(playerId);
        PlayerShopSetup existing = manager.normalizeSetup(manager.getPendingSetup(playerId));
        int startingQuantity = existing != null ? existing.quantity() : Math.max(1, configuration.minQuantity());
        double startingPrice = existing != null ? existing.price() : defaultPrice();
        MenuState state = new MenuState(startingQuantity, startingPrice);
        state.inventory = Bukkit.createInventory(player, INVENTORY_SIZE, messages.menu().inventoryTitle());
        state.itemTemplate = sanitizeTemplate(existing != null ? existing.itemTemplate() : null);
        openMenus.put(playerId, state);
        redraw(state);
        player.openInventory(state.inventory);
        player.sendMessage(messages.setupOpen());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        MenuState state = openMenus.get(player.getUniqueId());
        if (state == null || state.inventory == null) {
            return;
        }
        if (!event.getInventory().equals(state.inventory)) {
            return;
        }
        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        pendingInputs.remove(player.getUniqueId());
        if (rawSlot >= state.inventory.getSize()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null) {
                ItemStack template = sanitizeTemplate(clicked);
                if (template != null) {
                    state.itemTemplate = template;
                    player.sendMessage(messages.setupItemSelected(describeItem(template)));
                    redraw(state);
                }
            }
            return;
        }
        if (rawSlot < 0) {
            return;
        }
        if (rawSlot == SLOT_ITEM_DISPLAY) {
            if (state.itemTemplate != null) {
                state.itemTemplate = null;
                player.sendMessage(messages.setupItemCleared());
                redraw(state);
            } else {
                player.sendMessage(messages.setupSelectItem());
            }
            return;
        }
        if (rawSlot == SLOT_CONFIRM) {
            manager.setPendingSetup(player.getUniqueId(),
                    new PlayerShopSetup(state.quantity, state.price, copyTemplate(state.itemTemplate)));
            player.closeInventory();
            player.sendMessage(messages.setupSaved());
            return;
        }
        switch (rawSlot) {
            case SLOT_QUANTITY_MINUS_ONE -> adjustQuantity(state, -1);
            case SLOT_QUANTITY_MINUS_SIXTEEN -> adjustQuantity(state, -16);
            case SLOT_QUANTITY_PLUS_SIXTEEN -> adjustQuantity(state, 16);
            case SLOT_QUANTITY_PLUS_ONE -> adjustQuantity(state, 1);
            case SLOT_QUANTITY_INPUT -> startQuantityInput(player, state);
            case SLOT_PRICE_MINUS_ONE -> adjustPrice(state, -1.0d);
            case SLOT_PRICE_MINUS_TEN -> adjustPrice(state, -10.0d);
            case SLOT_PRICE_PLUS_TEN -> adjustPrice(state, 10.0d);
            case SLOT_PRICE_PLUS_ONE -> adjustPrice(state, 1.0d);
            case SLOT_PRICE_INPUT -> startPriceInput(player, state);
            default -> {
            }
        }
        redraw(state);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        MenuState state = openMenus.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.inventory != null && state.inventory.equals(event.getInventory())) {
            openMenus.remove(player.getUniqueId());
            pendingInputs.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PendingChatInput pending = pendingInputs.get(playerId);
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin, () -> handleChatInput(event.getPlayer(), message));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingInputs.remove(playerId);
        openMenus.remove(playerId);
    }

    private void adjustQuantity(MenuState state, int delta) {
        int newValue = state.quantity + delta;
        newValue = Math.max(configuration.minQuantity(), newValue);
        if (configuration.maxQuantity() > 0) {
            newValue = Math.min(configuration.maxQuantity(), newValue);
        }
        state.quantity = Math.max(1, newValue);
    }

    private void adjustPrice(MenuState state, double delta) {
        double newValue = state.price + delta;
        double min = Math.max(configuration.minPrice(), 0.01d);
        if (newValue < min) {
            newValue = min;
        }
        if (configuration.maxPrice() > 0.0d) {
            newValue = Math.min(configuration.maxPrice(), newValue);
        }
        state.price = roundPrice(newValue);
    }

    private void startQuantityInput(Player player, MenuState state) {
        if (player == null || state == null) {
            return;
        }
        pendingInputs.put(player.getUniqueId(), new PendingChatInput(state, InputType.QUANTITY));
        player.sendMessage(messages.setupQuantityChatPrompt());
    }

    private void startPriceInput(Player player, MenuState state) {
        if (player == null || state == null) {
            return;
        }
        pendingInputs.put(player.getUniqueId(), new PendingChatInput(state, InputType.PRICE));
        player.sendMessage(messages.setupPriceChatPrompt());
    }

    private void handleChatInput(Player player, String message) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        PendingChatInput pending = pendingInputs.get(playerId);
        if (pending == null || pending.state == null) {
            return;
        }
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.equalsIgnoreCase("cancel")) {
            pendingInputs.remove(playerId);
            player.sendMessage(messages.setupChatCancelled());
            redraw(pending.state);
            return;
        }

        switch (pending.type) {
            case QUANTITY -> {
                int value;
                try {
                    value = Integer.parseInt(trimmed);
                } catch (NumberFormatException ex) {
                    player.sendMessage(messages.setupChatInvalidNumber());
                    player.sendMessage(messages.setupQuantityChatPrompt());
                    return;
                }
                if (applyQuantityInput(player, pending.state, value)) {
                    pendingInputs.remove(playerId);
                } else {
                    player.sendMessage(messages.setupQuantityChatPrompt());
                }
            }
            case PRICE -> {
                double value;
                try {
                    value = Double.parseDouble(trimmed);
                } catch (NumberFormatException ex) {
                    player.sendMessage(messages.setupChatInvalidNumber());
                    player.sendMessage(messages.setupPriceChatPrompt());
                    return;
                }
                if (!Double.isFinite(value)) {
                    player.sendMessage(messages.setupChatInvalidNumber());
                    player.sendMessage(messages.setupPriceChatPrompt());
                    return;
                }
                if (applyPriceInput(player, pending.state, value)) {
                    pendingInputs.remove(playerId);
                } else {
                    player.sendMessage(messages.setupPriceChatPrompt());
                }
            }
            default -> {
            }
        }
    }

    private boolean applyQuantityInput(Player player, MenuState state, int value) {
        if (value <= 0) {
            player.sendMessage(messages.creationQuantityPositive());
            return false;
        }
        int min = Math.max(configuration.minQuantity(), 1);
        if (value < min) {
            player.sendMessage(messages.creationQuantityMin(min));
            return false;
        }
        if (configuration.maxQuantity() > 0 && value > configuration.maxQuantity()) {
            player.sendMessage(messages.creationQuantityMax(configuration.maxQuantity()));
            return false;
        }
        state.quantity = value;
        player.sendMessage(messages.setupQuantityUpdated(state.quantity));
        redraw(state);
        return true;
    }

    private boolean applyPriceInput(Player player, MenuState state, double value) {
        if (value <= 0.0d) {
            player.sendMessage(messages.creationPricePositive());
            return false;
        }
        double min = configuration.minPrice();
        if (min > 0.0d && value < min) {
            player.sendMessage(messages.creationPriceMin(formatPrice(min)));
            return false;
        }
        double minimumAllowed = Math.max(min, 0.01d);
        if (value < minimumAllowed) {
            player.sendMessage(messages.creationPricePositive());
            return false;
        }
        if (configuration.maxPrice() > 0.0d && value > configuration.maxPrice()) {
            player.sendMessage(messages.creationPriceMax(formatPrice(configuration.maxPrice())));
            return false;
        }
        state.price = roundPrice(value);
        player.sendMessage(messages.setupPriceUpdated(formatPrice(state.price)));
        redraw(state);
        return true;
    }

    private void redraw(MenuState state) {
        if (state.inventory == null) {
            return;
        }
        PlayerShopMessages.MenuMessages menuMessages = messages.menu();
        fillSpacer(state.inventory);
        state.inventory.setItem(SLOT_CONFIRM, item(Material.EMERALD_BLOCK, menuMessages.confirmButtonName(),
                menuMessages.confirmButtonLore().toArray(new String[0])));

        if (state.itemTemplate != null) {
            state.inventory.setItem(SLOT_ITEM_DISPLAY, itemDisplay(state.itemTemplate));
        } else {
            state.inventory.setItem(SLOT_ITEM_DISPLAY,
                    item(Material.CHEST, menuMessages.selectItemName(),
                            menuMessages.selectItemLore().toArray(new String[0])));
        }

        state.inventory.setItem(SLOT_QUANTITY_DISPLAY,
                item(Material.PAPER, menuMessages.quantityName(), menuMessages.quantityValue(state.quantity)));
        state.inventory.setItem(SLOT_QUANTITY_INPUT,
                item(Material.WRITABLE_BOOK, menuMessages.quantityTypeName(),
                        menuMessages.quantityTypeLore().toArray(new String[0])));
        state.inventory.setItem(SLOT_PRICE_DISPLAY,
                item(Material.GOLD_INGOT, menuMessages.priceName(), menuMessages.priceValue(formatPrice(state.price))));
        state.inventory.setItem(SLOT_PRICE_INPUT,
                item(Material.NAME_TAG, menuMessages.priceTypeName(),
                        menuMessages.priceTypeLore().toArray(new String[0])));

        state.inventory.setItem(SLOT_QUANTITY_MINUS_ONE,
                item(Material.RED_STAINED_GLASS_PANE, menuMessages.quantityMinusOneLabel()));
        state.inventory.setItem(SLOT_QUANTITY_MINUS_SIXTEEN,
                item(Material.REDSTONE, menuMessages.quantityMinusSixteenLabel()));
        state.inventory.setItem(SLOT_QUANTITY_PLUS_SIXTEEN,
                item(Material.EMERALD, menuMessages.quantityPlusSixteenLabel()));
        state.inventory.setItem(SLOT_QUANTITY_PLUS_ONE,
                item(Material.LIME_STAINED_GLASS_PANE, menuMessages.quantityPlusOneLabel()));

        state.inventory.setItem(SLOT_PRICE_MINUS_ONE,
                item(Material.REDSTONE_TORCH, menuMessages.priceMinusOneLabel()));
        state.inventory.setItem(SLOT_PRICE_MINUS_TEN,
                item(Material.REDSTONE_BLOCK, menuMessages.priceMinusTenLabel()));
        state.inventory.setItem(SLOT_PRICE_PLUS_TEN,
                item(Material.EMERALD_BLOCK, menuMessages.pricePlusTenLabel()));
        state.inventory.setItem(SLOT_PRICE_PLUS_ONE,
                item(Material.SEA_LANTERN, menuMessages.pricePlusOneLabel()));
    }

    private void fillSpacer(Inventory inventory) {
        ItemStack spacer = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, spacer);
        }
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            meta.setLocalizedName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack itemDisplay(ItemStack template) {
        ItemStack display = template.clone();
        display.setAmount(1);
        ItemMeta meta = display.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (meta != null) {
            if (meta.hasLore() && meta.getLore() != null) {
                lore.addAll(meta.getLore());
                if (!lore.isEmpty()) {
                    lore.add(" ");
                }
            }
            lore.add(messages.menu().itemSelectedClearHint());
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack sanitizeTemplate(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        ItemStack clone = item.clone();
        clone.setAmount(1);
        return clone;
    }

    private ItemStack copyTemplate(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemStack clone = item.clone();
        clone.setAmount(Math.max(1, clone.getAmount()));
        return clone;
    }

    private String describeItem(ItemStack item) {
        if (item == null) {
            return messages.menu().unknownItemSingularPlain();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String displayName = ChatColor.stripColor(meta.getDisplayName());
            if (displayName != null && !displayName.isBlank()) {
                return displayName;
            }
        }
        String[] parts = item.getType().name().toLowerCase(Locale.ENGLISH).split("_");
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
        String result = builder.toString().trim();
        return result.isEmpty() ? item.getType().name() : result;
    }

    private double defaultPrice() {
        double min = configuration.minPrice();
        if (min > 0.0d) {
            return roundPrice(min);
        }
        return 1.0d;
    }

    private String formatPrice(double price) {
        return String.format(Locale.US, "%.2f", price);
    }

    private double roundPrice(double price) {
        return Math.round(price * 100.0d) / 100.0d;
    }

    private static final class PendingChatInput {

        private final MenuState state;
        private final InputType type;

        private PendingChatInput(MenuState state, InputType type) {
            this.state = state;
            this.type = type;
        }
    }

    private enum InputType {
        QUANTITY,
        PRICE
    }

    private static final class MenuState {

        private Inventory inventory;
        private int quantity;
        private double price;
        private ItemStack itemTemplate;

        private MenuState(int quantity, double price) {
            this.quantity = quantity;
            this.price = price;
        }
    }
}
