package com.skyblockexp.ezshops.teams;

import com.skyblockexp.teamsapi.event.TeamDeleteEvent;
import com.skyblockexp.teamsapi.event.TeamJoinEvent;
import com.skyblockexp.teamsapi.event.TeamLeaveEvent;
import com.skyblockexp.teamsapi.event.TeamRoleChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listens for TeamsAPI events to keep team-shop data in sync.
 */
public class TeamsEventListener implements Listener {

    private final TeamStockManager teamStockManager;
    private final TeamTreasury teamTreasury;

    public TeamsEventListener(TeamStockManager teamStockManager, TeamTreasury teamTreasury) {
        this.teamStockManager = teamStockManager;
        this.teamTreasury = teamTreasury;
    }

    /** When a team is deleted, clean up all associated data. */
    @EventHandler
    public void onTeamDelete(TeamDeleteEvent event) {
        UUID teamId = event.getTeam().getId();
        teamStockManager.deleteTeamData(teamId);
        teamTreasury.deleteTeamData(teamId);
    }

    /** No action needed on join — data files are created on first use. */
    @EventHandler
    public void onTeamJoin(TeamJoinEvent event) {
        // No-op: data is initialised lazily
    }

    /** No action needed on leave — treasury and stock stay until team is deleted. */
    @EventHandler
    public void onTeamLeave(TeamLeaveEvent event) {
        // No-op: data persists for the team
    }

    /** No action needed on role change — multipliers are read live from TeamsAPI. */
    @EventHandler
    public void onTeamRoleChange(TeamRoleChangeEvent event) {
        // No-op: multipliers are computed dynamically per request
    }
}
