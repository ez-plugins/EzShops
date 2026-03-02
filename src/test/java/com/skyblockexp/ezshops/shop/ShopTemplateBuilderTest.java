package com.skyblockexp.ezshops.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.skyblockexp.ezshops.shop.api.ShopItem;
import com.skyblockexp.ezshops.shop.api.ShopTemplateBuilder;
import com.skyblockexp.ezshops.shop.api.ShopTemplateCategoryBuilder;
import com.skyblockexp.ezshops.shop.template.ShopTemplate;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ShopTemplateBuilderTest {

    @Test
    public void buildTemplateWithCategoryAndItem() {
        ShopTemplateBuilder builder = new ShopTemplateBuilder("test-template", "Test Template");
        ShopItem item = ShopItem.builder("dirt").material("DIRT").amount(32).buy(64).sell(32).build();
        ShopTemplateCategoryBuilder catBuilder = new ShopTemplateCategoryBuilder("building");
        catBuilder.property("name", "Building");
        catBuilder.addItem("dirt", item);
        builder.addCategory(catBuilder.build());

        ShopTemplate t = builder.build();
        assertNotNull(t);
        assertTrue(t.categories().containsKey("building"));
        assertEquals("Test Template", t.name());
    }
}
