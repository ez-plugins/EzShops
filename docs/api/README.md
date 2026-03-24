
# EzShops API

This folder contains the public developer documentation for the EzShops plugin. It provides a centralized reference and short how‑tos for integrating with shop pricing, templates, and the stock market.

Quick links
-----------

- Canonical API reference: [docs/api.md](docs/api.md)
- Shop template how‑to: [docs/api/shop-template-api.md](docs/api/shop-template-api.md)
- Shop icon reference: [docs/api/shop-icon.md](docs/api/shop-icon.md)
- Shop pricing reference: [docs/api/shop-pricing.md](docs/api/shop-pricing.md)
- Stock API reference: [docs/api/stock-api.md](docs/api/stock-api.md)
- Class references: [docs/api/class/](docs/api/class/)

Overview
--------

EzShops exposes three primary public surfaces for integrations:

- Shop pricing: query buy/sell prices for ItemStacks via `ShopPriceService`.
- Shop templates: create, register and import YAML templates via `ShopTemplateService`.
- Stock market: real-time stock prices and player holdings via `StockAPI`.

Recommended acquisition pattern
-------------------------------

Use the unified `EzShopsAPI` entry point where possible — it is initialized by the plugin and provides convenient helpers for each feature:

```java
EzShopsAPI api = EzShopsAPI.getInstance();
ShopPriceService shop = api.getShopAPI(); // may be null when disabled
ShopTemplateService templates = api.getTemplateAPI();
StockAPI stock = api.getStockAPI(); // may be null when stock disabled
```

If you must, you may fall back to Bukkit's `ServicesManager` to look up interfaces directly, but that is considered legacy compared to the `EzShopsAPI` helpers.

Service lifecycle & guidance
---------------------------

- `EzShopsAPI.getInstance()` throws `IllegalStateException` if EzShops has not yet initialized. Call it from your plugin's `onEnable()` or later, and catch the exception if uncertain.
- `getShopAPI()` and `getTemplateAPI()` may return `null` when the corresponding features are disabled; always handle `null` and `OptionalDouble.empty()` return values.
- `StockAPI` is documented as thread-safe and safe to call from async tasks; prefer async for bulk price operations.
- EzShops registers services with Bukkit's `ServicesManager`; the plugin's `CoreShopComponent` handles registration in the bootstrap.

Class reference links
---------------------

- `EzShopsAPI` — [docs/api/class/EzShopsAPI.md](docs/api/class/EzShopsAPI.md) — entry point
- `ShopPriceService` — [docs/api/class/ShopPriceService.md](docs/api/class/ShopPriceService.md) — price lookups
- `ShopTemplateService` — [docs/api/class/ShopTemplateService.md](docs/api/class/ShopTemplateService.md) — template management
- `StockAPI` — [docs/api/class/StockAPI.md](docs/api/class/StockAPI.md) — stock market operations
- `ShopItem`, `ShopIcon`, `ShopTemplateBuilder` and category builders — see [docs/api/class/](docs/api/class/)

Contributing
------------

Keep examples concise and runnable. When editing API docs, prefer linking to the Java sources in `src/main/java` for accuracy. If you add examples that rely on templates or resources, include minimal example files under `docs/examples` or reference `src/main/resources/templates/example-template.yml`.

