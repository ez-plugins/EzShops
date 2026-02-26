package com.skyblockexp.ezshops.boost;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class DiscountBoostEffect {

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
                                return "ezshops_discountboost";
                            case "toString":
                                return "EzShopsDiscountBoostEffect";
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
