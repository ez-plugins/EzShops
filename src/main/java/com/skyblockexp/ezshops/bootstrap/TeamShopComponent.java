package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.gui.teams.TeamDashboardGui;
import com.skyblockexp.ezshops.gui.teams.TeamGuiListener;
import com.skyblockexp.ezshops.gui.teams.TeamStockGui;
import com.skyblockexp.ezshops.gui.teams.TeamTreasuryGui;
import com.skyblockexp.ezshops.teams.TeamStockManager;
import com.skyblockexp.ezshops.teams.TeamTreasury;
import com.skyblockexp.ezshops.teams.TeamsEventListener;
import com.skyblockexp.ezshops.teams.TeamsIntegration;
import com.skyblockexp.ezshops.teams.command.TeamShopCommand;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Bootstrap component for the TeamsAPI integration.
 * Must be registered BEFORE {@link CoreShopComponent} in the components list
 * so that {@link TeamsIntegration} and {@link TeamTreasury} exist when
 * {@link CoreShopComponent#enable(EzShopsPlugin)} wires the transaction service.
 */
public final class TeamShopComponent implements PluginComponent {

    private final Economy economy;

    private TeamsIntegration teamsIntegration;
    private TeamStockManager teamStockManager;
    private TeamTreasury teamTreasury;
    private boolean enabled = false;

    public TeamShopComponent(Economy economy) {
        this.economy = economy;
    }

    @Override
    public void enable(EzShopsPlugin plugin) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("teams-integration");
        boolean cfgEnabled = cfg == null || cfg.getBoolean("enabled", true);

        if (!cfgEnabled) {
            plugin.getLogger().info("[EzShops] teams-integration is disabled in config.yml — skipping.");
            return;
        }

        if (!TeamsAPI.isAvailable()) {
            plugin.getLogger().info("[EzShops] TeamsAPI not found — team shop features disabled.");
            return;
        }

        try {
            teamsIntegration = new TeamsIntegration(cfg);
            teamStockManager = new TeamStockManager(plugin.getDataFolder());
            teamTreasury = new TeamTreasury(plugin.getDataFolder(), economy);

            // GUI instances
            TeamDashboardGui dashboardGui = new TeamDashboardGui(teamsIntegration, teamTreasury);
            TeamTreasuryGui treasuryGui = new TeamTreasuryGui(teamsIntegration, teamTreasury, economy);
            TeamStockGui stockGui = new TeamStockGui(teamsIntegration, teamStockManager);

            // Listeners
            TeamsEventListener eventListener = new TeamsEventListener(teamStockManager, teamTreasury);
            TeamGuiListener guiListener = new TeamGuiListener(teamsIntegration, teamTreasury, dashboardGui, treasuryGui, economy);
            plugin.getServer().getPluginManager().registerEvents(eventListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);

            // Command
            TeamShopCommand command = new TeamShopCommand(dashboardGui, treasuryGui, stockGui);
            PluginCommand pluginCommand = plugin.getCommand("teamshop");
            if (pluginCommand != null) {
                pluginCommand.setExecutor(command);
                pluginCommand.setTabCompleter(command);
            } else {
                plugin.getLogger().warning("[EzShops] 'teamshop' command not found in plugin.yml — check configuration.");
            }

            // Wire into CoreShopComponent if already constructed (safe guard; normally CoreShopComponent wires itself)
            if (plugin.getCoreComponent() != null) {
                double split = plugin.getConfig().getDouble("teams-integration.treasury-split", 0.05);
                plugin.getCoreComponent().setTeamsData(teamsIntegration, teamTreasury);
            }

            enabled = true;
            plugin.getLogger().info("[EzShops] TeamsAPI integration enabled.");
        } catch (Exception e) {
            plugin.getLogger().severe("[EzShops] Failed to enable TeamsAPI integration: " + e.getMessage());
            teamsIntegration = null;
            teamTreasury = null;
            teamStockManager = null;
        }
    }

    @Override
    public void disable() {
        teamsIntegration = null;
        teamStockManager = null;
        teamTreasury = null;
        enabled = false;
    }

    public TeamsIntegration getTeamsIntegration() {
        return teamsIntegration;
    }

    public TeamTreasury getTeamTreasury() {
        return teamTreasury;
    }

    public TeamStockManager getTeamStockManager() {
        return teamStockManager;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
