package com.skyblockexp.ezshops.teams;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * YAML-backed team treasury.
 * Each team's balance is stored in {@code data/team-treasury/<teamId>.yml}.
 * Deposits and withdrawals are mirrored against the player's Vault balance.
 */
public final class TeamTreasury {

    private static final String KEY_BALANCE = "balance";

    private final File dataDir;
    private final Economy economy;

    public TeamTreasury(File pluginDataFolder, Economy economy) {
        this.dataDir = new File(pluginDataFolder, "team-treasury");
        this.economy = economy;
        if (!this.dataDir.exists()) {
            this.dataDir.mkdirs();
        }
    }

    private File fileFor(UUID teamId) {
        return new File(dataDir, teamId.toString() + ".yml");
    }

    public double getBalance(UUID teamId) {
        File f = fileFor(teamId);
        if (!f.exists()) return 0.0;
        return YamlConfiguration.loadConfiguration(f).getDouble(KEY_BALANCE, 0.0);
    }

    /**
     * Player pays {@code amount} from their Vault balance into the team treasury.
     * Returns false if the player cannot afford it or the Vault transaction fails.
     */
    public boolean deposit(UUID teamId, Player player, double amount) {
        if (amount <= 0) return false;
        if (economy.getBalance(player) < amount) return false;
        var resp = economy.withdrawPlayer(player, amount);
        if (!resp.transactionSuccess()) return false;
        if (!addToBalance(teamId, amount)) {
            economy.depositPlayer(player, amount); // refund: player money taken but save failed
            return false;
        }
        return true;
    }

    /**
     * Withdraws {@code amount} from the treasury and deposits it into the player's Vault balance.
     * Returns false if the treasury has insufficient funds.
     *
     * <p>The treasury is debited <em>first</em>. If the YAML save fails the method
     * returns {@code false} and the player's economy balance is never touched,
     * preventing money from being created from nothing on a failed save.
     */
    public boolean withdraw(UUID teamId, Player player, double amount) {
        if (amount <= 0) return false;
        double current = getBalance(teamId);
        if (current < amount) return false;
        // Deduct from treasury first — if the save fails we abort before paying the player.
        if (!addToBalance(teamId, -amount)) return false;
        var resp = economy.depositPlayer(player, amount);
        if (!resp.transactionSuccess()) {
            addToBalance(teamId, amount); // refund treasury
            return false;
        }
        return true;
    }

    /**
     * Directly adds {@code amount} to the treasury without touching a player account.
     * Used for the automatic treasury-split on sell transactions.
     */
    public void splitDeposit(UUID teamId, double amount) {
        if (amount > 0) {
            addToBalance(teamId, amount);
        }
    }

    /** Delete all treasury data for a team (called on TeamDeleteEvent). */
    public void deleteTeamData(UUID teamId) {
        File f = fileFor(teamId);
        if (f.exists()) f.delete();
    }

    /**
     * Atomically adjusts the stored balance by {@code delta} and persists it.
     * Returns {@code false} if the file could not be saved (the in-memory YAML
     * state is discarded so the on-disk value remains unchanged).
     */
    private boolean addToBalance(UUID teamId, double delta) {
        File f = fileFor(teamId);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
        double current = yaml.getDouble(KEY_BALANCE, 0.0);
        yaml.set(KEY_BALANCE, Math.max(0.0, current + delta));
        try {
            yaml.save(f);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
