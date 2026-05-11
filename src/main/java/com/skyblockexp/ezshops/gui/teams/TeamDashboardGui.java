package com.skyblockexp.ezshops.gui.teams;

import com.skyblockexp.ezshops.teams.TeamsIntegration;
import com.skyblockexp.ezshops.teams.TeamTreasury;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Optional;

/**
 * Main team shop dashboard GUI (27 slots, 3 rows).
 *
 * <p>Slot layout:
 * <pre>
 *  Row 0: [filler] [filler] [filler] [filler] [team info @ 4] ...
 *  Row 1: [sell bonus @ 9?] [treasury @ 10] [filler] [market @ 13] [filler] [stocks @ 16] [buy discount @ 17?]
 *  Row 2: [filler] [filler] [filler] [filler] [close @ 22] ...
 * </pre>
 *
 * <p>The sell-bonus (slot 9) and buy-discount (slot 17) icons are only rendered
 * when their respective multipliers differ from {@code 1.0} (i.e., the feature
 * is actually active for the player's role).
 */
public class TeamDashboardGui {

    static final String TITLE_PREFIX = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Team Shop";

    // Fixed layout slots
    static final int SLOT_TEAM_INFO   =  4;
    static final int SLOT_SELL_BONUS  =  9;  // only shown if sell mult != 1.0
    static final int SLOT_TREASURY    = 10;
    static final int SLOT_MARKET      = 13;  // core — Team Market
    static final int SLOT_STOCKS      = 16;
    static final int SLOT_BUY_DISC    = 17;  // only shown if buy mult != 1.0
    static final int SLOT_CLOSE       = 22;

    private final TeamsIntegration teamsIntegration;
    private final TeamTreasury teamTreasury;
    private final TeamMarketGui marketGui;

    public TeamDashboardGui(TeamsIntegration teamsIntegration,
                            TeamTreasury teamTreasury,
                            TeamMarketGui marketGui) {
        this.teamsIntegration = teamsIntegration;
        this.teamTreasury = teamTreasury;
        this.marketGui = marketGui;
    }

    public void open(Player player) {
        Optional<Team> teamOpt = teamsIntegration.getPlayerTeam(player.getUniqueId());
        if (teamOpt.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }
        Team team = teamOpt.get();

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + " - " + team.getName());

        // ── Team info ──────────────────────────────────────────────────────
        TeamRole role = teamsIntegration.getMemberRole(team.getId(), player.getUniqueId())
                .orElse(TeamRole.MEMBER);
        double balance = teamTreasury.getBalance(team.getId());
        inv.setItem(SLOT_TEAM_INFO, buildItem(Material.BEACON,
                ChatColor.GOLD + "" + ChatColor.BOLD + team.getName(),
                ChatColor.GRAY + "Your role: " + ChatColor.YELLOW + role.name(),
                ChatColor.GRAY + "Treasury: " + ChatColor.GREEN + String.format("$%.2f", balance)));

        // ── Treasury ───────────────────────────────────────────────────────
        inv.setItem(SLOT_TREASURY, buildItem(Material.CHEST,
                ChatColor.YELLOW + "Team Treasury",
                ChatColor.GRAY + "Balance: " + ChatColor.GREEN + String.format("$%.2f", balance),
                ChatColor.GRAY + "Click to manage deposits and withdrawals."));

        // ── Team Market (core) ─────────────────────────────────────────────
        inv.setItem(SLOT_MARKET, buildItem(Material.WRITABLE_BOOK,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Team Market",
                ChatColor.GRAY + "Buy and sell items with",
                ChatColor.GRAY + "your team members."));

        // ── Team Stocks — only shown when shared-stock is enabled ─────────
        if (teamsIntegration.isSharedStockEnabled()) {
            inv.setItem(SLOT_STOCKS, buildItem(Material.GOLD_INGOT,
                    ChatColor.GOLD + "Team Stocks",
                    ChatColor.GRAY + "View your team's shared stock pool."));
        }

        // ── Sell bonus — only if multiplier is active (> 1.0) ─────────────
        double sellMult = teamsIntegration.getSellMultiplier(player);
        if (Double.compare(sellMult, 1.0) != 0) {
            inv.setItem(SLOT_SELL_BONUS, buildItem(Material.GOLD_NUGGET,
                    ChatColor.GREEN + "Sell Bonus",
                    ChatColor.GRAY + "Your multiplier: " + ChatColor.YELLOW + String.format("x%.2f", sellMult),
                    ChatColor.GRAY + "(role: " + role.name() + ")"));
        }

        // ── Buy discount — only if multiplier is active (< 1.0) ───────────
        double buyMult = teamsIntegration.getBuyMultiplier(player);
        if (Double.compare(buyMult, 1.0) != 0) {
            inv.setItem(SLOT_BUY_DISC, buildItem(Material.EMERALD,
                    ChatColor.AQUA + "Buy Discount",
                    ChatColor.GRAY + "Your multiplier: " + ChatColor.YELLOW + String.format("x%.2f", buyMult),
                    ChatColor.GRAY + "(role: " + role.name() + ")"));
        }

        // ── Close ──────────────────────────────────────────────────────────
        inv.setItem(SLOT_CLOSE, buildItem(Material.BARRIER, ChatColor.RED + "Close"));

        // ── Filler ─────────────────────────────────────────────────────────
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    TeamMarketGui getMarketGui() { return marketGui; }

    static ItemStack buildItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
