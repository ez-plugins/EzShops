package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.shop.command.PriceCommand;
import com.skyblockexp.ezshops.shop.command.SellHandCommand;
import com.skyblockexp.ezshops.shop.command.SellInventoryCommand;
import com.skyblockexp.ezshops.shop.command.ShopCommand;
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
    private PriceCommand priceCommand;
    private ShopPriceService shopPriceService;
    private IslandLevelProvider islandLevelProvider;
    private boolean ignoreIslandRequirements;

    public CoreShopComponent(Economy economy) {
        this.economy = economy;
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

        ServicesManager servicesManager = plugin.getServer().getServicesManager();
        shopPriceService = new ShopPriceLookupService(pricingManager, plugin.getLogger());
        servicesManager.register(ShopPriceService.class, shopPriceService, plugin, ServicePriority.Normal);

        islandLevelProvider = createIslandLevelProvider(plugin);
        ignoreIslandRequirements = islandLevelProvider == null;
        if (ignoreIslandRequirements && plugin.isDebugMode()) {
            plugin.getLogger().info(
                    "Island level provider not detected; island requirements will be ignored.");
        }

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

        rotationManager = new ShopRotationManager(plugin, pricingManager, shopMenu);
        rotationManager.enable();

        shopCommand = new ShopCommand(pricingManager, transactionService, shopMenu, commandMessages.shop(),
                transactionMessages.errors(), transactionMessages.restrictions());
        sellHandCommand = new SellHandCommand(transactionService, commandMessages.sellHand());
        sellInventoryCommand = new SellInventoryCommand(transactionService, commandMessages.sellInventory());
        priceCommand = new PriceCommand(pricingManager, transactionService, commandMessages.price());

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        registerListener(pluginManager, shopMenu);
        registerCommand("shop", shopCommand);
        registerCommand("sellhand", sellHandCommand);
        registerCommand("sellinventory", sellInventoryCommand);
        registerCommand("price", priceCommand);
    }

    @Override
    public void disable() {
        if (rotationManager != null) {
            rotationManager.disable();
            rotationManager = null;
        }

        unregisterListener(shopMenu);
        if (plugin != null) {
            ServicesManager servicesManager = plugin.getServer().getServicesManager();
            if (shopPriceService != null) {
                servicesManager.unregister(ShopPriceService.class, shopPriceService);
                shopPriceService = null;
            }
        } else {
            shopPriceService = null;
        }

        shopCommand = null;
        sellHandCommand = null;
        sellInventoryCommand = null;
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

    public IslandLevelProvider islandLevelProvider() {
        return islandLevelProvider;
    }

    public boolean ignoreIslandRequirements() {
        return ignoreIslandRequirements;
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
