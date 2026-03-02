# ShopPriceService

Package: com.skyblockexp.ezshops.shop.api

Overview
--------

`ShopPriceService` is the primary public interface to obtain buy/sell prices for `ItemStack`s. Implementations return `OptionalDouble` to indicate price availability.

Key methods
-----------
- `OptionalDouble findBuyPrice(ItemStack itemStack)` — total cost to purchase the given stack from a shop
- `OptionalDouble findSellPrice(ItemStack itemStack)` — total payout when selling the given stack to a shop

Usage
-----

```java
ShopPriceService svc = EzShopsAPI.getInstance().getShopAPI();
if (svc != null) {
    OptionalDouble sell = svc.findSellPrice(new ItemStack(Material.DIAMOND, 5));
}
```

Notes
-----
- Handle `OptionalDouble.empty()` when price is not configured for the item.
- Prefer `EzShopsAPI.getInstance().getShopAPI()` for discovery; `ServicesManager` may be used as a fallback.

Source
------
[src/main/java/com/skyblockexp/ezshops/shop/api/ShopPriceService.java](src/main/java/com/skyblockexp/ezshops/shop/api/ShopPriceService.java)
