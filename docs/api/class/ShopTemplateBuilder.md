# ShopTemplateBuilder

Package: com.skyblockexp.ezshops.shop.api

Overview
--------

Helper for programmatically building `ShopTemplate` instances. Provides methods to add categories and items before producing a serializable template.

Key methods
-----------
- `ShopTemplateBuilder(String id, String name)` — constructor
- `addCategory(ShopTemplateCategory category)`
- `addCategory(String id, Map<String,Object> properties)`
- `addItemToCategory(String categoryId, String itemId, ShopItem item)`
- `ShopTemplate build()`

Usage
-----

```java
ShopTemplate template = new ShopTemplateBuilder("id","Name")
    .addCategory(category)
    .build();
```

Source
------
[src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateBuilder.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateBuilder.java)
