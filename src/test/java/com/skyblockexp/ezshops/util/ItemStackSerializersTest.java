package com.skyblockexp.ezshops.util;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import com.skyblockexp.ezshops.AbstractEzShopsTest;

public class ItemStackSerializersTest extends AbstractEzShopsTest {

    @Test
    public void roundtripSerializesAndDeserializes() throws Exception {
        ItemStack original = new ItemStack(Material.DIAMOND, 3);
        String b64 = ItemStackSerializers.toBase64(original);
        assertNotNull(b64);

        ItemStack parsed = ItemStackSerializers.fromBase64(b64);
        assertNotNull(parsed);
        assertEquals(original.getType(), parsed.getType());
        assertEquals(original.getAmount(), parsed.getAmount());
    }
}
