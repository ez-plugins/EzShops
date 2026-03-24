# ShopTemplateService

Package: com.skyblockexp.ezshops.shop.api

Overview
--------

`ShopTemplateService` lets other plugins register, list and import shop templates which are persisted under `plugins/EzShops/templates/`.

Key methods
-----------
- `void registerTemplate(ShopTemplate template)` — persist and register a template
- `Collection<ShopTemplate> listTemplates()` — list registered templates
- `Optional<ShopTemplate> importTemplate(String templateId)` — import by id

Usage
-----

```java
ShopTemplateService svc = EzShopsAPI.getInstance().getTemplateAPI();
svc.registerTemplate(myTemplate);
```

Notes
-----
- Templates can include `itemstack-base64` entries to preserve full `ItemStack` metadata.
- Admins may import templates via `/shop import <id>` which will write `shop/categories/*.yml` files.

Source
------
[src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateService.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopTemplateService.java)
