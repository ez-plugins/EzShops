# EzShops Permissions

This document lists all permissions available in the EzShops plugin, their default values, and a description of what each permission allows.

---

## Table of Contents
- [Permission Overview](#permission-overview)
- [Player Permissions](#player-permissions)
  - [Shop Access](#shop-access)
  - [Player Shops](#player-shops)
  - [Stock Market](#stock-market)
- [Admin Permissions](#admin-permissions)
  - [Shop Administration](#shop-administration)
  - [Sign Shop Management](#sign-shop-management)
  - [Stock Market Administration](#stock-market-administration)
- [Permission Examples](#permission-examples)

---

## Permission Overview

EzShops uses a hierarchical permission system. Permissions are organized into categories for easy management with permission plugins like LuckPerms, PermissionsEx, or GroupManager.

**Default Permission Levels:**
- `true` - All players have this permission by default
- `op` - Only server operators have this permission by default

---

## Player Permissions

### Shop Access

| Permission Node           | Default | Description                                      |
|--------------------------|---------|--------------------------------------------------|
| `ezshops.shop`           | true    | Access the `/shop` command and GUI               |
| `ezshops.shop.buy`       | true    | Purchase items from the shop                     |
| `ezshops.shop.sell`      | true    | Sell items to the shop, use `/sellhand` and `/sellinventory` |

**Details:**
- `ezshops.shop` - Required to open the shop GUI and use the `/price` command
- `ezshops.shop.buy` - Allows buying items through GUI or `/shop buy <item>` command
- `ezshops.shop.sell` - Allows selling items through GUI, `/shop sell`, `/sellhand`, and `/sellinventory`

### Player Shops

| Permission Node              | Default | Description                                      |
|-----------------------------|---------|--------------------------------------------------|
| `ezshops.playershop.create` | true    | Create sign-based chest shops with `/playershop` |
| `ezshops.playershop.buy`    | true    | Purchase from player-owned chest shops           |
| `ezshops.playershop.admin`  | op      | Manage any player shop (remove, edit)            |

**Details:**
- `ezshops.playershop.create` - Allows using `/playershop` command to configure and place shop signs
- `ezshops.playershop.buy` - Required to purchase items from other players' shops
- `ezshops.playershop.admin` - Administrative access to all player shops, including removal and editing

### Stock Market

| Permission Node           | Default | Description                                      |
|--------------------------|---------|--------------------------------------------------|
| `ezshops.stock.view`     | true    | View stock prices and use `/stock` commands      |
| `ezshops.stock.refresh`  | op      | Manually refresh stock market quotes             |

**Details:**
- `ezshops.stock.view` - Allows using `/stock buy`, `/stock sell`, `/stock info`, `/stock list`, and `/stocks` commands
- `ezshops.stock.refresh` - Allows forcing a refresh of stock market data (typically automatic)

---

## Admin Permissions

### Shop Administration

| Permission Node              | Default | Description                                      |
|-----------------------------|---------|--------------------------------------------------|
| `ezshops.reload`            | op      | Reload shop configuration with `/shop reload`    |
| `ezshops.shop.admin.minionhead` | op  | Purchase minion heads directly (bypass restrictions) |

**Details:**
- `ezshops.reload` - Allows reloading all shop configurations, menus, categories, and pricing without server restart
- `ezshops.shop.admin.minionhead` - Bypasses normal restrictions on minion heads (usually crate-only items)

### Sign Shop Management

| Permission Node              | Default | Description                                      |
|-----------------------------|---------|--------------------------------------------------|
| `ezshops.shop.sign.setup`   | op      | Access `/signshop` setup GUI                     |
| `ezshops.shop.sign.create`  | op      | Create shop signs                                |
| `ezshops.shop.sign.scan`    | op      | Scan and convert legacy shop signs               |

**Details:**
- `ezshops.shop.sign.setup` - Opens the sign shop configuration GUI
- `ezshops.shop.sign.create` - Allows placing shop signs that mirror shop menu entries
- `ezshops.shop.sign.scan` - Scans for and converts old shop sign formats

### Stock Market Administration

| Permission Node           | Default | Description                                      |
|--------------------------|---------|--------------------------------------------------|
| `ezshops.stock.admin`    | op      | Full access to `/stockadmin` commands            |

**Details:**
- Grants access to all stock market administrative commands:
  - `/stockadmin set <item> <price>` - Set prices directly
  - `/stockadmin reset <item>` - Remove price overrides
  - `/stockadmin freeze <item>` - Freeze/unfreeze prices
  - `/stockadmin reload` - Reload stock market configuration
  - `/stockadmin listfrozen` - List all frozen items
  - `/stockadmin listoverrides` - List price overrides with metadata

---

## Permission Examples

### Granting Basic Shop Access
To give a player or group basic shop access (buy/sell only):

**LuckPerms:**
```
/lp user <player> permission set ezshops.shop true
/lp user <player> permission set ezshops.shop.buy true
/lp user <player> permission set ezshops.shop.sell true
```

**PermissionsEx:**
```
/pex user <player> add ezshops.shop
/pex user <player> add ezshops.shop.buy
/pex user <player> add ezshops.shop.sell
```

### Granting Player Shop Permissions
To allow players to create and use player shops:

**LuckPerms:**
```
/lp group default permission set ezshops.playershop.create true
/lp group default permission set ezshops.playershop.buy true
```

**PermissionsEx:**
```
/pex group default add ezshops.playershop.create
/pex group default add ezshops.playershop.buy
```

### Granting Stock Market Access
To enable stock market features for a group:

**LuckPerms:**
```
/lp group vip permission set ezshops.stock.view true
```

**PermissionsEx:**
```
/pex group vip add ezshops.stock.view
```

### Granting All Admin Permissions
To give a player full administrative access to EzShops:

**LuckPerms:**
```
/lp user <player> permission set ezshops.reload true
/lp user <player> permission set ezshops.shop.sign.setup true
/lp user <player> permission set ezshops.shop.sign.create true
/lp user <player> permission set ezshops.shop.sign.scan true
/lp user <player> permission set ezshops.stock.admin true
/lp user <player> permission set ezshops.playershop.admin true
```

### Removing Sell Permission
To prevent a player from selling items (buy-only access):

**LuckPerms:**
```
/lp user <player> permission set ezshops.shop.sell false
```

**PermissionsEx:**
```
/pex user <player> remove ezshops.shop.sell
```

---

## Permission Wildcards

While EzShops doesn't explicitly define wildcard nodes, most permission plugins support them:

- `ezshops.*` - All EzShops permissions
- `ezshops.shop.*` - All shop-related permissions
- `ezshops.playershop.*` - All player shop permissions
- `ezshops.stock.*` - All stock market permissions

**Note:** Wildcards are permission plugin features and may behave differently depending on your configuration.

---

## Configuration Integration

Permissions can be referenced in shop configuration files to create restricted categories or items:

**Example in `shop/categories/vip_items.yml`:**
```yaml
permission: "ezshops.category.vip"
```

Players without this permission won't see the VIP category in the shop menu.

---

For more information on commands that use these permissions, see the [Commands](commands.md) documentation. For general configuration, see the [Configuration Guide](configuration.md).
