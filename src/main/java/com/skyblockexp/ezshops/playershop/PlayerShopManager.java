package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.repository.PlayerShopRepository;
import com.skyblockexp.ezshops.config.PlayerShopConfiguration;
import com.skyblockexp.ezshops.config.PlayerShopConfiguration.SignFormat;
import com.skyblockexp.ezshops.shop.ShopTransactionResult;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Coordinates player created chest shops.
 */
public final class PlayerShopManager {

    public static final String PERMISSION_CREATE = "ezshops.playershop.create";
    public static final String PERMISSION_ADMIN = "ezshops.playershop.admin";
    public static final String PERMISSION_BUY = "ezshops.playershop.buy";

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private final JavaPlugin plugin;
    private final Economy economy;
    private final PlayerShopConfiguration configuration;
    private final PlayerShopMessages messages;
    private final PlayerShopRepository repository;
    private final Map<String, PlayerShop> shopsBySign;
    private final Map<String, PlayerShop> shopsByChest;
    private final Map<UUID, PlayerShopSetup> pendingSetups;

    public PlayerShopManager(JavaPlugin plugin, Economy economy, PlayerShopConfiguration configuration,
            PlayerShopRepository repository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.economy = economy;
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.messages = configuration.messages();
        this.repository = Objects.requireNonNull(repository, "repository");
        this.shopsBySign = new HashMap<>();
        this.shopsByChest = new HashMap<>();
        this.pendingSetups = new HashMap<>();
    }

    public void enable() {
        loadShops();
    }

    public void disable() {
        saveShops();
        shopsBySign.clear();
        shopsByChest.clear();
        pendingSetups.clear();
    }
    
    private void loadShops() {
        shopsBySign.clear();
        shopsByChest.clear();
        
        Collection<PlayerShop> shops = repository.loadShops();
        for (PlayerShop shop : shops) {
            registerShop(shop);
            refreshSign(shop);
        }
    }
    
    private void registerShop(PlayerShop shop) {
        shopsBySign.put(repository.locationKey(shop.signLocation()), shop);
        for (Location chestLocation : shop.chestLocations()) {
            shopsByChest.put(repository.locationKey(chestLocation), shop);
        }
    }

    public Collection<PlayerShop> getShops() {
        return List.copyOf(shopsBySign.values());
    }

    public PlayerShop getShopBySign(Block block) {
        if (block == null) {
            return null;
        }
        return shopsBySign.get(repository.locationKey(block.getLocation()));
    }

    public PlayerShop getShopBySign(Location location) {
        if (location == null) {
            return null;
        }
        return shopsBySign.get(repository.locationKey(location));
    }

    public PlayerShop getShopByChest(Block block) {
        if (block == null) {
            return null;
        }
        return shopsByChest.get(repository.locationKey(block.getLocation()));
    }

    public PlayerShop getShopByChest(Location location) {
        if (location == null) {
            return null;
        }
        return shopsByChest.get(repository.locationKey(location));
    }

    public PlayerShopSetup getPendingSetup(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return pendingSetups.get(playerId);
    }

    public void setPendingSetup(UUID playerId, PlayerShopSetup setup) {
        if (playerId == null || setup == null) {
            return;
        }
        pendingSetups.put(playerId, normalizeSetup(setup));
    }

    public void clearPendingSetup(UUID playerId) {
        if (playerId == null) {
            return;
        }
        pendingSetups.remove(playerId);
    }

    public PlayerShopSetup normalizeSetup(PlayerShopSetup setup) {
        if (setup == null) {
            return null;
        }
        int quantity = Math.max(configuration.minQuantity(), setup.quantity());
        if (configuration.maxQuantity() > 0) {
            quantity = Math.min(configuration.maxQuantity(), quantity);
        }
        quantity = Math.max(1, quantity);

        double minimumPrice = configuration.minPrice() > 0.0d ? configuration.minPrice() : 0.01d;
        double price = Math.max(minimumPrice, setup.price());
        if (configuration.maxPrice() > 0.0d) {
            price = Math.min(configuration.maxPrice(), price);
        }
        return new PlayerShopSetup(quantity, price, setup.itemTemplate());
    }

