# ShopTemplateCategoryBuilder

Package: com.skyblockexp.ezshops.shop.api

Overview
--------

Builder for `ShopTemplateCategory` instances. Use to set category properties and add `ShopItem`s before producing a category object.

Key methods
-----------
- `ShopTemplateCategoryBuilder(String id)`
- `property(String key, Object value)`
- `addItem(String itemId, ShopItem item)`
- `ShopTemplateCategory build()`

Usage
-----

```java
ShopTemplateCategoryBuilder b = new ShopTemplateCategoryBuilder("building")
    .property("name","Building Blocks")
    .addItem("dirt", dirtItem);
ShopTemplateCategory cat = b.build();
```

Source
------
[src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateCategoryBuilder.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateCategoryBuilder.java)
