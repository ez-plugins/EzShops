package com.skyblockexp.ezshops;

import com.skyblockexp.ezshops.api.EzShopsAPI;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.bootstrap.MetricsComponent;
import com.skyblockexp.ezshops.bootstrap.PluginComponent;
import com.skyblockexp.ezshops.bootstrap.SignShopComponent;
import com.skyblockexp.ezshops.bootstrap.StockComponent;
import com.skyblockexp.ezshops.bootstrap.PlayerShopComponent;
import com.skyblockexp.ezshops.boost.SellPriceBoostEffect;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Standalone plugin responsible for the /shop command and sign shops.
 */
public class EzShopsPlugin extends JavaPlugin {
    private static final List<String> DEFAULT_SHOP_RESOURCES = List.of(
            "shop.yml",
            "stock-gui.yml",
            "shop/menu.yml",
            "shop/categories/building.yml",
            "shop/categories/daily_specials.yml",
            "shop/categories/decorations.yml",
            "shop/categories/enchantments.yml",
            "shop/categories/farming.yml",
            "shop/categories/fishing.yml",
            "shop/categories/food.yml",
            "shop/categories/mining.yml",
            "shop/categories/mob_drops.yml",
            "shop/categories/redstone.yml",
            "shop/categories/spawners.yml",
            "shop/categories/valuables.yml",
            "shop/categories/wood.yml",
            "shop/rotations/daily-specials.yml",

            // Add any bundled locale files here so they are copied alongside the defaults.
            "messages/messages_en.yml",
            "messages/messages_es.yml",
            "messages/messages_nl.yml",
            "messages/messages_zh.yml");

    private Economy economy;
    private List<PluginComponent> components;
    private CoreShopComponent coreComponent;
    private StockComponent stockComponent;
    private boolean debugMode;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault economy provider not found; disabling EzShops.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultResources();
        saveDefaultConfig();

        debugMode = getConfig().getBoolean("debug", false);

        coreComponent = new CoreShopComponent(economy);
        PlayerShopComponent playerShopComponent = new PlayerShopComponent(economy, getConfig());
        stockComponent = new StockComponent();
        components = new ArrayList<>();
        components.add(coreComponent);
        components.add(stockComponent);
        components.add(playerShopComponent);
        components.add(new SignShopComponent(coreComponent));
        components.add(new MetricsComponent());

        try {
            for (PluginComponent component : components) {
                component.enable(this);
            }
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Failed to enable EzShops component", ex);
            throw ex;
        }
        
        // Initialize the EzShops API after all components are enabled
        try {
            EzShopsAPI.initialize(this);
            getLogger().info("EzShops API initialized successfully.");
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Failed to initialize EzShops API", ex);
        }

        // Register EzBoost integration if EzBoost is present and integration is enabled
        boolean ezboostIntegration = getConfig().getBoolean("ezboost-integration", true);
        if (ezboostIntegration && getServer().getPluginManager().getPlugin("EzBoost") != null) {
            try {
                // Get the EzBoost plugin instance to access its class loader
                org.bukkit.plugin.Plugin ezBoostPlugin = getServer().getPluginManager().getPlugin("EzBoost");

                ClassLoader ezBoostClassLoader = ezBoostPlugin.getClass().getClassLoader();

                // Check if EzBoost classes are available using the plugin's class loader
                Class<?> ezBoostAPIClass = Class.forName("com.skyblockexp.ezboost.api.EzBoostAPI", true, ezBoostClassLoader);
                Class<?> customBoostEffectClass = Class.forName("com.skyblockexp.ezboost.boost.CustomBoostEffect", true, ezBoostClassLoader);

                Object boostEffect = SellPriceBoostEffect.create();
                if (boostEffect != null) {
                    java.lang.reflect.Method registerMethod = ezBoostAPIClass.getMethod("registerCustomBoostEffect", customBoostEffectClass);
                    registerMethod.invoke(null, boostEffect);
                    getLogger().info("EzBoost sell price boost integration enabled.");
                }
            } catch (ClassNotFoundException e) {
                getLogger().info("EzBoost classes not found, integration disabled: " + e.getMessage());
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "Failed to register EzBoost sell price boost effect", ex);
            }
        } else if (!ezboostIntegration) {
            getLogger().info("EzBoost integration is disabled in config.");
        }

        getLogger().info("EzShops plugin enabled.");
    }

    @Override
    public void onDisable() {
        // Shutdown the API first
        try {
            EzShopsAPI.shutdown();
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Error shutting down EzShops API", ex);
        }
        
        if (components != null) {
            ListIterator<PluginComponent> iterator = components.listIterator(components.size());
            while (iterator.hasPrevious()) {
                PluginComponent component = iterator.previous();
                try {
                    component.disable();
                } catch (RuntimeException ex) {
                    getLogger().log(Level.SEVERE,
                            "Failed to disable EzShops component " + component.getClass().getSimpleName(), ex);
                }
            }
            components = null;
        }
        economy = null;
        getLogger().info("EzShops plugin disabled.");
    }

    public CoreShopComponent getCoreShopComponent() {
        return this.coreComponent;
    }

    public StockComponent getStockComponent() {
        return this.stockComponent;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    private boolean setupEconomy() {
        // First check if an Economy provider is already registered via ServicesManager.
        RegisteredServiceProvider<Economy> registration =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            economy = registration.getProvider();
            return economy != null;
        }

        // Fallback: ensure the Vault plugin is present (even if no provider registered yet).
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        return false;
    }

    private void saveDefaultResources() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().warning("Unable to create plugin data folder for default EzShops configuration.");
            return;
        }

        for (String resourcePath : DEFAULT_SHOP_RESOURCES) {
            saveResourceIfAbsent(resourcePath);
        }
    }

    private void saveResourceIfAbsent(String resourcePath) {
        File destination = new File(getDataFolder(), resourcePath.replace('/', File.separatorChar));
        if (destination.exists()) {
            return;
        }

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            getLogger().warning("Unable to create directory for default resource: " + parent.getAbsolutePath());
            return;
        }

        try {
            saveResource(resourcePath, false);
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Missing packaged resource '" + resourcePath + "': " + ex.getMessage());
        }
    }
}
