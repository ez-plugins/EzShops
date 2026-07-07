package com.skyblockexp.ezshops;

import com.skyblockexp.ezshops.bootstrap.EzShopsBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Thin plugin entry point. Startup/shutdown orchestration lives in bootstrap.
 */
public class EzShopsPlugin extends JavaPlugin {

    private final EzShopsBootstrap bootstrap = new EzShopsBootstrap(this);

    @Override
    public void onEnable() {
        bootstrap.start();
    }

    @Override
    public void onDisable() {
        bootstrap.stop();
    }
}
