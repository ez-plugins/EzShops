package com.skyblockexp.ezshops.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import com.skyblockexp.ezshops.AbstractEzShopsTest;

public class TemplateWriterTest extends AbstractEzShopsTest {

    @Test
    public void createTemplateFromStacksIncludesSerializedItem() throws Exception {
        ItemStack s1 = new ItemStack(Material.DIAMOND, 1);
        ItemStack s2 = new ItemStack(Material.APPLE, 5);
        var template = TemplateWriter.createTemplateFromStacks("tpl-id", "My Template", List.of(s1, s2));
        assertEquals("tpl-id", template.id());
        assertEquals("My Template", template.name());
        assertEquals(2, template.items().size());
        // At least one entry should contain either itemstack-base64 or material key
        boolean hasBase64 = template.items().stream().anyMatch(m -> m.containsKey("itemstack-base64"));
        boolean hasMaterial = template.items().stream().anyMatch(m -> m.containsKey("material"));
        assertTrue(hasBase64 || hasMaterial);

        String yaml = TemplateWriter.templateToYaml(template);
        assertNotNull(yaml);
        assertTrue(yaml.contains("tpl-id") || yaml.contains("My Template"));
    }
}
