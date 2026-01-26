package com.skyblockexp.ezshops.config;

import com.skyblockexp.ezshops.common.MessageUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {translate:key.path} tokens inside configuration values using ShopMessageConfiguration.
 */
public final class ConfigTranslator {

    private static final Pattern TRANSLATE_PATTERN = Pattern.compile("\\{translate:([a-zA-Z0-9_.-]+)\\}");

    private ConfigTranslator() {}

    public static String resolve(String raw, ShopMessageConfiguration messages) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        if (messages == null) {
            try {
                org.bukkit.plugin.Plugin p = org.bukkit.Bukkit.getPluginManager().getPlugin("EzShops");
                if (p instanceof com.skyblockexp.ezshops.EzShopsPlugin ez) {
                    messages = ez.getCoreShopComponent().messageConfiguration();
                }
            } catch (Exception ignored) {
                // ignore and fallback
            }
            if (messages == null) {
                return MessageUtil.translateColors(raw);
            }
        }
        Matcher m = TRANSLATE_PATTERN.matcher(raw);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String replacement = messages.lookup(key, "");
            // lookup already colorizes; escape dollars
            replacement = replacement.replace("$", "\\$");
            m.appendReplacement(sb, replacement == null ? "" : replacement);
        }
        m.appendTail(sb);
        return MessageUtil.translateColors(sb.toString());
    }
}
