package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.shop.ShopTransactionPersistenceListener;
import com.skyblockexp.ezshops.repository.transaction.TransactionRepository;

import com.skyblockexp.ezshops.shop.command.PriceCommand;
import com.skyblockexp.ezshops.shop.command.PricingAdminCommand;
import com.skyblockexp.ezshops.shop.command.SellCommand;
import com.skyblockexp.ezshops.shop.command.SellHandCommand;
import com.skyblockexp.ezshops.shop.command.SellInventoryCommand;
import com.skyblockexp.ezshops.shop.command.ShopCommand;
import com.skyblockexp.ezshops.gui.quicksell.QuickSellMenu;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.gui.IslandLevelProvider;
import com.skyblockexp.ezshops.gui.ShopMenu;
import com.skyblockexp.ezshops.config.DynamicPricingConfiguration;
import com.skyblockexp.ezshops.shop.ShopPriceLookupService;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.shop.ShopRotationManager;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import com.skyblockexp.ezshops.shop.api.ShopPriceService;
import com.skyblockexp.ezshops.shop.api.ShopTemplateService;
import com.skyblockexp.ezshops.shop.ShopTemplateServiceImpl;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

/**
 * Boots the core shop systems such as pricing, transactions, GUI, and commands.
 */
public final class CoreShopComponent implements PluginComponent {
    public int getCategoryCount() {
        if (pricingManager == null || pricingManager.getMenuLayout() == null) return 0;
        return pricingManager.getMenuLayout().categories().size();
    }

    private final Economy economy;

    private EzShopsPlugin plugin;
    private ShopMessageConfiguration messageConfiguration;
    private ShopPricingManager pricingManager;
    private ShopTransactionService transactionService;
    private ShopMenu shopMenu;
    private ShopRotationManager rotationManager;
    private ShopCommand shopCommand;
    private SellHandCommand sellHandCommand;
    private SellInventoryCommand sellInventoryCommand;
    private SellCommand sellCommand;
    private QuickSellMenu quickSellMenu;
    private PriceCommand priceCommand;
    private ShopPriceService shopPriceService;
    private ShopTemplateService shopTemplateService;
    private IslandLevelProvider islandLevelProvider;
    private boolean ignoreIslandRequirements;
    private com.skyblockexp.ezshops.teams.TeamsIntegration teamsIntegration;
    private com.skyblockexp.ezshops.teams.TeamTreasury teamTreasury;
    private TransactionRepository transactionRepository;
    private Listener transactionPersistenceListener;

public CoreShopComponent(Economy economy) {
        this.economy = economy;
    }

    /** Called by TeamShopComponent before enable() to inject team services. */
    public void setTeamsData(com.skyblockexp.ezshops.teams.TeamsIntegration teamsIntegration,
                             com.skyblockexp.ezshops.teams.TeamTreasury teamTreasury) {
        this.teamsIntegration = teamsIntegration;
        this.teamTreasury = teamTreasury;
    }