    public PlayerShopCreationResult createShop(Player owner, Block signBlock, Block attachedBlock, int quantity,
            double price) {
        if (owner == null || signBlock == null || attachedBlock == null) {
            return PlayerShopCreationResult.failure(messages.creationInvalidConfiguration());
        }

        if (quantity <= 0) {
            return PlayerShopCreationResult.failure(messages.creationQuantityPositive());
        }

        if (quantity < configuration.minQuantity()) {
            return PlayerShopCreationResult.failure(messages.creationQuantityMin(configuration.minQuantity()));
        }

        if (configuration.maxQuantity() > 0 && quantity > configuration.maxQuantity()) {
            return PlayerShopCreationResult.failure(messages.creationQuantityMax(configuration.maxQuantity()));
        }

        if (price <= 0) {
            return PlayerShopCreationResult.failure(messages.creationPricePositive());
        }

        if (price < configuration.minPrice()) {
            return PlayerShopCreationResult.failure(messages.creationPriceMin(formatCurrency(configuration.minPrice())));
        }

        if (configuration.maxPrice() > 0.0d && price > configuration.maxPrice()) {
            return PlayerShopCreationResult.failure(messages.creationPriceMax(formatCurrency(configuration.maxPrice())));
        }

        if (getShopBySign(signBlock) != null) {
            return PlayerShopCreationResult.failure(messages.creationSignInUse());
        }

        if (!isContainer(attachedBlock)) {
            return PlayerShopCreationResult.failure(messages.signRequiresChest());
        }

        List<Location> chestLocations = resolveChestLocations(attachedBlock);
        if (chestLocations.isEmpty()) {
            return PlayerShopCreationResult.failure(messages.creationChestUnresolved());
        }

        for (Location chestLocation : chestLocations) {
            if (getShopByChest(chestLocation) != null) {
                return PlayerShopCreationResult.failure(messages.creationChestInUse());
            }
        }

        Inventory chestInventory = getInventory(attachedBlock);
        if (chestInventory == null) {
            return PlayerShopCreationResult.failure(messages.creationChestInaccessible());
        }

        PlayerShopSetup pendingSetup = normalizeSetup(getPendingSetup(owner.getUniqueId()));
        ItemStack template = null;
        if (pendingSetup != null) {
            template = pendingSetup.itemTemplate();
        }
        if (template != null) {
            template = findMatchingItem(chestInventory, template);
            if (template == null) {
                return PlayerShopCreationResult.failure(messages.creationSelectedItemMissing());
            }
        } else {
            template = findTemplateItem(chestInventory);
            if (template == null) {
                return PlayerShopCreationResult.failure(messages.creationItemMissing());
            }
        }

        if (configuration.requireStockOnCreation() && countItems(chestInventory, template) < quantity) {
            return PlayerShopCreationResult.failure(messages.creationInsufficientStock());
        }

        PlayerShop shop = new PlayerShop(owner.getUniqueId(), signBlock.getLocation(), attachedBlock.getLocation(),
                chestLocations, template, quantity, price);
        registerShop(shop);
        refreshSign(shop);
        saveShops();
        return PlayerShopCreationResult.success(messages.creationSuccess(), shop);
    }

    public ShopTransactionResult purchase(PlayerShop shop, Player buyer) {
        if (shop == null || buyer == null) {
            return ShopTransactionResult.failure(messages.purchaseInvalid());
        }

        if (!buyer.hasPermission(PERMISSION_BUY)) {
            return ShopTransactionResult.failure(messages.purchaseNoPermission());
        }

        if (economy == null) {
            return ShopTransactionResult.failure(messages.purchaseNoEconomy());
        }

        if (buyer.getUniqueId().equals(shop.ownerId())) {
            return ShopTransactionResult.failure(messages.purchaseOwnShop());
        }

        Inventory inventory = getInventory(shop.primaryChestLocation());
        if (inventory == null) {
            removeShop(shop);
            saveShops();
            return ShopTransactionResult.failure(messages.purchaseMissingChest());
        }

        ItemStack template = shop.itemTemplate();
        int available = countItems(inventory, template);
        if (available < shop.quantityPerSale()) {
            refreshSign(shop);
            return ShopTransactionResult.failure(messages.purchaseOutOfStock());
        }

        ItemStack toGive = template.clone();
        toGive.setAmount(shop.quantityPerSale());
        if (!hasInventorySpace(buyer, toGive)) {
            return ShopTransactionResult.failure(messages.purchaseNoSpace());
        }

        double price = shop.price();
        EconomyResponse withdraw = economy.withdrawPlayer(buyer, price);
        if (!withdraw.transactionSuccess()) {
            return ShopTransactionResult.failure(messages.purchaseTransactionFailed(withdraw.errorMessage));
        }

        removeItems(inventory, template, shop.quantityPerSale());

        Map<Integer, ItemStack> leftovers = buyer.getInventory().addItem(toGive);
        int deliveredAmount = shop.quantityPerSale();
        if (!leftovers.isEmpty()) {
            deliveredAmount = deliveredAmount - leftovers.values().stream().filter(Objects::nonNull)
                    .mapToInt(ItemStack::getAmount).sum();
        }

        if (deliveredAmount < shop.quantityPerSale()) {
            int amountToReturn = shop.quantityPerSale() - deliveredAmount;
            addItems(inventory, template, amountToReturn);
            economy.depositPlayer(buyer, price);
            removeItems(buyer.getInventory(), template, deliveredAmount);
            refreshSign(shop);
            return ShopTransactionResult.failure(messages.purchaseNoSpace());
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.ownerId());
        EconomyResponse deposit = economy.depositPlayer(owner, price);
        if (!deposit.transactionSuccess()) {
            removeItems(buyer.getInventory(), template, shop.quantityPerSale());
            addItems(inventory, template, shop.quantityPerSale());
            economy.depositPlayer(buyer, price);
            refreshSign(shop);
            return ShopTransactionResult.failure(messages.purchaseTransactionFailed(deposit.errorMessage));
        }

        refreshSign(shop);
        String ownerName = owner != null ? owner.getName() : messages.unknownSellerName();
        if (ownerName == null || ownerName.isEmpty()) {
            ownerName = messages.unknownSellerName();
        }
        String itemName = describeItem(template, shop.quantityPerSale());
        if (owner != null && owner.isOnline()) {
            Player ownerPlayer = owner.getPlayer();
            if (ownerPlayer != null) {
                ownerPlayer
                        .sendMessage(messages.purchaseOwnerNotify(buyer.getName(), itemName, formatCurrency(price)));
            }
        }
        return ShopTransactionResult
                .success(messages.purchaseBuyerSuccess(itemName, ownerName, formatCurrency(price)));
    }

