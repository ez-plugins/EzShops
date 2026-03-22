# 🖥️ Shop Menu Layout (menu.yml)

Customize the visual appearance of your shop. You can adjust the GUI size, titles, and filler items to match your server's theme.

---

## 🧭 Basic Menu Settings

Configure the primary GUI properties in `shop/menu.yml`.

```yaml
menu:
  title: "&6&lShop Menu"    # GUI title
  size: 54                  # Must be a multiple of 9 (max 54)
  categories-enabled: true
  
  # Filler item for empty slots
  filler:
    enabled: true
    material: GRAY_STAINED_GLASS_PANE
```