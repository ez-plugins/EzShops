# 📉 Stock Market System

The Stock Market system introduces a global economic layer where item prices fluctuate based on volatility and player demand.

---

## ⚙️ Configuration Options

Configure the stock market behavior in your `config.yml`.

```yaml
stock-market:
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