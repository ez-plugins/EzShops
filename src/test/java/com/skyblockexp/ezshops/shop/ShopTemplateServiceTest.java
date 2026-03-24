package com.skyblockexp.ezshops.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.skyblockexp.ezshops.shop.api.ShopTemplateService;
import com.skyblockexp.ezshops.shop.template.ShopTemplate;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ShopTemplateServiceTest {

    @Test
    public void registerAndImportTemplate() {
        ShopTemplateService svc = new ShopTemplateServiceImpl();
        ShopTemplate t = new ShopTemplate("example", "Example Template", List.of(Map.of("item", "STONE", "amount", 64)));
        svc.registerTemplate(t);

        var list = svc.listTemplates();
        assertEquals(1, list.size());

        var imported = svc.importTemplate("example");
        assertTrue(imported.isPresent());
        assertEquals("Example Template", imported.get().name());
    }

    @Test
    public void yamlRoundtrip() throws Exception {
        String yaml = "id: sample\nname: Sample Template\nitems:\n  - item: DIAMOND\n    amount: 5\n";
        try (var in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            ShopTemplate t = ShopTemplate.fromYaml(in);
            assertEquals("sample", t.id());
            String out = t.toYaml();
            assertTrue(out.contains("sample"));
            assertTrue(out.contains("DIAMOND") || out.contains("diamond"));
        }
    }
}
