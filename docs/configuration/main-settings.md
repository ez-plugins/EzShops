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

## 📉 Stock Market & Price Calculation

*(For a full deep-dive into the Stock Market system, check out the [Stock Market Documentation](../shops/pricing/stock-market.md))*

**Version 2.0.0+ Security Improvements:**
- All stock sales now require confirmation through a GUI
- Fixed infinite money glitch vulnerability
- Enhanced transaction validation

### Available Settings Overview
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

### Price Calculation Formula
The stock market calculates price fluctuations using the following formula:
```text
New price = max(min-price, current price × (1 + (demand × demand-multiplier) + random volatility))
```