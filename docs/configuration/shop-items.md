# 📦 Shop Items & Pricing (shop.yml)

The `shop.yml` file is where you define every item available in your server's shop, along with its base price and special properties.

---

## 💰 Basic Item Configuration

Each item is identified by its Minecraft Material Name.

```yaml
items:
  DIAMOND:
    buy: 100.0          # Price to buy from shop
    sell: 50.0          # Price received when selling
    
  SPAWNER:
    buy: 10000.0
    sell: -1            # -1 means the item cannot be sold