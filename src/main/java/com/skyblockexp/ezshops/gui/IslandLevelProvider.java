package com.skyblockexp.ezshops.gui;

import org.bukkit.entity.Player;

/**
 * Resolves the island level for a player. Implementations may integrate with other plugins
 * to obtain the information or provide fallback values when it is unavailable. When no
 * provider is available EzShops falls back to a disabled state that treats every player as
 * meeting island level requirements.
 */
@FunctionalInterface
public interface IslandLevelProvider {

    /**
     * Resolves the island level for the provided player.
     *
     * @param player the player whose level should be resolved
     * @return the island level, or {@code 0} if none is available
     */
    int getIslandLevel(Player player);
}
