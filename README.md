# EzShops

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21%2B-blue)](https://adoptium.net/)
[![Release](https://img.shields.io/github/v/release/ez-plugins/EzShops)](https://github.com/ez-plugins/EzShops/releases/latest)
[![Stars](https://img.shields.io/github/stars/ez-plugins/EzShops?style=social)](https://github.com/ez-plugins/EzShops/stargazers)
[![Issues](https://img.shields.io/github/issues/ez-plugins/EzShops)](https://github.com/ez-plugins/EzShops/issues)
[![CI](https://github.com/ez-plugins/EzShops/actions/workflows/ci.yml/badge.svg)](https://github.com/ez-plugins/EzShops/actions)
[![Docs](https://github.com/ez-plugins/EzShops/actions/workflows/docs.yml/badge.svg)](https://ez-plugins.github.io/EzShops)
[![Modrinth](https://img.shields.io/modrinth/v/zaW55ehx?label=modrinth&color=00AF5C)](https://modrinth.com/plugin/ezshops)

**EzShops** is a modern, feature-rich Minecraft shop plugin for Paper 1.21+ servers. It provides guided storefront menus, dynamic pricing, rotating daily specials, sign shop mirroring, player-run marketplaces, a live stock market system, and team-based shop bonuses via [TeamsAPI](https://modrinth.com/plugin/teams-api).

---

## 📑 Table of Contents
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Features](#-features)
- [Configuration](#️-configuration)
- [Permissions & Commands](#️-permissions--commands)
- [Documentation](#-documentation)
- [Usage Examples](#️-usage-examples)
- [Contributing](#-contributing)
- [Support & Community](#️-support--community)
- [License](#-license)

---

## 📦 Requirements
- Java 21 or higher ([Adoptium](https://adoptium.net/))
- Paper / Purpur / Folia 1.21.4+
- [Vault](https://www.spigotmc.org/resources/vault.34315/) + a compatible economy plugin
- *(Optional)* [TeamsAPI ≥ 1.4.1](https://modrinth.com/plugin/teams-api) for team shop features

## 🚀 Installation
1. Download the latest EzShops JAR from the releases page.
2. Place the JAR in your server's `plugins/` directory.
3. Ensure Vault and a compatible economy plugin are installed.
4. Start or reload your server.
5. Configure the plugin as needed (see below).

## ✨ Features
- **Guided storefront menus** - Category icons, quantity pickers, bulk buttons, and lore templates
- **Smart price automation** - Dynamic buy/sell multipliers that adjust after each transaction
- **Rotating daily specials** - Schedule weighted or sequential rotations from `shop/rotations/`
- **Sign shop mirroring** - Sync right-click signs with menu entries and customize headers/formats
- **Specialty entries** - Sell spawners with correct block states, minion/vote crate keys
- **Player-run marketplaces** - `[shop]` signs convert into owner-branded listings from linked chests
- **Stock market system** - Real-time pricing based on supply/demand with admin controls
- **Category commands** - Run server commands when clicking category icons (warps, info, etc.)
- **Live config reload** - Use `/shop reload` to instantly reload configurations
- **Multi-language support** - Bundled with English, Spanish, Dutch, and Chinese translations
- **TeamsAPI integration** - Role-based sell multipliers & buy discounts, shared team treasury, and pooled stock for faction servers

## ⚙️ Configuration
Default configuration files are generated on first run in `plugins/EzShops/`.

**Key configuration options in `config.yml`:**

- `language`: Set the plugin language (en, es, nl, zh)
- `player-shops.enabled`: Toggle player shop system
- `stock-market.enabled`: Enable/disable stock market features
- `dynamic-pricing.enabled`: Enable automatic price adjustments

Shop categories, items, and rotations are configured in the `shop/` subdirectory.

See the [Configuration Guide](docs/configuration.md) for full details.

## 🛡️ Permissions & Commands

**Main Commands:**

| Command                | Description                        | Permission                |
|------------------------|------------------------------------|---------------------------|
| `/shop`                | Open the shop GUI                  | `ezshops.shop`            |
| `/shop buy <item>`     | Buy items from chat                | `ezshops.shop.buy`        |
| `/shop sell <item>`    | Sell items from chat               | `ezshops.shop.sell`       |
| `/sellhand`            | Sell item in hand                  | `ezshops.shop.sell`       |
| `/sellinventory`       | Sell all sellable items            | `ezshops.shop.sell`       |
| `/price <material>`    | Check shop price                   | `ezshops.shop`            |
| `/playershop`          | Create player shop sign            | `ezshops.playershop.create` |
| `/stock buy <item> <amount>` | Buy stock market item    | `ezshops.stock.view`      |
| `/stock sell <item> <amount>` | Sell stock market item  | `ezshops.stock.view`      |
| `/teamshop`             | Open team shop dashboard           | `ezshops.teamshop`        |
| `/teamshop treasury`    | View/deposit/withdraw team funds   | `ezshops.teamshop`        |
| `/teamshop stocks`      | Browse team shared stock           | `ezshops.teamshop`        |

See the [Commands](docs/commands.md) and [Permissions](docs/permissions.md) documentation for complete lists.

## 📚 Documentation

Full documentation is available at **<https://ez-plugins.github.io/EzShops>**.

| Page | Description |
|------|-------------|
| [Commands](https://ez-plugins.github.io/EzShops/commands) | All commands and their usage |
| [Permissions](https://ez-plugins.github.io/EzShops/permissions) | All permissions and defaults |
| [Configuration](https://ez-plugins.github.io/EzShops/configuration) | Complete configuration reference |
| [Shops](https://ez-plugins.github.io/EzShops/shops) | Pagination, price IDs, dynamic pricing, stock market |
| [Integrations](https://ez-plugins.github.io/EzShops/integrations) | Vault, EzBoost, EzAuction, TeamsAPI, Adventure |
| [API Reference](https://ez-plugins.github.io/EzShops/api) | Public classes, methods, and integration examples |
| [MiniMessage](https://ez-plugins.github.io/EzShops/minimessage) | Text formatting guide |

## 🛠️ Usage Examples

### Opening the Shop
Players can use `/shop` to open the main shop GUI. Admins can reload configuration with `/shop reload`.

### Using the Stock Market
```
/stock buy DIAMOND 10  - Purchase 10 shares of DIAMOND at current price
/stock sell DIAMOND 5  - Sell 5 shares of DIAMOND
/stock info DIAMOND    - View current price and recent changes
/stock list            - List all stock market items
```

### Creating a Player Shop
1. Use `/playershop` to configure your shop
2. Set quantity and pricing through the setup menu
3. Place a `[shop]` sign linked to a chest with your items

---

## 🤝 Contributing
Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines, or open an issue/PR.

## 🛡️ Support & Community
- For help, open an issue on GitHub or contact the maintainers
- Feature requests and bug reports are encouraged
- Join our [Discord server](https://discord.gg/yWP95XfmBS)

## 📄 License
EzShops is licensed under the [MIT License](LICENSE). Copyright (c) 2026 ez-plugins.

---

For full documentation, visit [ez-plugins.github.io/EzShops](https://ez-plugins.github.io/EzShops). For support, open an issue or join the [Discord](https://discord.gg/yWP95XfmBS).
