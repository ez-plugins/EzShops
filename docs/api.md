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
Located in `com.skyblockexp.shop.api.ShopPriceService`

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
- `boolean hasBuyPrice(Material material)` - Check if material can be bought
- `boolean hasSellPrice(Material material)` - Check if material can be sold
- `void reload()` - Reload pricing configuration

### ShopTransactionService
Handles buy/sell transactions between players and the shop.

**Key Methods:**
- `ShopTransactionResult buyItem(Player player, Material material, int quantity)` - Purchase items
- `ShopTransactionResult sellItem(Player player, ItemStack itemStack)` - Sell items

### Stock Market API
The stock market system provides real-time pricing with supply/demand mechanics.

**Key Classes:**
- `StockMarketManager` - Core stock market operations
- `StockPrice` - Represents a stock item's current price and metadata
- `StockTransaction` - Represents a buy/sell transaction

## Code Samples

### Accessing the EzShops API
```java
// Get the main API instance
EzShopsAPI api = EzShopsAPI.getInstance();

// The API provides access to both shop and stock functionality
ShopPriceService shopApi = api.getShopAPI();
StockAPI stockApi = api.getStockAPI();
```

### Managing Stock Prices
```java
EzShopsAPI api = EzShopsAPI.getInstance();
StockAPI stockApi = api.getStockAPI();

if (stockApi != null) {
    // Get current price
    double currentPrice = stockApi.getStockPrice("DIAMOND");
    
    // Set a new price
    stockApi.setStockPrice("DIAMOND", 200.0);
    
    // Update price based on demand (positive = increase, negative = decrease)
    stockApi.updateStockPrice("DIAMOND", 10); // Simulates buying 10 units
    stockApi.updateStockPrice("IRON_INGOT", -5); // Simulates selling 5 units
    
    // Get all available products
    Set<String> allProducts = stockApi.getAllProductIds();
    for (String productId : allProducts) {
        double price = stockApi.getStockPrice(productId);
        System.out.println(productId + ": " + price);
    }
}
```

### Managing Player Stock Holdings
```java
EzShopsAPI api = EzShopsAPI.getInstance();
StockAPI stockApi = api.getStockAPI();

if (stockApi != null) {
    Player player = ...; // Your player instance
    
    // Check how much stock a player owns
    int diamondStock = stockApi.getPlayerStockAmount(player, "DIAMOND");
    
    // Add stock to a player
    boolean added = stockApi.addPlayerStock(player, "DIAMOND", 10);
    if (added) {
        player.sendMessage("Added 10 diamond stock to your portfolio!");
    }
    
    // Remove stock from a player
    boolean removed = stockApi.removePlayerStock(player, "DIAMOND", 5);
    if (removed) {
        player.sendMessage("Sold 5 diamond stock!");
    } else {
        player.sendMessage("You don't have enough stock to sell!");
    }
    
    // Get all stocks owned by a player
    List<String> ownedStocks = stockApi.getPlayerOwnedStocks(player);
    player.sendMessage("You own stock in: " + String.join(", ", ownedStocks));
}
```

### Getting Shop Prices
```java
// Method 1: Using the unified API (recommended)
EzShopsAPI api = EzShopsAPI.getInstance();
ShopPriceService priceService = api.getShopAPI();

if (priceService != null) {
    ItemStack itemStack = new ItemStack(Material.DIAMOND, 10);
    
    // Get buy price (what player pays to purchase from shop)
    OptionalDouble buyPrice = priceService.findBuyPrice(itemStack);
    if (buyPrice.isPresent()) {
        player.sendMessage("Buy price: " + buyPrice.getAsDouble());
    }
    
    // Get sell price (what player receives when selling to shop)
    OptionalDouble sellPrice = priceService.findSellPrice(itemStack);
    if (sellPrice.isPresent()) {
        player.sendMessage("Sell price: " + sellPrice.getAsDouble());
    }
}

// Method 2: Direct access via ServicesManager (legacy approach)
ServicesManager servicesManager = Bukkit.getServicesManager();
RegisteredServiceProvider<ShopPriceService> provider = 
    servicesManager.getRegistration(ShopPriceService.class);

if (provider != null) {
    ShopPriceService priceService = provider.getProvider();
    
    ItemStack itemStack = new ItemStack(Material.DIAMOND, 10);
    
    // Get buy price (what player pays to purchase from shop)
    OptionalDouble buyPrice = priceService.findBuyPrice(itemStack);
    if (buyPrice.isPresent()) {
        player.sendMessage("Buy price: " + buyPrice.getAsDouble());
    }
    
    // Get sell price (what player receives when selling to shop)
    OptionalDouble sellPrice = priceService.findSellPrice(itemStack);
    if (sellPrice.isPresent()) {
        player.sendMessage("Sell price: " + sellPrice.getAsDouble());
    }
}
```

### Checking API Availability
```java
// Check if EzShops is loaded and the API is available
try {
    EzShopsAPI api = EzShopsAPI.getInstance();
    
    // Check if specific features are available
    if (api.isStockAPIAvailable()) {
        // Stock market features are enabled
        StockAPI stockApi = api.getStockAPI();
        // Use stock API...
    } else {
        // Stock market is disabled in config
        player.sendMessage("Stock market features are not available.");
    }
} catch (IllegalStateException e) {
    // EzShops is not loaded or not initialized
    getLogger().warning("EzShops API is not available: " + e.getMessage());
}
```

