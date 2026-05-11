package com.skyblockexp.ezshops.teams;

import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.model.TeamRole;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * YAML-backed team P2P market.
 *
 * <p>Each team's listings are stored in {@code data/team-market/<teamId>.yml},
 * one YAML section per listing keyed by the listing's UUID.
 *
 * <p>Items are held in <em>escrow</em>: they are removed from the seller's
 * inventory at listing time and returned when a listing is cancelled, so they
 * cannot be lost or used while listed.
 */
public final class TeamMarketManager {

    public static final String PERMISSION_ADMIN = "ezshops.shop.admin";

    private static final String KEY_SELLER   = "seller";
    private static final String KEY_ITEM     = "item";
    private static final String KEY_QUANTITY = "quantity";
    private static final String KEY_PRICE    = "price";
    private static final String KEY_LISTED   = "listed-at";

    private final File dataDir;
    private final Economy economy;
    private final Logger logger;
    private final TeamsIntegration teamsIntegration;

    /** In-memory cache: teamId → (listingId → listing). */
    private final Map<UUID, Map<UUID, TeamMarketListing>> listings = new HashMap<>();

    public TeamMarketManager(File pluginDataFolder, Economy economy,
                             TeamsIntegration teamsIntegration, Logger logger) {
        this.dataDir = new File(pluginDataFolder, "team-market");
        this.economy = economy;
        this.teamsIntegration = teamsIntegration;
        this.logger = logger;
        if (!this.dataDir.exists()) this.dataDir.mkdirs();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void onEnable() {
        File[] files = dataDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            UUID teamId = parseUUID(file.getName().replace(".yml", ""));
            if (teamId == null) continue;
            loadTeamFile(teamId, file);
        }
        logger.info("[EzShops] Loaded team market data for " + listings.size() + " team(s).");
    }

