# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.5.8] - 2026-07-06

### Fixed
- **Config persistence after reloads** — `/shop reload` now reloads `config.yml` from disk before refreshing pricing/features, and setup GUI toggles now reload config before saving. This prevents stale in-memory values from being written back over server-owner edits.
- **Shop category parser resilience** — category parsing now isolates failures per category so one malformed entry can no longer break loading of the category set.
- **Duplicate-material shop entries** — when multiple items intentionally share the same material (for example splash potion variants), pricing keys are now resolved per-item to prevent collisions that could cause broken/empty category behavior.
- **List-style `on-buy` / `on-sell` hooks now execute** — command hook parsing now accepts both list syntax (`on-buy: - "cmd"`) and nested syntax (`on-buy.commands`) for compatibility with the documented format, preventing silent no-op purchases for `item-type: COMMAND` entries.

## [2.5.7] - 2026-05-23

### Added
- **Player Shops documentation**: a dedicated [Player Shops guide](docs/shops/player-shops.md) covering the full setup flow (direct sign placement and `/playershop` GUI), commands, permissions, configuration reference, purchase flow, double-chest support, and tips.
- **Player Shops: browse GUI**: added a `/playershops` command that opens a paginated, in-game inventory allowing players to browse all active player shops and purchase items without visiting each chest.
- **Player Shops: MySQL storage**: added `MysqlPlayerShopRepository` and support for `player-shops.storage.type: mysql` (configurable `host`, `port`, `database`, `username`, `password`, `table-prefix`). The repository serialises item stacks as YAML and preserves deferred entries for worlds that are not loaded; the plugin falls back to the YAML backend if MySQL is unavailable at startup.
- **Repository factory & wiring**: `PlayerShopComponent` now selects the configured storage backend (YAML or MySQL) at startup and registers the browse GUI and `/playershops` command when player shops are enabled.
- **API additions**: `PlayerShopManager` exposes `hasStock(PlayerShop)` and `formatPrice(double)` (public) to support the browse GUI and other integrations.
- **Permissions & commands**: added the `playershops` command and the `ezshops.playershop.browse` permission (default: true).

### Changed
- **Build target baseline**: Maven compiler settings now target Java 17 (`source`/`target` via `java.version`), replacing the previous Java 21 compile target in `pom.xml`.
- **Paper API baseline for Java 17 builds**: default `paper.version` changed to `1.20.6-R0.1-SNAPSHOT` to align with the Java 17 compatibility target.
- **EzFramework dependency alignment**: pinned and updated persistence stack dependencies in `pom.xml`:
  - `com.github.EzFramework:jaloquent` -> `1.3.3`
  - `com.github.EzFramework:JavaQueryBuilder` -> `1.2.1`
  - `com.github.EzFramework:Jaker` -> `1.0.7`

### Fixed
- **Incorrect `sign-format` keys in `main-settings.md`** — the Player Shops configuration section documented non-existent keys (`header`, `owner-line`, `item-line`, `stock-line`, `price-line`, `{stock}` placeholder). It now documents the real keys (`available-header`, `out-of-stock-header`, `owner-format`, `unknown-owner-name`, `item-format`, `price-format`, `out-of-stock-line`) with the correct placeholders (`{owner}`, `{amount}`, `{item}`, `{price}`).

## [2.5.6] - 2026-05-22

Through time we've made the stock market more stable, by doing this the documentation got outdated and configuration options that existed before got phased out. This version there was focus on updating the documentation and adding back options that got phased out in a more stable way.

### Added
- **Configurable stock price-engine parameters** - `volatility-min`, `volatility-max`, `demand-multiplier`, `min-price`, and `update-interval` are now real `config.yml` options under the `stock:` section. The plugin reads them on startup and applies them to the price engine. All defaults match the previously hardcoded values so existing behaviour is preserved.

