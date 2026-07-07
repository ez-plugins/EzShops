package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.gui.teams.TeamDashboardGui;
import com.skyblockexp.ezshops.gui.teams.TeamGuiListener;
import com.skyblockexp.ezshops.gui.teams.TeamMarketGui;
import com.skyblockexp.ezshops.gui.teams.TeamMarketGuiListener;
import com.skyblockexp.ezshops.gui.teams.TeamMarketListGui;
import com.skyblockexp.ezshops.gui.teams.TeamStockGui;
import com.skyblockexp.ezshops.gui.teams.TeamTreasuryGui;
import com.skyblockexp.ezshops.teams.TeamMarketManager;
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
    private TeamMarketManager teamMarketManager;
    private boolean enabled = false;

    public TeamShopComponent(Economy economy) {
        this.economy = economy;
    }

    @Override
    public void enable(EzShopsPlugin plugin) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("teams-integration");
        boolean cfgEnabled = cfg == null || cfg.getBoolean("enabled", true);

        if (!cfgEnabled) {
            plugin.getLogger().info("[EzShops] teams-integration is disabled in config.yml - skipping.");
            return;
        }

        try {
            if (!TeamsAPI.isAvailable()) {
                plugin.getLogger().info("[EzShops] TeamsAPI not found - team shop features disabled.");
                return;
            }
        } catch (NoClassDefFoundError ignored) {
            plugin.getLogger().info("[EzShops] TeamsAPI not found - team shop features disabled.");
            return;
        }

        try {
            teamsIntegration  = new TeamsIntegration(cfg);
            teamStockManager  = new TeamStockManager(plugin.getDataFolder());
            teamTreasury      = new TeamTreasury(plugin.getDataFolder(), economy);
            teamMarketManager = new TeamMarketManager(
                    plugin.getDataFolder(), economy, teamsIntegration, plugin.getLogger());
            teamMarketManager.onEnable();

            // ── GUI instances ──────────────────────────────────────────────
            // Read configurable price step sizes (small, medium, large)
            double[] priceSteps = TeamMarketListGui.DEFAULT_PRICE_STEPS;
            java.util.List<Double> stepCfg = cfg == null
                    ? java.util.List.of() : cfg.getDoubleList("market-price-steps");
            if (stepCfg.size() >= 3) {
                priceSteps = new double[]{stepCfg.get(0), stepCfg.get(1), stepCfg.get(2)};
            }
            TeamMarketListGui marketListGui = new TeamMarketListGui(teamsIntegration, teamMarketManager, priceSteps);
            TeamMarketGui marketGui = new TeamMarketGui(teamsIntegration, teamMarketManager, marketListGui);

            TeamDashboardGui dashboardGui = new TeamDashboardGui(teamsIntegration, teamTreasury, marketGui);
            TeamTreasuryGui  treasuryGui  = new TeamTreasuryGui(teamsIntegration, teamTreasury, economy);
            TeamStockGui     stockGui     = new TeamStockGui(teamsIntegration, teamStockManager);

            // ── Listeners ──────────────────────────────────────────────────
            TeamsEventListener eventListener = new TeamsEventListener(teamStockManager, teamTreasury);
            TeamGuiListener guiListener = new TeamGuiListener(
                    teamsIntegration, teamTreasury, dashboardGui, treasuryGui, stockGui, marketGui, economy);
            TeamMarketGuiListener marketGuiListener = new TeamMarketGuiListener(
                    marketGui, marketListGui, teamMarketManager, teamsIntegration);

            plugin.getServer().getPluginManager().registerEvents(eventListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(marketGuiListener, plugin);

            // ── Command ────────────────────────────────────────────────────
            TeamShopCommand command = new TeamShopCommand(dashboardGui, treasuryGui, stockGui, marketGui);
            PluginCommand pluginCommand = plugin.getCommand("teamshop");
            if (pluginCommand != null) {
                pluginCommand.setExecutor(command);
                pluginCommand.setTabCompleter(command);
            } else {
                plugin.getLogger().warning("[EzShops] 'teamshop' command not found in plugin.yml - check configuration.");
            }

            // ── Wire TeamsData into CoreShopComponent ──────────────────────
            if (EzShopsRegistry.current().getCoreShopComponent() != null) {
                EzShopsRegistry.current().getCoreShopComponent().setTeamsData(teamsIntegration, teamTreasury);
            }

            enabled = true;
            plugin.getLogger().info("[EzShops] TeamsAPI integration enabled.");
        } catch (Exception e) {
            plugin.getLogger().severe("[EzShops] Failed to enable TeamsAPI integration: " + e.getMessage());
            teamsIntegration  = null;
            teamTreasury      = null;
            teamStockManager  = null;
            teamMarketManager = null;
        }
    }

    @Override
    public void disable() {
        if (teamMarketManager != null) {
            teamMarketManager.onDisable();
            teamMarketManager = null;
        }
        teamsIntegration = null;
        teamStockManager = null;
        teamTreasury     = null;
        enabled          = false;
    }

    public TeamsIntegration  getTeamsIntegration()  { return teamsIntegration; }
    public TeamTreasury      getTeamTreasury()       { return teamTreasury; }
    public TeamStockManager  getTeamStockManager()   { return teamStockManager; }
    public TeamMarketManager getTeamMarketManager()  { return teamMarketManager; }
    public boolean           isEnabled()             { return enabled; }
}
