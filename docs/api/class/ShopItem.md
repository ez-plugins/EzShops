# ShopItem

Package: com.skyblockexp.ezshops.shop.api

Overview
--------

`ShopItem` is an immutable, builder-backed model that represents an item entry in a shop or template. It serializes to a YAML-friendly Map via `toMap()`.

Construction
------------
- `ShopItem.builder(String id)` — start a builder for the item id; builder exposes fluent setters (`material`, `amount`, `icon`, `buy`, `sell`, etc.) and `build()`.

Key methods
-----------
- `String id()`
- `String material()`
- `int amount()`
- `Map<String,Object> toMap()` — YAML-friendly representation

Usage
-----

```java
ShopItem item = ShopItem.builder("dirt")
    .material("DIRT")
    .amount(16)
    .buy(250)
    .sell(100)
    .build();
```

Source
------
[src/main/java/com/skyblockexp/ezshops/shop/api/ShopItem.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopItem.java)
