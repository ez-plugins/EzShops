package com.skyblockexp.ezshops.util;

import com.skyblockexp.ezshops.shop.template.ShopTemplate;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Helper to produce ShopTemplate YAML from runtime ItemStacks.
 * Plugins can use this to export templates including full serialized ItemStacks.
 */
public final class TemplateWriter {

    private TemplateWriter() {}

    public static ShopTemplate createTemplateFromStacks(String id, String name, List<ItemStack> stacks) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemStack stack : stacks) {
            Map<String, Object> m = new HashMap<>();
            try {
                String b64 = ItemStackSerializers.toBase64(stack);
                m.put("itemstack-base64", b64);
            } catch (Exception ex) {
                // fallback to material+amount
                if (stack != null && stack.getType() != null) {
                    m.put("material", stack.getType().name());
                    m.put("amount", stack.getAmount());
                }
            }
            items.add(m);
        }
        return new ShopTemplate(id, name, items);
    }

    public static ShopTemplate createTemplateFromFiles(String id, String name, Map<String, String> files) {
        return new ShopTemplate(id, name, java.util.List.of(), files);
    }

    public static String templateToYaml(ShopTemplate template) {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(opts);
        StringWriter sw = new StringWriter();
        yaml.dump(Map.of("id", template.id(), "name", template.name(), "items", template.items()), sw);
        return sw.toString();
    }
}