### Fixed
- **Incorrect stock-market configuration documented** - `docs/configuration/main-settings.md` and `docs/shops/pricing/stock-market.md` previously documented a non-existent `stock-market:` config block. Both pages now document the real `stock:` section (`enabled`, `cooldown-millis`, `blocked`, `overrides`, `categories`). Price-engine parameters (`volatility-min`, `volatility-max`, `demand-multiplier`, `min-price`, `update-interval`) are now implemented as real config options (see Unreleased → Added).
- **`/sell` command missing from documentation** - the Quick Sell GUI command (`/sell`) is now documented in the Commands reference with its behavior and permission node.
- **`/shopadmin` command missing from documentation** - `/shopadmin [browse|market]` is now documented under Admin Commands including both the player-shops and team-market views.
- **`/teamshop market` subcommand missing from documentation** - the team P2P market subcommand is now documented in Commands, the TeamsAPI integration page, and the tab-completion list for `/teamshop`.
- **Missing permission nodes in documentation** - the following nodes were present in `plugin.yml` but absent from the Permissions reference; they are now documented:
  - `ezshops.shop.admin` (open `/shopadmin` GUI)
  - `ezshops.teamshop.market` (access team P2P market)
  - `ezshops.pricing.admin.set`, `ezshops.pricing.admin.disable`, `ezshops.pricing.admin.list` (granular pricing-admin nodes)

## [2.5.5] - 2026-05-18

### Fixed
- **`on-sell` commands not executing for `item-type: COMMAND` items** — `sell()` no longer checks or removes physical items from the player's inventory when the item's delivery type is `COMMAND`. Previously the transaction exited early with "insufficient items" because the player had no material to hand over, preventing sell commands from running at all.
- **`on-sell execute-as` overridden by `on-buy execute-as`** — `ShopPricingManager` now tracks `execute-as` independently for the `on-buy` and `on-sell` blocks. Previously a single shared flag meant that setting `on-buy: execute-as: player` would silently override `on-sell: execute-as: console`, causing sell commands to run as the player instead of the console.

### Added
- **Code coverage reporting** — JaCoCo is now configured in the Maven build (`jacoco-maven-plugin 0.8.12`). Coverage reports (`jacoco.xml`) are generated on every `mvn test` run and uploaded to Codecov by the CI workflow for both unit-test and feature-test jobs.

## [2.5.4] - 2026-05-14

### Fixed
- **Quick Sell GUI "Nothing to sell" after shift-click** — `handleConfirm` now calls a new `ShopTransactionService.sellDirect()` method that skips the player-inventory item count/removal steps. Previously, shift-clicking items into the GUI moved them out of the player's inventory, so the old `sell()` path found zero items and reported nothing to sell.
- **Quick Sell GUI shows "Nothing to sell" when sell fails after shift-click** — `handleConfirm` now distinguishes between a genuinely empty GUI and a GUI that has items but whose `sellDirect` call failed (e.g. economy down, dynamic price driven to $0.00 by a previous sale, rotation expired). The actual failure reason is shown to the player instead of the misleading "No items to sell." message. Items that failed to sell remain in the GUI so the player can retry.
- **Quick Sell GUI rejects rotation items even when directly sellable** — `sellDirect` and `isSellable` both applied a rotation-visibility check that belongs only in the main shop menu. The Quick Sell GUI is designed to accept any item with a configured sell price; rotation restrictions are now only enforced in `sell()`, which is the path used when a player types `/sell` or `/sellhand`.
- **Legacy `shop.yml` item keys not found when using lowercase material names** — `loadLegacyEntries` now normalises every price key to `Material.name()` (e.g. `BIRCH_LOG`) before registering it in the price map. Previously, a config entry written as `birch_log:` was stored under the lowercase key, so `getPrice(Material.BIRCH_LOG)` could not find it, silently treating the item as unpriced.

### Added
- **Stripped log variants in the wood category** — all 9 stripped log types (`STRIPPED_OAK_LOG`, `STRIPPED_SPRUCE_LOG`, `STRIPPED_BIRCH_LOG`, `STRIPPED_JUNGLE_LOG`, `STRIPPED_ACACIA_LOG`, `STRIPPED_DARK_OAK_LOG`, `STRIPPED_MANGROVE_LOG`, `STRIPPED_PALE_OAK_LOG`, `STRIPPED_CHERRY_LOG`) are now included in the default `wood.yml` config so players can sell stripped logs with `/sellhand` and the Quick Sell GUI out of the box.
- **Wood (all-bark) block variants in the wood category** — all 9 wood block types (`OAK_WOOD`, `SPRUCE_WOOD`, `BIRCH_WOOD`, `JUNGLE_WOOD`, `ACACIA_WOOD`, `DARK_OAK_WOOD`, `MANGROVE_WOOD`, `PALE_OAK_WOOD`, `CHERRY_WOOD`) are now included. Previously, attempting to `/sellhand` a "Birch Wood" block (as opposed to a "Birch Log") would return "That item is not configured in the shop."
- **Plank variants in the building category** — all 9 plank types (`OAK_PLANKS`, `SPRUCE_PLANKS`, `BIRCH_PLANKS`, `JUNGLE_PLANKS`, `ACACIA_PLANKS`, `DARK_OAK_PLANKS`, `MANGROVE_PLANKS`, `PALE_OAK_PLANKS`, `CHERRY_PLANKS`) are now included in the default `building.yml` config.

