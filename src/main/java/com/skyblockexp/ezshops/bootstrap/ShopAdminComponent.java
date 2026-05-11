package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.gui.admin.ShopAdminBrowseGui;
import com.skyblockexp.ezshops.playershop.PlayerShopManager;
import com.skyblockexp.ezshops.shop.command.ShopAdminCommand;
import com.skyblockexp.ezshops.teams.TeamMarketManager;
import org.bukkit.command.PluginCommand;

/**
 * Wires the {@code /shopadmin} command and its browse GUI.
 *
 * <p>Must be added to the component list <em>after</em> both
 * {@link PlayerShopComponent} and {@link TeamShopComponent} so their managers
 * are fully initialised.
 */
public final class ShopAdminComponent implements PluginComponent {

    private final PlayerShopComponent playerShopComponent;
    private final TeamShopComponent   teamShopComponent;

    private ShopAdminCommand command;

    public ShopAdminComponent(PlayerShopComponent playerShopComponent,
                              TeamShopComponent teamShopComponent) {
        this.playerShopComponent = playerShopComponent;
        this.teamShopComponent   = teamShopComponent;
    }

    @Override
    public void enable(EzShopsPlugin plugin) {
        PlayerShopManager playerShopManager = playerShopComponent.getManager();
        if (playerShopManager == null) {
            plugin.getLogger().info("[EzShops] /shopadmin: player shops disabled; shop admin GUI will show an empty player-shops view.");
        }

        TeamMarketManager teamMarketManager = teamShopComponent.getTeamMarketManager();

        // ShopAdminBrowseGui handles null managers gracefully
        ShopAdminBrowseGui browseGui = new ShopAdminBrowseGui(playerShopManager, teamMarketManager);
        command = new ShopAdminCommand(browseGui);

        PluginCommand pluginCommand = plugin.getCommand("shopadmin");
        if (pluginCommand == null) {
            plugin.getLogger().warning("[EzShops] 'shopadmin' command not found in plugin.yml — /shopadmin will be unavailable.");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
        plugin.getServer().getPluginManager().registerEvents(command, plugin);

        plugin.getLogger().info("[EzShops] /shopadmin command registered.");
    }

    @Override
    public void disable() {
        command = null;
    }
}
