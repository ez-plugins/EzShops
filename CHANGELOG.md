# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
