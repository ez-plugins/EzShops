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
        metrics = new Metrics(plugin, 27734);
    }

    @Override
    public void disable() {
        metrics = null;
    }
}
