# EzShops Configuration Guide

This document provides a comprehensive overview of all configuration options available in EzShops. Use this guide to customize the plugin to fit your server's needs.

---

## Table of Contents
- [Configuration Overview](#configuration-overview)
- [Main Configuration (config.yml)](#main-configuration-configyml)
- [Shop Configuration (shop.yml)](#shop-configuration-shopyml)
- [Shop Menu Layout](#shop-menu-layout)
- [Shop Categories](#shop-categories)
- [Shop Items](#shop-items)
- [Rotations & Daily Specials](#rotations--daily-specials)
- [Player Shops](#player-shops)
- [Stock Market](#stock-market)
- [Messages & Localization](#messages--localization)
- [Advanced Configuration](#advanced-configuration)

---

## Configuration Overview

Configuration files are generated in the `plugins/EzShops/` directory on first run.

**Directory Structure:**
```
plugins/EzShops/
├── config.yml                    # Main plugin configuration
├── shop.yml                      # Shop pricing and items
├── stock-gui.yml                 # Stock market GUI layout
├── shop-dynamic.yml              # Dynamic pricing state (auto-generated)
├── shop/
│   ├── menu.yml                  # Shop menu layout and GUI settings
│   ├── categories/               # Individual category configurations
│   │   ├── building.yml
│   │   ├── farming.yml
│   │   ├── mining.yml
│   │   └── ...
│   └── rotations/                # Rotation definitions for daily specials
│       └── daily-specials.yml
└── messages/                     # Localization files
    ├── messages_en.yml           # English (default)
    ├── messages_es.yml           # Spanish
    ├── messages_nl.yml           # Dutch
    └── messages_zh.yml           # Chinese
```

---

## Main Configuration (config.yml)

The `config.yml` file contains global plugin settings.

### Basic Settings

```yaml
# Language for plugin messages (en, es, nl, zh)
language: en

# Enable debug logging for troubleshooting
debug: false

# Economy integration
economy:
  # Provider: vault (default)
  provider: vault
  
  # Currency format
  currency-symbol: "$"
  decimal-places: 2
```

### Dynamic Pricing

```yaml
dynamic-pricing:
  # Enable automatic price adjustments based on supply/demand
  enabled: true
  
  # Global multipliers for buy/sell prices
  buy-multiplier: 1.0
  sell-multiplier: 0.5
  
  # Persistence file for price state
  state-file: shop-dynamic.yml
```

### Player Shops

```yaml
player-shops:
  # Enable player-owned chest shop system
  enabled: true
  
  # Minimum and maximum prices
  min-price: 1.0
  max-price: 1000000.0
  
  # Quantity limits
  min-quantity: 1
  max-quantity: 64
  
  # Sign format configuration
  sign-format:
    header: "[shop]"
    owner-line: "&b{owner}"
    item-line: "&e{item}"
    stock-line: "&7Stock: {stock}"
    price-line: "&a${price}"
```

### Stock Market

**Version 2.0.0+ Security Improvements:**
- All stock sales now require confirmation through a GUI
- Fixed infinite money glitch vulnerability  
- Enhanced transaction validation

```yaml
stock-market:
  # Enable stock market system
  enabled: true
  
  # Price volatility range (-10% to +10% by default)
  volatility-min: -0.10
  volatility-max: 0.10
  
  # Demand multiplier for price changes
  demand-multiplier: 0.02
  
  # Minimum price floor (prevents prices from going too low)
  min-price: 1.0
  
  # Auto-update interval in minutes
  update-interval: 15
```

**Important:** In version 2.0.0+, all stock selling operations require player confirmation through a dedicated GUI. This prevents accidental sales and exploit abuse. The old instant-sell behavior has been completely removed.

---

## Shop Configuration (shop.yml)

The `shop.yml` file defines all items available in the shop with their base prices.

### Basic Item Configuration

```yaml
items:
  DIAMOND:
    buy: 100.0          # Price player pays to buy from shop
    sell: 50.0          # Price player receives when selling to shop
    
  EMERALD:
    buy: 75.0
    sell: 37.5
    
  IRON_INGOT:
    buy: 10.0
    sell: 5.0
    
  # Item with no sell price (can only be purchased)
  SPAWNER:
    buy: 10000.0
    sell: -1            # -1 means not sellable
    
  # Item with no buy price (can only be sold)
  ROTTEN_FLESH:
    buy: -1             # -1 means not purchasable
    sell: 1.0
```

### Advanced Item Options

```yaml
items:
  DIAMOND_SWORD:
    buy: 500.0
    sell: 250.0
    
    # Dynamic pricing for this item
    dynamic:
      enabled: true
      buy-multiplier: 1.05    # Adjust per-transaction
      sell-multiplier: 0.95
      
    # Stock market integration
    stock-market: true
    
    # Quantity limits
    max-quantity: 10
    
    # Permission requirement
    permission: "ezshops.buy.valuable"
    
    # Custom display name in menus
    display-name: "&bDiamond Sword"
    
    # Lore in shop GUI
    lore:
      - "&7A powerful weapon"
      - "&7for defeating mobs"
```

---

## Shop Menu Layout

Configure the shop GUI appearance in `shop/menu.yml`.

### Menu Settings

```yaml
menu:
  # GUI title
  title: "&6&lShop Menu"
  
  # Inventory size (must be multiple of 9, max 54)
  size: 54
  
  # Enable category-based navigation
  categories-enabled: true
  
  # Filler item for empty slots
  filler:
    enabled: true
    material: GRAY_STAINED_GLASS_PANE
    display-name: " "
  
  # Back button
  back-button:
    enabled: true
    material: ARROW
    display-name: "&cBack"
    slot: 49
    
  # Search feature
  search:
    enabled: true
    material: COMPASS
    display-name: "&eSearch Items"
    slot: 45
```

### Category Layout

```yaml
categories:
  # Slot positions for category icons (0-53)
  slots:
    - 10  # Building
    - 11  # Farming
    - 12  # Mining
    - 13  # Fishing
    - 14  # Mob Drops
    - 15  # Food
    - 16  # Enchantments
    - 19  # Decorations
    - 20  # Redstone
    - 21  # Valuables
    - 22  # Wood
    - 23  # Spawners
    - 24  # Daily Specials
```

---

## Shop Categories

Each category is configured in its own file under `shop/categories/`.

### Example: `shop/categories/building.yml`

```yaml
# Category display settings
menu-title: "&6Building Materials"
icon:
  material: BRICKS
  display-name: "&eBuilding"
  lore:
    - "&7Click to view building materials"
    - "&7for your construction projects"

# Slot in the main shop menu
slot: 10

# Permission required to see this category (optional)
permission: ""

# Items in this category
items:
  STONE:
    buy: 5.0
    sell: 2.5
    slot: 0
    
  COBBLESTONE:
    buy: 3.0
    sell: 1.5
    slot: 1
    
  BRICKS:
    buy: 15.0
    sell: 7.5
    slot: 2
    
  # Add more items...
```

### Category Commands

Categories can execute commands instead of opening a menu:

```yaml
# Example: shop/categories/warp.yml
menu-title: "&bWarp Menu"
icon:
  material: ENDER_PEARL
  display-name: "&bWarps"
slot: 25

# Command to run (supports {player} placeholder)
command: "warp menu {player}"
```

---

## Shop Items

### Item Properties

| Property | Type | Description |
|----------|------|-------------|
| `buy` | number | Price to purchase from shop (-1 = not buyable) |
| `sell` | number | Price received when selling to shop (-1 = not sellable) |
| `slot` | number | Position in category GUI |
| `material` | string | Minecraft material name |
| `display-name` | string | Custom name in GUI (supports color codes) |
| `lore` | list | Description lines in GUI |
| `amount` | number | Stack size per purchase (default: 1) |
| `permission` | string | Required permission to buy/sell |
| `stock-market` | boolean | Enable stock market pricing |
| `dynamic.enabled` | boolean | Enable dynamic pricing |

### Special Item Types

#### Spawners
```yaml
SPAWNER:
  buy: 50000.0
  sell: -1
  spawner-type: PIG    # Mob type for spawner
  slot: 10
```

#### Minion Heads
```yaml
PLAYER_HEAD:
  buy: -1              # Only available from crates
  sell: -1
  skull-owner: "Shadow48402"
  display-name: "&6Minion Head"
  permission: "ezshops.shop.admin.minionhead"
```

---

## Rotations & Daily Specials

Configure rotating items in `shop/rotations/`.

### Example: `shop/rotations/daily-specials.yml`

```yaml
rotation:
  # Rotation mode: sequential or weighted
  mode: weighted
  
  # How often to rotate (in hours)
  interval: 24
  
  # Items in rotation
  items:
    - material: DIAMOND
      buy: 80.0        # Discounted price
      sell: 45.0
      weight: 10       # Higher weight = more common
      
    - material: EMERALD
      buy: 60.0
      sell: 32.0
      weight: 15
      
    - material: NETHERITE_INGOT
      buy: 500.0
      sell: 250.0
      weight: 5        # Rare special
```

### Rotation Modes

- **sequential**: Cycles through items in order
- **weighted**: Random selection based on weight values

---

## Player Shops

Player shops are configured in the main `config.yml` under the `player-shops` section (see above) and don't require individual files.

### Creating a Player Shop

1. Player runs `/playershop`
2. Configure quantity and pricing in the GUI
3. Place a sign with `[shop]` on the first line
4. Link to a chest containing the items

### Sign Format Placeholders

- `{owner}` - Shop owner's name
- `{item}` - Item being sold
- `{stock}` - Current stock in chest
- `{price}` - Price per item
- `{quantity}` - Quantity per purchase

---

## Stock Market

Stock market items are marked in `shop.yml` with `stock-market: true`.

### Stock Market Configuration

Configured in `config.yml` under `stock-market` section (see above).

### Price Calculation

```
New price = max(min-price, current price × (1 + (demand × demand-multiplier) + random volatility))
```

Where:
- **demand** is positive for purchases, negative for sales
- **random volatility** is between `volatility-min` and `volatility-max`
- **min-price** is the configured floor price

### Admin Controls

Stock prices can be frozen or overridden using `/stockadmin` commands (see [Commands](commands.md)).

---

## Messages & Localization

Messages are configured in YAML files under the `messages/` directory.

### Selecting a Language

In `config.yml`:
```yaml
language: en    # en, es, nl, or zh
```

### Message File Structure

Example from `messages/messages_en.yml`:

```yaml
commands:
  shop:
    usage: "&cUsage: /shop [buy|sell] [item] [amount]"
    reload-success: "&aShop configuration reloaded!"
    no-permission: "&cYou don't have permission to do that."

transactions:
  buy-success: "&aYou bought {amount}x {item} for ${price}!"
  sell-success: "&aYou sold {amount}x {item} for ${price}!"
  insufficient-funds: "&cYou don't have enough money!"
  inventory-full: "&cYour inventory is full!"

stock-market:
  price-frozen: "&eThis item's price is currently frozen."
  buy-success: "&aYou bought {amount} shares of {item} for ${price}!"
```

### Adding Custom Languages

1. Create a new file: `messages/messages_<code>.yml`
2. Copy structure from `messages_en.yml`
3. Translate all message keys
4. Add to `DEFAULT_SHOP_RESOURCES` in `EzShopsPlugin.java`
5. Rebuild plugin to bundle the new language

---

## Advanced Configuration

### Reloading Configuration

Use `/shop reload` (requires `ezshops.reload` permission) to reload:
- `config.yml`
- `shop.yml`
- All category files
- All rotation files
- Menu layouts
- Messages (language files)

**Note:** Some changes may require a server restart, such as economy provider changes.

### Performance Tuning

For large servers with many players:

```yaml
# In config.yml
performance:
  # Cache duration for price lookups (seconds)
  price-cache-duration: 60
  
  # Batch transaction processing
  batch-transactions: true
  
  # Async operations
  async-enabled: true
```

### Integration with Other Plugins

EzShops integrates with:
- **Vault** - Required for economy operations
- **EzAuction** - Shop prices appear in auction listings
- **EzEconomy** - Shared economy features
- **PlaceholderAPI** - Shop price placeholders (if installed)

### Backup and Migration

Always backup your configuration before major changes:

```bash
cp -r plugins/EzShops plugins/EzShops.backup
```

To migrate from old configs, check `../REFACTORING.md` in the plugin directory.

---

## Troubleshooting

### Common Issues

**Shop items not appearing:**
- Check `shop.yml` for correct material names
- Verify category files are in `shop/categories/`
- Ensure `categories-enabled: true` in `shop/menu.yml`

**Prices not updating:**
- Verify `dynamic-pricing.enabled: true`
- Check `shop-dynamic.yml` exists and is writable
- Use `/shop reload` to refresh prices

**Permission errors:**
- Verify permission plugin is installed
- Check permissions are granted correctly
- Use `/shop` as OP to test basic functionality

**Stock market not working:**
- Ensure `stock-market.enabled: true` in config
- Check items have `stock-market: true` in `shop.yml`
- Verify economy plugin is working

---

For more information, see the [Commands](commands.md) and [Permissions](permissions.md) documentation, or the main [README](../README.md).

---

## Execute Commands on Buy/Sell

It would be great if the shop system had an option to execute commands when a player buys or sells an item. This command could be executed either as the player or from the console, depending on the setup. That would allow more advanced integrations, like giving ranks, triggering quests, granting permissions, or running custom events directly through shop interactions.

Basic usage ideas:

- **Per-item hooks:** Allow `on-buy` and/or `on-sell` blocks on individual items in `shop.yml` that list commands to run.
- **Execution context:** Support `execute-as: player` or `execute-as: console` so server owners control permissions and context.
- **Placeholders:** Support `{player}`, `{amount}`, `{item}`, `{price}` (and other common placeholders) in commands.

Example (item-level in `shop.yml`):

```yaml
items:
  DIAMOND:
    buy: 100.0
    sell: 50.0
    on-buy:
      execute-as: player    # or 'console'
      commands:
        - "lp user {player} parent add VIP"        # run as player (if allowed)
        - "quest progress give {player} starter"
    on-sell:
      execute-as: console
      commands:
        - "broadcast {player} sold {amount}x {item} for ${price}!"
```

Example (global defaults in `config.yml`):

```yaml
transactions:
  execute-commands: true           # enable command hooks globally
  default-execute-as: console      # fallback execution context
```

Notes:

- Servers should be careful when allowing `execute-as: player` because it runs commands with the player's permissions.
- Implementations may choose to run commands sync/async depending on the command type and server safety.
- Consider adding permission checks such as `ezshops.hooks.use` or per-item permission keys.

