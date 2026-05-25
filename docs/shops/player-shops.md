---
Examples — unified storage block

```yaml
player-shops:
  storage:
    # one of: jaloquent | mysql | sqlite
    type: jaloquent

    # Common option for table prefix used by SQL backends
    table-prefix: "ez_"

    # MySQL configuration (used when type = mysql)
    mysql:
      host: localhost
      port: 3306
      database: minecraft
      username: root
      password: "secret"

    # SQLite configuration (used when type = sqlite)
    sqlite:
      # Path to the SQLite file (relative or absolute)
      file: plugins/EzShops/ezshops.sqlite

    # Jaloquent configuration (used when type = jaloquent)
    # Provide a full JDBC URL or host/port/database below.
    jaloquent:
      # url: jdbc:mysql://localhost:3306/minecraft?useSSL=false&serverTimezone=UTC
      host: localhost
      port: 3306
      database: minecraft
      username: root
      password: "secret"
```

Notes:
- `jaloquent` is the recommended default. The adapter preserves the same
  table schema and YAML item-serialization used historically by the MySQL
  backend and uses JDBC under the hood.
- If the configured backend cannot be initialised at startup the plugin will
  automatically fall back to the YAML file backend (`player-shops.yml`).
- If you use a custom JDBC driver or a non-MySQL database, ensure the driver
  is accessible to the plugin; the bundled Jaloquent integration covers the
  typical MySQL case.
- Item stacks are serialised to YAML in the database, and the repository
  preserves entries for shops whose worlds are not currently loaded.

---

## Sign Appearance

EzShops automatically writes four lines onto the sign:

| Line | Default Content | Example |
|------|----------------|---------|
| 1 | Available/Out-of-stock header | `[PlayerShop]` (green) or `[PlayerShop]` (red) |
| 2 | Shop owner's name | `Steve` |
| 3 | Quantity × item name | `3x Sharpness V Book` |
| 4 | Price (or "Out of Stock") | `$25.00` |

### Smart Item Names

EzShops automatically generates descriptive item names on signs:

- **Potions** — shows the specific effect, e.g. `Strength Potion`, `Poison Splash Pot`, `Healing Ling. Pot`
- **Enchanted Books** — shows the first stored enchantment with Roman numeral level, e.g. `Sharpness V Book`, `Unbreaking III Book`
- **Custom display names** — if an item has a custom display name (set via rename anvil, for example), that name is used instead (color codes are stripped to fit the sign)
- **All other items** — formatted material name, e.g. `Diamond`, `Oak Log`

---

## Commands

### `/playershop`

Opens the player shop setup GUI to configure quantity, price, and item.

**Usage:** `/playershop`  
**Permission:** `ezshops.playershop.create`  
**Aliases:** `/pshop`

### `/playershop remove`

Removes the player shop sign you are currently looking at.

**Usage:** `/playershop remove`  
**Permission:** `ezshops.playershop.create` (own shops) or `ezshops.playershop.admin` (any shop)

### `/playershops`

Open a paginated GUI that lets players browse all active player shops on the server and purchase directly from them without visiting each chest.

**Usage:** `/playershops [page]`  
**Permission:** `ezshops.playershop.browse`

---

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `ezshops.playershop.create` | `op` | Create and remove own player shops |
| `ezshops.playershop.buy` | `true` | Buy from player shops |
| `ezshops.playershop.admin` | `op` | Remove any player shop (admin override) |
| `ezshops.playershop.browse` | `true` | Browse all active player shops via `/playershops` |

---

## Configuration

All player shop settings live under the `player-shops:` section of `config.yml`.

```yaml
player-shops:
  # Master switch — set to false to disable all player shop features.
  enabled: true

  # Sign header tokens that trigger shop creation.
  # All tokens are case-insensitive and color-code-stripped before matching.
  headers:
    - "[playershop]"

  # Whether the chest must already contain enough items when the shop is created.
  require-stock-on-creation: true

  # Chest/sign protection: prevent other players from breaking the chest or sign.
  protection-enabled: true

  # Quantity limits. 0 = no upper limit.
  min-quantity: 1
  max-quantity: 0

  # Price limits. 0.0 = no limit.
  min-price: 0.01
  max-price: 0.0

  # Sign format — controls how the four sign lines look.
  # Placeholders: {owner}, {amount}, {item}, {price}
  # Supports legacy color codes (&a, &b, etc.)
  sign-format:
    available-header: "&2[PlayerShop]"
    out-of-stock-header: "&c[PlayerShop]"
    owner-format: "&7{owner}"
    unknown-owner-name: "Owner"
    item-format: "&b{amount}&7x &b{item}"
    price-format: "&6{price}"
    out-of-stock-line: "&cOut of Stock"

  # Optional message customisation (all keys under messages:)
  messages:
    creation-success: "&aShop created successfully!"
    purchase-buyer-success: "&aPurchased &b{item}&a from &b{seller}&a for &6{price}&a."
    # ... (see Localization for the full list)
```

### Storage Backend

