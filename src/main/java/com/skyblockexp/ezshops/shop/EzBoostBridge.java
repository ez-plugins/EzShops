package com.skyblockexp.ezshops.shop;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caches reflective access to EzBoost's API so that per-transaction multiplier
 * lookups never re-resolve {@code Class.forName} or {@code getMethod} after the
 * first successful setup.
 *
 * <p>Boost-class and effect-class {@link Method} objects are captured from the
 * first live instance encountered and reused for all subsequent calls.  If
 * EzBoost is absent or the API shape changes the methods return the neutral
 * multiplier {@code 1.0}.</p>
 */
public final class EzBoostBridge {

    static final double NEUTRAL = 1.0;

    private static final String SELL_EFFECT = "ezshops_sellprice";
    private static final String BUY_EFFECT  = "ezshops_discountboost";

    private final Logger logger;

    // ---------- lazily resolved, written once, then read-only ----------
    private volatile boolean setupDone = false;
    private volatile boolean available = false;

    // Manager-level methods (keyed on EzBoostAPI / BoostManager class)
    private Method mGetBoostManager; // static: () → BoostManager
    private Method mGetBoosts;        // instance: (Player) → Map
    private Method mIsActive;         // instance: (Player, String) → Boolean

    // Boost-level and effect-level methods (keyed on first instance's class)
    private Method mGetKey;       // boost.key()
    private Method mGetEffects;   // boost.effects() → Collection
    private Method mCustomName;   // effect.customName()
    private Method mAmplifier;    // effect.amplifier() → Number

    public EzBoostBridge(Logger logger) {
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    double getSellMultiplier(Player player) {
        ensureSetup();
        if (!available) return NEUTRAL;
        try {
            return computeMultiplier(player, SELL_EFFECT, true);
        } catch (Throwable t) {
            logger.log(Level.FINE, "EzBoostBridge sell multiplier error", t);
            return NEUTRAL;
        }
    }

    double getBuyMultiplier(Player player) {
        ensureSetup();
        if (!available) return NEUTRAL;
        try {
            double m = computeMultiplier(player, BUY_EFFECT, false);
            return Math.max(0.0, m);
        } catch (Throwable t) {
            logger.log(Level.FINE, "EzBoostBridge buy multiplier error", t);
            return NEUTRAL;
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private double computeMultiplier(Player player, String effectKey, boolean additive) throws Throwable {
        Object manager = mGetBoostManager.invoke(null);
        if (manager == null) return NEUTRAL;

        Map<String, Object> boosts = (Map<String, Object>) mGetBoosts.invoke(manager, player);
        if (boosts == null || boosts.isEmpty()) return NEUTRAL;

        // Ensure boost/effect methods are resolved from the first live instance
        ensureBoostMethods(manager, player, boosts);

        double multiplier = NEUTRAL;
        for (Object boost : boosts.values()) {
            String key = (String) mGetKey.invoke(boost);
            if (!Boolean.TRUE.equals(mIsActive.invoke(manager, player, key))) continue;

            Collection<Object> effects = (Collection<Object>) mGetEffects.invoke(boost);
            if (effects == null) continue;
            for (Object effect : effects) {
                if (effectKey.equals(mCustomName.invoke(effect))) {
                    Number amp = (Number) mAmplifier.invoke(effect);
                    if (additive) {
                        multiplier += amp.doubleValue() / 100.0;
                    } else {
                        multiplier -= amp.doubleValue() / 100.0;
                    }
                }
            }
        }
        return multiplier;
    }

    private void ensureSetup() {
        if (setupDone) return;
        synchronized (this) {
            if (setupDone) return;
            setupDone = true;
            trySetup();
        }
    }

    private void trySetup() {
        org.bukkit.plugin.Plugin ezBoostPlugin =
                org.bukkit.Bukkit.getPluginManager().getPlugin("EzBoost");
        if (ezBoostPlugin == null) return;
        try {
            ClassLoader cl = ezBoostPlugin.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.skyblockexp.ezboost.api.EzBoostAPI", true, cl);
            mGetBoostManager = apiClass.getMethod("getBoostManager");

            // Obtain the manager instance to discover its class for getBoosts / isActive
            Object manager = mGetBoostManager.invoke(null);
            if (manager == null) {
                logger.fine("EzBoostBridge: getBoostManager() returned null; retrying on first transaction.");
                setupDone = false; // allow retry
                return;
            }
            Class<?> managerClass = manager.getClass();
            mGetBoosts = managerClass.getMethod("getBoosts", Player.class);
            mIsActive  = managerClass.getMethod("isActive", Player.class, String.class);

            available = true;
            logger.fine("EzBoostBridge: manager-level setup complete.");
            // Note: boost/effect methods are resolved lazily from the first live instance.
        } catch (Throwable t) {
            logger.log(Level.FINE, "EzBoostBridge setup failed; integration disabled.", t);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void ensureBoostMethods(Object manager, Player player,
                                                  Map<String, Object> boosts) throws Throwable {
        if (mGetKey != null) return; // already resolved

        Object firstBoost = boosts.values().iterator().next();
        Class<?> boostClass = firstBoost.getClass();
        mGetKey     = boostClass.getMethod("key");
        mGetEffects = boostClass.getMethod("effects");

        // Find a non-empty effects collection to resolve the effect class
        for (Object boost : boosts.values()) {
            Collection<Object> effects = (Collection<Object>) mGetEffects.invoke(boost);
            if (effects != null && !effects.isEmpty()) {
                Object firstEffect = effects.iterator().next();
                Class<?> effectClass = firstEffect.getClass();
                mCustomName = effectClass.getMethod("customName");
                mAmplifier  = effectClass.getMethod("amplifier");
                logger.fine("EzBoostBridge: boost/effect method setup complete.");
                return;
            }
        }
        // No effects found yet — will retry next call
        mGetKey = null;
    }
}

