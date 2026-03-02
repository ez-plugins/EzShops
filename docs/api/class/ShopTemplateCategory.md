# ShopTemplateCategory

Package: com.skyblockexp.ezshops.shop.api

Overview
--------

Model representing a category inside a `ShopTemplate`. Mirrors the YAML structure used for `shop/categories/*.yml` with properties and item entries.

Key methods
-----------
- `String id()`
- `Map<String,Object> properties()`
- `Map<String,ShopItem> items()`
- `Map<String,Object> toMap()` — YAML-friendly representation

Usage
-----

```java
ShopTemplateCategory c = new ShopTemplateCategoryBuilder("building").property("name","Building").build();
```

Source
------
[src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateCategory.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateCategory.java)