By default EzShops uses the `jaloquent` backend (recommended). The plugin bundles Jaloquent and a MySQL JDBC driver by default so no server-side JDBC driver installation is required in most cases. If the configured backend cannot be initialised at startup the plugin will automatically fall back to the YAML file backend (`player-shops.yml`).

Unified database configuration
-----------------------------

You can centralise all SQL connection settings under a top-level `database:`
section in `config.yml`. When present this block is preferred by the plugin
and will be used for player shops, transactions, and other SQL-backed
features. Per-feature `player-shops.storage.*` blocks remain supported for
backwards compatibility but are no longer required when `database:` is set.

Example — centralised database settings:

```yaml
database:
  # Optional full JDBC URL (preferred when you need custom params)
  # url: jdbc:mysql://localhost:3306/minecraft?useSSL=false&serverTimezone=UTC
  host: localhost
  port: 3306
  database: minecraft
  username: root
  password: "secret"
  table-prefix: "ez_"
```

Example — Jaloquent (default):

```yaml
player-shops:
  storage:
    type: jaloquent
    jaloquent:
      # Full JDBC URL (optional) or host/port/database below
      # url: jdbc:mysql://localhost:3306/minecraft?useSSL=false&serverTimezone=UTC
      host: localhost
      port: 3306
      database: minecraft
      username: root
      password: "secret"
      table-prefix: "ez_"
```

Example — MySQL (alternative):

```yaml
player-shops:
  storage:
    type: mysql
    mysql:
      host: localhost
      port: 3306
      database: minecraft
      username: root
      password: "secret"
      table-prefix: "ez_"
```

Notes:
- The Jaloquent adapter preserves the same table schema and YAML item-serialization used historically by the MySQL backend.
- If you provide a custom JDBC driver or use a different database, ensure the driver is compatible with the bundled Jaloquent integration.
- Item stacks are serialised to YAML in the database, and the repository preserves entries for shops whose worlds are not currently loaded.

### Jaloquent backend

EzShops also supports an Eloquent-style backend using Jaloquent. Set `player-shops.storage.type` to `jaloquent` and configure the `jaloquent` block. The plugin bundles Jaloquent and a MySQL JDBC driver by default, so no server-side driver installation is required unless you prefer a different driver.

```yaml
player-shops:
  storage:
    type: jaloquent
    jaloquent:
      # Full JDBC URL (optional) or host/port/database below
      # url: jdbc:mysql://localhost:3306/minecraft?useSSL=false&serverTimezone=UTC
      host: localhost
      port: 3306
      database: minecraft
      username: root
      password: "secret"
      table-prefix: "ez_"
```

Notes:
- The Jaloquent adapter uses JDBC under the hood and preserves the same table schema and YAML item-serialization used by the MySQL backend.
- If the plugin cannot initialise the Jaloquent adapter at startup it will fall back to the YAML backend automatically.

### `sign-format` Reference

| Key | Placeholders | Description |
|-----|-------------|-------------|
| `available-header` | — | Line 1 when shop has stock |
| `out-of-stock-header` | — | Line 1 when shop is out of stock |
| `owner-format` | `{owner}` | Line 2 — shop owner's name |
| `unknown-owner-name` | — | Fallback when owner's name cannot be resolved |
| `item-format` | `{amount}`, `{item}` | Line 3 — quantity and item name |
| `price-format` | `{price}` | Line 4 when shop has stock |
| `out-of-stock-line` | — | Line 4 when shop is out of stock |

---

## How Purchases Work

1. A buyer **right-clicks** the shop sign.
2. EzShops checks the buyer's permissions (`ezshops.playershop.buy`) and inventory space.
3. The configured price is **withdrawn** from the buyer's Vault balance.
4. The items are **removed** from the chest and **given** to the buyer.
5. The price is **deposited** into the shop owner's Vault account (works for offline players too).
6. The sign is refreshed — if stock runs out, the header turns red and line 4 shows "Out of Stock".
7. The **shop owner receives a notification** if they are currently online.

### Failure cases

| Situation | Behaviour |
|-----------|-----------|
| Chest is missing | Shop is automatically removed |
| Insufficient stock | Transaction cancelled; sign updated to "Out of Stock" |
| Buyer has no inventory space | Transaction cancelled; no money deducted |
| Economy error | Transaction cancelled; any partial payments refunded |

---

## Double Chests & Barrels

Player shops support **double chests** and **barrels** as the backing container. When you attach a shop sign to one half of a double chest, EzShops automatically tracks both halves so the entire double-chest stock is counted.

---

## Tips & Best Practices

- **Pre-select items with `/playershop`** before placing the sign when selling items like potions or enchanted books so the shop matches exactly the variant you intend to sell.
- Use **`protection-enabled: true`** (default) to prevent other players from breaking your chest or sign.
- The shop owner does **not** need to be online for purchases to work — payments go directly to their Vault account.
- If you resize a double chest or break and replace it, remove and recreate the shop sign to re-register the chest locations.

---

## See Also

- [Sign Shops](../configuration/main-settings) — for admin-managed buy/sell signs
- [Permissions Reference](../permissions) — full permission node list
- [Localization](../configuration/localization) — customise all player shop messages
- [Main Settings](../configuration/main-settings) — full `config.yml` reference including `player-shops:`
