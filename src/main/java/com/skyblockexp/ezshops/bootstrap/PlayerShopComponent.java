package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.PluginComponent;
import com.skyblockexp.ezshops.repository.PlayerShopRepository;
import com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository;
import com.skyblockexp.ezshops.playershop.PlayerShopCommand;
import com.skyblockexp.ezshops.config.PlayerShopConfiguration;
import com.skyblockexp.ezshops.playershop.PlayerShopListener;
import com.skyblockexp.ezshops.playershop.PlayerShopManager;
import com.skyblockexp.ezshops.playershop.PlayerShopMessages;
import com.skyblockexp.ezshops.playershop.PlayerShopSetupMenu;
import java.util.Objects;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

/**
 * Lifecycle component responsible for player shops and the /playershop command.
 */
public final class PlayerShopComponent implements PluginComponent {

    private static final String COMMAND_NAME = "playershop";

    private final Economy economy;
    private final FileConfiguration configurationSource;

    private EzShopsPlugin plugin;
    private PlayerShopConfiguration configuration;
    private PlayerShopManager manager;
    private PlayerShopListener listener;
    private PlayerShopSetupMenu setupMenu;
    private PlayerShopCommand command;
    private PluginCommand pluginCommand;
    private boolean enabled;
    private String disabledMessage;

    public PlayerShopComponent(Economy economy, FileConfiguration configurationSource) {
        this.economy = Objects.requireNonNull(economy, "economy");
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
    }

    @Override
    public void enable(EzShopsPlugin plugin) {
        this.plugin = plugin;

        configuration = PlayerShopConfiguration.from(configurationSource, plugin.getLogger(), ((EzShopsPlugin) plugin).getCoreShopComponent().messageConfiguration());
        PlayerShopMessages messages = configuration.messages();
        disabledMessage = messages.commandDisabled();

        pluginCommand = requireCommand(plugin, COMMAND_NAME);
        if (!configuration.enabled()) {
            registerFallbackCommand(pluginCommand);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Player shops are disabled via configuration.");
            }
            enabled = false;
            return;
        }

        PlayerShopRepository repository = new YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
        manager = new PlayerShopManager(plugin, economy, configuration, repository);
        manager.enable();

        listener = new PlayerShopListener(manager, configuration);
        setupMenu = new PlayerShopSetupMenu(plugin, manager, configuration);
        command = new PlayerShopCommand(manager, setupMenu, messages);

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(listener, plugin);
        pluginManager.registerEvents(setupMenu, plugin);

        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(null);
        enabled = true;
    }

    @Override
    public void disable() {
        if (manager != null) {
            manager.disable();
            manager = null;
        }

        unregisterListener(listener);
        listener = null;

        unregisterListener(setupMenu);
        setupMenu = null;

        command = null;
        enabled = false;

        if (pluginCommand != null) {
            pluginCommand.setExecutor(null);
            pluginCommand.setTabCompleter(null);
            pluginCommand = null;
        }

        plugin = null;
        configuration = null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String disabledCommandMessage() {
        if (disabledMessage != null && !disabledMessage.isBlank()) {
            return disabledMessage;
        }
        return PlayerShopConfiguration.defaults().messages().commandDisabled();
    }

    private PluginCommand requireCommand(EzShopsPlugin plugin, String name) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().severe("Plugin command '" + name + "' is not defined in plugin.yml. EzShops will be unusable.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("Missing required command '" + name + "'.");
        }
        return command;
    }

    private void registerFallbackCommand(PluginCommand command) {
        CommandExecutor executor = new DisabledCommandExecutor(disabledCommandMessage());
        command.setExecutor(executor);
        command.setTabCompleter(null);
    }

    private void unregisterListener(Listener listener) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }

    private static final class DisabledCommandExecutor implements CommandExecutor {

        private final String message;

        private DisabledCommandExecutor(String message) {
            this.message = message;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (message != null && !message.isBlank()) {
                sender.sendMessage(message);
            }
            return true;
        }
    }
}
