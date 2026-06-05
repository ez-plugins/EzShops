package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.gui.admin.ShopAdminBrowseGui;
import com.skyblockexp.ezshops.gui.admin.SetupShopsGui;
import com.skyblockexp.ezshops.gui.admin.SetupShopsGuiListener;
import com.skyblockexp.ezshops.playershop.PlayerShopManager;
import com.skyblockexp.ezshops.shop.command.ShopAdminCommand;
import com.skyblockexp.ezshops.shop.command.SetupShopsCommand;
import com.skyblockexp.ezshops.teams.TeamMarketManager;
import org.bukkit.command.PluginCommand;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Wires the {@code /shopadmin} command, the setup shops GUI, and their listeners.
 *
 * <p>Must be added to the component list <em>after</em> both
 * {@link PlayerShopComponent} and {@link TeamShopComponent} so their managers
 * are fully initialised.
 */
public final class ShopAdminComponent implements PluginComponent {

    private final PlayerShopComponent playerShopComponent;
    private final TeamShopComponent   teamShopComponent;

    private ShopAdminCommand shopAdminCommand;
    private SetupShopsCommand setupShopsCommand;
    private SetupShopsGuiListener setupShopsGuiListener;

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
        shopAdminCommand = new ShopAdminCommand(browseGui);

        PluginCommand pluginCommand = plugin.getCommand("shopadmin");
        if (pluginCommand == null) {
            plugin.getLogger().warning("[EzShops] 'shopadmin' command not found in plugin.yml — /shopadmin will be unavailable.");
        } else {
            pluginCommand.setExecutor(shopAdminCommand);
            pluginCommand.setTabCompleter(shopAdminCommand);
            plugin.getServer().getPluginManager().registerEvents(shopAdminCommand, plugin);
            plugin.getLogger().info("[EzShops] /shopadmin command registered.");
        }

        // Setup Shops GUI for toggling features
        Set<String> gameModes = discoverGameModes(plugin);
        SetupShopsGui setupGui = new SetupShopsGui(plugin.getConfig(), gameModes);
        setupShopsCommand = new SetupShopsCommand(setupGui);
        setupShopsGuiListener = new SetupShopsGuiListener(plugin, setupGui);

        PluginCommand setupCommand = plugin.getCommand("setupshops");
        if (setupCommand == null) {
            plugin.getLogger().warning("[EzShops] 'setupshops' command not found in plugin.yml — /setupshops will be unavailable.");
        } else {
            setupCommand.setExecutor(setupShopsCommand);
            setupCommand.setTabCompleter(setupShopsCommand);
            plugin.getServer().getPluginManager().registerEvents(setupShopsGuiListener, plugin);
            plugin.getLogger().info("[EzShops] /setupshops command registered.");
        }
    }

    private Set<String> discoverGameModes(EzShopsPlugin plugin) {
        Set<String> modes = new LinkedHashSet<>();
        File shopDir = new File(plugin.getDataFolder(), "shop");
        if (shopDir.exists() && shopDir.isDirectory()) {
            File[] modeDirs = shopDir.listFiles(File::isDirectory);
            if (modeDirs != null) {
                for (File dir : modeDirs) {
                    modes.add(dir.getName());
                }
            }
        }
        // Default to "prison" if no modes found (will be created on first save)
        if (modes.isEmpty()) {
            modes.add("prison");
        }
        return modes;
    }

    @Override
    public void disable() {
        shopAdminCommand = null;
        setupShopsCommand = null;
        setupShopsGuiListener = null;
    }
}
