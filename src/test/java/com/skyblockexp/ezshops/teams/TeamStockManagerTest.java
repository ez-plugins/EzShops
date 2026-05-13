package com.skyblockexp.ezshops.teams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TeamStockManagerTest {

    @TempDir
    Path tempDir;

    TeamStockManager manager;
    UUID teamId;

    @BeforeEach
    void setUp() {
        manager = new TeamStockManager(tempDir.toFile());
        teamId  = UUID.randomUUID();
    }

    // ── addTeamStock ──────────────────────────────────────────────────────────

    @Test
    void add_stock_increases_amount() {
        assertTrue(manager.addTeamStock(teamId, "DIAMOND", 10));
        assertEquals(10, manager.getTeamStockAmount(teamId, "DIAMOND"));
    }

    @Test
    void add_stock_accumulates_across_calls() {
        manager.addTeamStock(teamId, "DIAMOND", 5);
        manager.addTeamStock(teamId, "DIAMOND", 3);
        assertEquals(8, manager.getTeamStockAmount(teamId, "DIAMOND"));
    }

    @Test
    void add_stock_rejects_zero_and_negative_amount() {
        assertFalse(manager.addTeamStock(teamId, "DIAMOND", 0));
        assertFalse(manager.addTeamStock(teamId, "DIAMOND", -3));
        assertEquals(0, manager.getTeamStockAmount(teamId, "DIAMOND"));
    }

    // ── removeTeamStock ───────────────────────────────────────────────────────

    @Test
    void remove_stock_reduces_amount() {
        manager.addTeamStock(teamId, "DIAMOND", 10);
        assertTrue(manager.removeTeamStock(teamId, "DIAMOND", 4));
        assertEquals(6, manager.getTeamStockAmount(teamId, "DIAMOND"));
    }

    @Test
    void remove_stock_returns_false_when_insufficient() {
        manager.addTeamStock(teamId, "DIAMOND", 5);
        assertFalse(manager.removeTeamStock(teamId, "DIAMOND", 10));
        assertEquals(5, manager.getTeamStockAmount(teamId, "DIAMOND"));
    }

    @Test
    void remove_all_stock_clears_entry() {
        manager.addTeamStock(teamId, "DIAMOND", 5);
        assertTrue(manager.removeTeamStock(teamId, "DIAMOND", 5));
        assertEquals(0, manager.getTeamStockAmount(teamId, "DIAMOND"));
        assertTrue(manager.getTeamOwnedStocks(teamId).isEmpty());
    }

    /**
     * Bug: {@code removeTeamStock} lacked the {@code amount <= 0} guard that
     * {@code addTeamStock} has. Passing a negative amount computes
     * {@code current - (negative) = current + |amount|}, silently inflating
     * the team stock rather than failing or removing anything.
     */
    @Test
    void remove_stock_rejects_zero_and_negative_amount() {
        manager.addTeamStock(teamId, "DIAMOND", 5);

        // Zero removal should be rejected.
        assertFalse(manager.removeTeamStock(teamId, "DIAMOND", 0),
                "removeTeamStock(0) should return false");

        // Negative amount must be rejected — must NOT increase stock.
        assertFalse(manager.removeTeamStock(teamId, "DIAMOND", -3),
                "removeTeamStock(-3) should return false, not inflate stock");

        // Stock must be exactly as it was before the invalid calls.
        assertEquals(5, manager.getTeamStockAmount(teamId, "DIAMOND"),
                "Stock must be unchanged after rejected removal");
    }

    // ── getTeamOwnedStocks ────────────────────────────────────────────────────

    @Test
    void owned_stocks_lists_only_items_with_positive_amount() {
        manager.addTeamStock(teamId, "DIAMOND", 5);
        manager.addTeamStock(teamId, "GOLD_INGOT", 3);
        manager.removeTeamStock(teamId, "GOLD_INGOT", 3); // removes entry

        List<String> owned = manager.getTeamOwnedStocks(teamId);
        assertTrue(owned.contains("DIAMOND"));
        assertFalse(owned.contains("GOLD_INGOT"));
    }

    @Test
    void owned_stocks_empty_when_no_file() {
        assertTrue(manager.getTeamOwnedStocks(UUID.randomUUID()).isEmpty());
    }

    // ── deleteTeamData ────────────────────────────────────────────────────────

    @Test
    void delete_team_data_returns_zero_amount_afterwards() {
        manager.addTeamStock(teamId, "DIAMOND", 10);
        manager.deleteTeamData(teamId);
        assertEquals(0, manager.getTeamStockAmount(teamId, "DIAMOND"));
        assertTrue(manager.getTeamOwnedStocks(teamId).isEmpty());
    }
}
