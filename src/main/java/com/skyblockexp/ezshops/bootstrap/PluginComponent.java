package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;

/**
 * Basic lifecycle contract for EzShops plugin components.
 */
public interface PluginComponent {

    /**
     * Enable the component for the supplied plugin instance.
     *
     * @param plugin EzShops plugin instance
     */
    void enable(EzShopsPlugin plugin);

    /**
     * Disable the component, releasing any resources it holds.
     */
    void disable();
}
