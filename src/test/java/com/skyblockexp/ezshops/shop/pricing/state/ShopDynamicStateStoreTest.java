package com.skyblockexp.ezshops.shop.pricing.state;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopDynamicStateStoreTest {

    @Test
    void persists_multipliers_rotation_and_removal() throws Exception {
        Path tempDir = Files.createTempDirectory("shop-dynamic-state-store-test");
        Path file = tempDir.resolve("shop-dynamic.yml");
        ShopDynamicStateStore store = new ShopDynamicStateStore(file.toFile(), Logger.getLogger("test"));
        store.load();

        store.saveMultiplier("DIAMOND", 1.75D);
        store.saveRotationOption("daily", "a");

        ShopDynamicStateStore reloaded = new ShopDynamicStateStore(file.toFile(), Logger.getLogger("test"));
        reloaded.load();

        assertTrue(reloaded.isSet("DIAMOND"));
        assertEquals(1.75D, reloaded.getDouble("DIAMOND", 0.0D), 1e-9);
        assertEquals(Set.of("DIAMOND", "rotations"), reloaded.rootKeys());
        assertTrue(reloaded.isSet("rotations.daily"));

        assertTrue(reloaded.removeSavedEntry("DIAMOND"));
        assertFalse(reloaded.isSet("DIAMOND"));
        assertFalse(reloaded.removeSavedEntry("DIAMOND"));
    }

    @Test
    void cleanup_prunes_invalid_entries_and_empty_rotation_section() throws Exception {
        Path tempDir = Files.createTempDirectory("shop-dynamic-state-store-cleanup");
        Path file = tempDir.resolve("shop-dynamic.yml");
        ShopDynamicStateStore store = new ShopDynamicStateStore(file.toFile(), Logger.getLogger("test"));
        store.load();

        store.set("DIAMOND", 1.5D);
        store.set("INVALID", 9.9D);
        store.saveRotationOption("daily", "a");
        store.saveRotationOption("weekly", "stale");

        store.cleanup(
                key -> "DIAMOND".equals(key),
                (rotationId, optionId) -> "daily".equals(rotationId) && "a".equals(optionId));

        ShopDynamicStateStore reloaded = new ShopDynamicStateStore(file.toFile(), Logger.getLogger("test"));
        reloaded.load();

        assertTrue(reloaded.isSet("DIAMOND"));
        assertFalse(reloaded.isSet("INVALID"));
        assertTrue(reloaded.isSet("rotations.daily"));
        assertFalse(reloaded.isSet("rotations.weekly"));

        reloaded.cleanup(key -> true, (rotationId, optionId) -> false);
        ShopDynamicStateStore afterEmptyRotation = new ShopDynamicStateStore(file.toFile(), Logger.getLogger("test"));
        afterEmptyRotation.load();
        assertFalse(afterEmptyRotation.isSet("rotations"));
    }

    @Test
    void clear_all_dynamic_entries_keeps_rotation_state() throws Exception {
        Path tempDir = Files.createTempDirectory("shop-dynamic-state-store-clear");
        Path file = tempDir.resolve("shop-dynamic.yml");
        ShopDynamicStateStore store = new ShopDynamicStateStore(file.toFile(), Logger.getLogger("test"));
        store.load();

        store.set("DIAMOND", 1.2D);
        store.set("EMERALD", 0.8D);
        store.saveRotationOption("daily", "a");

        int removed = store.clearAllDynamicEntries();
        store.flush();

        ShopDynamicStateStore reloaded = new ShopDynamicStateStore(file.toFile(), Logger.getLogger("test"));
        reloaded.load();

        assertEquals(2, removed);
        assertFalse(reloaded.isSet("DIAMOND"));
        assertFalse(reloaded.isSet("EMERALD"));
        assertTrue(reloaded.isSet("rotations.daily"));
    }
}
