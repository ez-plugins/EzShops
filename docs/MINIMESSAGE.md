# MiniMessage Support in EzShops

EzShops now supports both legacy color codes and modern MiniMessage formatting for all configurable messages and titles.

## What is MiniMessage?

MiniMessage is a modern text formatting system that uses tags instead of codes. It's more readable and supports advanced features like gradients, hover text, and click actions.

## Format Support

EzShops automatically detects which format you're using:

- **Legacy Color Codes**: `&a`, `&c`, `&l`, etc.
- **MiniMessage Tags**: `<red>`, `<green>`, `<bold>`, etc.

You can use **either** format in your configuration files, and EzShops will handle the translation automatically.

## Examples

### Legacy Color Codes (Traditional)
```yaml
shop:
  title: "&a&lSkyblock Shop"
  messages:
    welcome: "&7Welcome to the &aShop&7!"
    price: "&6Price: &e{price}"
```

### MiniMessage Tags (Modern)
```yaml
shop:
  title: "<green><bold>Skyblock Shop"
  messages:
    welcome: "<gray>Welcome to the <green>Shop<gray>!"
    price: "<gold>Price: <yellow>{price}"
```

### MiniMessage Color Names

| Tag | Color | Legacy Equivalent |
|-----|-------|-------------------|
| `<black>` | Black | `&0` |
| `<dark_blue>` | Dark Blue | `&1` |
| `<dark_green>` | Dark Green | `&2` |
| `<dark_aqua>` | Dark Aqua | `&3` |
| `<dark_red>` | Dark Red | `&4` |
| `<dark_purple>` | Dark Purple | `&5` |
| `<gold>` | Gold | `&6` |
| `<gray>` | Gray | `&7` |
| `<dark_gray>` | Dark Gray | `&8` |
| `<blue>` | Blue | `&9` |
| `<green>` | Green | `&a` |
| `<aqua>` | Aqua | `&b` |
| `<red>` | Red | `&c` |
| `<light_purple>` | Light Purple | `&d` |
| `<yellow>` | Yellow | `&e` |
| `<white>` | White | `&f` |

### MiniMessage Formatting

| Tag | Effect | Legacy Equivalent |
|-----|--------|-------------------|
| `<bold>` | Bold text | `&l` |
| `<italic>` | Italic text | `&o` |
| `<underlined>` | Underlined text | `&n` |
| `<strikethrough>` | Strikethrough | `&m` |
| `<obfuscated>` | Obfuscated/magic | `&k` |
| `<reset>` | Reset formatting | `&r` |

### Advanced MiniMessage Features

MiniMessage supports advanced features that are not available with legacy codes:

#### Gradients
```yaml
title: "<gradient:red:blue>Rainbow Shop</gradient>"
```

#### Hover Text
```yaml
button: "<hover:show_text:'Click to buy'>Buy Item</hover>"
```

#### Click Actions
```yaml
link: "<click:open_url:'https://example.com'>Visit Website</click>"
```

#### Rainbow Text
```yaml
title: "<rainbow>Colorful Shop</rainbow>"
```

## Mixing Formats

While EzShops supports both formats, we recommend:
1. **Choose one format** per configuration file for consistency
2. **Use MiniMessage** for new configurations (more features, more readable)
3. **Keep legacy codes** in existing configurations (backward compatibility)

## Migration Guide

To migrate from legacy codes to MiniMessage:

### Before (Legacy)
```yaml
messages:
  welcome: "&a&lWelcome &7to the &eShop&7!"
  buy: "&aYou bought &bx{amount} {item} &afor &6${price}"
```

### After (MiniMessage)
```yaml
messages:
  welcome: "<green><bold>Welcome <gray>to the <yellow>Shop<gray>!"
  buy: "<green>You bought <aqua>x{amount} {item} <green>for <gold>${price}"
```

## Compatibility

- All configurable messages and titles support both formats
- This includes:
  - Shop menu titles
  - Category names
  - Item display names and lore
  - Chat messages
  - Sign text
  - GUI titles
  - Player shop messages
  - Stock market messages

## Troubleshooting

If your MiniMessage tags aren't working:

1. Check that tags are properly closed: `<red>text</red>` or `<red>text` (auto-closes)
2. Make sure tag names are correct (case-sensitive)
3. Verify no conflicting legacy codes in the same string
4. Check server logs for parsing errors

## Notes

- MiniMessage tags are only parsed when detected (performance optimization)
- Legacy codes continue to work everywhere as before
- No configuration changes are required - both formats work out of the box
