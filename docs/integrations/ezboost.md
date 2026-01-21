# EzBoost Integration

EzShops supports integration with [EzBoost](https://modrinth.com/plugin/ezboost) to provide sell price boosts for players.

## Setup

1. Install the EzBoost plugin on your server
2. Ensure EzShops is also installed
3. The integration is enabled by default, but can be controlled via config

## Configuration

In `plugins/EzShops/config.yml`, you can control the integration:

```yaml
# Enable integration with EzBoost for sell price boosts.
# Requires EzBoost plugin to be installed.
# Default: true
ezboost-integration: true
```

Set to `false` to disable the integration entirely.

## Creating Sell Price Boosts

In EzBoost's `boosts.yml` config, create boosts with the `sellprice` effect type:

```yaml
sellboost:
  display-name: "Sell Price Boost"
  icon: EMERALD
  effects:
    - type: ezshops_sellprice
      amplifier: 50  # 50% increase in sell prices
  duration: 300     # 5 minutes
  cooldown: 3600    # 1 hour cooldown
  cost: 1000        # Vault cost to activate
  permission: "ezboost.sellboost"
  enabled: true
```

## How It Works

- When a player activates a boost containing the `ezshops_sellprice` effect, their sell prices in EzShops are multiplied
- The multiplier is calculated as `1 + (amplifier / 100)`
- Multiple active boosts stack additively
- The boost applies to both individual item sales and bulk inventory sales
- Boosts only affect selling to shops, not buying from shops

## Example

With `amplifier: 50`, sell prices increase by 50%. A item that normally sells for $100 will sell for $150 when the boost is active.

## Permissions

- `ezshops.shop.sell` - Required to sell items (standard EzShops permission)
- Boost activation permissions are controlled by EzBoost

## Compatibility

- Requires [EzBoost](https://modrinth.com/plugin/ezboost) 1.5.3 or later
- Fully optional - EzShops works normally without EzBoost
- No performance impact when EzBoost is not present