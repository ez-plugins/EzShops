package com.skyblockexp.ezshops.shop.pricing.layout;

import com.skyblockexp.ezshops.common.MessageUtil;
import com.skyblockexp.ezshops.shop.ShopMenuLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Shared menu/layout parsing helpers used by shop pricing config loaders.
 */
public final class ShopPricingLayoutSupport {

    private final Logger logger;

    public ShopPricingLayoutSupport(Logger logger) {
        this.logger = logger;
    }

    public int normalizeSize(int requested) {
        return normalizeSize(requested, 54);
    }

    public int normalizeSize(int requested, int defaultSize) {
        int size = requested <= 0 ? defaultSize : requested;
        size = Math.min(54, Math.max(9, size));
        if (size % 9 != 0) {
            size += 9 - (size % 9);
        }
        return size;
    }

    public int clampSlot(int slot, int menuSize) {
        if (slot < 0) {
            return 0;
        }
        if (slot >= menuSize) {
            return menuSize - 1;
        }
        return slot;
    }

    public List<ShopMenuLayout.ConfigurableButton> parseButtons(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<ShopMenuLayout.ConfigurableButton> buttons = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                continue;
            }
            ConfigurationSection btnSection = section.getConfigurationSection(key);
            if (btnSection == null || !btnSection.contains("action")) {
                continue;
            }
            String actionStr = btnSection.getString("action");
            ShopMenuLayout.ButtonAction action = ShopMenuLayout.ButtonAction.fromConfig(actionStr);
            if (action == null) {
                logger.warning("Unknown button action '" + actionStr + "' for button '" + key + "' - skipping.");
                continue;
            }
            int slot = btnSection.getInt("slot", -1);
            if (slot < 0) {
                logger.warning("Button '" + key + "' has no valid slot - skipping.");
                continue;
            }
            ShopMenuLayout.ItemDecoration display = btnSection.contains("material")
                    ? parseDecoration(btnSection, null, "unknown") : null;
            String sound = btnSection.getString("sound", null);
            float soundVolume = (float) btnSection.getDouble("volume", 1.0);
            float soundPitch = (float) btnSection.getDouble("pitch", 1.0);
            String command = btnSection.getString("command", null);
            buttons.add(new ShopMenuLayout.ConfigurableButton(key, slot, display, action,
                    sound, soundVolume, soundPitch, command));
        }
        return buttons;
    }

    public ShopMenuLayout.ItemDecoration parseDecoration(ConfigurationSection section,
            ShopMenuLayout.ItemDecoration fallback, String sourceInfo) {
        if (section == null) {
            return fallback;
        }

        String materialKey = section.getString("material");
        Material material = fallback != null ? fallback.material() : Material.AIR;
        if (materialKey != null) {
            Material parsed = Material.matchMaterial(materialKey, false);
            if (parsed == null) {
                logger.warning(sourceInfo + ": Unknown material '" + materialKey
                        + "' in menu decoration configuration. Check for typos or invalid material names.");
            } else {
                material = parsed;
            }
        }

        int amount = Math.max(1, section.getInt("amount", fallback != null ? fallback.amount() : 1));
        String displayName = colorize(section.getString("display-name", fallback != null ? fallback.displayName() : null));
        List<String> lore = colorize(section.getStringList("lore"));
        return new ShopMenuLayout.ItemDecoration(material, amount, displayName, lore);
    }

    public List<String> colorize(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> colored = new ArrayList<>(lines.size());
        for (String line : lines) {
            colored.add(colorize(line));
        }
        return colored;
    }

    public String colorize(String text) {
        if (text == null) {
            return null;
        }
        try {
            return com.skyblockexp.ezshops.config.ConfigTranslator.resolve(text, null);
        } catch (Throwable t) {
            return MessageUtil.translateColors(text);
        }
    }

    public String friendlyName(String raw) {
        String lower = raw.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
            builder.append(' ');
        }
        if (builder.length() == 0) {
            return lower;
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }
}
