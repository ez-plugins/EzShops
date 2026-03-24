# EzShops Plugin API Documentation

This document provides a comprehensive reference for developers integrating with the EzShops plugin. All public classes, methods, and extension points are documented with usage examples.

## Table of Contents
- [Overview](#overview)
- [Main API Classes](#main-api-classes)
  - [EzShopsAPI](#ezshopsapi)
  - [StockAPI](#stockapi)
  - [ShopPriceService](#shopppriceservice)
- [Key Interfaces & Services](#key-interfaces--services)
- [Code Samples](#code-samples)
- [Integration Guide](#integration-guide)

---

## Overview
EzShops exposes a robust API for interacting with shop pricing, transactions, and the stock market. Use these APIs to query prices, execute transactions, and integrate with other plugins programmatically.

The new **EzShopsAPI** class (since version 2.0.0) provides a unified entry point to access both the Shop API and the Stock API, making integration simpler and more maintainable.

## Main API Classes

### EzShopsAPI
**Package:** `com.skyblockexp.ezshops.api.EzShopsAPI`

The main entry point for accessing all EzShops functionality. This class provides access to both the Shop API and Stock API through a single, easy-to-use interface.

**Key Methods:**
- `EzShopsAPI getInstance()` - Get the singleton API instance
- `ShopPriceService getShopAPI()` - Get the shop pricing API
- `StockAPI getStockAPI()` - Get the stock market API
- `boolean isStockAPIAvailable()` - Check if stock market features are enabled

**Usage Example:**
```java
// Get the main EzShops API
EzShopsAPI api = EzShopsAPI.getInstance();

// Check if stock market is available
if (api.isStockAPIAvailable()) {
    StockAPI stockApi = api.getStockAPI();
    double price = stockApi.getStockPrice("DIAMOND");
    player.sendMessage("Diamond stock price: " + price);
}

// Access shop pricing
ShopPriceService shopApi = api.getShopAPI();
if (shopApi != null) {
    OptionalDouble sellPrice = shopApi.findSellPrice(new ItemStack(Material.DIAMOND, 1));
    sellPrice.ifPresent(price -> 
        player.sendMessage("Diamond sell price: " + price));
}
```

### StockAPI
**Package:** `com.skyblockexp.ezshops.api.StockAPI`

Provides programmatic access to the stock market system, allowing you to query and manipulate stock prices and manage player stock holdings.

**Key Methods:**
```java
// Price management
double getStockPrice(String productId)
void setStockPrice(String productId, double price)
void updateStockPrice(String productId, int demand)

// Player stock management
int getPlayerStockAmount(Player player, String productId)
boolean addPlayerStock(Player player, String productId, int amount)
boolean removePlayerStock(Player player, String productId, int amount)
List<String> getPlayerOwnedStocks(Player player)

// Market information
Set<String> getAllProductIds()
```

**Thread Safety:** All methods are thread-safe and can be called from async tasks.

### ShopPriceService
Located in `com.skyblockexp.ezshops.shop.api.ShopPriceService`

The primary interface for accessing shop pricing information from other plugins.

**Interface Definition:**
```java
public interface ShopPriceService {
    /**
     * Looks up the total buy price for the provided item stack.
     *
     * @param itemStack the item stack to price
     * @return the total buy price for the stack, or OptionalDouble.empty() when unavailable
     */
    OptionalDouble findBuyPrice(ItemStack itemStack);

    /**
     * Looks up the total sell price for the provided item stack.
     *
     * @param itemStack the item stack to price
     * @return the total sell price for the stack, or OptionalDouble.empty() when unavailable
     */
    OptionalDouble findSellPrice(ItemStack itemStack);
}
```

## Key Interfaces & Services

### ShopPricingManager
Manages pricing for shop items including dynamic pricing calculations.

**Key Methods:**
- `ShopPrice getPrice(Material material)` - Get pricing for a material
# EzShops Plugin API (legacy)

This file has moved. The maintained API documentation and class references are now under `docs/api/`.

Please use the following entry points:

- Canonical index: [docs/api/README.md](docs/api/README.md)
- Class references: [docs/api/class/](docs/api/class/)
- Shop pricing: [docs/api/shop-pricing.md](docs/api/shop-pricing.md)
- Stock API: [docs/api/stock-api.md](docs/api/stock-api.md)

The old, long-form reference content was preserved in Git history for reference. Use the `docs/api/*` pages for up-to-date guidance and examples.
The stock market system provides real-time pricing with supply/demand mechanics.
