---
layout: default
title: Main Settings
parent: Configuration
nav_order: 1
description: "config.yml reference  -  dynamic pricing, teams, player shops."
---

# ⚙️ Main Configuration (config.yml)

The `config.yml` file contains the global settings for EzShops. This is the heart of the plugin where you define how the economy, pricing, and special systems behave across your server.

---

## 📂 Configuration Overview

When you run EzShops for the first time, configuration files are automatically generated in the `plugins/EzShops/` directory.

### Directory Structure
```text
plugins/EzShops/
├── config.yml                    # Main plugin configuration
├── shop.yml                      # Shop pricing and items
├── stock-gui.yml                 # Stock market GUI layout
├── shop-dynamic.yml              # Dynamic pricing state (auto-generated)
└── messages/                     # Localization files
```

---

## 📈 Dynamic Pricing

Dynamic pricing creates a living economy by adjusting item prices based on player activity.

**How does it refresh?**
- When players **sell** an item repeatedly, the supply increases, and the price **drops**.
- When players **buy** an item, the demand increases, and the price **rises**.
- **Normalization:** Prices do not stay low forever. Over time, as activity balances out, prices naturally trend back to their original base value defined in `shop.yml`.

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

---

## 🏪 Player Shops

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

---

## 📉 Stock Market

*(For a full deep-dive into the Stock Market system, check out the [Stock Market Documentation](../shops/pricing/stock-market.md))*

**All stock sales require confirmation through a GUI before the transaction is finalised.**

### Configuration (`config.yml`)

The stock section controls which items are tradeable and how the GUI presents them. Volatility and demand parameters are internal engine constants and are **not configurable** via `config.yml`.

```yaml
stock:
  # Master switch – set to false to disable all /stock commands and GUIs.
  enabled: true

  # Per-player cooldown between trades in milliseconds. 0 = no cooldown.
  cooldown-millis: 10000

  # Materials that cannot be traded on the stock market.
  blocked:
    - BEDROCK
    - COMMAND_BLOCK

  # Custom items to expose in the stock market.
  # 'display' sets the GUI label; 'base-price' is the reference price shown
  # in the all-stocks listing (the live trading price evolves independently).
  overrides:
    - id: "DIAMOND"
      display: "&bDiamond"
      base-price: 100.0

  # Optional: group items under named category tabs in the GUI.
  categories:
    gems:
      - DIAMOND
      - EMERALD
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | boolean | `true` | Enable/disable all stock features |
| `cooldown-millis` | integer | `0` | Milliseconds between player trades |
| `blocked` | list | `[]` | Materials blocked from trading |
| `overrides` | list | `[]` | Custom items with display names and reference prices |
| `categories` | map | `{}` | Named category → list of item groupings |

### Internal Price Engine

The following values are hardcoded engine constants and **cannot be changed in `config.yml`**:

| Constant | Value | Description |
|----------|-------|-------------|
| Starting price | 100.0 | Default price for any item not yet traded |
| Demand factor | ±2 % per unit | Price rises 2 % per unit bought, falls 2 % per unit sold |
| Random volatility | ±10 % per trade | Random noise added on top of demand factor |
| Price floor | 1.0 | Prices cannot drop below this value |
| Auto-save interval | 5 minutes | How often market prices are persisted to disk |

### Price Calculation Formula
```text
Per-unit change = (±2% demand factor) + random(−10%, +10%)
New price after N units = current × (1 + per-unit-change)^N,  floor at 1.0
```

---

## 🤝 Teams Integration

Requires [TeamsAPI ≥ 1.4.1](../integrations/teams-api). All options are ignored when TeamsAPI is absent.

```yaml
teams-integration:
  # Master switch. Set to false to disable all TeamsAPI features without
  # removing TeamsAPI from the server.
  enabled: true

  # Sell price multipliers applied per team role.
  # Value is the factor applied to the base sell price (1.0 = no bonus).
  sell-multiplier:
    member: 1.05   # +5 %
    admin:  1.10   # +10 %
    owner:  1.15   # +15 %

  # Buy price discounts applied per team role.
  # Value is the fraction subtracted from 1.0 (0.05 = 5 % cheaper).
  buy-discount:
    member: 0.05
    admin:  0.10
    owner:  0.15

  # Fraction of every sell transaction deposited into the team treasury.
  # 0.05 means 5 % of the sell revenue goes to the team pool.
  treasury-split: 0.05

  # When true, all team members share one combined stock pool.
  # When false, each player's stock is tracked individually (default behaviour).
  shared-stock: true
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | boolean | `true` | Enable/disable all team features |
| `sell-multiplier.member` | decimal | `1.05` | Sell bonus for members |
| `sell-multiplier.admin` | decimal | `1.10` | Sell bonus for admins |
| `sell-multiplier.owner` | decimal | `1.15` | Sell bonus for owners |
| `buy-discount.member` | decimal | `0.05` | Buy discount for members |
| `buy-discount.admin` | decimal | `0.10` | Buy discount for admins |
| `buy-discount.owner` | decimal | `0.15` | Buy discount for owners |
| `treasury-split` | decimal | `0.05` | Treasury deduction from each sell |
| `shared-stock` | boolean | `true` | Share stock pool across team |