## [2.5.3] - 2026-05-14

### Fixed
- **TeamTreasury transaction safety** — `deposit` now refunds the player's Vault balance if the YAML save fails, preventing money from being lost on a failed write. `withdraw` debits the treasury *before* paying the player so a failed save aborts the operation without creating currency from nothing.
- **TeamStockManager negative-amount guard** — `removeTeamStock` now returns `false` immediately when `amount <= 0`, preventing invalid stock mutations.

## [2.5.2] - 2026-05-12

### Fixed
- **TeamsAPI optional integration** — `NoClassDefFoundError` no longer crashes plugin startup when TeamsAPI is absent from the classpath. The availability check is now wrapped in a `NoClassDefFoundError` catch block so the integration degrades gracefully whether the JAR is missing entirely or the plugin is simply not loaded.

## [2.5.1] - 2026-05-12

### Added
- **Folia support** — EzShops now runs on Folia servers. All scheduler calls are routed through a new `SchedulerAdapter` that transparently delegates to `GlobalRegionScheduler` / `AsyncScheduler` on Folia and to `BukkitScheduler` on Paper/Spigot/Bukkit. The `folia-supported: true` flag has been added to `plugin.yml`.

### Changed
- **EzBoost engine improvement** — reflective access to EzBoost's price-multiplier API is now cached after the first lookup. `Class.forName`, `getMethod`, and `getPlugin` are no longer called on every transaction; instead, resolved `Method` references are reused for the lifetime of the server.

## [2.5.0] - 2026-05-11

This version we focussed on adding **Team Shops**, a full team-based economy layer built on top of [TeamsAPI](https://modrinth.com/plugin/teams-api). Teams get their own market, shared treasury, stock pool, and role-based pricing, all accessible through a new `/teamshop` command.

![Team Market GUI](https://i.ibb.co/bgtM010v/image.png)

### Added

- **TeamsAPI integration** — EzShops now integrates with [TeamsAPI](https://modrinth.com/plugin/teams-api) as a soft dependency; all features degrade gracefully when TeamsAPI is absent.
- **Role-based sell multipliers** — MEMBER, ADMIN and OWNER each receive a configurable sell price bonus when selling to the shop (`teams-integration.sell-multiplier` in `config.yml`).
- **Role-based buy discounts** — MEMBER, ADMIN and OWNER each receive a configurable buy price reduction when purchasing from the shop (`teams-integration.buy-discount` in `config.yml`).
- **Team treasury** — a shared balance funded automatically by a configurable percentage of every sell transaction (`teams-integration.treasury-split`). Members can deposit and withdraw funds through the treasury GUI.
- **Shared team stock** — stock quantities are pooled per team; all members draw from and contribute to the same pool (`teams-integration.shared-stock`).
- **`/teamshop` command** — opens the team shop dashboard showing team info, current multipliers and quick links.
  - `/teamshop treasury` — opens the team treasury GUI (deposit / withdraw; requires `ezshops.teamshop.treasury.withdraw`).
  - `/teamshop stocks` — browse all items your team currently has in stock.
- **Permissions**:
  - `ezshops.teamshop` (default: true) — access the team shop dashboard.
  - `ezshops.teamshop.treasury.withdraw` (default: true) — withdraw from the team treasury.
  - `ezshops.teamshop.admin` (default: op) — administrative team stock commands.
- **Automatic data cleanup** — team stock and treasury data are deleted automatically when a team is disbanded via the `TeamDeleteEvent`.
