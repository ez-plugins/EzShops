package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.api.EzShopsAPI;
import com.skyblockexp.ezshops.boost.SellPriceBoostEffect;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Central lifecycle orchestrator for EzShops startup and shutdown.
 */
public final class EzShopsBootstrap {

    private static final List<String> CORE_DEFAULT_RESOURCES = List.of(
            "shop.yml",
            "stock-gui.yml",
            // Add any bundled locale files here so they are copied alongside the defaults.
            "messages/messages_en.yml",
            "messages/messages_es.yml",
            "messages/messages_nl.yml",
            "messages/messages_zh.yml");

    // Fallback in case runtime shop resource discovery fails in unusual classloader setups.
    private static final List<String> FALLBACK_SHOP_RESOURCES = List.of(
            "shop/prison/menu.yml",
            "shop/prison/categories/building.yml",
            "shop/prison/categories/daily_specials.yml",
            "shop/prison/categories/decorations.yml",
            "shop/prison/categories/enchantments.yml",
            "shop/prison/categories/farming.yml",
            "shop/prison/categories/fishing.yml",
            "shop/prison/categories/food.yml",
            "shop/prison/categories/mining.yml",
            "shop/prison/categories/mob_drops.yml",
            "shop/prison/categories/redstone.yml",
            "shop/prison/categories/spawners.yml",
            "shop/prison/categories/valuables.yml",
            "shop/prison/categories/wood.yml",
            "shop/prison/rotations/daily-specials.yml",
            "shop/smp/menu.yml",
            "shop/smp/categories/building.yml",
            "shop/smp/categories/daily_specials.yml",
            "shop/smp/categories/decorations.yml",
            "shop/smp/categories/enchantments.yml",
            "shop/smp/categories/farming.yml",
            "shop/smp/categories/fishing.yml",
            "shop/smp/categories/food.yml",
            "shop/smp/categories/mining.yml",
            "shop/smp/categories/mob_drops.yml",
            "shop/smp/categories/redstone.yml",
            "shop/smp/categories/valuables.yml",
            "shop/smp/categories/wood.yml",
            "shop/smp/rotations/daily-specials.yml");

    private final EzShopsPlugin plugin;
    private final EzShopsRegistry registry;

    private Economy economy;
    private List<PluginComponent> components;
    private CoreShopComponent coreComponent;
    private StockComponent stockComponent;
    private TeamShopComponent teamShopComponent;
    private PlayerShopComponent playerShopComponent;

    public EzShopsBootstrap(EzShopsPlugin plugin) {
        this.plugin = plugin;
        this.registry = new EzShopsRegistry();
        this.registry.setReseedCategoryDefaults(this::reseedCategoryDefaults);
        this.registry.setBundledShopModes(this::getBundledShopModes);
    }

