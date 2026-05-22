---
layout: default
title: Stock Market
parent: Shops
nav_order: 4
description: "Volatile market system driven by player activity."
---

# 📉 Stock Market System

The Stock Market system introduces a global economic layer where item prices fluctuate based on volatility and player demand.

---

## ⚙️ Configuration

Configure the stock market in the `stock:` section of `config.yml`. All price-engine parameters are fully configurable.

```yaml
stock:
  # Master switch. Set to false to disable all /stock commands and GUIs.
  enabled: true

  # Per-player cooldown between trades in milliseconds. 0 = no cooldown.
  cooldown-millis: 10000

  # Price volatility range: random noise applied per trade (fraction of price).
  volatility-min: -0.10
  volatility-max: 0.10

  # Demand multiplier: fraction of price change per unit bought (+) or sold (-).
  demand-multiplier: 0.02

  # Minimum price floor: prices cannot drop below this value.
  min-price: 1.0

  # How often (in minutes) market prices are saved to disk.
  update-interval: 5

  # Materials that cannot be traded on the stock market.
  blocked:
    - BEDROCK
    - COMMAND_BLOCK

  # Custom items to expose in the stock market.
  # 'display'    – label shown in the all-stocks GUI listing.
  # 'base-price' – reference price shown in the all-stocks listing;
  #                the live trading price evolves independently.
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
| `volatility-min` | decimal | `-0.10` | Lower bound of random volatility per trade (-10 %) |
| `volatility-max` | decimal | `0.10` | Upper bound of random volatility per trade (+10 %) |
| `demand-multiplier` | decimal | `0.02` | Price change per unit traded (2 % per unit) |
| `min-price` | decimal | `1.0` | Absolute price floor |
| `update-interval` | integer | `5` | Minutes between automatic price saves to disk |
| `blocked` | list | `[]` | Materials blocked from trading |
| `overrides` | list | `[]` | Custom display names and reference prices |
| `categories` | map | `{}` | Named category → list of item groupings |

---

## 🧮 Price Calculation Formula

The engine applies a **per-unit multiplicative update** each time an item is bought or sold.

| Value | Default | Description |
|-------|---------|-------------|
| Starting price | 100.0 | Default price for any item not yet traded |
| Demand factor | ±2 % per unit | `demand-multiplier` — +2 % per unit bought, −2 % per unit sold |
| Random volatility | ±10 % per trade | Drawn from `[volatility-min, volatility-max]` |
| Price floor | 1.0 | `min-price` — prices cannot drop below this value |
| Auto-save interval | 5 minutes | `update-interval` — how often prices are written to disk |

```text
Per-unit change = (±demand-multiplier) + random(volatility-min, volatility-max)
New price after N units = current × (1 + per-unit-change)^N,  floor at min-price
```