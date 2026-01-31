package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import org.bstats.bukkit.Metrics;

/**
 * Registers plugin metrics via bStats.
 */
public final class MetricsComponent implements PluginComponent {

    private Metrics metrics;

    @Override
    public void enable(EzShopsPlugin plugin) {
        try {
            metrics = new Metrics(plugin, 27734);
        } catch (IllegalStateException ex) {
            // bStats may not be relocated or available in test environments (MockBukkit).
            plugin.getLogger().warning("bStats not available; skipping metrics: " + ex.getMessage());
            metrics = null;
        }
    }

    @Override
    public void disable() {
        metrics = null;
    }
}