    public void start() {
        if (!setupEconomy()) {
            plugin.getLogger().severe("Vault economy provider not found; disabling EzShops.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        saveDefaultResources();
        plugin.saveDefaultConfig();

        registry.setDebugMode(plugin.getConfig().getBoolean("debug", false));

        coreComponent = new CoreShopComponent(economy);
        teamShopComponent = new TeamShopComponent(economy);
        playerShopComponent = new PlayerShopComponent(economy, plugin.getConfig());
        stockComponent = new StockComponent();
        registry.setCoreShopComponent(coreComponent);
        registry.setTeamShopComponent(teamShopComponent);
        registry.setPlayerShopComponent(playerShopComponent);
        registry.setStockComponent(stockComponent);
        EzShopsRegistry.install(registry);

        components = new ArrayList<>();
        components.add(teamShopComponent); // MUST be first so services exist when CoreShopComponent.enable() runs
        components.add(coreComponent);
        components.add(stockComponent);
        components.add(playerShopComponent);
        components.add(new SignShopComponent(coreComponent));
        components.add(new MetricsComponent());
        components.add(new ShopAdminComponent(playerShopComponent, teamShopComponent));

        try {
            for (PluginComponent component : components) {
                component.enable(plugin);
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to enable EzShops component", ex);
            throw ex;
        }

        try {
            EzShopsAPI.initialize(plugin);
            plugin.getLogger().info("EzShops API initialized successfully.");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize EzShops API", ex);
        }

        registerEzBoostIntegration();
        plugin.getLogger().info("EzShops plugin enabled.");
    }

    public void stop() {
        try {
            EzShopsAPI.shutdown();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Error shutting down EzShops API", ex);
        }

        if (components != null) {
            ListIterator<PluginComponent> iterator = components.listIterator(components.size());
            while (iterator.hasPrevious()) {
                PluginComponent component = iterator.previous();
                try {
                    component.disable();
                } catch (RuntimeException ex) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Failed to disable EzShops component " + component.getClass().getSimpleName(), ex);
                }
            }
            components = null;
        }
        economy = null;
        registry.setCoreShopComponent(null);
        registry.setTeamShopComponent(null);
        registry.setPlayerShopComponent(null);
        registry.setStockComponent(null);
        registry.setDebugMode(false);
        EzShopsRegistry.install(null);
        plugin.getLogger().info("EzShops plugin disabled.");
    }

    /**
     * Reseeds bundled category defaults for all modes or a specific mode.
     * Existing files are preserved; only missing category files are created.
     */
    public int reseedCategoryDefaults(String modeFilter) {
        List<String> resourcesToSeed = buildDefaultResourceList();
        int created = 0;
        for (String resourcePath : resourcesToSeed) {
            if (!isDefaultCategoryResource(resourcePath)) {
                continue;
            }

            String mode = extractModeFromShopResource(resourcePath);
            if (mode == null) {
                continue;
            }
            if (modeFilter != null && !modeFilter.isBlank()
                    && !mode.equalsIgnoreCase(modeFilter.strip())) {
                continue;
            }

            if (saveResourceIfAbsent(resourcePath)) {
                created++;
            }
        }
        return created;
    }

    public Set<String> getBundledShopModes() {
        Set<String> modes = new LinkedHashSet<>();
        for (String resourcePath : buildDefaultResourceList()) {
            if (resourcePath == null || !resourcePath.startsWith("shop/")) {
                continue;
            }
            String mode = extractModeFromShopResource(resourcePath);
            if (mode != null) {
                modes.add(mode);
            }
        }
        return modes;
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            plugin.getLogger().severe("No Vault economy provider is registered. Install an economy "
                    + "plugin that provides a Vault economy service (e.g. EssentialsX, CMI).");
            return false;
        }
        economy = registration.getProvider();
        if (economy == null) {
            plugin.getLogger().severe("Vault registered a null economy provider.");
            return false;
        }
        return true;
    }

    private void saveDefaultResources() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for default EzShops configuration.");
            return;
        }

        List<String> resourcesToSeed = buildDefaultResourceList();
        Set<String> preExistingCategoryModes = findPreExistingCategoryModes(resourcesToSeed);
        Set<String> skippedCategoryModes = new LinkedHashSet<>();
        for (String resourcePath : resourcesToSeed) {
            if (shouldSkipCategoryResource(resourcePath, preExistingCategoryModes, skippedCategoryModes)) {
                continue;
            }
            saveResourceIfAbsent(resourcePath);
        }

