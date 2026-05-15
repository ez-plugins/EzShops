---
layout: default
title: Material Names
parent: Configuration
nav_order: 5
description: "Valid Minecraft material names for use in shop configuration files."
---

# 🪨 Material Names

EzShops identifies every item in your shop by its **Bukkit Material name** — an uppercase, underscore-separated identifier like `DIAMOND_SWORD` or `OAK_LOG`.

---

## Finding Valid Material Names

The authoritative source of all valid names for your server version is the **Paper/Bukkit Material enum javadoc**:

- [Paper 1.21.x — `org.bukkit.Material`](https://jd.papermc.io/paper/1.21/org/bukkit/Material.html)

Material names are **case-insensitive** inside EzShops config files — both `DIAMOND` and `diamond` work.

---

## Historical Renames

Minecraft has renamed a number of items over the years. If an item suddenly stops working after a server upgrade, check the table below for a rename.

| Old Name | New Name | Since Version | Notes |
|---|---|---|---|
| `LOG` | `OAK_LOG`, `BIRCH_LOG`, `SPRUCE_LOG`, `JUNGLE_LOG`, `ACACIA_LOG`, `DARK_OAK_LOG` | 1.13 "The Flattening" | One meta-value block became six separate materials |
| `LOG_2` | `ACACIA_LOG`, `DARK_OAK_LOG` | 1.13 | Same flattening event |
| `WOOD` | `OAK_PLANKS`, `BIRCH_PLANKS`, etc. | 1.13 | Old `WOOD` meta ID → individual plank names |
| `GRASS` | `SHORT_GRASS` | 1.20.3 (Dec 2023) | Disambiguated from the grass block (`GRASS_BLOCK`) |
| `CHAIN` | `IRON_CHAIN` | 1.21.9 "The Copper Age" (Sep 2025) | Renamed when copper chain variants were added |

> **Tip:** If you have old configs with `CHAIN`, update them to `IRON_CHAIN` for 1.21.9+ servers.

---

## New Materials by Version

Use this section when adding newly released items to your shop. All names listed match the Bukkit/Paper Material enum for the given version.

### 1.21.4 — The Garden Awakens (December 3, 2024)

**Pale Oak wood set:**
`PALE_OAK_LOG`, `STRIPPED_PALE_OAK_LOG`, `PALE_OAK_WOOD`, `STRIPPED_PALE_OAK_WOOD`,
`PALE_OAK_PLANKS`, `PALE_OAK_STAIRS`, `PALE_OAK_SLAB`, `PALE_OAK_SIGN`,
`PALE_OAK_HANGING_SIGN`, `PALE_OAK_BUTTON`, `PALE_OAK_PRESSURE_PLATE`,
`PALE_OAK_DOOR`, `PALE_OAK_TRAPDOOR`, `PALE_OAK_FENCE`, `PALE_OAK_FENCE_GATE`,
`PALE_OAK_BOAT`, `PALE_OAK_CHEST_BOAT`, `PALE_OAK_LEAVES`, `PALE_OAK_SAPLING`

**Pale Moss & Eyeblossoms:**
`PALE_HANGING_MOSS`, `PALE_MOSS_BLOCK`, `PALE_MOSS_CARPET`,
`OPEN_EYEBLOSSOM`, `CLOSED_EYEBLOSSOM`

**Resin:**
`RESIN_CLUMP`, `RESIN_BLOCK`, `RESIN_BRICK`, `RESIN_BRICKS`,
`CHISELED_RESIN_BRICKS`, `RESIN_BRICK_SLAB`, `RESIN_BRICK_STAIRS`, `RESIN_BRICK_WALL`

**Other:**
`CREAKING_HEART`

---

### 1.21.5 — Spring to Life (March 25, 2025)

`LEAF_LITTER`, `WILDFLOWERS`, `BUSH`, `FIREFLY_BUSH`, `CACTUS_FLOWER`,
`SHORT_DRY_GRASS`, `TALL_DRY_GRASS`, `BLUE_EGG`, `BROWN_EGG`

---

### 1.21.6 — Chase the Skies (June 17, 2025)

**Dried Ghast block/item:**
`DRIED_GHAST`

**Harnesses (16 dye variants):**
`WHITE_HARNESS`, `LIGHT_GRAY_HARNESS`, `GRAY_HARNESS`, `BLACK_HARNESS`,
`BROWN_HARNESS`, `RED_HARNESS`, `ORANGE_HARNESS`, `YELLOW_HARNESS`,
`GREEN_HARNESS`, `LIME_HARNESS`, `CYAN_HARNESS`, `LIGHT_BLUE_HARNESS`,
`BLUE_HARNESS`, `MAGENTA_HARNESS`, `PURPLE_HARNESS`, `PINK_HARNESS`

**Spawn egg & music disc:**
`HAPPY_GHAST_SPAWN_EGG`, `MUSIC_DISC_TEARS`

---

### 1.21.7 (June 30, 2025)

Minor hotfix — no new blocks, items, or material renames.

---

### 1.21.8 (July 17, 2025)

Minor hotfix — no new blocks, items, or material renames.

---

### 1.21.9 — The Copper Age (September 30, 2025)

> ⚠️ **Breaking rename:** `CHAIN` → `IRON_CHAIN`

**Copper chests (8 variants — base + 3 oxidation stages + 4 waxed variants):**
`COPPER_CHEST`, `EXPOSED_COPPER_CHEST`, `WEATHERED_COPPER_CHEST`, `OXIDIZED_COPPER_CHEST`,
`WAXED_COPPER_CHEST`, `WAXED_EXPOSED_COPPER_CHEST`, `WAXED_WEATHERED_COPPER_CHEST`, `WAXED_OXIDIZED_COPPER_CHEST`

**Shelves (one per wood type):**
`OAK_SHELF`, `BIRCH_SHELF`, `SPRUCE_SHELF`, `JUNGLE_SHELF`, `ACACIA_SHELF`,
`DARK_OAK_SHELF`, `CRIMSON_SHELF`, `WARPED_SHELF`, `MANGROVE_SHELF`,
`BAMBOO_SHELF`, `CHERRY_SHELF`, `PALE_OAK_SHELF`

**Copper golem statues (8 variants):**
`COPPER_GOLEM_STATUE`, `EXPOSED_COPPER_GOLEM_STATUE`, `WEATHERED_COPPER_GOLEM_STATUE`, `OXIDIZED_COPPER_GOLEM_STATUE`,
`WAXED_COPPER_GOLEM_STATUE`, `WAXED_EXPOSED_COPPER_GOLEM_STATUE`, `WAXED_WEATHERED_COPPER_GOLEM_STATUE`, `WAXED_OXIDIZED_COPPER_GOLEM_STATUE`

**Copper bars (8 variants):**
`COPPER_BARS`, `EXPOSED_COPPER_BARS`, `WEATHERED_COPPER_BARS`, `OXIDIZED_COPPER_BARS`,
`WAXED_COPPER_BARS`, `WAXED_EXPOSED_COPPER_BARS`, `WAXED_WEATHERED_COPPER_BARS`, `WAXED_OXIDIZED_COPPER_BARS`

**Copper chains (8 variants):**
`COPPER_CHAIN`, `EXPOSED_COPPER_CHAIN`, `WEATHERED_COPPER_CHAIN`, `OXIDIZED_COPPER_CHAIN`,
`WAXED_COPPER_CHAIN`, `WAXED_EXPOSED_COPPER_CHAIN`, `WAXED_WEATHERED_COPPER_CHAIN`, `WAXED_OXIDIZED_COPPER_CHAIN`

**Copper torches & lanterns:**
`COPPER_TORCH`,
`COPPER_LANTERN`, `EXPOSED_COPPER_LANTERN`, `WEATHERED_COPPER_LANTERN`, `OXIDIZED_COPPER_LANTERN`,
`WAXED_COPPER_LANTERN`, `WAXED_EXPOSED_COPPER_LANTERN`, `WAXED_WEATHERED_COPPER_LANTERN`, `WAXED_OXIDIZED_COPPER_LANTERN`

**Lightning rod oxidation variants (7 new):**
`EXPOSED_LIGHTNING_ROD`, `WEATHERED_LIGHTNING_ROD`, `OXIDIZED_LIGHTNING_ROD`,
`WAXED_LIGHTNING_ROD`, `WAXED_EXPOSED_LIGHTNING_ROD`, `WAXED_WEATHERED_LIGHTNING_ROD`, `WAXED_OXIDIZED_LIGHTNING_ROD`

**Copper tools:**
`COPPER_SWORD`, `COPPER_AXE`, `COPPER_PICKAXE`, `COPPER_SHOVEL`, `COPPER_HOE`

**Copper armor:**
`COPPER_HELMET`, `COPPER_CHESTPLATE`, `COPPER_LEGGINGS`, `COPPER_BOOTS`

**Other items:**
`COPPER_HORSE_ARMOR`, `COPPER_NUGGET`, `COPPER_GOLEM_SPAWN_EGG`

---

### 1.21.10 (October 7, 2025)

Minor hotfix — no new blocks, items, or material renames.

---

### 1.21.11 — Mounts of Mayhem (December 9, 2025)

**Spears:**
`WOODEN_SPEAR`, `STONE_SPEAR`, `COPPER_SPEAR`, `IRON_SPEAR`,
`GOLDEN_SPEAR`, `DIAMOND_SPEAR`, `NETHERITE_SPEAR`

**Nautilus armor:**
`COPPER_NAUTILUS_ARMOR`, `IRON_NAUTILUS_ARMOR`, `GOLDEN_NAUTILUS_ARMOR`,
`DIAMOND_NAUTILUS_ARMOR`, `NETHERITE_NAUTILUS_ARMOR`

**Other equipment:**
`NETHERITE_HORSE_ARMOR`

**Spawn eggs:**
`NAUTILUS_SPAWN_EGG`, `ZOMBIE_NAUTILUS_SPAWN_EGG`,
`CAMEL_HUSK_SPAWN_EGG`, `PARCHED_SPAWN_EGG`

---

### 26.1 — Tiny Takeover (March 24, 2026)

> **Note:** Starting with this version, Minecraft Java Edition switched from `1.x` versioning to `26.x` (year-based). No material renames occurred with this version change.

`GOLDEN_DANDELION`

---

### 26.1.2 (April 9, 2026)

Minor hotfix — no new blocks, items, or material renames.

---

## Version Numbers and Material Names

Mojang switched from `1.x` versioning to year-based versioning (e.g. `26.1`) starting in early 2026. **This version scheme change did not cause any material renames.** Material names only change when Mojang explicitly renames an item, which happens rarely and is documented in the table above.
