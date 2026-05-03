package com.skyblockexp.ezshops;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * Abstract base for MockBukkit-based tests.
 * Sets up a ServerMock and provides helpers for loading plugins and registering an Economy provider.
 *
 * <p>Uses {@link Unsafe#allocateInstance} to bypass the {@link JavaPlugin} no-arg constructor,
 * which on Java 21+ throws {@link IllegalStateException} unless the class is loaded by a
 * {@code ConfiguredPluginClassLoader}. After allocation, {@link JavaPlugin#init} is called
 * directly to set all required fields.</p>
 */
public abstract class AbstractEzShopsTest {

    protected ServerMock server;

    @BeforeEach
    void setupMockBukkit() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void cleanupMockBukkit() {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Loads and enables a plugin, bypassing MockBukkit's broken ByteBuddy proxy-class loading.
     *
     * <p>Uses {@link Unsafe#allocateInstance} so the {@link JavaPlugin} constructor check is
     * skipped, then calls {@link JavaPlugin#init} to properly initialise the instance.</p>
     */
    protected <T extends JavaPlugin> T loadPlugin(Class<T> pluginClass) {
        try {
            PluginDescriptionFile description;
            try (InputStream is = pluginClass.getResourceAsStream("/plugin.yml")) {
                description = new PluginDescriptionFile(is);
            }
            File dataFolder = Files.createTempDirectory("test-plugin-" + description.getName()).toFile();
            @SuppressWarnings("unchecked")
            T plugin = (T) getUnsafe().allocateInstance(pluginClass);
            plugin.init(server, description, dataFolder, new File(""),
                    pluginClass.getClassLoader(), description, Logger.getLogger(description.getName()));
            server.getPluginManager().registerLoadedPlugin(plugin);
            server.getPluginManager().enablePlugin(plugin);
            return plugin;
        } catch (InvalidDescriptionException | IOException | InstantiationException e) {
            throw new RuntimeException("Failed to load plugin " + pluginClass.getSimpleName(), e);
        }
    }

    /**
     * Load a simple test plugin that registers the provided Economy instance on enable.
     */
    protected void loadProviderPlugin(Economy econ) {
        TestProviderPlugin.econToRegister = econ;
        try {
            PluginDescriptionFile description = new PluginDescriptionFile(
                    "TestEconomyProvider", "1.0", TestProviderPlugin.class.getName());
            File dataFolder = Files.createTempDirectory("test-plugin-provider").toFile();
            @SuppressWarnings("unchecked")
            TestProviderPlugin plugin = (TestProviderPlugin) getUnsafe().allocateInstance(TestProviderPlugin.class);
            plugin.init(server, description, dataFolder, new File(""),
                    TestProviderPlugin.class.getClassLoader(), description,
                    Logger.getLogger("TestEconomyProvider"));
            server.getPluginManager().registerLoadedPlugin(plugin);
            server.getPluginManager().enablePlugin(plugin);
        } catch (IOException | InstantiationException e) {
            throw new RuntimeException("Failed to load provider plugin", e);
        }
    }

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot access Unsafe", e);
        }
    }

    public static class TestProviderPlugin extends JavaPlugin {
        static Economy econToRegister;

        @Override
        public void onEnable() {
            if (econToRegister != null) {
                getServer().getServicesManager().register(Economy.class, econToRegister, this, org.bukkit.plugin.ServicePriority.Normal);
            }
        }
    }
}