        if (!skippedCategoryModes.isEmpty()) {
            plugin.getLogger().info("Skipping bundled default category files for existing mode directories: "
                    + String.join(", ", skippedCategoryModes));
        }
    }

    private Set<String> findPreExistingCategoryModes(List<String> resourcesToSeed) {
        Set<String> preExistingModes = new LinkedHashSet<>();
        for (String resourcePath : resourcesToSeed) {
            if (!isDefaultCategoryResource(resourcePath)) {
                continue;
            }
            String mode = extractModeFromShopResource(resourcePath);
            if (mode == null) {
                continue;
            }
            File categoriesDir = new File(plugin.getDataFolder(),
                    ("shop/" + mode + "/categories").replace('/', File.separatorChar));
            if (categoriesDir.exists()) {
                preExistingModes.add(mode);
            }
        }
        return preExistingModes;
    }

    private void registerEzBoostIntegration() {
        boolean ezboostIntegration = plugin.getConfig().getBoolean("ezboost-integration", true);
        if (ezboostIntegration && plugin.getServer().getPluginManager().getPlugin("EzBoost") != null) {
            try {
                org.bukkit.plugin.Plugin ezBoostPlugin = plugin.getServer().getPluginManager().getPlugin("EzBoost");
                ClassLoader ezBoostClassLoader = ezBoostPlugin.getClass().getClassLoader();

                Class<?> ezBoostAPIClass = Class.forName("com.skyblockexp.ezboost.api.EzBoostAPI", true,
                        ezBoostClassLoader);
                Class<?> customBoostEffectClass = Class.forName("com.skyblockexp.ezboost.boost.CustomBoostEffect",
                        true, ezBoostClassLoader);

                Object boostEffect = SellPriceBoostEffect.create();
                if (boostEffect != null) {
                    java.lang.reflect.Method registerMethod = ezBoostAPIClass.getMethod(
                            "registerCustomBoostEffect", customBoostEffectClass);
                    registerMethod.invoke(null, boostEffect);
                    plugin.getLogger().info("EzBoost sell price boost integration enabled.");
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().info("EzBoost classes not found, integration disabled: " + e.getMessage());
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to register EzBoost sell price boost effect", ex);
            }
        } else if (!ezboostIntegration) {
            plugin.getLogger().info("EzBoost integration is disabled in config.");
        }
    }

    private List<String> buildDefaultResourceList() {
        Set<String> resources = new LinkedHashSet<>(CORE_DEFAULT_RESOURCES);
        resources.addAll(discoverBundledShopResources());
        return new ArrayList<>(resources);
    }

    private boolean shouldSkipCategoryResource(
            String resourcePath,
            Set<String> preExistingCategoryModes,
            Set<String> skippedCategoryModes) {
        if (!isDefaultCategoryResource(resourcePath)) {
            return false;
        }

        String mode = extractModeFromShopResource(resourcePath);
        if (mode == null) {
            return false;
        }

        boolean skip = preExistingCategoryModes.contains(mode);
        if (skip) {
            skippedCategoryModes.add(mode);
        }
        return skip;
    }

    private boolean isDefaultCategoryResource(String resourcePath) {
        if (resourcePath == null || !resourcePath.startsWith("shop/")) {
            return false;
        }
        return resourcePath.contains("/categories/")
                && resourcePath.toLowerCase(Locale.ROOT).endsWith(".yml");
    }

    private String extractModeFromShopResource(String resourcePath) {
        if (resourcePath == null || !resourcePath.startsWith("shop/")) {
            return null;
        }
        int modeStart = "shop/".length();
        int nextSlash = resourcePath.indexOf('/', modeStart);
        if (nextSlash <= modeStart) {
            return null;
        }
        return resourcePath.substring(modeStart, nextSlash);
    }

    private List<String> discoverBundledShopResources() {
        Set<String> discovered = new LinkedHashSet<>();
        try {
            Path codeSourcePath = Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isDirectory(codeSourcePath)) {
                Path shopRoot = codeSourcePath.resolve("shop");
                if (Files.exists(shopRoot)) {
                    try (java.util.stream.Stream<Path> pathStream = Files.walk(shopRoot)) {
                        pathStream.filter(Files::isRegularFile)
                                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                                .forEach(path -> {
                                    Path relativePath = codeSourcePath.relativize(path);
                                    discovered.add(relativePath.toString().replace(File.separatorChar, '/'));
                                });
                    }
                }
            } else if (codeSourcePath.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                try (JarFile jarFile = new JarFile(codeSourcePath.toFile())) {
                    java.util.Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        if (entry.isDirectory()) {
                            continue;
                        }
                        if (entryName.startsWith("shop/") && entryName.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                            discovered.add(entryName);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.FINE, "Unable to enumerate bundled shop resources dynamically.", ex);
        }

        if (discovered.isEmpty()) {
            discovered.addAll(FALLBACK_SHOP_RESOURCES);
        }

        List<String> sorted = new ArrayList<>(discovered);
        Collections.sort(sorted, String::compareToIgnoreCase);
        return sorted;
    }

    private boolean saveResourceIfAbsent(String resourcePath) {
        File destination = new File(plugin.getDataFolder(), resourcePath.replace('/', File.separatorChar));
        if (destination.exists()) {
            return false;
        }

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Unable to create directory for default resource: " + parent.getAbsolutePath());
            return false;
        }

        try {
            plugin.saveResource(resourcePath, false);
            return true;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Missing packaged resource '" + resourcePath + "': " + ex.getMessage());
            return false;
        }
    }
}
