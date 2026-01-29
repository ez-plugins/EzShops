# Shop Pagination and Per-item Pages

EzShops supports assigning shop items to a specific GUI page and provides configuration to improve pagination behaviour for category menus.

**Summary**
- `page` — optional per-item integer (1-based) to pin an item to a specific category menu page.
- `preserve-last-row` — optional category `menu` flag (boolean, default `true`) that reserves the entire last inventory row for navigation/back buttons and prevents auto-filling items into that row.

Configuration
-------------
Under a category's `menu` block and item entries in `shop.yml` you can use the following fields:

- `page` (integer, optional): the 1-based page number where the item should appear. If omitted or `0`, the item is auto-paginated.
- `slot` (integer, optional): the inventory slot index to attempt to place the item in. Slots are zero-based in the internal inventory; choose a value within the menu `size` bounds.
- `preserve-last-row` (boolean, optional, category `menu` block): when `true` (default) the plugin reserves the entire last row (e.g. slots `inventorySize-9..inventorySize-1`) for navigation/back buttons; items will not be auto-filled into that row. Set to `false` to allow the whole inventory to be used for items.

Behavior notes
--------------
- Page numbers in configuration are 1-based. The plugin converts them internally to the GUI's 0-based page index when opening menus.
- The plugin computes the number of item slots per page from the menu `size` and whether the last row is preserved.
- When presenting a page the composer:
  - first places items that explicitly declare `page` equal to the current page into their configured `slot` (if valid and available),
  - then fills remaining available item slots with auto-paginated items for that page,
  - finally attempts to place any explicit items that could not be placed in their desired slot into the remaining free slots for that page.
- If an explicit `slot` is out-of-bounds or collides with a reserved navigation slot, the item will be deferred and placed into an available slot for that page when possible (a warning can be enabled in logs if desired).

Example
-------
```yaml
categories:
  building:
    name: "Building"
    menu:
      title: "Building"
      size: 54
      preserve-last-row: true
    items:
      tuff_brick_stairs:
        material: POLISHED_TUFF_STAIRS
        slot: 1
        page: 2
        amount: 16
        buy: 56.0
        sell: 26.0
```

This places `tuff_brick_stairs` on the category's second page at slot `1`. With `preserve-last-row: true` the last row is kept for navigation/back buttons; set it to `false` to allow auto-filling the last row when desired.

Troubleshooting
---------------
- If a page does not show navigation buttons even though items declare higher pages, ensure you are using a supported `menu.size` (e.g. 27, 36, 45, 54) and that `preserve-last-row` is not unintentionally hiding nav slots.
- Use the server logs (the plugin prints page, slot and nav-slot info when opening menus) to verify the computed `totalPages`, `previousSlot`, `nextSlot`, and available item slots for the opened category.

See also: [docs/shops/pricing/dynamic-pricing.md](docs/shops/pricing/dynamic-pricing.md) for `price-id` and dynamic pricing configuration details.
