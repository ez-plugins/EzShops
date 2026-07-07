## EzShops – The Ultimate Minecraft Shop Plugin

**EzShops** is the all-in-one shop solution for Minecraft servers, offering advanced shop menus, dynamic pricing, sign trading, player marketplaces, and seamless API integrations. Designed for Paper, Purpur, Spigot, and Bukkit (1.7–26.1+), EzShops empowers server owners to deliver a polished, feature-rich economy experience with minimal setup.

Check our latest project: [PvPIndex]([https://pvpindex.com/](https://modrinth.com/plugin/pvpindex-battle))

![Ez Shops Minecraft Plugin Header](https://i.ibb.co/tMvtk3KG/ezshops-header.png)

[![ez shops plugin documentation button](https://i.ibb.co/dskCvgLP/documentation-button-1.png)](https://ez-plugins.github.io/EzShops)
[![ez shops plugin support button](https://i.ibb.co/Wpy2w1cH/support-button-1.png)](https://discord.gg/yWP95XfmBS)

![Minecraft Versions](https://ezbanners.org/shields/plugins/73ab8f99-e37c-4a42-b4b3-45683d5b5792/minecraft-versions.png)
![GitHub Stars](https://ezbanners.org/shields/plugins/73ab8f99-e37c-4a42-b4b3-45683d5b5792/github-stars.png)
![License](https://ezbanners.org/shields/plugins/73ab8f99-e37c-4a42-b4b3-45683d5b5792/license.png)

- **Requires Vault & compatible economy plugin**
- **Dynamic pricing engine & persistent market state**
- **ShopPriceService API for plugin integration**
- **Full documentation:** [EzShops Docs](https://ez-plugins.github.io/EzShops)

> **Note:** Vault and an economy plugin are required for currency transactions.

[Join our Discord for support & community](https://discord.gg/yWP95XfmBS)

---

### ⭐ Why this shop plugin?

- **Modern Shop Menus:** Intuitive GUIs with category icons, quantity selectors, bulk buy/sell, and customizable lore.
- **Dynamic Pricing:** Prices automatically adjust based on player activity, with persistent state across restarts.
- **Rotating Specials:** Schedule daily or timed specials with weighted or random rotations, no manual recoding needed.
- **Sign Shop Integration:** Create buy/sell signs that mirror menu prices and formats, perfect for towns and markets.
- **Player Shops:** Let players run their own chest shops with secure, branded signs, automatic stock management, and smart item labels — sign names automatically show potion effects and enchantment details.
- **Stock Market System:** Enable real-time, demand-driven pricing for select items, with admin controls and persistent overrides.
- **Instant Config Reload:** Use `/shop reload` to update all configs and menus live, no server restart required.
- **Sell boosts**: Option for sell boosts through integration with our [EzBoost plugin](https://modrinth.com/plugin/ezboost) - see the [EzBoost integration docs](https://ez-plugins.github.io/EzShops/integrations/ezboost).
- **API Ready:** Integrate with other plugins or custom features using the robust ShopPriceService API.

![Category Menu](https://i.ibb.co/XvgPdSZ/ez-shops-category.png)
![Item Overview](https://i.ibb.co/0yYvMjQf/ez-shops-item-overview.png)

---

### ⚡ Key features of EzShops

- **Easy Setup:** Auto-generates default configs, categories, and layouts on first run.
- **Flexible Customization:** Edit categories, item prices, menu layouts, and sign formats to match your server’s branding.
- **Advanced Permissions:** Fine-tune access for shop, stock market, sign creation, and player shop management.
- **Localization:** Multi-language support, just set your preferred language in `config.yml`.
- **Performance Optimized:** Designed for large servers with async operations and caching.

---

### 🛒 Powerful Shop & Stock Market Commands

- `/shop` – Open the shop GUI or browse all items.
- `/shop buy <item> [amount]` – Buy items directly from chat.
- `/shop sell <item> [amount]` – Sell items directly from chat.
- `/sellhand`, `/sellinventory` – Quick-sell items or entire inventories.
- `/price <material>` – Check live buy/sell prices.
- `/shop reload` – Reload all configs instantly.
- `/shopadmin reseed [mode]` – Restore missing bundled category defaults (all modes or one mode) without overwriting existing files.
- `/stock buy <item> <amount>` – Buy stock market items at the current price.
- `/stock sell <item> <amount>` – Sell your stock market items.
- `/stock overview` – View all stock market items and their current prices.
- `/stockadmin` – Full admin controls for the stock market.

See the [full command list](https://ez-plugins.github.io/EzShops/commands) and [permissions](https://ez-plugins.github.io/EzShops/permissions).

---

### 🛠️ Configuration & Customization

- **Categories & Items:** Organize your shop with unlimited categories and custom item settings.
- **Dynamic Pricing:** Enable per-item or global dynamic pricing for a living, player-driven economy.
- **Rotations:** Schedule daily specials or timed offers with easy YAML config.
- **Sign Shops:** Mirror menu offers on signs, perfect for player towns and spawn markets.
- **Player Shops:** Empower your community to create secure, automated chest shops.
- **Localization:** Support for English, Spanish, Dutch, Chinese, and more.

Full setup and config details: [Configuration Guide](https://ez-plugins.github.io/EzShops/configuration) - including [shop items](https://ez-plugins.github.io/EzShops/configuration/shop-items), [menu layout](https://ez-plugins.github.io/EzShops/configuration/menu-layout), and [localization](https://ez-plugins.github.io/EzShops/configuration/localization).

---

### 🪧 Sign Shops & Player Shops

![Sign Shops](https://i.ibb.co/0RFSYmLC/ez-shops-sign-shops.png)
![Sign Setup GUI](https://i.ibb.co/0RLw862m/ez-shops-sign-setup-gui.png)
![Player Shop Setup](https://i.ibb.co/6cWJNW5w/ez-shops-player-setup.png)
![Player Shop Sign](https://i.ibb.co/bjsBpvpN/ez-shops-player-shop-sign.png)

---

### 📦 API & Plugin Integration

- **ShopPriceService API:** Query prices, execute transactions, and integrate with other plugins.
- **Stock Market API:** Real-time price updates, player holdings, and admin controls.
- **Ready for EzAuction & EzEconomy:** Out-of-the-box integration with other EzPlugins.

See [API Reference & Examples](https://ez-plugins.github.io/EzShops/api)

---

### 🚀 Get Started in Minutes

1. Install Vault and an economy plugin.
2. Drop `EzShops.jar` into your `plugins/` folder.
3. Restart your server to auto-generate configs.
4. Edit `shop.yml` and category files to customize your shop.
5. Grant permissions to your staff and players as needed.

---

**Deliver a next-level shop experience on your Minecraft server with EzShops!**

🔗 [Full Documentation](https://ez-plugins.github.io/EzShops) · [GitHub](https://github.com/ez-plugins/EzShops)

---

*Looking for more? Try [EzAuction](https://modrinth.com/plugin/ezauction) and [EzEconomy](https://modrinth.com/plugin/ezeconomy) for a complete economy suite!*

![Usage of the EzShops plugin](https://bstats.org/signatures/bukkit/ezshops.svg)

[![Try the other Minecraft plugins in the EzPlugins series](https://i.ibb.co/PzfjNjh0/ezplugins-try-other-plugins.png)](https://modrinth.com/collection/Q98Ov6dA)