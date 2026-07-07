package com.skyblockexp.ezshops;

import com.skyblockexp.ezshops.bootstrap.EzShopsBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Thin plugin entry point. Startup/shutdown orchestration lives in bootstrap.
 */
public class EzShopsPlugin extends JavaPlugin {

    private EzShopsBootstrap bootstrap;

    @Override
    public void onEnable() {
        if (bootstrap == null) {
            bootstrap = new EzShopsBootstrap(this);
        }
        bootstrap.start();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.stop();
        }
    }
}
