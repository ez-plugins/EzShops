## Stock API

Purpose
-------

Documentation for EzShops' Stock API. This page covers the public `StockAPI` class used to query and manipulate the in-plugin stock market and to manage player holdings.

Obtaining the API
-----------------

Preferred:

```java
StockAPI stock = EzShopsAPI.getInstance().getStockAPI();
if (!EzShopsAPI.getInstance().isStockAPIAvailable()) {
    // stock market features disabled
}
```

Core capabilities
-----------------

- `double getStockPrice(String productId)` — get the current market price for a product
- `void setStockPrice(String productId, double price)` — set/override a product price
- `void updateStockPrice(String productId, int demand)` — adjust price due to simulated demand
- `int getPlayerStockAmount(Player player, String productId)` — query player holdings
- `boolean addPlayerStock(Player player, String productId, int amount)` — add stock to player
- `boolean removePlayerStock(Player player, String productId, int amount)` — remove stock from player
- `List<String> getPlayerOwnedStocks(Player player)` — list a player's stock entries
- `Set<String> getAllProductIds()` — list tradable product ids

Thread-safety
-------------

`StockAPI` is documented as thread-safe and safe to call from async contexts. Use async tasks for bulk operations when appropriate.

Example usage
-------------

Query and update:

```java
StockAPI stock = EzShopsAPI.getInstance().getStockAPI();
if (stock != null) {
    double price = stock.getStockPrice("DIAMOND");
    stock.updateStockPrice("DIAMOND", 5); // simulate demand
}
```

Player holdings:

```java
int amount = stock.getPlayerStockAmount(player, "DIAMOND");
boolean bought = stock.addPlayerStock(player, "DIAMOND", 10);
```

Source
------

[src/main/java/com/skyblockexp/ezshops/api/StockAPI.java](src/main/java/com/skyblockexp/ezshops/api/StockAPI.java)

Notes
-----

- Validate `productId` inputs (non-null, non-empty) before calling; methods throw `IllegalArgumentException` on invalid input.
- When calling `setStockPrice` or updating prices, coordinate with server admin policies — programmatic overrides can affect gameplay balance.
