## Shop Pricing API

Purpose
-------

Documentation for EzShops' pricing interfaces and helpers. Use this page to learn how to obtain price information for items and how to integrate safe price lookups into your plugin.

Primary interfaces
------------------

- `com.skyblockexp.ezshops.shop.api.ShopPriceService` — recommended public interface for price lookups (see source).
- `ShopPricingManager` — internal pricing manager used by the plugin for price resolution and dynamic pricing rules (consult source when extending behavior).

Service acquisition
-------------------

Preferred (recommended):

```java
ShopPriceService svc = EzShopsAPI.getInstance().getShopAPI();
// svc may be null if shop pricing is disabled
```

Fallback (legacy) via Bukkit ServicesManager:

```java
RegisteredServiceProvider<ShopPriceService> p = Bukkit.getServicesManager().getRegistration(ShopPriceService.class);
ShopPriceService svc = p != null ? p.getProvider() : null;
```

Core methods
------------

- `OptionalDouble findBuyPrice(ItemStack itemStack)` — total price to buy the provided stack from a shop
- `OptionalDouble findSellPrice(ItemStack itemStack)` — total price the shop will pay for the provided stack

Usage notes
-----------

- Always handle `OptionalDouble.empty()` when a price is not available for the requested item.
- For single lookups on the main thread the calls are fast; for bulk operations (many different materials or large counts) prefer running lookups on an async task to avoid main-thread stalls.
- Respect currency/formatting conventions in your plugin when presenting prices to players.

Examples
--------

Simple sell price lookup:

```java
ShopPriceService svc = EzShopsAPI.getInstance().getShopAPI();
if (svc != null) {
    OptionalDouble sell = svc.findSellPrice(new ItemStack(Material.DIAMOND, 10));
    sell.ifPresent(p -> player.sendMessage("Sell price: " + p));
}
```

Bulk (async) example sketch:

```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    ShopPriceService svc = EzShopsAPI.getInstance().getShopAPI();
    if (svc == null) return;
    for (ItemStack s : stacks) {
        OptionalDouble buy = svc.findBuyPrice(s);
        // aggregate results and schedule sync update if needed
    }
});
```

Source
------

[src/main/java/com/skyblockexp/ezshops/shop/api/ShopPriceService.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopPriceService.java)

See also
--------

- Pricing configuration and dynamic rules: check the plugin's `ShopPricingManager` and configuration files under `src/main/resources` for rules and defaults.
