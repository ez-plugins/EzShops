package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.config.ShopSignConfiguration;
import com.skyblockexp.ezshops.shop.ShopSignListener;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import com.skyblockexp.ezshops.shop.sign.SignShopGenerator;
import com.skyblockexp.ezshops.shop.sign.SignShopScanner;
import com.skyblockexp.ezshops.shop.sign.SignShopSetupCommand;
import com.skyblockexp.ezshops.shop.sign.SignShopSetupMenu;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

/**
 * Boots the sign shop ecosystem, including generators, setup menus, and commands.
 */
public final class SignShopComponent implements PluginComponent {

    private final CoreShopComponent coreComponent;

    private EzShopsPlugin plugin;
    private ShopSignListener shopSignListener;
    private SignShopGenerator signShopGenerator;
    private SignShopSetupMenu signShopSetupMenu;
    private SignShopScanner signShopScanner;
    private SignShopSetupCommand signShopSetupCommand;

    public SignShopComponent(CoreShopComponent coreComponent) {
        this.coreComponent = coreComponent;
    }

    @Override
    public void enable(EzShopsPlugin plugin) {
        this.plugin = plugin;

        ShopPricingManager pricingManager = coreComponent.pricingManager();
        ShopTransactionService transactionService = coreComponent.transactionService();
        ShopMessageConfiguration messageConfiguration = coreComponent.messageConfiguration();
        if (pricingManager == null || transactionService == null || messageConfiguration == null) {
            throw new IllegalStateException("Core shop component must be enabled before the sign shop component.");
        }

        ShopMessageConfiguration.CommandMessages commandMessages = messageConfiguration.commands();
        ShopMessageConfiguration.SignMessages signMessages = messageConfiguration.signs();
        ShopSignConfiguration signConfiguration = ShopSignConfiguration.from(plugin.getConfig(), plugin.getLogger(), messageConfiguration);

        shopSignListener = new ShopSignListener(plugin, pricingManager, transactionService, signConfiguration,
                signMessages);
        signShopGenerator = new SignShopGenerator(plugin, pricingManager, transactionService, signConfiguration);
        signShopSetupMenu = new SignShopSetupMenu(plugin, signShopGenerator, pricingManager);
        signShopScanner = new SignShopScanner(plugin, pricingManager, signShopGenerator, signConfiguration);
        signShopSetupCommand = new SignShopSetupCommand(signShopSetupMenu, signShopScanner,
                commandMessages.signShopScan(), signMessages);

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(shopSignListener, plugin);
        pluginManager.registerEvents(signShopSetupMenu, plugin);

        registerCommand("signshop", signShopSetupCommand);
    }

    @Override
    public void disable() {
        unregisterListener(shopSignListener);
        unregisterListener(signShopSetupMenu);

        signShopSetupCommand = null;
        signShopScanner = null;
        signShopSetupMenu = null;
        signShopGenerator = null;
        shopSignListener = null;
        plugin = null;
    }

    private void registerCommand(String name, CommandExecutor executor) {
        if (executor == null) {
            return;
        }
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().severe("Plugin command '" + name + "' is not defined in plugin.yml. EzShops will be unusable.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("Missing required command '" + name + "'.");
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    private void unregisterListener(Listener listener) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }
}
