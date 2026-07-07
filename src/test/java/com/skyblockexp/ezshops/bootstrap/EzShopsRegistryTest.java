package com.skyblockexp.ezshops.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EzShopsRegistryTest {

    @AfterEach
    void resetRegistry() {
        EzShopsRegistry.install(null);
    }

    @Test
    void current_has_safe_defaults() {
        EzShopsRegistry.install(null);

        EzShopsRegistry registry = EzShopsRegistry.current();
        assertNotNull(registry);
        assertNull(registry.getCoreShopComponent());
        assertNull(registry.getTeamShopComponent());
        assertNull(registry.getStockComponent());
        assertNull(registry.getPlayerShopComponent());
        assertFalse(registry.isDebugMode());
        assertEquals(0, registry.reseedCategoryDefaults(null));
        assertTrue(registry.getBundledShopModes().isEmpty());
    }

    @Test
    void install_and_accessors_expose_runtime_state() {
        EzShopsRegistry registry = new EzShopsRegistry();
        CoreShopComponent core = mock(CoreShopComponent.class);
        TeamShopComponent team = mock(TeamShopComponent.class);
        StockComponent stock = mock(StockComponent.class);
        PlayerShopComponent player = mock(PlayerShopComponent.class);

        registry.setCoreShopComponent(core);
        registry.setTeamShopComponent(team);
        registry.setStockComponent(stock);
        registry.setPlayerShopComponent(player);
        registry.setDebugMode(true);

        registry.setReseedCategoryDefaults(mode -> "smp".equals(mode) ? 2 : 1);
        registry.setBundledShopModes(() -> Set.of("prison", "smp"));

        EzShopsRegistry.install(registry);

        EzShopsRegistry current = EzShopsRegistry.current();
        assertEquals(core, current.getCoreShopComponent());
        assertEquals(team, current.getTeamShopComponent());
        assertEquals(stock, current.getStockComponent());
        assertEquals(player, current.getPlayerShopComponent());
        assertTrue(current.isDebugMode());
        assertEquals(1, current.reseedCategoryDefaults(null));
        assertEquals(2, current.reseedCategoryDefaults("smp"));
        assertEquals(Set.of("prison", "smp"), current.getBundledShopModes());
    }

    @Test
    void reloadFeatures_calls_component_reload_when_present() {
        EzShopsRegistry registry = new EzShopsRegistry();
        StockComponent stock = mock(StockComponent.class);
        PlayerShopComponent player = mock(PlayerShopComponent.class);

        registry.setStockComponent(stock);
        registry.setPlayerShopComponent(player);
        registry.reloadFeatures();

        verify(stock).reload();
        verify(player).reload();
    }

    @Test
    void reloadFeatures_skips_missing_components() {
        EzShopsRegistry registry = new EzShopsRegistry();
        registry.reloadFeatures();
        assertTrue(true, "Missing components should be ignored safely.");
    }

    @Test
    void bundled_modes_are_returned_as_defensive_copy() {
        EzShopsRegistry registry = new EzShopsRegistry();
        registry.setBundledShopModes(() -> new LinkedHashSet<>(Set.of("prison")));

        Set<String> first = registry.getBundledShopModes();
        first.add("mutated");

        Set<String> second = registry.getBundledShopModes();
        assertEquals(Set.of("prison"), second);
    }

    @Test
    void null_callbacks_fall_back_to_safe_defaults() {
        EzShopsRegistry registry = new EzShopsRegistry();

        registry.setReseedCategoryDefaults(null);
        registry.setBundledShopModes(null);

        assertEquals(0, registry.reseedCategoryDefaults("anything"));
        assertTrue(registry.getBundledShopModes().isEmpty());
    }
}
