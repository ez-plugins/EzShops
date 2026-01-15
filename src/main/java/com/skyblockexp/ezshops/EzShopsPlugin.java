package com.skyblockexp.ezshops;

import com.skyblockexp.ezshops.api.EzShopsAPI;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.bootstrap.MetricsComponent;
import com.skyblockexp.ezshops.bootstrap.PluginComponent;
import com.skyblockexp.ezshops.bootstrap.SignShopComponent;
import com.skyblockexp.ezshops.bootstrap.StockComponent;
import com.skyblockexp.ezshops.bootstrap.PlayerShopComponent;
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
public final class EzShopsPlugin extends JavaPlugin {
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
        if (debugMode) {
            getLogger().info("Debug mode is enabled. Verbose logging will be shown.");
        }

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
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> registration =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return false;
        }
        economy = registration.getProvider();
        return economy != null;
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
