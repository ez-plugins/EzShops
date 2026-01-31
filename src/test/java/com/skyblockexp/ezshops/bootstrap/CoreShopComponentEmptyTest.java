package com.skyblockexp.ezshops.bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class CoreShopComponentEmptyTest {

    @Test
    void getCategoryCount_initially_zero() {
        CoreShopComponent comp = new CoreShopComponent(null);
        assertEquals(0, comp.getCategoryCount());
    }
}
