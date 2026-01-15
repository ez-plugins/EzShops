package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.repository.StockMarketRepository;
import com.skyblockexp.ezshops.repository.yml.YmlStockMarketRepository;
import com.skyblockexp.ezshops.stock.StockAdminCommand;
import com.skyblockexp.ezshops.stock.StockCommand;
import com.skyblockexp.ezshops.config.StockMarketConfig;
import com.skyblockexp.ezshops.stock.StockMarketFrozenStore;
import com.skyblockexp.ezshops.stock.StockMarketManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.*;

/**
 * Boots the stock market system, including commands and tab completion.
 */
public final class StockComponent implements PluginComponent, TabCompleter {
        private com.skyblockexp.ezshops.gui.stock.StockOverviewGuiListener stockOverviewGuiListener;
    private EzShopsPlugin plugin;
    private StockMarketManager stockMarketManager;
    private StockMarketConfig stockMarketConfig;
    private StockMarketFrozenStore frozenStore;
    private long cooldownMillis;

    @Override
    public void enable(EzShopsPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        boolean stockEnabled = true;
        if (config.getConfigurationSection("stock") != null) {
            stockEnabled = config.getBoolean("stock.enabled", true);
        }
        if (!stockEnabled) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Stock features are disabled via config. Skipping stock system initialization.");
            }
            return;
        }
        this.stockMarketConfig = new StockMarketConfig(config);
        StockMarketRepository repository = new YmlStockMarketRepository(plugin.getDataFolder());
        this.frozenStore = new StockMarketFrozenStore(repository);
        this.stockMarketManager = new StockMarketManager();
        this.stockMarketManager.setStockMarketRepository(repository);
        // Enable async periodic persistence (every 5 minutes = 6000 ticks)
        this.stockMarketManager.enablePersistence(plugin, 6000L);
        this.cooldownMillis = config.getConfigurationSection("stock") != null ? config.getLong("stock.cooldown-millis", 0L) : 0L;
        registerCommand("stock", new StockCommand(plugin, stockMarketManager, cooldownMillis, stockMarketConfig, frozenStore));
        registerCommand("stockadmin", new StockAdminCommand(stockMarketManager, frozenStore, stockMarketConfig));
        // Register StockOverviewGuiListener for GUI sell actions
        com.skyblockexp.ezshops.gui.stock.StockOverviewGui stockOverviewGui = new com.skyblockexp.ezshops.gui.stock.StockOverviewGui(
            stockMarketManager, stockMarketConfig, frozenStore,
            new java.io.File(plugin.getDataFolder(), "stock-gui.yml"),
            plugin.isDebugMode()
        );
        stockOverviewGuiListener = new com.skyblockexp.ezshops.gui.stock.StockOverviewGuiListener(stockMarketManager, stockOverviewGui);
        plugin.getServer().getPluginManager().registerEvents(stockOverviewGuiListener, plugin);

        // Register AllStocksGuiListener for all-stocks GUI pagination
        // The listener creates AllStocksGui instances dynamically with proper references
        java.io.File stockGuiConfigFile = new java.io.File(plugin.getDataFolder(), "stock-gui.yml");
        com.skyblockexp.ezshops.gui.stock.AllStocksGuiListener allStocksGuiListener = 
            new com.skyblockexp.ezshops.gui.stock.AllStocksGuiListener(
                stockMarketManager, stockMarketConfig, frozenStore, stockGuiConfigFile, stockOverviewGui);
        plugin.getServer().getPluginManager().registerEvents(allStocksGuiListener, plugin);
        
        // Register StockTransactionConfirmGuiListener for buy/sell confirmation dialogs
        com.skyblockexp.ezshops.gui.stock.AllStocksGui allStocksGuiForTransactions = 
            new com.skyblockexp.ezshops.gui.stock.AllStocksGui(
                stockMarketManager, stockMarketConfig, frozenStore, stockGuiConfigFile);
        allStocksGuiForTransactions.setStockOverviewGui(stockOverviewGui);
        com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGuiListener transactionListener = 
            new com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGuiListener(stockMarketManager, allStocksGuiForTransactions);
        plugin.getServer().getPluginManager().registerEvents(transactionListener, plugin);

        // Register StockHistoryGuiListener for price history GUI
        com.skyblockexp.ezshops.gui.stock.StockHistoryGuiListener stockHistoryGuiListener = new com.skyblockexp.ezshops.gui.stock.StockHistoryGuiListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(stockHistoryGuiListener, plugin);
        com.skyblockexp.ezshops.gui.stock.StockHistoryGui.setListener(stockHistoryGuiListener);
    }

    @Override
    public void disable() {
        if (stockMarketManager != null) {
            stockMarketManager.disablePersistence();
        }
        plugin = null;
        stockMarketManager = null;
        stockMarketConfig = null;
        frozenStore = null;
        cooldownMillis = 0L;
    }

    private void registerCommand(String name, Object executor) {
        if (plugin == null || executor == null) return;
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().severe("Plugin command '" + name + "' is not defined in plugin.yml. EzShops will be unusable.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("Missing required command '" + name + "'.");
        }
        if (executor instanceof org.bukkit.command.CommandExecutor ce) {
            command.setExecutor(ce);
        }
        command.setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (cmd.equals("stock")) {
            if (args.length == 1) {
                return filterPrefix(args[0], Arrays.asList("buy", "sell", "overview"));
            }
            if ((args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("sell")) && args.length == 2) {
                Set<String> items = new HashSet<>();
                for (StockMarketConfig.OverrideItem override : stockMarketConfig.getAllOverrides()) {
                    items.add(override.id);
                }
                items.addAll(stockMarketManager.getAllProductIds());
                items.removeIf(stockMarketConfig::isBlocked);
                items.removeIf(frozenStore::isFrozen);
                return filterPrefix(args[1], new ArrayList<>(items));
            }
        } else if (cmd.equals("stockadmin")) {
            if (args.length == 1) {
                return filterPrefix(args[0], Arrays.asList("set", "reset", "freeze", "unfreeze", "reload", "listfrozen", "listoverrides"));
            }
            if (Arrays.asList("set", "reset", "freeze", "unfreeze").contains(args[0].toLowerCase(Locale.ROOT)) && args.length == 2) {
                Set<String> items = new HashSet<>();
                items.addAll(stockMarketManager.getAllProductIds());
                items.addAll(stockMarketConfig.getAllOverrides().stream().map(o -> o.id).toList());
                return filterPrefix(args[1], new ArrayList<>(items));
            }
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(String prefix, List<String> options) {
        if (prefix == null || prefix.isEmpty()) return options;
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(opt);
            }
        }
        return result;
    }

    public StockMarketManager getStockMarketManager() { return stockMarketManager; }
    public StockMarketConfig getStockMarketConfig() { return stockMarketConfig; }
    public StockMarketFrozenStore getFrozenStore() { return frozenStore; }
}