### Accessing Internal APIs
**⚠️ Warning:** Direct access to internal plugin classes may break in future versions. Use the public API whenever possible.

```java
// Only use this approach if the public API doesn't provide what you need
EzShopsPlugin plugin = (EzShopsPlugin) Bukkit.getPluginManager().getPlugin("EzShops");

// Access through the public API instead (recommended)
EzShopsAPI api = EzShopsAPI.getInstance();
StockAPI stockApi = api.getStockAPI();
```

### Listening to Shop Events
EzShops fires Bukkit events for shop transactions. Listen to these in your plugin:

```java
@EventHandler
public void onShopTransaction(ShopTransactionEvent event) {
    Player player = event.getPlayer();
    Material material = event.getMaterial();
    int quantity = event.getQuantity();
    double price = event.getPrice();
    
    // Custom logic here
    player.sendMessage("You traded " + quantity + "x " + material);
}
```

**Note:** Event classes may vary based on your EzShops version. Check the source code for the exact event class names and fields available.

### Integration with EzAuction
EzShops integrates with EzAuction to provide shop prices for auction listings:

```java
// EzAuction can query EzShops pricing automatically
// when both plugins are installed

// This happens transparently when ShopPriceService is registered
// in Bukkit's ServicesManager
```

## Integration Guide

### Adding EzShops as a Dependency

**Maven:**
```xml
<dependency>
    <groupId>com.skyblockexp</groupId>
    <artifactId>ezshops</artifactId>
    <version>2.0.0</version> <!-- Check releases for latest version -->
    <scope>provided</scope>
</dependency>
```

**In plugin.yml:**
```yaml
depend: [EzShops]
# or
softdepend: [EzShops]
```

### Checking if EzShops is Available
```java
Plugin ezShops = Bukkit.getPluginManager().getPlugin("EzShops");
if (ezShops != null && ezShops.isEnabled()) {
    // EzShops is available - get the API
    try {
        EzShopsAPI api = EzShopsAPI.getInstance();
        // Use the API...
    } catch (IllegalStateException e) {
        // API not initialized yet
        getLogger().warning("EzShops is loaded but API is not ready");
    }
}
```

### Best Practices
1. **Use the EzShopsAPI class** as your main entry point for all functionality
2. **Check for null** when getting API instances (ShopAPI, StockAPI may be null if disabled)
3. **Handle OptionalDouble.empty()** when prices are not available
4. **Respect permissions** - check that players have appropriate permissions before executing transactions
5. **Use async operations** for price lookups when possible to avoid blocking the main thread
6. **Validate input** - the API throws IllegalArgumentException for invalid parameters
7. **Catch exceptions** - wrap API calls in try-catch blocks to handle edge cases gracefully

### Error Handling
```java
try {
    EzShopsAPI api = EzShopsAPI.getInstance();
    StockAPI stockApi = api.getStockAPI();
    
    if (stockApi != null) {
        double price = stockApi.getStockPrice("DIAMOND");
        player.sendMessage("Diamond price: " + price);
    }
} catch (IllegalStateException e) {
    // API not initialized
    getLogger().warning("EzShops API not available: " + e.getMessage());
} catch (IllegalArgumentException e) {
    // Invalid parameters provided
    getLogger().warning("Invalid API parameters: " + e.getMessage());
}
```

## Stock Market Security

**Version 2.0.0+ includes important security fixes:**

1. **Confirmation GUI for Stock Sales**: All stock sales now require player confirmation through a GUI, preventing accidental or exploited instant sales.

2. **Infinite Money Glitch Fixed**: The previous vulnerability where players could instantly sell stock without confirmation has been patched. All sell operations now go through the confirmation system.

3. **Transaction Validation**: The system validates stock ownership and balances before completing any transaction.

Server owners should ensure they are running version 2.0.0 or later to benefit from these security improvements.

---

## API Changes in Version 2.0.0

### New Features
- **EzShopsAPI**: Unified API entry point for all EzShops functionality
- **StockAPI**: Complete API for stock market integration
- **Security Improvements**: Stock selling now requires confirmation GUI

### Migration Guide
If you were previously accessing stock features directly, update your code:

**Old way (deprecated):**
```java
EzShopsPlugin plugin = (EzShopsPlugin) Bukkit.getPluginManager().getPlugin("EzShops");
StockMarketManager manager = plugin.getStockComponent().getStockMarketManager();
double price = manager.getPrice("DIAMOND");
```

**New way (recommended):**
```java
EzShopsAPI api = EzShopsAPI.getInstance();
StockAPI stockApi = api.getStockAPI();
double price = stockApi.getStockPrice("DIAMOND");
```

---

For more details on events and internal structure, review the source code in the `com.skyblockexp.shop` and `com.skyblockexp.ezshops` packages. For troubleshooting, refer to the main [README](../README.md) or open an issue on GitHub.
