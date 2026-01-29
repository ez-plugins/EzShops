# Dynamic Pricing

This page explains EzShops' dynamic pricing system for server owners: what it does, how it behaves, how to configure it, and how to test it in-game.

**Summary**
- Dynamic pricing makes item buy/sell prices change over time based on player trades.
- Two systems use it in the plugin:
	- Shop pricing (`shop.yml`) — per-material `DynamicSettings` that modify a stored `multiplier`.
	- Stock market (`stock` commands / GUI) — a market price per product that moves on trades.
- The plugin uses per-unit multiplicative updates and provides a non-mutating estimator to compute bulk totals (the sum of progressively changing per-unit prices) for previews and GUI displays.

**Key concepts**
- Base unit price: the configured buy/sell price for a single item.
- Multiplier: a factor that scales the base price. Multipliers are clamped between configured `minMultiplier` and `maxMultiplier`.
- Per-unit change: each unit traded updates the multiplier (or market price) multiplicatively. For example, a 1% buyChange per unit will multiply the multiplier by 1.01 for each unit bought.
- Bulk estimate: when showing totals for N items, the plugin computes the sum of per-unit prices using the current multiplier/price and simulating N per-unit updates — without changing saved state. This produces a larger (progressive) total than simply `unit_price * N` when change ≠ 0.

Configuration
-------------

Dynamic pricing configuration for shop items is defined in `shop.yml` under each item using `dynamic-pricing` settings. Fields are:

- `starting-multiplier` (double): initial multiplier for the item (default 1.0).
- `min-multiplier` (double): minimum allowed multiplier (non-zero positive).
- `max-multiplier` (double): maximum allowed multiplier.
- `buy-change` (double): per-unit fractional change applied when buying. Example: `0.01` = +1% per unit.
- `sell-change` (double): per-unit fractional change applied when selling. Example: `0.01` = -1% per unit when selling.

Example `shop.yml` snippet:

