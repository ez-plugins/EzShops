package com.skyblockexp.ezshops.teams;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TeamTreasuryTest {

    @TempDir
    Path tempDir;

    Economy economy;
    Player player;
    TeamTreasury treasury;
    UUID teamId;

    @BeforeEach
    void setUp() {
        economy = mock(Economy.class);
        player  = mock(Player.class);
        teamId  = UUID.randomUUID();
        treasury = new TeamTreasury(tempDir.toFile(), economy);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    // ── deposit ───────────────────────────────────────────────────────────────

    @Test
    void deposit_debits_player_and_credits_treasury() {
        when(economy.getBalance(player)).thenReturn(200.0);
        when(economy.withdrawPlayer((OfflinePlayer) player, 50.0)).thenReturn(success(50.0));

        boolean result = treasury.deposit(teamId, player, 50.0);

        assertTrue(result);
        assertEquals(50.0, treasury.getBalance(teamId), 0.001);
        verify(economy).withdrawPlayer((OfflinePlayer) player, 50.0);
    }

    @Test
    void deposit_rejects_when_player_balance_insufficient() {
        when(economy.getBalance(player)).thenReturn(10.0);

        assertFalse(treasury.deposit(teamId, player, 50.0));
        assertEquals(0.0, treasury.getBalance(teamId), 0.001);
        verify(economy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
    }

    @Test
    void deposit_rejects_zero_and_negative_amount() {
        when(economy.getBalance(player)).thenReturn(1000.0);

        assertFalse(treasury.deposit(teamId, player, 0.0));
        assertFalse(treasury.deposit(teamId, player, -10.0));
        assertEquals(0.0, treasury.getBalance(teamId), 0.001);
    }

    // ── withdraw ──────────────────────────────────────────────────────────────

    @Test
    void withdraw_credits_player_and_debits_treasury() {
        treasury.splitDeposit(teamId, 100.0);
        when(economy.depositPlayer((OfflinePlayer) player, 50.0)).thenReturn(success(50.0));

        boolean result = treasury.withdraw(teamId, player, 50.0);

        assertTrue(result);
        assertEquals(50.0, treasury.getBalance(teamId), 0.001);
        verify(economy).depositPlayer((OfflinePlayer) player, 50.0);
    }

    @Test
    void withdraw_rejects_when_treasury_insufficient() {
        treasury.splitDeposit(teamId, 30.0);

        assertFalse(treasury.withdraw(teamId, player, 50.0));
        assertEquals(30.0, treasury.getBalance(teamId), 0.001);
        verify(economy, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
    }

    @Test
    void withdraw_rejects_zero_and_negative_amount() {
        treasury.splitDeposit(teamId, 100.0);

        assertFalse(treasury.withdraw(teamId, player, 0.0));
        assertFalse(treasury.withdraw(teamId, player, -10.0));
        assertEquals(100.0, treasury.getBalance(teamId), 0.001);
        verify(economy, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
    }

    /**
     * Bug: the original code called {@code economy.depositPlayer()} BEFORE saving the
     * reduced treasury balance. If the YAML save fails (the file is read-only here),
     * the player still receives the money while the treasury is never debited —
     * creating currency from nothing.
     *
     * Fix: deduct the treasury balance first; only pay the player after the save
     * succeeds; return {@code false} (and do NOT call depositPlayer) on save failure.
     */
    @Test
    void withdraw_does_not_pay_player_when_treasury_save_fails() throws Exception {
        treasury.splitDeposit(teamId, 100.0);

        // Make the treasury YAML file read-only so the next save attempt fails.
        File teamFile = new File(tempDir.toFile(), "team-treasury/" + teamId + ".yml");
        assertTrue(teamFile.exists(), "Treasury file should exist after splitDeposit");
        teamFile.setWritable(false);

        try {
            when(economy.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(success(50.0));

            boolean result = treasury.withdraw(teamId, player, 50.0);

            // After fix: treasury deduction attempted first → save fails → return false,
            // depositPlayer never called (player receives no money).
            assertFalse(result, "Withdraw should fail when treasury YAML cannot be saved");
            verify(economy, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
        } finally {
            teamFile.setWritable(true);
        }

        // Treasury balance unchanged (file was read-only, save failed).
        assertEquals(100.0, treasury.getBalance(teamId), 0.001);
    }

    // ── splitDeposit ──────────────────────────────────────────────────────────

    @Test
    void split_deposit_increases_balance_without_touching_vault() {
        treasury.splitDeposit(teamId, 25.0);
        assertEquals(25.0, treasury.getBalance(teamId), 0.001);
        verifyNoInteractions(economy);
    }

    @Test
    void split_deposit_ignores_zero_and_negative_amounts() {
        treasury.splitDeposit(teamId, 0.0);
        treasury.splitDeposit(teamId, -5.0);
        assertEquals(0.0, treasury.getBalance(teamId), 0.001);
    }

    // ── deleteTeamData ────────────────────────────────────────────────────────

    @Test
    void delete_team_data_returns_zero_balance_afterwards() {
        treasury.splitDeposit(teamId, 50.0);
        treasury.deleteTeamData(teamId);
        assertEquals(0.0, treasury.getBalance(teamId), 0.001);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static EconomyResponse success(double amount) {
        return new EconomyResponse(amount, 0.0, EconomyResponse.ResponseType.SUCCESS, "");
    }
}
