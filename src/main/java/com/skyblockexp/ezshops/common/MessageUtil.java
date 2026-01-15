package com.skyblockexp.ezshops.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import java.util.regex.Pattern;

/**
 * Utility class for handling chat color translation and MiniMessage support.
 * Centralizes message formatting across the plugin.
 */
public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
            LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = 
            LegacyComponentSerializer.legacySection();
    private static final Pattern MINIMESSAGE_TAG_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*(:.*)?");

    private MessageUtil() {
    }

    /**
     * Translates legacy color codes using the '&' character to formatted text.
     * This is the standard method for translating color codes from configuration files.
     *
     * @param text the text with '&' color codes
     * @return the text with translated color codes, or empty string if input is null/empty
     */
    public static String translateLegacyColors(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Translates text that may contain legacy color codes or MiniMessage tags.
     * This method attempts to detect MiniMessage format (by presence of tags like &lt;red&gt;)
     * and uses the appropriate parser. If MiniMessage tags are detected, it parses as MiniMessage
     * and converts to legacy format. Otherwise, it translates legacy color codes.
     * 
     * @param text the text to translate (may contain legacy codes or MiniMessage tags)
     * @return the translated text with color codes
     */
    public static String translateColors(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Check if the text contains MiniMessage tags (simple heuristic)
        if (containsMiniMessageTags(text)) {
            try {
                // Parse as MiniMessage and convert to legacy format
                Component component = MINI_MESSAGE.deserialize(text);
                return SECTION_SERIALIZER.serialize(component);
            } catch (Exception e) {
                // Fallback to legacy translation if MiniMessage parsing fails
                return translateLegacyColors(text);
            }
        }
        
        // Default to legacy color code translation
        return translateLegacyColors(text);
    }

    /**
     * Simple heuristic to detect if text contains MiniMessage tags.
     * Checks for common MiniMessage patterns using a single-pass approach.
     * 
     * @param text the text to check
     * @return true if MiniMessage tags are detected
     */
    private static boolean containsMiniMessageTags(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Single pass check for any angle bracket pattern that looks like a tag
        // This is more efficient than multiple contains() calls
        int length = text.length();
        for (int i = 0; i < length - 1; i++) {
            if (text.charAt(i) == '<') {
                // Found opening bracket, check if it looks like a tag
                int closeIdx = text.indexOf('>', i + 1);
                if (closeIdx > i + 1 && closeIdx < i + 30) { // Reasonable tag length
                    String potential = text.substring(i + 1, closeIdx);
                    // Check if it matches common MiniMessage tag patterns
                    // More restrictive pattern to avoid false positives
                    if (MINIMESSAGE_TAG_PATTERN.matcher(potential).matches()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Strips all color codes from the given text.
     *
     * @param text the text to strip colors from
     * @return the text without color codes, or empty string if input is null/empty
     */
    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return ChatColor.stripColor(text);
    }

    /**
     * Parses MiniMessage format into a Component.
     * MiniMessage uses tags like &lt;red&gt;, &lt;bold&gt;, etc.
     *
     * @param miniMessage the MiniMessage formatted text
     * @return the parsed Component
     */
    public static Component parseMiniMessage(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    /**
     * Serializes a Component back to MiniMessage format.
     *
     * @param component the component to serialize
     * @return the MiniMessage formatted string
     */
    public static String serializeToMiniMessage(Component component) {
        if (component == null) {
            return "";
        }
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Converts legacy color-coded text to a Component.
     * This uses the '&' character as the color code indicator.
     *
     * @param legacyText the legacy formatted text
     * @return the parsed Component
     */
    public static Component legacyToComponent(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(legacyText);
    }

    /**
     * Converts a Component to legacy color-coded text using '§' section signs.
     *
     * @param component the component to convert
     * @return the legacy formatted text
     */
    public static String componentToLegacy(Component component) {
        if (component == null) {
            return "";
        }
        return SECTION_SERIALIZER.serialize(component);
    }

    /**
     * Converts legacy '&' color codes to '§' section signs.
     * This is useful when you need to use the text with Bukkit APIs that expect section signs.
     *
     * @param text the text with '&' color codes
     * @return the text with '§' section signs
     */
    public static String ampersandToSection(String text) {
        return translateLegacyColors(text);
    }
}
