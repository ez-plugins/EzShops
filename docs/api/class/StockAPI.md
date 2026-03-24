# StockAPI

Package: com.skyblockexp.ezshops.api

Overview
--------

`StockAPI` exposes programmatic access to the EzShops stock market: price queries, programmatic price updates, and player holdings management. Methods are documented as thread-safe.

Constructors
------------
- `public StockAPI(StockMarketManager stockMarketManager)` — created by the plugin when the stock component is enabled.

Key methods
-----------
- `double getStockPrice(String productId)` — get current price; validates input
- `void setStockPrice(String productId, double price)` — set/override price (price validated)
- `void updateStockPrice(String productId, int demand)` — adjust price by demand
- `int getPlayerStockAmount(Player player, String productId)` — query player holdings
- `boolean addPlayerStock(Player player, String productId, int amount)` — add to holdings
- `boolean removePlayerStock(Player player, String productId, int amount)` — remove from holdings
- `List<String> getPlayerOwnedStocks(Player player)` — list player's products
- `Set<String> getAllProductIds()` — list tradable product ids

Usage (short)
--------------

```java
StockAPI stock = EzShopsAPI.getInstance().getStockAPI();
if (stock != null) {
    double price = stock.getStockPrice("DIAMOND");
    stock.updateStockPrice("DIAMOND", 5);
}
```

Notes
-----
- Methods throw `IllegalArgumentException` for invalid inputs (null/empty productId, null player, non-positive amounts).
- Designed to be safe in async contexts; prefer async tasks for bulk operations.

Source
------
[src/main/java/com/skyblockexp/ezshops/api/StockAPI.java](src/main/java/com/skyblockexp/ezshops/api/StockAPI.java)
