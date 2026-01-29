**Price-ID**

- **What:** A configurable per-item identifier used to separate pricing and dynamic state from the Minecraft `Material` name.
- **Why:** Two or more shop items can use the same `Material` (for example, `EXPERIENCE_BOTTLE`) but should have independent prices and dynamic multipliers. `price-id` provides a stable key for dynamic pricing and persistence.

**Behavior & compatibility**
- `price-id` is optional. If omitted the plugin uses the material name (e.g. `EXPERIENCE_BOTTLE`) as the price key for backward compatibility.
- When provided, `price-id` becomes the canonical identifier for pricing, dynamic multipliers, and persistence in `shop-dynamic.yml`.
- The `price-id` value is treated case-insensitively for matching in commands and tab-completion.

**Where it is used**
- In item definitions in `shop/*` category files where you configure items and their buy/sell prices.
- Dynamic pricing state (multipliers, last-updated, stock adjustments) is stored under the `price-id` key.
- Command-based purchases/sales accept `price-id` or the item id as an alternative to `Material` names (e.g. `/shop buy my_experience_bottles 5`).

**Example item configuration**
```yaml
items:
  exp_buy_small:
    material: EXPERIENCE_BOTTLE
    display-name: "Small XP Bottle"
    buy: 10.0
    sell: 5.0
    price-id: exp_small_2026
    page: 1
    slot: 10

  exp_buy_large:
    material: EXPERIENCE_BOTTLE
    display-name: "Large XP Bottle"
    buy: 50.0
    sell: 30.0
    price-id: exp_large_2026
    page: 2
    slot: 9
```

In this example the two `EXPERIENCE_BOTTLE` entries maintain independent dynamic multipliers and persisted state because they each declare distinct `price-id` values.

**Commands & tab-completion**
- The `/shop buy <item>` and `/shop sell <item>` chat commands accept either a `Material` name (e.g. `DIAMOND`), the configured item id (e.g. `exp_buy_small`), or the `price-id` (e.g. `exp_small_2026`).
- Tab completion includes material names plus configured item ids and `price-id` values to make command entry easier.

**Dynamic state and migration notes**
- Existing dynamic state keyed by material names continues to work when no `price-id` is set.
- If you add a `price-id` to an existing item and want to preserve previously recorded dynamic state, choose the same string as the previous material name (or manually move the entry in `shop-dynamic.yml`).

**Best practices**
- Use short, descriptive, and unique `price-id` values (letters, digits, underscores). Avoid characters that may confuse command parsing (spaces, colons).
- Prefer a stable `price-id` if you plan to rename the item id or display name later — the `price-id` is the persistent key used for pricing history and adjustments.

**Troubleshooting**
- If items show `N/A` prices in the GUI, ensure the configured `price-id` matches the key used in your dynamic pricing store (or omit `price-id` to use material name fallback).
- If an item is not matched by `/shop buy` tab-completion, try its `Material` name or item id; `price-id` completion is also supported.

For more details on dynamic pricing mechanics and persistence see: [docs/shops/pricing/dynamic-pricing.md](docs/shops/pricing/dynamic-pricing.md).