    public void onDisable() {
        for (UUID teamId : listings.keySet()) {
            saveTeamFile(teamId);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Creates a new listing.  The item is immediately removed from the seller's
     * inventory (escrow).  Returns the new listing, or {@code null} if the
     * seller does not have the item or the operation fails.
     */
    public TeamMarketListing addListing(UUID teamId, Player seller,
                                        ItemStack item, int quantity, double price) {
        // Take item from inventory (escrow)
        ItemStack toTake = item.clone();
        toTake.setAmount(quantity);
        if (!hasEnoughItems(seller, toTake)) return null;
        removeItemsFromInventory(seller, toTake);

        ItemStack stored = item.clone();
        stored.setAmount(quantity);

        UUID listingId = UUID.randomUUID();
        TeamMarketListing listing = new TeamMarketListing(
                listingId, teamId, seller.getUniqueId(), stored, quantity, price,
                System.currentTimeMillis());

        listings.computeIfAbsent(teamId, k -> new LinkedHashMap<>()).put(listingId, listing);
        saveTeamFile(teamId);
        return listing;
    }

    /**
     * Purchases a listing.  The buyer must be in the same team.
     * Returns an error message on failure, or {@code null} on success.
     */
    public String purchaseListing(UUID listingId, Player buyer) {
        TeamMarketListing listing = findListing(listingId);
        if (listing == null) return "This listing no longer exists.";

        if (listing.sellerUuid().equals(buyer.getUniqueId()))
            return "You cannot buy your own listing.";

        // Verify same team
        Optional<com.skyblockexp.teamsapi.model.Team> buyerTeam =
                teamsIntegration.getPlayerTeam(buyer.getUniqueId());
        if (buyerTeam.isEmpty() || !buyerTeam.get().getId().equals(listing.teamId()))
            return "You are not in the same team as the seller.";

        double price = listing.price();
        if (economy.getBalance(buyer) < price)
            return "You cannot afford this listing ($" + String.format("%.2f", price) + ").";

        ItemStack toGive = listing.item().clone();
        toGive.setAmount(listing.quantity());

        // Check inventory space
        if (!hasInventorySpace(buyer, toGive))
            return "You do not have enough inventory space.";

        // Withdraw from buyer
        EconomyResponse withdraw = economy.withdrawPlayer(buyer, price);
        if (!withdraw.transactionSuccess())
            return "Payment failed: " + withdraw.errorMessage;

        // Pay seller
        OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.sellerUuid());
        EconomyResponse deposit = economy.depositPlayer(seller, price);
        if (!deposit.transactionSuccess()) {
            economy.depositPlayer(buyer, price); // refund
            return "Could not pay the seller: " + deposit.errorMessage;
        }

        // Give item
        buyer.getInventory().addItem(toGive);

        // Remove listing
        removeListing(listing);

        // Notify seller if online
        Player onlineSeller = Bukkit.getPlayer(listing.sellerUuid());
        if (onlineSeller != null) {
            onlineSeller.sendMessage(
                    org.bukkit.ChatColor.GREEN + buyer.getName() + " bought your listing of "
                    + listing.quantity() + "x " + friendlyName(listing.item())
                    + " for $" + String.format("%.2f", price) + ".");
        }
        return null; // success
    }

    /**
     * Cancels a listing and returns the item to the original seller.
     * Only the seller, a team ADMIN/OWNER, or a player with
     * {@code ezshops.shop.admin} may cancel.
     * Returns an error message on failure, or {@code null} on success.
     */
    public String cancelListing(UUID listingId, Player requester) {
        TeamMarketListing listing = findListing(listingId);
        if (listing == null) return "This listing no longer exists.";

        boolean isSeller = listing.sellerUuid().equals(requester.getUniqueId());
        boolean isAdmin  = requester.hasPermission(PERMISSION_ADMIN);
        boolean isTeamManager = false;
        if (!isSeller && !isAdmin) {
            Optional<TeamRole> role = teamsIntegration.getMemberRole(
                    listing.teamId(), requester.getUniqueId());
            isTeamManager = role.map(r -> r == TeamRole.ADMIN || r == TeamRole.OWNER).orElse(false);
        }

        if (!isSeller && !isAdmin && !isTeamManager)
            return "You do not have permission to cancel this listing.";

        // Return item to seller
        ItemStack toReturn = listing.item().clone();
        toReturn.setAmount(listing.quantity());
        Player onlineSeller = Bukkit.getPlayer(listing.sellerUuid());
        if (onlineSeller != null && onlineSeller.isOnline()) {
            onlineSeller.getInventory().addItem(toReturn);
            if (!isSeller) {
                onlineSeller.sendMessage(org.bukkit.ChatColor.YELLOW
                        + "Your team market listing of " + listing.quantity() + "x "
                        + friendlyName(listing.item()) + " was cancelled by " + requester.getName() + ".");
            }
        } else {
            // Store in offline drop — log it, drop at world spawn as last resort
            logger.warning("[EzShops] Seller " + listing.sellerUuid()
                    + " is offline; dropping returned item at world spawn.");
            var world = Bukkit.getWorlds().get(0);
            world.dropItemNaturally(world.getSpawnLocation(), toReturn);
        }

        removeListing(listing);
        return null; // success
    }

    /** All listings for a team, newest first. */
    public List<TeamMarketListing> getTeamListings(UUID teamId) {
        Map<UUID, TeamMarketListing> teamMap = listings.get(teamId);
        if (teamMap == null) return List.of();
        List<TeamMarketListing> result = new ArrayList<>(teamMap.values());
        result.sort(Comparator.comparingLong(TeamMarketListing::listedAt).reversed());
        return result;
    }

    /** All listings across every team (for admin view). */
    public List<TeamMarketListing> getAllListings() {
        List<TeamMarketListing> all = new ArrayList<>();
        for (Map<UUID, TeamMarketListing> teamMap : listings.values()) {
            all.addAll(teamMap.values());
        }
        all.sort(Comparator.comparingLong(TeamMarketListing::listedAt).reversed());
        return all;
    }

    /** Force-cancel a listing from admin GUI without team-membership checks. */
    public String adminCancelListing(UUID listingId, Player requester) {
        TeamMarketListing listing = findListing(listingId);
        if (listing == null) return "This listing no longer exists.";

        ItemStack toReturn = listing.item().clone();
        toReturn.setAmount(listing.quantity());
        Player onlineSeller = Bukkit.getPlayer(listing.sellerUuid());
        if (onlineSeller != null && onlineSeller.isOnline()) {
            onlineSeller.getInventory().addItem(toReturn);
            onlineSeller.sendMessage(org.bukkit.ChatColor.YELLOW
                    + "Your team market listing of " + listing.quantity() + "x "
                    + friendlyName(listing.item()) + " was cancelled by an admin.");
        } else {
            var world = Bukkit.getWorlds().get(0);
            world.dropItemNaturally(world.getSpawnLocation(), toReturn);
        }
        removeListing(listing);
        return null;
    }

    public void deleteTeamData(UUID teamId) {
        listings.remove(teamId);
        File f = fileFor(teamId);
        if (f.exists()) f.delete();
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private TeamMarketListing findListing(UUID listingId) {
        for (Map<UUID, TeamMarketListing> teamMap : listings.values()) {
            TeamMarketListing l = teamMap.get(listingId);
            if (l != null) return l;
        }
        return null;
    }

    private void removeListing(TeamMarketListing listing) {
        Map<UUID, TeamMarketListing> teamMap = listings.get(listing.teamId());
        if (teamMap != null) teamMap.remove(listing.listingId());
        saveTeamFile(listing.teamId());
    }

    private File fileFor(UUID teamId) {
        return new File(dataDir, teamId + ".yml");
    }

    private void loadTeamFile(UUID teamId, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<UUID, TeamMarketListing> teamMap = new LinkedHashMap<>();
        for (String key : yaml.getKeys(false)) {
            UUID listingId = parseUUID(key);
            if (listingId == null) continue;
            ConfigurationSection sec = yaml.getConfigurationSection(key);
            if (sec == null) continue;
            try {
                UUID sellerUuid = UUID.fromString(sec.getString(KEY_SELLER, ""));
                ItemStack item = sec.getItemStack(KEY_ITEM);
                if (item == null) continue;
                int quantity = sec.getInt(KEY_QUANTITY, 1);
                double price = sec.getDouble(KEY_PRICE, 0.0);
                long listedAt = sec.getLong(KEY_LISTED, System.currentTimeMillis());
                teamMap.put(listingId, new TeamMarketListing(
                        listingId, teamId, sellerUuid, item, quantity, price, listedAt));
            } catch (Exception ignored) {
                // Corrupted entry – skip
            }
        }
        if (!teamMap.isEmpty()) listings.put(teamId, teamMap);
    }

    private void saveTeamFile(UUID teamId) {
        File f = fileFor(teamId);
        Map<UUID, TeamMarketListing> teamMap = listings.get(teamId);
        YamlConfiguration yaml = new YamlConfiguration();
        if (teamMap != null) {
            for (TeamMarketListing l : teamMap.values()) {
                String key = l.listingId().toString();
                yaml.set(key + "." + KEY_SELLER,   l.sellerUuid().toString());
                yaml.set(key + "." + KEY_ITEM,     l.item());
                yaml.set(key + "." + KEY_QUANTITY, l.quantity());
                yaml.set(key + "." + KEY_PRICE,    l.price());
                yaml.set(key + "." + KEY_LISTED,   l.listedAt());
            }
        }
        try {
            yaml.save(f);
        } catch (IOException e) {
            logger.warning("[EzShops] Failed to save team market data for " + teamId + ": " + e.getMessage());
        }
    }

    private static boolean hasEnoughItems(Player player, ItemStack needed) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(needed)) count += stack.getAmount();
        }
        return count >= needed.getAmount();
    }

    private static void removeItemsFromInventory(Player player, ItemStack toRemove) {
        int remaining = toRemove.getAmount();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack s = contents[i];
            if (s != null && s.isSimilar(toRemove)) {
                if (s.getAmount() <= remaining) {
                    remaining -= s.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    s.setAmount(s.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
    }

    private static boolean hasInventorySpace(Player player, ItemStack item) {
        return player.getInventory().firstEmpty() != -1 ||
               Arrays.stream(player.getInventory().getContents())
                     .filter(Objects::nonNull)
                     .anyMatch(s -> s.isSimilar(item) && s.getAmount() + item.getAmount() <= s.getMaxStackSize());
    }

    public static String friendlyName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        // For shulker boxes, append a contents count so sellers know what is inside
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm
                && bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulker) {
            long filled = Arrays.stream(shulker.getInventory().getContents())
                    .filter(s -> s != null && s.getType() != org.bukkit.Material.AIR)
                    .count();
            String raw = item.getType().name().replace('_', ' ');
            String base = raw.charAt(0) + raw.substring(1).toLowerCase(Locale.ROOT);
            return filled > 0 ? base + " (" + filled + " items)" : base;
        }
        String raw = item.getType().name().replace('_', ' ');
        return raw.charAt(0) + raw.substring(1).toLowerCase(Locale.ROOT);
    }

    private static UUID parseUUID(String s) {
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}