    public void removeShop(PlayerShop shop) {
        if (shop == null) {
            return;
        }
        shopsBySign.remove(repository.locationKey(shop.signLocation()));
        for (Location chestLocation : shop.chestLocations()) {
            shopsByChest.remove(repository.locationKey(chestLocation));
        }
    }

    public boolean removeShopBySign(Location location) {
        PlayerShop shop = getShopBySign(location);
        if (shop == null) {
            return false;
        }
        removeShop(shop);
        saveShops();
        return true;
    }

    public boolean removeShopByChest(Location location) {
        PlayerShop shop = getShopByChest(location);
        if (shop == null) {
            return false;
        }
        removeShop(shop);
        saveShops();
        return true;
    }

    public void refreshSign(PlayerShop shop) {
        if (shop == null) {
            return;
        }
        Location signLocation = shop.signLocation();
        Block signBlock = signLocation.getBlock();
        BlockState state = signBlock.getState();
        if (!(state instanceof Sign sign)) {
            removeShop(shop);
            saveShops();
            return;
        }

        Inventory inventory = getInventory(shop.primaryChestLocation());
        ItemStack template = shop.itemTemplate();
        boolean hasStock = inventory != null && countItems(inventory, template) >= shop.quantityPerSale();

        String[] lines = formatSignLines(shop, hasStock);
        for (int i = 0; i < lines.length && i < sign.getLines().length; i++) {
            sign.setLine(i, lines[i]);
        }
        sign.update();
    }

    public String[] formatSignLines(PlayerShop shop, boolean hasStock) {
        SignFormat signFormat = configuration.signFormat();
        String ownerName = Optional.ofNullable(Bukkit.getOfflinePlayer(shop.ownerId()).getName())
                .filter(name -> !name.isBlank()).orElse(signFormat.unknownOwnerName());
        ItemStack template = shop.itemTemplate();
        String itemName = friendlyItemName(template.getType());
        String priceText = formatCurrency(shop.price());
        return signFormat.formatLines(ownerName, shop.quantityPerSale(), itemName, priceText, hasStock);
    }

    public void saveShops() {
        repository.saveShops(shopsBySign, repository.getDeferredEntries());
    }

    private List<Location> resolveChestLocations(Block block) {
        List<Location> locations = new ArrayList<>();
        locations.add(block.getLocation());
        BlockState state = block.getState();
        if (state instanceof InventoryHolder holder) {
            Inventory inventory = holder.getInventory();
            if (inventory != null) {
                InventoryHolder inventoryHolder = inventory.getHolder();
                if (inventoryHolder instanceof org.bukkit.block.DoubleChest doubleChest) {
                    addChestLocation(locations, doubleChest.getLeftSide());
                    addChestLocation(locations, doubleChest.getRightSide());
                } else {
                    // Support barrels as containers
                    try {
                        Class<?> barrelClass = Class.forName("org.bukkit.block.Barrel");
                        if (barrelClass.isInstance(inventoryHolder)) {
                            org.bukkit.block.BlockState barrelState = ((org.bukkit.block.BlockState) inventoryHolder);
                            locations.add(barrelState.getLocation());
                        } else {
                            locations.add(block.getLocation());
                        }
                    } catch (ClassNotFoundException ignored) {
                        locations.add(block.getLocation());
                    }
                }
            }
        }
        Set<String> unique = new HashSet<>();
        List<Location> deduplicated = new ArrayList<>();
        for (Location location : locations) {
            String key = repository.locationKey(location);
            if (key.isEmpty() || !unique.add(key)) {
                continue;
            }
            deduplicated.add(location);
        }
        return deduplicated;
    }

