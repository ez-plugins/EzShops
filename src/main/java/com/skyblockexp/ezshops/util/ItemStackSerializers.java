package com.skyblockexp.ezshops.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class ItemStackSerializers {

    private ItemStackSerializers() {}

    public static String toBase64(ItemStack item) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeObject(item);
            boos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    public static ItemStack fromBase64(String base64) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(base64);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
            Object obj = bois.readObject();
            if (obj instanceof ItemStack is) {
                return is;
            }
            throw new ClassNotFoundException("Deserialized object is not an ItemStack");
        }
    }
}
