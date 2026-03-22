# 🖥️ Shop Menu Layout (menu.yml)

Customize how your shop looks to the players. You can change titles, inventory sizes, and navigation buttons.

---

## 🧭 Menu Settings

Configure the basic GUI appearance in `shop/menu.yml`.

```yaml
menu:
  title: "&6&lShop Menu"    # GUI title
  size: 54                  # Must be a multiple of 9 (max 54)
  categories-enabled: true
  
  # Filler item for empty slots
  filler:
    enabled: true
    material: GRAY_STAINED_GLASS_PANE