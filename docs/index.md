---
layout: home
title: EzShops
nav_order: 1
description: "A feature-rich Minecraft shop plugin with dynamic pricing, stock markets, and TeamsAPI integration."
permalink: /
---

# EzShops

[![Modrinth](https://img.shields.io/modrinth/v/zaW55ehx?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/ezshops)
[![GitHub Release](https://img.shields.io/github/v/release/ez-plugins/EzShops?logo=github)](https://github.com/ez-plugins/EzShops/releases)

**EzShops** is a feature-rich shop plugin for Paper/Spigot Minecraft servers. It delivers a fully configurable GUI shop, dynamic supply-and-demand pricing, a player-driven stock market, sign-based chest shops, and deep integration with the ez-plugins ecosystem.

---

## Features

- **GUI Shop**  -  browse categories, buy and sell items with a clean inventory-based interface
- **Dynamic Pricing**  -  prices shift automatically with supply and demand; normalise back to base values over time
- **Stock Market**  -  a server-wide market where item prices fluctuate based on volatility and player activity
- **Player Shops**  -  sign-based chest shops that let players trade with each other
- **TeamsAPI Integration**  -  role-based sell multipliers and buy discounts, shared team stock pool, and a shared team treasury funded by every sale
- **MiniMessage Support**  -  all messages and titles accept both legacy `&` codes and modern MiniMessage tags
- **Vault Economy**  -  works with any Vault-compatible economy plugin
- **EzBoost Support**  -  sell price multipliers stack with TeamsAPI role bonuses

---

## Quick start

**1. Drop the JAR into your `plugins/` folder** and start the server. EzShops generates its configuration automatically.

**2. Configure your shop items** in `plugins/EzShops/shop.yml`:

```yaml
items:
  DIAMOND:
    buy: 250.0
    sell: 125.0
```

**3. Open the shop in-game:**

```
/shop
```

**4. (Optional) Enable TeamsAPI integration** in `config.yml`:

```yaml
teams-integration:
  enabled: true
  sell-multiplier:
    member: 1.05
    admin:  1.10
    owner:  1.15
  buy-discount:
    member: 0.05
    admin:  0.10
    owner:  0.15
  treasury-split: 0.05
  shared-stock: true
```

---

## Documentation

| Page | What it covers |
|------|----------------|
| [Commands](commands) | Every command, its usage, and required permission |
| [Permissions](permissions) | Full permission node reference |
| [Configuration](configuration) | Overview of all config files |
| &nbsp;&nbsp;[Main Settings](configuration/main-settings) | `config.yml`  -  dynamic pricing, player shops, teams |
| &nbsp;&nbsp;[Shop Items](configuration/shop-items) | `shop.yml`  -  defining items and prices |
| &nbsp;&nbsp;[Menu Layout](configuration/menu-layout) | `menu.yml`  -  GUI size, titles, filler slots |
| &nbsp;&nbsp;[Localization](configuration/localization) | `messages/`  -  translating every message |
| [Shops](shops) | Shop system guides |
| &nbsp;&nbsp;[Pagination](shops/pagination) | Per-item page assignment |
| &nbsp;&nbsp;[Price IDs](shops/price-id) | Stable keys for dynamic pricing |
| &nbsp;&nbsp;[Dynamic Pricing](shops/pricing/dynamic-pricing) | Supply/demand pricing |
| &nbsp;&nbsp;[Stock Market](shops/pricing/stock-market) | Volatile market system |
| [Integrations](integrations) | Plugin compatibility |
| &nbsp;&nbsp;[Vault](integrations/vault) | Economy provider |
| &nbsp;&nbsp;[EzBoost](integrations/ezboost) | Sell multipliers |
| &nbsp;&nbsp;[EzAuction](integrations/ezauction) | Auction price display |
| &nbsp;&nbsp;[Adventure](integrations/adventure) | MiniMessage text |
| &nbsp;&nbsp;[TeamsAPI](integrations/teams-api) | Faction shop features |
| [API](api) | Developer integration reference |
| [MiniMessage](minimessage) | Text formatting guide |
