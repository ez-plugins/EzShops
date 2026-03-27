# EzShops

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://adoptium.net/)
[![Release](https://img.shields.io/github/v/release/ez-plugins/EzShops)](https://github.com/ez-plugins/EzShops/releases/latest)
[![Stars](https://img.shields.io/github/stars/ez-plugins/EzShops?style=social)](https://github.com/ez-plugins/EzShops/stargazers)
[![Issues](https://img.shields.io/github/issues/ez-plugins/EzShops)](https://github.com/ez-plugins/EzShops/issues)
[![CI](https://github.com/ez-plugins/EzShops/actions/workflows/ci.yml/badge.svg)](https://github.com/ez-plugins/EzShops/actions)

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

## 🤝 Contributing
Contributions are welcome! Please follow these steps to contribute:
1. Fork the repository and create your branch from `main`.
2. Ensure your code follows the existing style and passes all CI tests.
3. Open a Pull Request with a clear description of your changes or bug fixes.
4. Join our Discord for development discussions and feedback.

## 🛡️ Support & Community
- **Issue Tracker:** Report bugs or suggest features on [GitHub Issues](https://github.com/ez-plugins/EzShops/issues).
- **Discord:** Join our community for real-time support and updates.

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.