## ShopIcon — visual representation for shop items

**Purpose:** a compact representation of the visual/icon definition used by `ShopItem` entries. It supports lightweight fields (material, amount, name, lore) and an optional full `ItemStack` serialized as Base64.

Builder example:

```java
ShopIcon icon = ShopIcon.builder()
    .material("STONE")
    .amount(16)
    .displayName("Fancy Stone")
    .lore(List.of("Line 1", "Line 2"))
    .serializedItem(base64)
    .build();
```

Usage (short):

```java
ShopItem item = ShopItem.builder("stone")
    .material("STONE")
    .amount(16)
    .icon(icon)
    .buy(64)
    .sell(32)
    .build();
```

Serialization

`ShopIcon#toMap()` produces a YAML-friendly `Map` used in category files; use `ShopIcon.fromMap(map)` to reconstruct an icon from YAML.

Notes

- Use `serializedItem` when you need to preserve full ItemStack metadata (enchants, NBT, custom model data).
- Keep icons small for readability in category YAML files; prefer basic fields unless full metadata is required.

Source: [src/main/java/com/skyblockexp/ezshops/shop/api/ShopIcon.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopIcon.java)
