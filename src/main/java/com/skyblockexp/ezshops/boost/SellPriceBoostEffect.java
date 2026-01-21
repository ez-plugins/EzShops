package com.skyblockexp.ezshops.boost;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;

/**
 * Factory for creating EzBoost sell price boost effects.
 * This creates the appropriate boost effect object at runtime if EzBoost is available.
 */
public class SellPriceBoostEffect {
    /**
     * Creates a sell price boost effect if EzBoost is available.
     * @return the boost effect object, or null if EzBoost is not available
     */
    public static Object create() {
        try {
            // Get the EzBoost plugin instance to access its class loader
            Plugin ezBoostPlugin = Bukkit.getPluginManager().getPlugin("EzBoost");
            if (ezBoostPlugin == null) {
                System.out.println("[EzShops] EzBoost plugin not found");
                return null;
            }
            ClassLoader ezBoostClassLoader = ezBoostPlugin.getClass().getClassLoader();
            Class<?> customBoostEffectClass = Class.forName("com.skyblockexp.ezboost.boost.CustomBoostEffect", true, ezBoostClassLoader);
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                ezBoostClassLoader,
                new Class<?>[]{customBoostEffectClass},
                (proxyObj, method, args) -> {
                    switch (method.getName()) {
                        case "apply":
                            // No action needed, EzShops will check for active boost
                            return null;
                        case "remove":
                            // No action needed
                            return null;
                        case "getName":
                            return "ezshops_sellprice";
                        case "toString":
                            return "EzShopsSellPriceBoostEffect";
                        case "hashCode":
                            return 0;
                        case "equals":
                            return proxyObj == args[0];
                        default:
                            return null;
                    }
                }
            );
            return proxy;
        } catch (Exception e) {
            System.out.println("[EzShops] Failed to create boost effect: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}