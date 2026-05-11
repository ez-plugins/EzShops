package com.skyblockexp.ezshops.teams;

import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamRole;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Static helper that wraps TeamsAPI calls behind availability guards.
 * All methods return safe defaults when TeamsAPI is absent or disabled.
 */
public final class TeamsIntegration {

    private final boolean configEnabled;
    private final boolean sharedStock;
    private final double sellMember;
    private final double sellAdmin;
    private final double sellOwner;
    private final double buyMember;
    private final double buyAdmin;
    private final double buyOwner;

    public TeamsIntegration(ConfigurationSection cfg) {
        this.configEnabled = cfg != null && cfg.getBoolean("enabled", true);
        this.sharedStock   = cfg != null && cfg.getBoolean("shared-stock", false);
        ConfigurationSection sell = cfg != null ? cfg.getConfigurationSection("sell-multiplier") : null;
        ConfigurationSection buy  = cfg != null ? cfg.getConfigurationSection("buy-discount") : null;
        this.sellMember = read(sell, "member", 1.10);
        this.sellAdmin  = read(sell, "admin",  1.15);
        this.sellOwner  = read(sell, "owner",  1.20);
        this.buyMember  = read(buy,  "member", 0.95);
        this.buyAdmin   = read(buy,  "admin",  0.90);
        this.buyOwner   = read(buy,  "owner",  0.85);
    }

    private static double read(ConfigurationSection s, String key, double def) {
        return s != null ? s.getDouble(key, def) : def;
    }

    /** True when TeamsAPI is installed AND enabled in config. */
    public boolean isEnabled() {
        return configEnabled && TeamsAPI.isAvailable();
    }

    /** True when the shared team stock pool is enabled in config. */
    public boolean isSharedStockEnabled() {
        return sharedStock;
    }

    public Optional<Team> getPlayerTeam(UUID uuid) {
        if (!isEnabled()) return Optional.empty();
        try {
            return TeamsAPI.getService().getPlayerTeam(uuid);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<TeamRole> getMemberRole(UUID teamId, UUID playerUuid) {
        if (!isEnabled()) return Optional.empty();
        try {
            return TeamsAPI.getService().getMemberRole(teamId, playerUuid);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Sell price multiplier for the player's team role (stacks on top of EzBoost). */
    public double getSellMultiplier(Player player) {
        if (!isEnabled()) return 1.0;
        try {
            Optional<Team> team = TeamsAPI.getService().getPlayerTeam(player.getUniqueId());
            if (team.isEmpty()) return 1.0;
            Optional<TeamRole> role = TeamsAPI.getService().getMemberRole(team.get().getId(), player.getUniqueId());
            if (role.isEmpty()) return 1.0;
            return switch (role.get()) {
                case OWNER  -> sellOwner;
                case ADMIN  -> sellAdmin;
                case MEMBER -> sellMember;
            };
        } catch (Exception e) {
            return 1.0;
        }
    }

    /** Buy price multiplier for the player's team role (composable with EzBoost discount). */
    public double getBuyMultiplier(Player player) {
        if (!isEnabled()) return 1.0;
        try {
            Optional<Team> team = TeamsAPI.getService().getPlayerTeam(player.getUniqueId());
            if (team.isEmpty()) return 1.0;
            Optional<TeamRole> role = TeamsAPI.getService().getMemberRole(team.get().getId(), player.getUniqueId());
            if (role.isEmpty()) return 1.0;
            return switch (role.get()) {
                case OWNER  -> buyOwner;
                case ADMIN  -> buyAdmin;
                case MEMBER -> buyMember;
            };
        } catch (Exception e) {
            return 1.0;
        }
    }

    /** Friendly description of a player's current team perks, or null if not in a team. */
    public String describePerk(Player player) {
        if (!isEnabled()) return null;
        try {
            Optional<Team> team = TeamsAPI.getService().getPlayerTeam(player.getUniqueId());
            if (team.isEmpty()) return null;
            Optional<TeamRole> role = TeamsAPI.getService().getMemberRole(team.get().getId(), player.getUniqueId());
            if (role.isEmpty()) return null;
            double sell = getSellMultiplier(player);
            double buy  = getBuyMultiplier(player);
            String roleName = role.get().name().charAt(0) + role.get().name().substring(1).toLowerCase();
            return String.format("%s in %s: +%.0f%% sell, %.0f%% buy cost",
                roleName, team.get().getDisplayName(),
                (sell - 1.0) * 100, buy * 100);
        } catch (Exception e) {
            return null;
        }
    }
}
