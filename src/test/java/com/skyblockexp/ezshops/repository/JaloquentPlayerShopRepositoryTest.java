package com.skyblockexp.ezshops.repository;

import com.skyblockexp.ezshops.database.jaloquent.JaloquentPlayerShopRepository;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class JaloquentPlayerShopRepositoryTest {

    @Test
    void initThrowsWhenNotImplemented() {
        JaloquentPlayerShopRepository repo = new JaloquentPlayerShopRepository(Logger.getLogger("test"));
        assertThrows(IllegalStateException.class, repo::init);
    }
}