    @Override
    public void enable(EzShopsPlugin plugin) {
        this.plugin = plugin;

        DynamicPricingConfiguration dynamicPricingConfiguration =
                DynamicPricingConfiguration.from(plugin.getConfig(), plugin.getLogger());
        messageConfiguration = ShopMessageConfiguration.load(plugin);
        ShopMessageConfiguration.CommandMessages commandMessages = messageConfiguration.commands();
        ShopMessageConfiguration.TransactionMessages transactionMessages = messageConfiguration.transactions();
        ShopMessageConfiguration.GuiMessages guiMessages = messageConfiguration.gui();

        pricingManager = new ShopPricingManager(plugin, dynamicPricingConfiguration);
        transactionService = new ShopTransactionService(pricingManager, economy, transactionMessages);
        transactionService.setEzBoostBridge(new com.skyblockexp.ezshops.shop.EzBoostBridge(plugin.getLogger()));
        // Hook service for executing commands on buy/sell
        com.skyblockexp.ezshops.hook.TransactionHookService hookService = new com.skyblockexp.ezshops.hook.TransactionHookService(plugin);
        transactionService.setTransactionHookService(hookService);
        // Transaction persistence: create a repository and listener if configured.
        try {
            com.skyblockexp.ezshops.repository.transaction.TransactionRepository txRepo = null;
            String type = plugin.getConfig().getString("player-shops.storage.type", "yaml");
            if ("jaloquent".equalsIgnoreCase(type) || "mysql".equalsIgnoreCase(type)) {
                java.util.Map<String, String> cfg = com.skyblockexp.ezshops.config.DatabaseConfig.from(plugin.getConfig());
                txRepo = new com.skyblockexp.ezshops.database.jaloquent.JaloquentTransactionRepository(cfg);
                plugin.getLogger().info("Shop transactions: using SQL-backed transaction repository.");
            } else {
                txRepo = new com.skyblockexp.ezshops.repository.yml.YmlTransactionRepository(plugin.getDataFolder());
                plugin.getLogger().info("Shop transactions: using YAML transaction repository.");
            }
            ShopTransactionPersistenceListener listener = new ShopTransactionPersistenceListener(txRepo);
            this.transactionRepository = txRepo;
            this.transactionPersistenceListener = listener;
            registerListener(plugin.getServer().getPluginManager(), listener);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to initialise transaction repository: " + ex.getMessage());
        }
        // Wire TeamsAPI integration if available
        if (teamsIntegration != null && teamTreasury != null) {
            double split = plugin.getConfig().getDouble("teams-integration.treasury-split", 0.05);
            transactionService.setTeamsIntegration(teamsIntegration, teamTreasury, split);
        }

        ServicesManager servicesManager = plugin.getServer().getServicesManager();
        shopPriceService = new ShopPriceLookupService(pricingManager, plugin.getLogger());
        servicesManager.register(ShopPriceService.class, shopPriceService, plugin, ServicePriority.Normal);

        java.io.File templatesDir = new java.io.File(plugin.getDataFolder(), "templates");
        shopTemplateService = new ShopTemplateServiceImpl(templatesDir);
        servicesManager.register(ShopTemplateService.class, shopTemplateService, plugin, ServicePriority.Normal);

        islandLevelProvider = createIslandLevelProvider(plugin);
        ignoreIslandRequirements = islandLevelProvider == null;
        if (ignoreIslandRequirements && plugin.isDebugMode()) {
            plugin.getLogger().info(
                    "Island level provider not detected; island requirements will be ignored.");
        }

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        // Apply initial state based on config
        boolean categoriesEnabled = plugin.getConfig().getBoolean("categories.enabled", true);
        boolean singleListWhenDisabled = plugin.getConfig().getBoolean("categories.single-list-when-disabled", false);
        if (categoriesEnabled) {
            shopMenu = new ShopMenu(plugin, pricingManager, transactionService, islandLevelProvider,
                    ignoreIslandRequirements, ShopMenu.DisplayMode.CATEGORIES, guiMessages,
                    transactionMessages.restrictions());
        } else if (singleListWhenDisabled) {
            shopMenu = new ShopMenu(plugin, pricingManager, transactionService, islandLevelProvider,
                    ignoreIslandRequirements, ShopMenu.DisplayMode.FLAT_LIST, guiMessages,
                    transactionMessages.restrictions());
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Shop categories are disabled; displaying all items in a single list.");
            }
        } else {
            shopMenu = null;
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("Shop categories are disabled; the /shop menu will be unavailable.");
            }
        }

        shopCommand = new ShopCommand(plugin, pricingManager, transactionService, shopMenu, commandMessages.shop(),
                transactionMessages.errors(), transactionMessages.restrictions(), plugin.isDebugMode());
        sellHandCommand = new SellHandCommand(transactionService, pricingManager, commandMessages.sellHand());
        sellInventoryCommand = new SellInventoryCommand(transactionService, commandMessages.sellInventory());
        priceCommand = new PriceCommand(pricingManager, transactionService, commandMessages.price());

        registerListener(pluginManager, shopMenu);
        registerCommand("shop", shopCommand);
        registerCommand("sellhand", sellHandCommand);
        registerCommand("sellinventory", sellInventoryCommand);
        registerCommand("price", priceCommand);

        rotationManager = new ShopRotationManager(plugin, pricingManager, shopMenu);
        rotationManager.enable();

        boolean quickSellEnabled = plugin.getConfig().getBoolean("quick-sell.enabled", true);
        if (quickSellEnabled) {
            String confirmSound = plugin.getConfig().getString("quick-sell.confirm-sound.name", "entity.experience_orb.pickup");
            float confirmSoundVolume = (float) plugin.getConfig().getDouble("quick-sell.confirm-sound.volume", 1.0);
            float confirmSoundPitch = (float) plugin.getConfig().getDouble("quick-sell.confirm-sound.pitch", 1.0);
            quickSellMenu = new QuickSellMenu(pricingManager, transactionService, commandMessages.sell(),
                    confirmSound, confirmSoundVolume, confirmSoundPitch);
            sellCommand = new SellCommand(quickSellMenu, commandMessages.sell(), true);
        } else {
            quickSellMenu = null;
            sellCommand = new SellCommand(null, commandMessages.sell(), false);
        }
        registerListener(pluginManager, quickSellMenu);
        registerCommand("sell", sellCommand);

        registerCommand("pricingadmin", new com.skyblockexp.ezshops.shop.command.PricingAdminCommand(pricingManager, commandMessages.pricingAdmin()));
    }

    @Override
    public void disable() {
        if (rotationManager != null) {
            rotationManager.disable();
            rotationManager = null;
        }

        unregisterListener(shopMenu);
        unregisterListener(quickSellMenu);
        if (transactionPersistenceListener != null) {
            unregisterListener(transactionPersistenceListener);
            transactionPersistenceListener = null;
        }
        if (transactionRepository != null) {
            try {
                transactionRepository.close();
            } catch (Exception ex) {
                if (plugin != null) {
                    plugin.getLogger().warning("Failed to close transaction repository: " + ex.getMessage());
                }
            }
            transactionRepository = null;
        }
        if (plugin != null) {
            ServicesManager servicesManager = plugin.getServer().getServicesManager();
            if (shopPriceService != null) {
                servicesManager.unregister(ShopPriceService.class, shopPriceService);
                shopPriceService = null;
            }
            if (shopTemplateService != null) {
                servicesManager.unregister(ShopTemplateService.class, shopTemplateService);
                shopTemplateService = null;
            }
        } else {
            shopPriceService = null;
        }

        shopCommand = null;
        sellHandCommand = null;
        sellInventoryCommand = null;
        sellCommand = null;
        quickSellMenu = null;
        priceCommand = null;
        shopMenu = null;
        transactionService = null;
        pricingManager = null;
        messageConfiguration = null;
        islandLevelProvider = null;
        ignoreIslandRequirements = false;
        plugin = null;
    }

    public ShopPricingManager pricingManager() {
        return pricingManager;
    }

    public ShopTransactionService transactionService() {
        return transactionService;
    }

    public ShopMessageConfiguration messageConfiguration() {
        return messageConfiguration;
    }

    public ShopTemplateService shopTemplateService() {
        return shopTemplateService;
    }

    public IslandLevelProvider islandLevelProvider() {
        return islandLevelProvider;
    }

    public boolean ignoreIslandRequirements() {
        return ignoreIslandRequirements;
    }

    /**
     * Returns the currently registered shop menu, or null if categories are disabled.
     */
    public ShopMenu getShopMenu() {
        return shopMenu;
    }

    /**
     * Returns true if quick-sell is enabled and the menu is registered.
     */
    public boolean isQuickSellRegistered() {
        return quickSellMenu != null;
    }

    /**
     * Re-reads config and (re)registers core shop commands/listeners.
     */
    public void reloadFeatures() {
        if (plugin == null) return;

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        boolean categoriesEnabled = plugin.getConfig().getBoolean("categories.enabled", true);
        boolean singleListWhenDisabled = plugin.getConfig().getBoolean("categories.single-list-when-disabled", false);

        // Handle categories/shop GUI state
        if (categoriesEnabled && this.shopMenu == null) {
            ShopMessageConfiguration.GuiMessages guiMessages = messageConfiguration.gui();
            ShopMessageConfiguration.TransactionMessages transactionMessages = messageConfiguration.transactions();
            this.shopMenu = new ShopMenu(plugin, pricingManager, transactionService, islandLevelProvider,
                    ignoreIslandRequirements, ShopMenu.DisplayMode.CATEGORIES, guiMessages,
                    transactionMessages.restrictions());
            registerListener(pluginManager, this.shopMenu);
            this.rotationManager.setShopMenu(this.shopMenu);
            this.shopCommand.setShopMenu(this.shopMenu);
        } else if (singleListWhenDisabled && this.shopMenu == null) {
            ShopMessageConfiguration.GuiMessages guiMessages = messageConfiguration.gui();
            ShopMessageConfiguration.TransactionMessages transactionMessages = messageConfiguration.transactions();
            this.shopMenu = new ShopMenu(plugin, pricingManager, transactionService, islandLevelProvider,
                    ignoreIslandRequirements, ShopMenu.DisplayMode.FLAT_LIST, guiMessages,
                    transactionMessages.restrictions());
            registerListener(pluginManager, this.shopMenu);
            this.rotationManager.setShopMenu(this.shopMenu);
            this.shopCommand.setShopMenu(this.shopMenu);
        } else if (!categoriesEnabled && !singleListWhenDisabled && this.shopMenu != null) {
            unregisterListener(this.shopMenu);
            this.shopMenu = null;
            this.rotationManager.setShopMenu(null);
            this.shopCommand.setShopMenu(null);
        }

        // Handle quick-sell state
        boolean quickSellEnabled = plugin.getConfig().getBoolean("quick-sell.enabled", true);
        if (quickSellEnabled && this.quickSellMenu == null) {
            String confirmSound = plugin.getConfig().getString("quick-sell.confirm-sound.name", "entity.experience_orb.pickup");
            float confirmSoundVolume = (float) plugin.getConfig().getDouble("quick-sell.confirm-sound.volume", 1.0);
            float confirmSoundPitch = (float) plugin.getConfig().getDouble("quick-sell.confirm-sound.pitch", 1.0);
            this.quickSellMenu = new QuickSellMenu(pricingManager, transactionService, messageConfiguration.commands().sell(),
                    confirmSound, confirmSoundVolume, confirmSoundPitch);
            registerListener(pluginManager, this.quickSellMenu);
            this.sellCommand = new SellCommand(this.quickSellMenu, messageConfiguration.commands().sell(), true);
            plugin.getCommand("sell").setExecutor(this.sellCommand);
        } else if (!quickSellEnabled && this.quickSellMenu != null) {
            unregisterListener(this.quickSellMenu);
            this.quickSellMenu = null;
            this.sellCommand = new SellCommand(null, messageConfiguration.commands().sell(), false);
            plugin.getCommand("sell").setExecutor(this.sellCommand);
        }
    }

    private void registerListener(PluginManager pluginManager, Listener listener) {
        if (listener != null) {
            pluginManager.registerEvents(listener, plugin);
        }
    }

    private void unregisterListener(Listener listener) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
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

    private IslandLevelProvider createIslandLevelProvider(EzShopsPlugin plugin) {
        Plugin skyblock = plugin.getServer().getPluginManager().getPlugin("SkyblockExperience");
        if (skyblock != null) {
            try {
                return new SkyblockIslandLevelProvider(skyblock);
            } catch (ReflectiveOperationException ex) {
                plugin.getLogger().warning("Failed to initialize Skyblock island level integration: " + ex.getMessage());
            }
        }
        return null;
    }

    private static final class SkyblockIslandLevelProvider implements IslandLevelProvider {

        private final Plugin skyblockPlugin;
        private final Method getIslandManagerMethod;
        private final Method getDefaultIslandMethod;
        private final Method levelMethod;

        private SkyblockIslandLevelProvider(Plugin skyblockPlugin) throws ReflectiveOperationException {
            this.skyblockPlugin = skyblockPlugin;
            Class<?> pluginClass = skyblockPlugin.getClass();
            getIslandManagerMethod = pluginClass.getMethod("getIslandManager");

            ClassLoader loader = skyblockPlugin.getClass().getClassLoader();
            Class<?> islandManagerClass = Class.forName("com.skyblockexp.island.IslandManager", true, loader);
            getDefaultIslandMethod = islandManagerClass.getMethod("getDefaultIsland", UUID.class);

            Class<?> islandDataClass = Class.forName("com.skyblockexp.island.IslandManager$IslandData", true, loader);
            levelMethod = islandDataClass.getMethod("level");
        }

        @Override
        public int getIslandLevel(Player player) {
            try {
                Object islandManager = getIslandManagerMethod.invoke(skyblockPlugin);
                if (islandManager == null) {
                    return 0;
                }
                Object optional = getDefaultIslandMethod.invoke(islandManager, player.getUniqueId());
                if (optional instanceof Optional<?> islandOptional) {
                    if (islandOptional.isPresent()) {
                        Object islandData = islandOptional.get();
                        Object level = levelMethod.invoke(islandData);
                        if (level instanceof Number number) {
                            return number.intValue();
                        }
                    }
                    return 0;
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
            return 0;
        }
    }
}
