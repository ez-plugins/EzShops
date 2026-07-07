package com.skyblockexp.ezshops.bootstrap;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Global runtime registry for bootstrap-managed EzShops services/components.
 */
public final class EzShopsRegistry {

    private static volatile EzShopsRegistry current = new EzShopsRegistry();

    private CoreShopComponent coreShopComponent;
    private TeamShopComponent teamShopComponent;
    private StockComponent stockComponent;
    private PlayerShopComponent playerShopComponent;
    private boolean debugMode;

    private Function<String, Integer> reseedCategoryDefaults = mode -> 0;
    private Supplier<Set<String>> bundledShopModes = Set::of;

    public static EzShopsRegistry current() {
        return current;
    }

    static void install(EzShopsRegistry registry) {
        current = registry == null ? new EzShopsRegistry() : registry;
    }

    public CoreShopComponent getCoreShopComponent() {
        return coreShopComponent;
    }

    public TeamShopComponent getTeamShopComponent() {
        return teamShopComponent;
    }

    public StockComponent getStockComponent() {
        return stockComponent;
    }

    public PlayerShopComponent getPlayerShopComponent() {
        return playerShopComponent;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void reloadFeatures() {
        if (playerShopComponent != null) {
            playerShopComponent.reload();
        }
        if (stockComponent != null) {
            stockComponent.reload();
        }
    }

    public int reseedCategoryDefaults(String modeFilter) {
        return reseedCategoryDefaults.apply(modeFilter);
    }

    public Set<String> getBundledShopModes() {
        return new LinkedHashSet<>(bundledShopModes.get());
    }

    void setCoreShopComponent(CoreShopComponent coreShopComponent) {
        this.coreShopComponent = coreShopComponent;
    }

    void setTeamShopComponent(TeamShopComponent teamShopComponent) {
        this.teamShopComponent = teamShopComponent;
    }

    void setStockComponent(StockComponent stockComponent) {
        this.stockComponent = stockComponent;
    }

    void setPlayerShopComponent(PlayerShopComponent playerShopComponent) {
        this.playerShopComponent = playerShopComponent;
    }

    void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    void setReseedCategoryDefaults(Function<String, Integer> reseedCategoryDefaults) {
        this.reseedCategoryDefaults = reseedCategoryDefaults == null ? mode -> 0 : reseedCategoryDefaults;
    }

    void setBundledShopModes(Supplier<Set<String>> bundledShopModes) {
        this.bundledShopModes = bundledShopModes == null ? Set::of : bundledShopModes;
    }
}
