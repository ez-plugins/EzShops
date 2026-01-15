# EzShops Commands

This document lists all commands available in the EzShops plugin, their usage, descriptions, and required permissions.

---

## Table of Contents
- [Player Commands](#player-commands)
  - [Shop Commands](#shop-commands)
  - [Trading Commands](#trading-commands)
  - [Player Shop Commands](#player-shop-commands)
  - [Stock Market Commands](#stock-market-commands)
- [Admin Commands](#admin-commands)
  - [Sign Shop Setup](#sign-shop-setup)
  - [Stock Market Admin](#stock-market-admin)

---

## Player Commands

### Shop Commands

#### `/shop`
Opens the main shop GUI where players can browse categories and purchase items.

**Usage:** `/shop`  
**Permission:** `ezshops.shop`  
**Aliases:** None

**Subcommands:**
- `/shop buy <material> [amount]` - Buy items directly from chat
- `/shop sell <material> [amount]` - Sell items directly from chat
- `/shop reload` - Reload shop configuration (requires `ezshops.reload` permission)

**Examples:**
```
/shop
/shop buy DIAMOND 10
/shop sell IRON_INGOT 64
/shop reload
```

---

### Trading Commands

#### `/sellhand`
Sells the item currently held in your hand to the shop.

**Usage:** `/sellhand`  
**Permission:** `ezshops.shop.sell`  
**Aliases:** None

**Behavior:**
- Sells the entire stack in your main hand
- Displays the amount earned in chat
- Requires the item to be sellable in the shop configuration

#### `/sellinventory`
Sells all sellable items from your inventory to the shop in one command.

**Usage:** `/sellinventory`  
**Permission:** `ezshops.shop.sell`  
**Aliases:** None

**Behavior:**
- Scans your entire inventory for sellable items
- Sells all matching items at once
- Displays a summary of items sold and total earnings

#### `/price`
Checks the current buy and sell price of a material in the shop.

**Usage:** `/price <material>`  
**Permission:** `ezshops.shop`  
**Aliases:** None

**Examples:**
```
/price DIAMOND
/price EMERALD
/price IRON_INGOT
```

**Output:**
- Buy price (what you pay to purchase from shop)
- Sell price (what you receive when selling to shop)
- Stock market status if applicable

---

### Player Shop Commands

#### `/playershop`
Opens the player shop setup menu for creating sign-based chest shops.

**Usage:** `/playershop`  
**Permission:** `ezshops.playershop.create`  
**Aliases:** None

**Workflow:**
1. Use `/playershop` to open the setup GUI
2. Configure quantity and pricing using +/- buttons
3. Place a `[shop]` sign on or near a chest
4. The sign converts to a player shop automatically

**Features:**
- Link to chest inventory for automatic stock management
- Set custom prices for your items
- Enforce quantity limits and price caps
- Owner-branded listings with custom formatting

---

### Stock Market Commands

#### `/stock`
Main command for interacting with the EzShops stock market system.

**Usage:**
```
/stock buy <item> <amount>
/stock sell <item> <amount>
/stock info <item>
/stock list [page]
/stock overview
```

**Permission:** `ezshops.stock.view`  
**Aliases:** `stk`

**Subcommands:**

##### `/stock buy <item> <amount>`
Purchase shares in a stock market item at the current market price.

**Permission:** `ezshops.stock.view`  
**Example:** `/stock buy DIAMOND 10`

##### `/stock sell <item> <amount>`
Sell your shares at the current market price.

**Permission:** `ezshops.stock.view`  
**Example:** `/stock sell DIAMOND 5`

##### `/stock info <item>`
View detailed information about a stock market item including:
- Current price
- Recent price changes
- Volatility percentage
- Frozen status (if applicable)

**Permission:** `ezshops.stock.view`  
**Example:** `/stock info DIAMOND`

##### `/stock list [page]`
List all available stock market items with their current prices.

**Permission:** `ezshops.stock.view`  
**Example:** `/stock list` or `/stock list 2`

##### `/stock overview`
Display a summary of the stock market including trending items and your portfolio.

**Permission:** `ezshops.stock.view`  
**Example:** `/stock overview`

#### `/stocks`
Quick overview of cached stock quotes with pagination.

**Usage:** `/stocks [page]`  
**Permission:** `ezshops.stock.view`  
**Aliases:** `stks`

**Example:** `/stocks` or `/stocks 2`

---

## Admin Commands

### Sign Shop Setup

#### `/signshop`
Opens the sign shop setup GUI for creating and managing shop signs with custom backings.

**Usage:** `/signshop`  
**Permission:** `ezshops.shop.sign.setup`  
**Aliases:** None

**Features:**
- Generate shop signs with custom formats
- Configure sign backing materials
- Scan and convert legacy shop signs
- Preview sign layouts before placement

---

### Stock Market Admin

#### `/stockadmin`
Administrative commands for managing the stock market system.

**Usage:**
```
/stockadmin set <item> <price>
/stockadmin reset <item>
/stockadmin freeze <item>
/stockadmin unfreeze <item>
/stockadmin reload
/stockadmin listfrozen [page]
/stockadmin listoverrides [page]
```

**Permission:** `ezshops.stock.admin`  
**Aliases:** None

**Subcommands:**

##### `/stockadmin set <item> <price>`
Directly set the price of a stock market item, overriding market calculations.

**Permission:** `ezshops.stock.admin`  
**Example:** `/stockadmin set DIAMOND 100.0`

##### `/stockadmin reset <item>`
Remove any price override and return the item to normal market pricing.

**Permission:** `ezshops.stock.admin`  
**Example:** `/stockadmin reset DIAMOND`

##### `/stockadmin freeze <item>`
Freeze the current price of an item, preventing market fluctuations.

**Permission:** `ezshops.stock.admin`  
**Example:** `/stockadmin freeze DIAMOND`

**Note:** Frozen status persists across server restarts.

##### `/stockadmin unfreeze <item>`
Unfreeze a previously frozen item, allowing market prices to update normally.

**Permission:** `ezshops.stock.admin`  
**Example:** `/stockadmin unfreeze DIAMOND`

##### `/stockadmin reload`
Reload stock market configuration and pricing data.

**Permission:** `ezshops.stock.admin`  
**Example:** `/stockadmin reload`

##### `/stockadmin listfrozen [page]`
List all currently frozen stock market items with pagination.

**Permission:** `ezshops.stock.admin`  
**Example:** `/stockadmin listfrozen` or `/stockadmin listfrozen 2`

##### `/stockadmin listoverrides [page]`
List all items with admin price overrides, showing who set them and when.

**Permission:** `ezshops.stock.admin`  
**Example:** `/stockadmin listoverrides`

---

## Command Notes

### Tab Completion
Most commands support tab completion for:
- Material names (e.g., DIAMOND, EMERALD)
- Command arguments
- Subcommands

### Permission Inheritance
- Players with `ezshops.shop` can access basic shop features
- Operators have access to all admin commands by default
- Custom permission groups can be configured via permission plugins

### Configuration
Commands can be customized in `config.yml`:
- Enable/disable specific features
- Configure cooldowns
- Set transaction limits
- Customize messages and formats

---

For more information, see the [Permissions](permissions.md) documentation or the main [README](../README.md).
