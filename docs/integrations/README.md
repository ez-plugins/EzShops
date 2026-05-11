---
layout: default
title: Integrations
nav_order: 6
has_children: true
description: "Plugin integrations supported by EzShops."
permalink: /integrations
---

# Integrations

EzShops supports various integrations with other plugins to enhance functionality.

## Available Integrations

- [Vault](vault.md) - Economy provider integration (required)
- [EzBoost](ezboost.md) - Sell price boosts for players ([Modrinth](https://modrinth.com/plugin/ezboost))
- [EzAuction](ezauction.md) - Display shop prices in auction menus ([Modrinth](https://modrinth.com/plugin/ezauction))
- [TeamsAPI](teams-api.md) - Role bonuses, team treasury, and shared stock ([Modrinth](https://modrinth.com/plugin/teams-api)) *(soft dependency)*
- [Adventure / MiniMessage](adventure.md) - Modern text formatting and messaging

## Configuration

Most integrations can be enabled/disabled in `config.yml` with options like `integration-name: true/false`.