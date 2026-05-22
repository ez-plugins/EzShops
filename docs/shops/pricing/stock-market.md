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

Configure the stock market in the `stock:` section of `config.yml`. Volatility and demand constants are **built into the engine** and are not exposed as config options.

```yaml
stock:
  # Master switch. Set to false to disable all /stock commands and GUIs.
  enabled: true

  # Per-player cooldown between trades in milliseconds. 0 = no cooldown.
  cooldown-millis: 10000

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
| `blocked` | list | `[]` | Materials blocked from trading |
| `overrides` | list | `[]` | Custom display names and reference prices |
| `categories` | map | `{}` | Named category → list of item groupings |

---

## 🧮 Price Calculation Formula

The engine applies a **per-unit multiplicative update** each time an item is bought or sold. The following constants are hardcoded and cannot be changed in `config.yml`:

| Constant | Value | Description |
|----------|-------|-------------|
| Starting price | 100.0 | Default price for any item not yet traded |
| Demand factor | ±2 % per unit | +2 % per unit bought, −2 % per unit sold |
| Random volatility | ±10 % per trade | Random noise added on top of the demand factor |
| Price floor | 1.0 | Prices cannot drop below this value |
| Auto-save interval | 5 minutes | How often prices are written to disk |

```text
Per-unit change = (±2% demand factor) + random(−10%, +10%)
New price after N units = current × (1 + per-unit-change)^N,  floor at 1.0
```