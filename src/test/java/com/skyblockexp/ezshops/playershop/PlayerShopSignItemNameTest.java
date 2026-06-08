package com.skyblockexp.ezshops.playershop;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for the enhanced item name resolution in player shop signs,
 * covering potion types and enchanted books.
 */
public class PlayerShopSignItemNameTest extends AbstractEzShopsTest {

    private PlayerShopManager createManager() throws Exception {
        Economy econ = mock(Economy.class);
        loadProviderPlugin(econ);
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        var config = com.skyblockexp.ezshops.config.PlayerShopConfiguration.defaults();
        var repo = new com.skyblockexp.ezshops.repository.yml.YmlPlayerShopRepository(
                plugin.getDataFolder(), plugin.getLogger());
        PlayerShopManager manager = new PlayerShopManager(plugin, econ, config, repo);
        manager.enable();
        return manager;
    }

    @Test
    void friendlyItemNameDetailed_null_returns_Item() throws Exception {
        PlayerShopManager manager = createManager();
        assertEquals("Item", manager.friendlyItemNameDetailed(null));
    }

    @Test
    void friendlyItemNameDetailed_plain_material_uses_material_name() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack diamond = new ItemStack(Material.DIAMOND);
        assertEquals("Diamond", manager.friendlyItemNameDetailed(diamond));
    }

    @Test
    void friendlyItemNameDetailed_custom_display_name_takes_priority() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("§bSpecial Diamond");
        item.setItemMeta(meta);
        // Color codes should be stripped
        assertEquals("Special Diamond", manager.friendlyItemNameDetailed(item));
    }

    @Test
    void friendlyItemNameDetailed_strength_potion_shows_type() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        assertNotNull(meta);
        meta.setBasePotionType(PotionType.STRENGTH);
        potion.setItemMeta(meta);

        String name = manager.friendlyItemNameDetailed(potion);
        assertTrue(name.contains("Strength"), "Expected 'Strength' in: " + name);
        assertTrue(name.contains("Potion"), "Expected 'Potion' in: " + name);
    }

    @Test
    void friendlyItemNameDetailed_splash_potion_shows_type_and_splash() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        assertNotNull(meta);
        meta.setBasePotionType(PotionType.POISON);
        potion.setItemMeta(meta);

        String name = manager.friendlyItemNameDetailed(potion);
        assertTrue(name.contains("Poison"), "Expected 'Poison' in: " + name);
        assertTrue(name.toLowerCase().contains("splash"), "Expected 'splash' in: " + name);
    }

    @Test
    void friendlyItemNameDetailed_lingering_potion_shows_type_and_lingering() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack potion = new ItemStack(Material.LINGERING_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        assertNotNull(meta);
        meta.setBasePotionType(requiredPotionType("HEALING", "INSTANT_HEAL"));
        potion.setItemMeta(meta);

        String name = manager.friendlyItemNameDetailed(potion);
        assertTrue(name.contains("Healing"), "Expected 'Healing' in: " + name);
        assertTrue(name.toLowerCase().contains("ling"), "Expected lingering indicator in: " + name);
    }

    @Test
    void friendlyItemNameDetailed_water_potion_falls_back_to_material() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        assertNotNull(meta);
        meta.setBasePotionType(PotionType.WATER);
        potion.setItemMeta(meta);

        String name = manager.friendlyItemNameDetailed(potion);
        // Water/base potions have no meaningful type name, fall back to material
        assertEquals("Potion", name);
    }

    @Test
    void friendlyItemNameDetailed_enchanted_book_shows_enchantment() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        assertNotNull(meta);
        meta.addStoredEnchant(requiredEnchantment("sharpness"), 5, true);
        book.setItemMeta(meta);

        String name = manager.friendlyItemNameDetailed(book);
        assertTrue(name.contains("Sharpness") || name.toLowerCase().contains("sharpness"),
                "Expected 'Sharpness' in: " + name);
        assertTrue(name.contains("V") || name.contains("5"), "Expected level in: " + name);
        assertTrue(name.contains("Book"), "Expected 'Book' in: " + name);
    }

    @Test
    void friendlyItemNameDetailed_enchanted_book_unbreaking_shows_roman_numeral() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        assertNotNull(meta);
        meta.addStoredEnchant(requiredEnchantment("unbreaking"), 3, true);
        book.setItemMeta(meta);

        String name = manager.friendlyItemNameDetailed(book);
        assertTrue(name.contains("III"), "Expected Roman numeral III in: " + name);
        assertTrue(name.contains("Book"), "Expected 'Book' in: " + name);
    }

    @Test
    void friendlyItemNameDetailed_enchanted_book_no_enchantments_falls_back() throws Exception {
        PlayerShopManager manager = createManager();
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        // No stored enchantments; should fall back to material name
        String name = manager.friendlyItemNameDetailed(book);
        assertEquals("Enchanted Book", name);
    }

    @Test
    void formatSignLines_potion_shows_type_on_item_line() throws Exception {
        PlayerShopManager manager = createManager();

        if (org.bukkit.Bukkit.getWorlds().isEmpty()) {
            new org.bukkit.WorldCreator("test").createWorld();
        }
        org.bukkit.World world = org.bukkit.Bukkit.getWorlds().get(0);
        org.bukkit.Location signLoc = new org.bukkit.Location(world, 99, 65, 99);
        org.bukkit.Location chestLoc = new org.bukkit.Location(world, 99, 64, 99);

        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        assertNotNull(meta);
        meta.setBasePotionType(PotionType.NIGHT_VISION);
        potion.setItemMeta(meta);

        PlayerShop shop = new PlayerShop(java.util.UUID.randomUUID(), signLoc, chestLoc,
                java.util.List.of(chestLoc), potion, 1, 10.0);

        String[] lines = manager.formatSignLines(shop, true);
        // Default format: line 0 = {item1}, line 1 = {item2}, line 2 = {amount}x, line 3 = {price}
        String signContent = lines[0] + " " + lines[1];
        assertTrue(signContent.contains("Night Vision") || signContent.toLowerCase().contains("night vision"),
                "Sign lines 0+1 should contain 'Night Vision': " + java.util.Arrays.toString(lines));
    }

    @Test
    void splitItem_fits_in_one_line_returns_full_name_and_empty() {
        String[] parts = SignFormat.splitItem("Diamond", 14);
        assertEquals("Diamond", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void splitItem_splits_at_word_boundary() {
        // "Sharpness V Book" (16) -> split at last space before/at 14 = position 11
        String[] parts = SignFormat.splitItem("Sharpness V Book", 14);
        assertEquals("Sharpness V", parts[0]);
        assertEquals("Book", parts[1]);
    }

    @Test
    void splitItem_splits_long_potion_name() {
        // "Night Vision Potion" (19) -> last space at ≤14 is position 12
        String[] parts = SignFormat.splitItem("Night Vision Potion", 14);
        assertEquals("Night Vision", parts[0]);
        assertEquals("Potion", parts[1]);
    }

    @Test
    void splitItem_null_returns_two_empty_strings() {
        String[] parts = SignFormat.splitItem(null, 14);
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void formatSignLines_default_uses_item1_on_line0() throws Exception {
        PlayerShopManager manager = createManager();

        if (org.bukkit.Bukkit.getWorlds().isEmpty()) {
            new org.bukkit.WorldCreator("test").createWorld();
        }
        org.bukkit.World world = org.bukkit.Bukkit.getWorlds().get(0);
        org.bukkit.Location signLoc = new org.bukkit.Location(world, 98, 65, 98);
        org.bukkit.Location chestLoc = new org.bukkit.Location(world, 98, 64, 98);

        // "Unbreaking III Book" splits to "Unbreaking III" + "Book"
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        assertNotNull(meta);
        meta.addStoredEnchant(requiredEnchantment("unbreaking"), 3, true);
        book.setItemMeta(meta);

        PlayerShop shop = new PlayerShop(java.util.UUID.randomUUID(), signLoc, chestLoc,
                java.util.List.of(chestLoc), book, 1, 20.0);

        String[] lines = manager.formatSignLines(shop, true);
        // Line 0 should have first portion; line 1 should have remainder
        assertTrue(lines[0].contains("Unbreaking") || lines[0].contains("III"),
                "Line 0 should contain start of enchantment name: " + java.util.Arrays.toString(lines));
        assertTrue(lines[1].contains("Book") || lines[1].contains("III"),
                "Line 1 should contain remainder: " + java.util.Arrays.toString(lines));
        // Line 2 should be the quantity, line 3 the price
        assertTrue(lines[2].contains("1"), "Line 2 should contain quantity: " + lines[2]);
    }

    private static PotionType requiredPotionType(String... candidates) {
        for (String candidate : candidates) {
            try {
                return PotionType.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {
                // try next candidate
            }
        }
        fail("Expected one of potion types to exist: " + java.util.Arrays.toString(candidates));
        return PotionType.WATER;
    }

    private static Enchantment requiredEnchantment(String key) {
        Enchantment enchantment = Enchantment.getByKey(new NamespacedKey("minecraft", key));
        assertNotNull(enchantment, "Missing enchantment key: " + key);
        return enchantment;
    }
}