    private void addChestLocation(List<Location> locations, InventoryHolder holder) {
        if (holder instanceof org.bukkit.block.Chest chest) {
            locations.add(chest.getLocation());
        } else {
            // Support barrels as containers
            try {
                Class<?> barrelClass = Class.forName("org.bukkit.block.Barrel");
                if (barrelClass.isInstance(holder)) {
                    org.bukkit.block.BlockState state = ((org.bukkit.block.BlockState) holder);
                    locations.add(state.getLocation());
                }
            } catch (ClassNotFoundException ignored) {}
        }
    }

    private boolean isContainer(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST) return true;
        try {
            if (Material.valueOf("BARREL") == type) return true;
        } catch (IllegalArgumentException ignored) {}
        return false;
    }

    private Inventory getInventory(Location location) {
        if (location == null) {
            return null;
        }
        return getInventory(location.getBlock());
    }

    private Inventory getInventory(Block block) {
        if (block == null) {
            return null;
        }
        BlockState state = block.getState();
        if (state instanceof InventoryHolder holder) {
            return holder.getInventory();
        }
        // Support barrels as containers (for older Java versions, fallback)
        try {
            Class<?> barrelClass = Class.forName("org.bukkit.block.Barrel");
            if (barrelClass.isInstance(state)) {
                return ((InventoryHolder) state).getInventory();
            }
        } catch (ClassNotFoundException ignored) {}
        return null;
    }

    private ItemStack findTemplateItem(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            ItemStack template = item.clone();
            template.setAmount(1);
            return template;
        }
        return null;
    }

    private ItemStack findMatchingItem(Inventory inventory, ItemStack template) {
        if (inventory == null || template == null) {
            return null;
        }
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (!item.isSimilar(template)) {
                continue;
            }
            ItemStack clone = item.clone();
            clone.setAmount(1);
            return clone;
        }
        return null;
    }

    private int countItems(Inventory inventory, ItemStack template) {
        if (inventory == null || template == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (!item.isSimilar(template)) {
                continue;
            }
            total += item.getAmount();
        }
        return total;
    }

    private void removeItems(Inventory inventory, ItemStack template, int amount) {
        if (inventory == null || template == null || amount <= 0) {
            return;
        }
        int remaining = amount;
        for (int i = 0; i < inventory.getSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (!stack.isSimilar(template)) {
                continue;
            }
            int stackAmount = stack.getAmount();
            if (stackAmount <= remaining) {
                inventory.setItem(i, null);
                remaining -= stackAmount;
            } else {
                stack.setAmount(stackAmount - remaining);
                inventory.setItem(i, stack);
                remaining = 0;
            }
        }
    }

    private void addItems(Inventory inventory, ItemStack template, int amount) {
        if (inventory == null || template == null || amount <= 0) {
            return;
        }
        ItemStack toAdd = template.clone();
        toAdd.setAmount(amount);
        inventory.addItem(toAdd);
    }

    private boolean hasInventorySpace(Player player, ItemStack item) {
        if (player == null || item == null) {
            return false;
        }
        int remaining = item.getAmount();
        int stackLimit = Math.min(item.getMaxStackSize(), player.getInventory().getMaxStackSize());
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (remaining <= 0) {
                break;
            }
            if (content == null || content.getType() == Material.AIR) {
                remaining -= stackLimit;
                continue;
            }
            if (!content.isSimilar(item)) {
                continue;
            }
            int contentLimit = Math.min(stackLimit, content.getMaxStackSize());
            remaining -= Math.max(0, contentLimit - content.getAmount());
        }
        return remaining <= 0;
    }

    private String friendlyItemName(Material material) {
        return material == null ? "Item"
                : capitalize(material.name().replace('_', ' ').toLowerCase(Locale.US));
    }

    private String describeItem(ItemStack item, int amount) {
        if (item == null) {
            return messages.menu().unknownItemDescription(amount);
        }
        ItemMeta meta = item.getItemMeta();
        String itemName;
        if (meta != null && meta.hasDisplayName()) {
            itemName = meta.getDisplayName();
        } else {
            itemName = ChatColor.AQUA + friendlyItemName(item.getType());
        }
        return messages.menu().itemDescription(amount, itemName);
    }

    private String capitalize(String text) {
        String[] parts = text.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? text : builder.toString();
    }

    private String formatCurrency(double value) {
        synchronized (CURRENCY_FORMAT) {
            return CURRENCY_FORMAT.format(value);
        }
    }
}
