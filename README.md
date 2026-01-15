# EzShops

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://adoptium.net/)

**EzShops** is a modern, feature-rich Minecraft shop plugin for Skyblock and Survival servers. It provides guided storefront menus, dynamic pricing, rotating daily specials, sign shop mirroring, player-run marketplaces, and a live stock market system.

---

## 📑 Table of Contents
- [EzShops](#ezshops)
  - [📑 Table of Contents](#-table-of-contents)
  - [📦 Requirements](#-requirements)
  - [🚀 Installation](#-installation)
  - [✨ Features](#-features)
  - [⚙️ Configuration](#️-configuration)
  - [🛡️ Permissions \& Commands](#️-permissions--commands)
  - [📚 Documentation](#-documentation)
  - [🛠️ Usage Examples](#️-usage-examples)
    - [Opening the Shop](#opening-the-shop)
    - [Using the Stock Market](#using-the-stock-market)
    - [Creating a Player Shop](#creating-a-player-shop)
  - [🤝 Contributing](#-contributing)
  - [🛡️ Support \& Community](#️-support--community)
  - [📄 License](#-license)

---

## 📦 Requirements
- Java 17 or higher ([Adoptium](https://adoptium.net/))
- Bukkit/Spigot/Paper server (1.17+ recommended, 1.21.4+ for latest features)
- Vault economy plugin

## 🚀 Installation
1. Download the latest EzShops JAR from the releases page.
2. Place the JAR in your server's `plugins/` directory.
3. Ensure Vault and a compatible economy plugin are installed.
4. Start or reload your server.
5. Configure the plugin as needed (see below).

## ✨ Features
- **Guided storefront menus**: Category icons, quantity pickers, bulk buttons, and lore templates
- **Smart price automation**: Dynamic buy/sell multipliers that adjust after each transaction
- **Rotating daily specials**: Schedule weighted or sequential rotations from shop/rotations/
- **Sign shop mirroring**: Sync right-click signs with menu entries and customize headers/formats
- **Specialty entries**: Sell spawners with correct block states, minion/vote crate keys
- **Player-run marketplaces**: `[shop]` signs convert into owner-branded listings from linked chests
- **Stock market system**: Real-time pricing based on supply/demand with admin controls
- **Category commands**: Run server commands when clicking category icons (warps, info, etc.)
- **Live config reload**: Use `/shop reload` to instantly reload configurations
- **Multi-language support**: Bundled with English, Spanish, Dutch, and Chinese translations

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

See the [Commands](docs/commands.md) and [Permissions](docs/permissions.md) documentation for complete lists.

## 📚 Documentation
- [API Reference](docs/api.md): Public classes, methods, and integration examples
- [Commands](docs/commands.md): All commands and their usage
- [Permissions](docs/permissions.md): All permissions and defaults
- [Configuration Guide](docs/configuration.md): Complete configuration reference

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

For full documentation, see the [docs/](docs/) folder. For support, open an issue or contact the maintainers.
