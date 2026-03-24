# ShopIcon

Package: com.skyblockexp.ezshops.shop.api

Overview
--------

`ShopIcon` models the visual/icon metadata for a `ShopItem`. It supports a minimal representation (material, amount, display name, lore) and an optional Base64 `ItemStack` for full metadata.

Construction
------------
- `ShopIcon.builder()` — fluent builder with setters for `material`, `amount`, `displayName`, `lore`, and `serializedItem(String base64)`.

Key methods
-----------
- `String material()`
- `int amount()`
- `String displayName()`
- `List<String> lore()`
- `String serializedItem()` — optional Base64 itemstack
- `Map<String,Object> toMap()` / `static ShopIcon fromMap(Map)` — YAML serialization helpers

Usage
-----

```java
ShopIcon icon = ShopIcon.builder().material("STONE").amount(16).displayName("Fancy Stone").build();
```

Source
------
[src/main/java/com/skyblockexp/ezshops/shop/api/ShopIcon.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopIcon.java)
