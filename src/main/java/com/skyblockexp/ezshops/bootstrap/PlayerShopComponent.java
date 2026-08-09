package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.PluginComponent;
import com.skyblockexp.ezshops.gui.playershop.PlayerShopBrowseMenu;
import com.skyblockexp.ezshops.gui.playershop.PlayerShopBrowseMessages;
import com.skyblockexp.ezshops.repository.PlayerShopRepository;
import com.skyblockexp.ezshops.repository.mysql.MysqlPlayerShopRepository;
import com.skyblockexp.ezshops.database.jaloquent.JaloquentPlayerShopRepository;
import com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository;
import com.skyblockexp.ezshops.playershop.PlayerShopBrowseCommand;
import com.skyblockexp.ezshops.playershop.PlayerShopCommand;
import com.skyblockexp.ezshops.config.PlayerShopConfiguration;
import com.skyblockexp.ezshops.playershop.PlayerShopListener;
import com.skyblockexp.ezshops.playershop.PlayerShopManager;
import com.skyblockexp.ezshops.playershop.PlayerShopMessages;
import com.skyblockexp.ezshops.playershop.PlayerShopSetupMenu;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import com.skyblockexp.ezshops.database.jaloquent.JdbcJaloquentClientAdapter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

/**
 * Lifecycle component responsible for player shops and the /playershop command.
 */
public final class PlayerShopComponent implements PluginComponent {

    private static final String COMMAND_NAME = "playershop";
    private static final String BROWSE_COMMAND_NAME = "playershops";

    private final Economy economy;
    private final FileConfiguration configurationSource;

    private EzShopsPlugin plugin;
    private PlayerShopConfiguration configuration;
    private PlayerShopManager manager;
    private PlayerShopListener listener;
    private PlayerShopSetupMenu setupMenu;
    private PlayerShopBrowseMenu browseMenu;
    private PlayerShopCommand command;
    private PlayerShopBrowseCommand browseCommand;
    private PluginCommand pluginCommand;
    private PluginCommand browsePluginCommand;
    private boolean enabled;
    private String disabledMessage;

    public PlayerShopComponent(Economy economy, FileConfiguration configurationSource) {
        this.economy = Objects.requireNonNull(economy, "economy");
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
    }

    @Override
    public void enable(EzShopsPlugin plugin) {
        this.plugin = plugin;

        configuration = PlayerShopConfiguration.from(configurationSource, plugin.getLogger(),
            EzShopsRegistry.current().getCoreShopComponent().messageConfiguration());
        PlayerShopMessages messages = configuration.messages();
        disabledMessage = messages.commandDisabled();

        pluginCommand = requireCommand(plugin, COMMAND_NAME);
        browsePluginCommand = requireCommand(plugin, BROWSE_COMMAND_NAME);
        if (!configuration.enabled()) {
            registerFallbackCommand(pluginCommand);
            registerFallbackCommand(browsePluginCommand);
            if (EzShopsRegistry.current().isDebugMode()) {
                plugin.getLogger().info("Player shops are disabled via configuration.");
            }
            enabled = false;
            return;
        }

        PlayerShopRepository repository = createRepository(plugin);
        manager = new PlayerShopManager(plugin, economy, configuration, repository);
        manager.enable();

        listener = new PlayerShopListener(manager, configuration);
        setupMenu = new PlayerShopSetupMenu(plugin, manager, configuration);
        command = new PlayerShopCommand(manager, setupMenu, messages);

        ConfigurationSection browseSection = configurationSource
                .getConfigurationSection("player-shops.browse-gui");
        PlayerShopBrowseMessages browseMessages = PlayerShopBrowseMessages.from(browseSection);
        browseMenu = new PlayerShopBrowseMenu(plugin, manager, browseMessages);
        browseCommand = new PlayerShopBrowseCommand(browseMenu, browseMessages);

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(listener, plugin);
        pluginManager.registerEvents(setupMenu, plugin);
        pluginManager.registerEvents(browseMenu, plugin);

        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(null);
        browsePluginCommand.setExecutor(browseCommand);
        browsePluginCommand.setTabCompleter(null);
        enabled = true;
    }

    private PlayerShopRepository createRepository(EzShopsPlugin plugin) {
        String type = configurationSource.getString("player-shops.storage.type", "yaml");
        if ("jaloquent".equalsIgnoreCase(type)) {
            java.util.Map<String, String> config = com.skyblockexp.ezshops.config.DatabaseConfig.from(configurationSource);
            try {
                JdbcJaloquentClientAdapter adapter = new JdbcJaloquentClientAdapter();
                adapter.init(config);
                adapter.createTableIfAbsent();
                JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(plugin.getLogger(), adapter);
                plugin.getLogger().info("Player shops: using Jaloquent storage (JDBC adapter).");
                return repo;
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to initialise Jaloquent for player shops; falling back to YAML. " + ex.getMessage());
            }
        }
        if ("mysql".equalsIgnoreCase(type)) {
            java.util.Map<String, String> cfg = com.skyblockexp.ezshops.config.DatabaseConfig.from(configurationSource);
            String host = cfg.getOrDefault("host", "localhost");
            int port = Integer.parseInt(cfg.getOrDefault("port", "3306"));
            String database = cfg.getOrDefault("database", "minecraft");
            String username = cfg.getOrDefault("username", "root");
            String password = cfg.getOrDefault("password", "");
            String tablePrefix = cfg.getOrDefault("table-prefix", "ez_");
            MysqlPlayerShopRepository repo = new MysqlPlayerShopRepository(
                    host, port, database, username, password, tablePrefix, plugin.getLogger());
            try {
                repo.init();
                plugin.getLogger().info("Player shops: using MySQL storage.");
                return repo;
            } catch (IllegalStateException ex) {
                plugin.getLogger().warning("Failed to connect to MySQL for player shops; falling back to YAML. " + ex.getMessage());
            }
        }
        return new YmlPlayerShopRepository(plugin.getDataFolder(), plugin.getLogger());
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

        unregisterListener(browseMenu);
        browseMenu = null;

        command = null;
        browseCommand = null;
        enabled = false;

        if (pluginCommand != null) {
            pluginCommand.setExecutor(null);
            pluginCommand.setTabCompleter(null);
            pluginCommand = null;
        }

        if (browsePluginCommand != null) {
            browsePluginCommand.setExecutor(null);
            browsePluginCommand.setTabCompleter(null);
            browsePluginCommand = null;
        }

        plugin = null;
        configuration = null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Re-reads config and (re)registers player shop commands/listeners. */
    public void reload() {
        if (plugin == null) return;

        boolean wasEnabled = this.enabled;
        boolean nowEnabled = configurationSource.getBoolean("player-shops.enabled", true);

        if (wasEnabled && !nowEnabled) {
            disable();
                configuration = PlayerShopConfiguration.from(configurationSource, plugin.getLogger(),
                    EzShopsRegistry.current().getCoreShopComponent().messageConfiguration());
            disabledMessage = configuration.messages().commandDisabled();
            registerFallbackCommand(pluginCommand);
            enabled = false;
        } else if (!wasEnabled && nowEnabled) {
            enable(plugin);
        }
    }

    /** Returns the active {@link PlayerShopManager}, or {@code null} if player shops are disabled. */
    public PlayerShopManager getManager() {
        return manager;
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
