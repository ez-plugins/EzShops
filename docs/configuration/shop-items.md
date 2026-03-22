# 📦 Shop Items & Pricing (shop.yml)

The `shop.yml` file is where you define every item available in your server's shop, along with its base price and special properties.

---

## 💰 Basic Item Configuration

Each item is identified by its Minecraft Material Name. Here is how a basic setup looks:

```yaml
items:
  DIAMOND:
    buy: 100.0          # Price to buy from shop
    sell: 50.0          # Price received when selling
    
  SPAWNER:
    buy: 10000.0
    sell: -1            # -1 means the item cannot be sold
```

---

## ⚙️ Available Settings Overview

Below is the detailed list of configuration options available for each item. This allows you to customize everything from GUI positioning to dynamic pricing behavior.

### Buy Price (`buy`)
**Type:** `number`
The price a player must pay to purchase this item from the shop. Set this to `-1` if you want the item to be unbuyable.

### Sell Price (`sell`)
**Type:** `number`
The amount of money a player receives when selling this item to the shop. Set this to `-1` to prevent players from selling it.

### GUI Slot (`slot`)
**Type:** `number`
The specific slot position in the category GUI where this item will appear.

### Material (`material`)
**Type:** `string`
The valid Minecraft Material name (e.g., `DIAMOND_SWORD`, `STONE`).

### Display Name (`display-name`)
**Type:** `string`
A custom name for the item shown in the GUI. This fully supports standard Minecraft color codes.

### Lore (`lore`)
**Type:** `list`
A multi-line description shown when a player hovers over the item in the GUI.

### Amount (`amount`)
**Type:** `number`
The stack size given per single purchase (Default is 1).

### Permission (`permission`)
**Type:** `string`
A custom permission node required for a player to interact with (buy/sell) this item.

### Stock Market (`stock-market`)
**Type:** `boolean`
If set to `true`, this item's price will fluctuate based on the global stock market volatility and demand settings.

### Dynamic Pricing (`dynamic.enabled`)
**Type:** `boolean`
Enables or disables individual supply/demand dynamic pricing specifically for this item.

### Item Type (`item-type`)
**Type:** `string`
Defines the delivery method of the purchase. 
- `ITEM` (default): Gives the physical item.
- `COMMAND`: Does not give an item, but executes commands configured in `on-buy`.
- `NONE`: Charges the player but delivers nothing (useful for custom external triggers).

---

## 🚀 Specialized Item Types

### Command-Based Items
You can sell "actions" instead of physical items by setting the `item-type` to `COMMAND`.

```yaml
items:
  GOLD_INGOT:
    display-name: "&6VIP Rank"
    item-type: "COMMAND"
    buy: 5000.0
    on-buy:
      - "lp user {player} parent add vip"
```