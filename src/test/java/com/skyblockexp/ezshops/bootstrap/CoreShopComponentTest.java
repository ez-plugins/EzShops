package com.skyblockexp.ezshops.bootstrap;

import net.milkbowl.vault.economy.Economy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class CoreShopComponentTest {

    @Test
    void getCategoryCount_returnsZero_whenNotInitialized() {
        Economy econ = mock(Economy.class);
        CoreShopComponent comp = new CoreShopComponent(econ);

        // Before enable() the pricingManager is null so category count should be 0
        assertEquals(0, comp.getCategoryCount());
    }
}
