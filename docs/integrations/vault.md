# Vault Integration

EzShops uses [Vault](https://www.spigotmc.org/resources/vault.34315/) as its economy provider interface.

## Requirements

Vault is **required** for EzShops to function. You must install Vault along with a compatible economy plugin.

## Supported Economy Plugins

Vault supports many economy plugins. Common ones include:
- [EzEconomy](https://modrinth.com/plugin/ezeconomy) - Modern Vault economy plugin
- Essentials Economy
- iConomy
- BOSEconomy
- etc.

## Setup

1. Install Vault plugin
2. Install your preferred economy plugin
3. Install EzShops
4. Restart server

## Configuration

No specific configuration needed in EzShops. Economy settings are handled by your economy plugin.

## Permissions

Economy-related permissions are handled by the economy plugin, not EzShops.

## Troubleshooting

If EzShops fails to start with "Vault economy provider not found":
- Ensure Vault is installed and loaded
- Ensure you have a compatible economy plugin installed
- Check server logs for Vault initialization errors