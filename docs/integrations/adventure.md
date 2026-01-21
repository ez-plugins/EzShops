# Adventure Integration

EzShops uses the [Adventure](https://docs.adventure.kyori.net/) library for modern Minecraft text formatting and messaging.

## Features

Adventure provides:
- Rich text formatting with colors, styles, and decorations
- Component-based message building
- Better Unicode support
- Modern chat message handling
- MiniMessage format support for configuration

## Setup

Adventure is bundled with EzShops automatically. No additional setup required.

## Configuration

Messages in EzShops use MiniMessage format for formatting:

```yaml
# Example from messages files
purchase: "<green>You purchased <amount>x <item> for <price>!"
```

Supported formatting:
- `<color>` - Color codes (e.g., `<red>`, `<green>`)
- `<style>` - Text styles (e.g., `<bold>`, `<italic>`)
- `<hover>` - Hover text
- `<click>` - Click actions
- And more advanced features

## Benefits

- Consistent formatting across different Minecraft versions
- Better performance than legacy color codes
- Support for modern chat features
- Easier message customization

## Compatibility

- Works on all supported Minecraft versions
- Automatically falls back for older clients
- No external dependencies required