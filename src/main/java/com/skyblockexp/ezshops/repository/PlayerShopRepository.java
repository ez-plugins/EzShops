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
     * Saves all player shops to storage.  The repository is responsible for
     * merging any deferred entries (shops whose worlds were not loaded at
     * startup) back into the persisted file.
     *
     * @param shopsBySign map of active shops indexed by sign location key
     */
    void saveShops(Map<String, PlayerShop> shopsBySign);
    
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
}
