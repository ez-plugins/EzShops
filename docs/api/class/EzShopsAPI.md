# EzShopsAPI

Package: com.skyblockexp.ezshops.api

Overview
--------

`EzShopsAPI` is the recommended unified entry point to access EzShops services (shop pricing, templates, stock). It exposes helper getters and lifecycle management for integrations.

Constructors
------------
- `private EzShopsAPI(EzShopsPlugin plugin)` — initialized by the plugin; external code should use `getInstance()`.

Key methods
-----------
- `static void initialize(EzShopsPlugin plugin)` — called by the plugin during enable
- `static EzShopsAPI getInstance()` — returns the singleton; throws `IllegalStateException` if not initialized
- `ShopPriceService getShopAPI()` — may return `null` if shop pricing is disabled
- `ShopTemplateService getTemplateAPI()` — may return `null` if template support disabled
- `StockAPI getStockAPI()` — may return `null` when stock feature disabled
- `boolean isStockAPIAvailable()` — quick availability check
- `EzShopsPlugin getPlugin()` — access plugin instance
- `static void shutdown()` — clear singleton on plugin disable

Usage (short)
--------------

```java
EzShopsAPI api = EzShopsAPI.getInstance();
ShopPriceService shop = api.getShopAPI();
if (api.isStockAPIAvailable()) {
    StockAPI stock = api.getStockAPI();
}
```

Source
------
[src/main/java/com/skyblockexp/ezshops/api/EzShopsAPI.java](src/main/java/com/skyblockexp/ezshops/api/EzShopsAPI.java)
