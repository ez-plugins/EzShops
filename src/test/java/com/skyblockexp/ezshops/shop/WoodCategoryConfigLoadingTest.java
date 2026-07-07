package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests that confirm the wood category items are all correctly
 * loaded from the bundled wood.yml defaults and are sellable via the full
 * shop pipeline.
 *
 * <p>These tests exist to guard against regressions where items like BIRCH_LOG
 * appear to be missing from the price map after a fresh plugin load.</p>
 */
public class WoodCategoryConfigLoadingTest extends AbstractEzShopsTest {

    // -----------------------------------------------------------------------
    // Price-map loading tests
    // -----------------------------------------------------------------------

    /**
     * Confirms that every standard log type present in the bundled wood.yml
     * is registered in the pricing manager's price map and has a positive
     * sell price after a fresh plugin load.
     *
     * <p>If BIRCH_LOG, JUNGLE_LOG or any other log type is missing from the
     * price map this test will fail, reproducing the "not configured" error
     * that players see when they try to /sellhand one of these items.</p>
     */
    @Test
    void all_wood_category_logs_are_priced_and_sellable_after_fresh_load() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        when(econ.depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble()))
                .thenReturn(new EconomyResponse(0, 1000, EconomyResponse.ResponseType.SUCCESS, ""));
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        ShopPricingManager pm = getPricingManager(plugin);

        // All logs that appear in the bundled shop/categories/wood.yml
        Material[] expectedLogs = {
                Material.OAK_LOG,
                Material.SPRUCE_LOG,
                Material.BIRCH_LOG,
                Material.JUNGLE_LOG,
                Material.ACACIA_LOG,
                Material.DARK_OAK_LOG,
                Material.MANGROVE_LOG,
                requiredMaterial("CHERRY_LOG"),
        };
        Material paleOakLog = optionalMaterial("PALE_OAK_LOG");
        if (paleOakLog != null) {
            expectedLogs = append(expectedLogs, paleOakLog);
        }

        for (Material log : expectedLogs) {
            Optional<ShopPrice> price = pm.getPrice(log);
            assertTrue(price.isPresent(),
                    log.name() + " should be present in the price map after loading the bundled wood.yml");
            assertTrue(price.get().canSell(),
                    log.name() + " should have a positive sell price (canSell == true)");
        }

        // All stripped log variants added to the bundled wood.yml defaults
        Material[] expectedStrippedLogs = {
                Material.STRIPPED_OAK_LOG,
                Material.STRIPPED_SPRUCE_LOG,
                Material.STRIPPED_BIRCH_LOG,
                Material.STRIPPED_JUNGLE_LOG,
                Material.STRIPPED_ACACIA_LOG,
                Material.STRIPPED_DARK_OAK_LOG,
                Material.STRIPPED_MANGROVE_LOG,
                requiredMaterial("STRIPPED_CHERRY_LOG"),
        };
        Material strippedPaleOakLog = optionalMaterial("STRIPPED_PALE_OAK_LOG");
        if (strippedPaleOakLog != null) {
            expectedStrippedLogs = append(expectedStrippedLogs, strippedPaleOakLog);
        }

        for (Material stripped : expectedStrippedLogs) {
            Optional<ShopPrice> price = pm.getPrice(stripped);
            assertTrue(price.isPresent(),
                    stripped.name() + " should be present in the price map after loading the bundled wood.yml");
            assertTrue(price.get().canSell(),
                    stripped.name() + " should have a positive sell price (canSell == true)");
        }

        // All wood (all-bark) variants added to the bundled wood.yml defaults.
        // "Birch Wood" (BIRCH_WOOD) is the item players commonly confuse with "Birch Log".
        Material[] expectedWoodBlocks = {
                Material.OAK_WOOD,
                Material.SPRUCE_WOOD,
                Material.BIRCH_WOOD,
                Material.JUNGLE_WOOD,
                Material.ACACIA_WOOD,
                Material.DARK_OAK_WOOD,
                Material.MANGROVE_WOOD,
                requiredMaterial("CHERRY_WOOD"),
        };
        Material paleOakWood = optionalMaterial("PALE_OAK_WOOD");
        if (paleOakWood != null) {
            expectedWoodBlocks = append(expectedWoodBlocks, paleOakWood);
        }

        for (Material wood : expectedWoodBlocks) {
            Optional<ShopPrice> price = pm.getPrice(wood);
            assertTrue(price.isPresent(),
                    wood.name() + " should be present in the price map after loading the bundled wood.yml");
            assertTrue(price.get().canSell(),
                    wood.name() + " should have a positive sell price (canSell == true)");
        }
    }

    /**
     * BIRCH_LOG specifically must appear in {@code getConfiguredMaterials()}.
     * This is a targeted regression test for the bug where BIRCH_LOG was
     * reported as "not configured" despite being listed in wood.yml.
     */
    @Test
    void birch_log_is_in_configured_materials() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        ShopPricingManager pm = getPricingManager(plugin);

        Set<Material> configured = pm.getConfiguredMaterials();
        assertTrue(configured.contains(Material.BIRCH_LOG),
                "BIRCH_LOG must be in getConfiguredMaterials(); got: " + configured);
    }

    /**
     * JUNGLE_LOG specifically must appear in the price map with a sell price.
     */
    @Test
    void jungle_log_is_priced_and_sellable() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        ShopPricingManager pm = getPricingManager(plugin);

        Optional<ShopPrice> price = pm.getPrice(Material.JUNGLE_LOG);
        assertTrue(price.isPresent(), "JUNGLE_LOG should be present in the price map");
        assertTrue(price.get().canSell(), "JUNGLE_LOG should be sellable");
    }

    // -----------------------------------------------------------------------
    // End-to-end sell pipeline tests
    // -----------------------------------------------------------------------

    /**
     * Simulates "/sellhand" for a player holding BIRCH_LOG.  The transaction
     * must succeed, depositing money into the player's economy account.
     *
     * <p>Previously this returned "That item is not configured in the shop."
     * because {@code getPrice(BIRCH_LOG)} returned empty.</p>
     */
    @Test
    void sellhand_birch_log_succeeds() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        when(econ.depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble()))
                .thenReturn(new EconomyResponse(0, 1000, EconomyResponse.ResponseType.SUCCESS, ""));
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        ShopTransactionService txService = getTransactionService(plugin);

        PlayerMock player = server.addPlayer("birch-seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        // Give the player some birch logs to sell
        ItemStack birchStack = new ItemStack(Material.BIRCH_LOG, 16);
        player.getInventory().addItem(birchStack);

        ShopTransactionResult result = txService.sell(player, Material.BIRCH_LOG, 16);
        assertTrue(result.success(),
                "Selling BIRCH_LOG should succeed, but got: " + result.message());
        verify(econ, atLeastOnce()).depositPlayer(eq(player), anyDouble());
    }

    /**
     * Simulates "/sellhand" for a player holding JUNGLE_LOG.  The transaction
     * must succeed.
     */
    @Test
    void sellhand_jungle_log_succeeds() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        when(econ.depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble()))
                .thenReturn(new EconomyResponse(0, 1000, EconomyResponse.ResponseType.SUCCESS, ""));
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        ShopTransactionService txService = getTransactionService(plugin);

        PlayerMock player = server.addPlayer("jungle-seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        player.getInventory().addItem(new ItemStack(Material.JUNGLE_LOG, 16));

        ShopTransactionResult result = txService.sell(player, Material.JUNGLE_LOG, 16);
        assertTrue(result.success(),
                "Selling JUNGLE_LOG should succeed, but got: " + result.message());
    }

    /**
     * Simulates the Quick Sell GUI's {@code sellDirect} path for BIRCH_LOG.
     * Previously this returned "That item cannot be sold in the shop."
     * because {@code isSellable} called {@code getPrice} which returned empty.
     */
    @Test
    void sellDirect_birch_log_succeeds() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        when(econ.depositPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble()))
                .thenReturn(new EconomyResponse(0, 1000, EconomyResponse.ResponseType.SUCCESS, ""));
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        ShopTransactionService txService = getTransactionService(plugin);

        PlayerMock player = server.addPlayer("birch-quick-seller");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);

        ShopTransactionResult result = txService.sellDirect(player, Material.BIRCH_LOG, 8);
        assertTrue(result.success(),
                "sellDirect for BIRCH_LOG should succeed, but got: " + result.message());
        verify(econ, atLeastOnce()).depositPlayer(eq(player), anyDouble());
    }

    // -----------------------------------------------------------------------
    // Expected-failure tests (items genuinely not in the shop)
    // -----------------------------------------------------------------------

    /**
     * BIRCH_WOOD (the all-bark "Birch Wood" block) must appear in the price map.
     * Players sometimes have this item instead of BIRCH_LOG and expect to be able
     * to sell it via /sellhand.
     */
    @Test
    void birch_wood_is_priced_and_sellable() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        ShopPricingManager pm = getPricingManager(plugin);

        Optional<ShopPrice> price = pm.getPrice(Material.BIRCH_WOOD);
        assertTrue(price.isPresent(),
                "BIRCH_WOOD must be present in the price map after loading the bundled wood.yml");
        assertTrue(price.get().canSell(),
                "BIRCH_WOOD must have a positive sell price");
    }

    /**
     * STRIPPED_JUNGLE_LOG must appear in the price map now that it has been
     * added to the bundled wood.yml defaults.
     */
    @Test
    void stripped_jungle_log_is_priced_and_sellable() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        ShopPricingManager pm = getPricingManager(plugin);

        Optional<ShopPrice> price = pm.getPrice(Material.STRIPPED_JUNGLE_LOG);
        assertTrue(price.isPresent(),
                "STRIPPED_JUNGLE_LOG must be present in the price map after loading the bundled wood.yml");
        assertTrue(price.get().canSell(),
                "STRIPPED_JUNGLE_LOG must have a positive sell price");
    }

    /**
     * OAK_PLANKS must appear in the price map now that planks have been added
     * to the bundled building.yml defaults.
     */
    @Test
    void oak_planks_is_priced_and_sellable() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        ShopPricingManager pm = getPricingManager(plugin);

        Optional<ShopPrice> price = pm.getPrice(Material.OAK_PLANKS);
        assertTrue(price.isPresent(),
                "OAK_PLANKS must be present in the price map after loading the bundled building.yml");
        assertTrue(price.get().canSell(),
                "OAK_PLANKS must have a positive sell price");
    }

    // -----------------------------------------------------------------------
    // Legacy-format key-casing regression test
    // -----------------------------------------------------------------------

    /**
     * Regression test for the key-casing bug in legacy (flat) shop configs.
     *
     * <p>When a server uses the legacy single-file format, material keys can be
     * written in any case (e.g. {@code birch_log:} lowercase, {@code OAK_LOG:}
     * uppercase).  Before the fix, {@code loadLegacyEntries} called
     * {@code registerPrice(key, ...)} with the raw YAML key, so the priceMap
     * entry was stored under whatever case appeared in the file.  Because
     * {@code getPrice(Material)} always looks up {@code material.name()} which
     * is SCREAMING_SNAKE_CASE (e.g. {@code "BIRCH_LOG"}), any key that was not
     * written in upper-case was silently unreachable and players would see
     * "That item is not configured in the shop." even though the item was in the
     * config.</p>
     *
     * <p>After the fix, {@code loadLegacyEntries} normalises the key to
     * {@code material.name()} before registering, so case in the config file no
     * longer matters.</p>
     */
    @Test
    void legacy_lowercase_config_key_is_found_by_getPrice() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        loadProviderPlugin(econ);

        // Pre-seed a tempDir with:
        //  - shop.yml: legacy root-level format with a lowercase key ("birch_log")
        //    and an uppercase key ("OAK_LOG") as control
        //  - shop/categories/wood.yml: minimal stub with NO items, so BIRCH_LOG
        //    can only reach getPrice() via the legacy registration path
        Path tempDir = Files.createTempDirectory("legacy-key-casing-");
        Path categoriesDir = tempDir.resolve("shop/categories");
        Files.createDirectories(categoriesDir);

        // OAK_LOG uses the uppercase key (control â€” this always worked).
        // birch_log uses the lowercase key â€” this is what triggered the bug.
        String legacyShopYml =
                "OAK_LOG:\n"
                + "  buy: 24.0\n"
                + "  sell: 10.0\n"
                + "birch_log:\n"
                + "  buy: 26.0\n"
                + "  sell: 11.0\n";
        Files.writeString(tempDir.resolve("shop.yml"), legacyShopYml);

        // Minimal wood.yml stub with NO items block so that BIRCH_LOG is NOT
        // registered via the category code-path (only via legacy entries).
        String emptyWoodYml =
                "categories:\n"
                + "  wood:\n"
                + "    name: \"Wood\"\n"
                + "    slot: 16\n"
                + "    icon:\n"
                + "      material: OAK_LOG\n"
                + "      display-name: \"Wood\"\n"
                + "      lore: []\n"
                + "    menu:\n"
                + "      title: \"Wood Shop\"\n"
                + "      size: 54\n";
        Files.writeString(categoriesDir.resolve("wood.yml"), emptyWoodYml);

        EzShopsPlugin plugin = loadPluginWithDataFolder(EzShopsPlugin.class, tempDir.toFile());
        ShopPricingManager pm = getPricingManager(plugin);

        // OAK_LOG (uppercase key) is the control: it must always be found.
        assertTrue(pm.getPrice(Material.OAK_LOG).isPresent(),
                "OAK_LOG (uppercase legacy key) should be present in the price map");

        // BIRCH_LOG (lowercase key "birch_log" in shop.yml) must be found after
        // the fix.  Before the fix this returned empty because priceMap was keyed
        // by "birch_log" while getPrice(Material) looks up "BIRCH_LOG".
        assertTrue(pm.getPrice(Material.BIRCH_LOG).isPresent(),
                "BIRCH_LOG must be found even when the legacy config uses the lowercase "
                + "key 'birch_log' â€” loadLegacyEntries must normalise to material.name()");
        assertTrue(pm.getPrice(Material.BIRCH_LOG).get().canSell(),
                "BIRCH_LOG loaded from a lowercase legacy key must have a valid sell price");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Extracts the ShopPricingManager from the plugin via reflection. */
    private ShopPricingManager getPricingManager(EzShopsPlugin plugin) throws Exception {
        CoreShopComponent core = com.skyblockexp.ezshops.bootstrap.EzShopsRegistry.current().getCoreShopComponent();
        assertNotNull(core, "CoreShopComponent must not be null");
        Field f = CoreShopComponent.class.getDeclaredField("pricingManager");
        f.setAccessible(true);
        ShopPricingManager pm = (ShopPricingManager) f.get(core);
        assertNotNull(pm, "ShopPricingManager must not be null");
        return pm;
    }

    /** Extracts the ShopTransactionService from the plugin via reflection. */
    private ShopTransactionService getTransactionService(EzShopsPlugin plugin) throws Exception {
        CoreShopComponent core = com.skyblockexp.ezshops.bootstrap.EzShopsRegistry.current().getCoreShopComponent();
        assertNotNull(core, "CoreShopComponent must not be null");
        Field f = CoreShopComponent.class.getDeclaredField("transactionService");
        f.setAccessible(true);
        ShopTransactionService svc = (ShopTransactionService) f.get(core);
        assertNotNull(svc, "ShopTransactionService must not be null");
        return svc;
    }

    private static Material requiredMaterial(String name) {
        Material m = Material.matchMaterial(name);
        assertNotNull(m, "Expected material to exist in this API version: " + name);
        return m;
    }

    private static Material optionalMaterial(String name) {
        return Material.matchMaterial(name);
    }

    private static Material[] append(Material[] base, Material extra) {
        ArrayList<Material> out = new ArrayList<>(base.length + 1);
        for (Material m : base) out.add(m);
        out.add(extra);
        return out.toArray(new Material[0]);
    }

    /**
     * Like {@link #loadPlugin(Class)} but uses a caller-supplied dataFolder
     * so that tests can pre-seed the directory with a custom configuration
     * before the plugin's {@code onEnable()} copies default resources.
     */
    private <T extends org.bukkit.plugin.java.JavaPlugin> T loadPluginWithDataFolder(
            Class<T> pluginClass, File dataFolder) {
        try {
            org.bukkit.plugin.PluginDescriptionFile description;
            try (InputStream is = pluginClass.getResourceAsStream("/plugin.yml")) {
                description = new org.bukkit.plugin.PluginDescriptionFile(is);
            }
            @SuppressWarnings("unchecked")
            T plugin = (T) getUnsafeInstance().allocateInstance(pluginClass);
            plugin.init(server, description, dataFolder, new File(""),
                    pluginClass.getClassLoader(), description,
                    java.util.logging.Logger.getLogger(description.getName()));
            server.getPluginManager().registerLoadedPlugin(plugin);
            server.getPluginManager().enablePlugin(plugin);
            return plugin;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load plugin with custom dataFolder", e);
        }
    }

    private static sun.misc.Unsafe getUnsafeInstance() throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }
}