```yaml
DIAMOND:
	buy: 100.0
	---

	## Dynamic Pricing (EzShops)

	Clear, concise documentation for server owners who run EzShops. This page explains how dynamic pricing works, how to configure it, and how to test it on your server.

	---

	### At a glance

	- Dynamic pricing makes buy/sell prices move based on player trades.
	- Shop items use per-item `DynamicSettings` (configured in `shop.yml`).
	- The stock market (via `/stock` and stock GUIs) simulates market prices and also supports per-unit effects.
	- The plugin shows progressive bulk totals (sum of changing per-unit prices) rather than `unit_price * amount`.

	### Why this matters

	Players expect bulk purchases to reflect rising prices when demand increases. Showing `unit_price * amount` can be misleading when each purchased unit raises the price of the next unit. EzShops simulates per-unit effects and shows accurate bulk totals so players see a consistent preview and are charged the expected amount.

	---

	### Quick example (recommended)

	In `shop.yml`:

	```yaml
	DIAMOND:
		buy: 100.0
		sell: 80.0
		dynamic-pricing:
			starting-multiplier: 1.0
			min-multiplier: 0.5
			max-multiplier: 5.0
			buy-change: 0.01    # +1% per unit bought
			sell-change: 0.01   # -1% per unit sold
	```

	With the settings above, buying 10 diamonds does not simply charge `100 * 10`. Instead the plugin charges a progressive total where each unit is slightly more expensive than the previous one.

	---

	### Configuration reference

	Edit `shop.yml` entries under each material. The relevant `dynamic-pricing` fields:

	- `starting-multiplier` (double) — initial multiplier (default: `1.0`).
	- `min-multiplier` (double) — minimum allowed multiplier.
	- `max-multiplier` (double) — maximum allowed multiplier.
	- `buy-change` (double) — per-unit fractional increase when buying (e.g. `0.01` = +1%).
	- `sell-change` (double) — per-unit fractional decrease when selling (e.g. `0.01` = -1%).

	### Per-item price keys (`price-id`) (optional)

	To allow multiple independent price entries for the same Minecraft `Material` (for example two different shop items that both use `EXPERIENCE_BOTTLE`), items may declare an optional `price-id` string in their configuration. When present, the plugin uses this `price-id` as the pricing key (and as the dynamic-pricing state key stored in `shop-dynamic.yml`) instead of the material name.

	Notes:
	- `price-id` is optional; if omitted the material name (e.g. `DIAMOND`, `EXPERIENCE_BOTTLE`) is used and pricing remains material-scoped (backwards compatible).
	- `price-id` must be unique across your shop configuration if you want independent pricing/multipliers for two items that share the same material.
	- Dynamic state (multipliers) are saved by `price-id` in `shop-dynamic.yml` when present; legacy material keys continue to be supported.

	Example (category item):

	```yaml
	my-exp-bottle-item:
		material: EXPERIENCE_BOTTLE
		price-id: exotic_exp_1   # optional unique key to separate pricing
		buy: 10.0
		sell: 5.0
		dynamic-pricing:
			starting-multiplier: 1.0
			min-multiplier: 0.5
			max-multiplier: 3.0
			buy-change: 0.01
			sell-change: 0.01
	```

	Troubleshooting tip: if two shop items using the same `Material` show the same price or share dynamic changes, add distinct `price-id` values to each item to separate them.

	Stock market tuning is in `stock-gui.yml` / `config.yml` (see `StockMarketConfig`). The stock market uses a deterministic per-unit demand factor (default `0.02`) plus a configurable random component for flavor.

	---

	### How it works (technical summary)

	- The shop stores a `multiplier` per item and applies multiplicative updates per traded unit:

		multiplier := clamp(multiplier * (1 + buyChange)) on buys

		multiplier := clamp(multiplier * (1 - sellChange)) on sells

	- Bulk totals are computed by simulating N per-unit updates and summing the per-unit prices. The estimator does not mutate saved state — it is only used for previews and GUI displays.

	---

	### Formulae

	- After `N` buys (starting multiplier `M0`):

		M_N = clamp(M0 * (1 + buyChange)^N)

	- Bulk total for base unit price `P`:

		total = sum_{i=0..N-1} normalizeCurrency( P * M0 * (1 + buyChange)^i )

	`normalizeCurrency` is the plugin's rounding/normalization for currency values.

	---

	### Commands & testing (in-game)

	Build & install:

	```bash
	mvn clean package -DskipTests
	cp target/*.jar /path/to/paper/plugins/
	# restart or reload server
	```

	Basic checks:

	- Confirm plugin loaded: `/plugins` and look for `EzShops`.
	- Open the shop GUI and inspect items with `dynamic-pricing` configured — bulk lines use `{buy_bulk_total}` and `{sell_bulk_total}` placeholders.

	Preview (stock market):

	```text
	/stock preview buy DIAMOND 64
	```

	This prints per-unit price and an estimated total. The estimator is deterministic (no randomness) so previews are stable.

	Execute trade:

	```text
	/stock buy DIAMOND 64
	```

	The plugin charges the estimated total (based on progressive per-unit prices) and updates the stored market price accordingly.

	---

	### UI notes

	- The confirmation GUI for stock transactions shows totals for the following amounts by default: `1, 8, 16, 32, 64`.
	- Administrators can change the GUI files or request an enhancement to make these amounts configurable via `stock-gui.yml`.

	---

	### Admin tips & tuning

	- Use smaller `buy-change` / `sell-change` for gentle price movement (e.g. `0.002`).
	- Use `min-multiplier` / `max-multiplier` to limit extreme swings.
	- Cap preview amounts in GUI to avoid heavy computation on very large inputs.

	---

	### Migration & compatibility

	- Existing `shop-dynamic.yml` files are preserved. Multipliers already stored will be used as the starting point after upgrade.
	- If you rely on additive semantics, consider adding a config flag `dynamic-pricing.mode` (not currently present) or keep a fallback branch.

	---

	### Troubleshooting

	- If bulk totals appear incorrect:
		- Ensure `shop-dynamic.yml` exists and contains expected multipliers.
		- Check console logs for errors during startup or trade handling.
		- Verify `shop.yml` entries are valid and that the material keys match (`DIAMOND`, `STONE`, etc.).

	### Menu pagination and per-item pages

	EzShops supports assigning shop items to a specific GUI page and improves pagination behavior for category menus.

	- `page` (integer, optional): assign an item to a specific 1-based page inside its category menu. If omitted the item is auto-paginated.
	- `preserve-last-row` (boolean, optional, category `menu` block): when `true` (default) the entire last inventory row is reserved for navigation and back buttons; items are not auto-filled into that row. Set to `false` to allow auto-filling into the last row.

	Notes:
	- Page numbers are 1-based in configuration. For example `page: 2` places the item on the second page of the category menu.
	- The menu's `size` (e.g. `54`) and reserved last row determine how many item slots are available per page; explicit `slot` values must be within the menu bounds.
	- If an explicit `slot` for an item collides with another item or a reserved slot, the composer will attempt to place it on that page; if it cannot, the item will be deferred into the remaining available slots for that page.

	Example category snippet:

	```yaml
	categories:
		building:
			name: "Building"
			menu:
				title: "Building"
				size: 54
				preserve-last-row: true
			items:
				tuff_brick_stairs:
					material: TUFF_BRICK_STAIRS
					slot: 1
					page: 2
					amount: 16
					buy: 56.0
					sell: 26.0
	```

	This configuration places `tuff_brick_stairs` on the second page at slot `1`. Setting `preserve-last-row: false` allows the plugin to use the whole inventory for items (useful for compact menus where you don't need a dedicated navigation row).

	- If players report surprising totals, reduce `buy-change`/`sell-change` and test again.

	---

	### Contributing & support

	If you want enhancements (config-mode toggle for additive vs multiplicative, configurable GUI amounts, extra debug commands, unit tests), open an issue or a PR in the repository. Include example configs and expected behavior.

	For quick debugging: check `shop-dynamic.yml` on the server and use `/stock preview` to verify estimator outputs.