package com.skyblockexp.ezshops.repository;

import com.skyblockexp.ezshops.playershop.PlayerShop;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Map;

/**
 * Repository interface for player shop persistence operations.
 */
public interface PlayerShopRepository {
    
    /**
     * Loads all player shops from storage.
     *
     * @return collection of loaded shops
     */
    Collection<PlayerShop> loadShops();
    
    /**
     * Saves all player shops to storage.
     *
     * @param shopsBySign map of shops indexed by sign location key
     * @param deferredEntries map of deferred shop entries for unloaded worlds
     */
    void saveShops(Map<String, PlayerShop> shopsBySign, Map<String, Map<String, Object>> deferredEntries);
    
    /**
     * Generates a location key for indexing.
     *
     * @param location the location
     * @return string key representation
     */
    String locationKey(Location location);
    
    /**
     * Parses a location from a key string.
     *
     * @param key the location key
     * @return the parsed location or null
     */
    Location parseLocation(String key);
    
    /**
     * Extracts the world name from a location key.
     *
     * @param key the location key
     * @return the world name or null
     */
    String worldNameForKey(String key);
    
    /**
     * Gets deferred shop entries for worlds not yet loaded.
     *
     * @return map of deferred entries
     */
    Map<String, Map<String, Object>> getDeferredEntries();
}
