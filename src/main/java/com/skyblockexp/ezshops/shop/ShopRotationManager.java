package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.gui.ShopMenu;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles automatic advancement of configured shop rotations.
 */
public final class ShopRotationManager {

    private final JavaPlugin plugin;
    private final ShopPricingManager pricingManager;
    private final ShopMenu shopMenu;
    private final File stateFile;
    private final Map<String, RotationState> rotationStates = new LinkedHashMap<>();
    private BukkitTask task;

    public ShopRotationManager(JavaPlugin plugin, ShopPricingManager pricingManager, ShopMenu shopMenu) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.pricingManager = Objects.requireNonNull(pricingManager, "pricingManager");
        this.shopMenu = shopMenu;
        this.stateFile = new File(plugin.getDataFolder(), "shop-rotations.yml");
    }

    public void enable() {
        loadState();
        Map<String, ShopRotationDefinition> definitions = pricingManager.getRotationDefinitions();
        Map<String, String> activeOptions = pricingManager.getActiveRotationOptions();
        long now = System.currentTimeMillis();

        UpdateResult syncResult = synchronizeStates(definitions, activeOptions, now);
        if (syncResult.stateChanged()) {
            saveState();
        }
        if (shopMenu != null && syncResult.layoutChanged()) {
            shopMenu.refreshViewers();
        }

        if (task != null) {
            task.cancel();
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        saveState();
        rotationStates.clear();
    }

    private void tick() {
        Map<String, ShopRotationDefinition> definitions = pricingManager.getRotationDefinitions();
        Map<String, String> activeOptions = pricingManager.getActiveRotationOptions();
        long now = System.currentTimeMillis();

        UpdateResult syncResult = synchronizeStates(definitions, activeOptions, now);
        UpdateResult advanceResult = processDueRotations(definitions, now);

        boolean stateChanged = syncResult.stateChanged() || advanceResult.stateChanged();
        boolean layoutChanged = syncResult.layoutChanged() || advanceResult.layoutChanged();

        if (stateChanged) {
            saveState();
        }
        if (layoutChanged && shopMenu != null) {
            shopMenu.refreshViewers();
        }
    }

    private UpdateResult synchronizeStates(Map<String, ShopRotationDefinition> definitions,
            Map<String, String> activeOptions, long now) {
        boolean stateChanged = false;
        boolean layoutChanged = false;

        if (rotationStates.keySet().removeIf(id -> !definitions.containsKey(id))) {
            stateChanged = true;
        }

        for (Map.Entry<String, ShopRotationDefinition> entry : definitions.entrySet()) {
            String rotationId = entry.getKey();
            ShopRotationDefinition definition = entry.getValue();
            RotationState state = rotationStates.computeIfAbsent(rotationId, key -> new RotationState());

            String previousActive = state.activeOptionId;
            Long previousNext = state.nextChangeMillis;

            String pricingActive = activeOptions.get(rotationId);
            if (pricingActive != null && !definition.containsOption(pricingActive)) {
                pricingActive = null;
            }

            String savedActive = state.activeOptionId;
            if (savedActive != null && !definition.containsOption(savedActive)) {
                savedActive = null;
            }

            String resolvedActive = pricingActive != null ? pricingActive
                    : (savedActive != null ? savedActive : definition.defaultOptionId());

            if (!Objects.equals(pricingActive, resolvedActive)) {
                if (!pricingManager.setActiveRotationOption(rotationId, resolvedActive)) {
                    resolvedActive = definition.defaultOptionId();
                    pricingManager.setActiveRotationOption(rotationId, resolvedActive);
                }
                pricingActive = resolvedActive;
                layoutChanged = true;
            }

            if (!Objects.equals(previousActive, pricingActive)) {
                state.activeOptionId = pricingActive;
                stateChanged = true;
                layoutChanged = true;
            }

            Long targetNext;
            if (!Objects.equals(previousActive, pricingActive)) {
                targetNext = computeNextChange(definition, now);
            } else {
                targetNext = normalizeNextChange(definition, previousNext, now);
            }

            if (!Objects.equals(previousNext, targetNext)) {
                state.nextChangeMillis = targetNext;
                stateChanged = true;
            }
        }

        return new UpdateResult(stateChanged, layoutChanged);
    }

    private UpdateResult processDueRotations(Map<String, ShopRotationDefinition> definitions, long now) {
        boolean stateChanged = false;
        boolean layoutChanged = false;

        for (Map.Entry<String, ShopRotationDefinition> entry : definitions.entrySet()) {
            String rotationId = entry.getKey();
            ShopRotationDefinition definition = entry.getValue();
            RotationState state = rotationStates.get(rotationId);
            if (state == null) {
                continue;
            }

            Long nextChange = state.nextChangeMillis;
            if (nextChange == null || nextChange > now) {
                continue;
            }

            String currentOption = state.activeOptionId;
            if (currentOption == null || !definition.containsOption(currentOption)) {
                currentOption = definition.defaultOptionId();
            }

            String nextOption = determineNextOption(definition, currentOption);
            if (!definition.containsOption(nextOption)) {
                nextOption = definition.defaultOptionId();
            }

            if (!Objects.equals(currentOption, nextOption)) {
                if (!pricingManager.setActiveRotationOption(rotationId, nextOption)) {
                    nextOption = definition.defaultOptionId();
                    pricingManager.setActiveRotationOption(rotationId, nextOption);
                }
                state.activeOptionId = nextOption;
                layoutChanged = true;
                stateChanged = true;
                plugin.getLogger().info("Rotation '" + rotationId + "' advanced to option '" + nextOption + "'.");
            }

            Long newNext = computeNextChange(definition, now);
            if (!Objects.equals(state.nextChangeMillis, newNext)) {
                state.nextChangeMillis = newNext;
                stateChanged = true;
            }
        }

        return new UpdateResult(stateChanged, layoutChanged);
    }

    private String determineNextOption(ShopRotationDefinition definition, String currentOption) {
        return switch (definition.mode()) {
            case SEQUENTIAL -> pickSequential(definition, currentOption);
            case RANDOM -> pickRandom(definition, currentOption);
        };
    }

    private String pickSequential(ShopRotationDefinition definition, String currentOption) {
        List<ShopRotationOption> options = definition.options();
        if (options.isEmpty()) {
            return currentOption;
        }
        int currentIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id().equalsIgnoreCase(currentOption)) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = currentIndex >= 0 ? (currentIndex + 1) % options.size() : 0;
        return options.get(nextIndex).id();
    }

    private String pickRandom(ShopRotationDefinition definition, String currentOption) {
        List<ShopRotationOption> options = definition.options();
        if (options.isEmpty()) {
            return currentOption;
        }

        double totalWeight = 0.0D;
        for (ShopRotationOption option : options) {
            totalWeight += Math.max(0.0D, option.weight());
        }

        if (totalWeight <= 0.0D) {
            return options.get(ThreadLocalRandom.current().nextInt(options.size())).id();
        }

        double selection = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0D;
        for (ShopRotationOption option : options) {
            double weight = Math.max(0.0D, option.weight());
            if (weight <= 0.0D) {
                continue;
            }
            cumulative += weight;
            if (selection < cumulative) {
                return option.id();
            }
        }
        return options.get(options.size() - 1).id();
    }

    private Long computeNextChange(ShopRotationDefinition definition, long now) {
        Duration interval = definition.interval();
        if (interval == null || interval.isZero() || interval.isNegative()) {
            return null;
        }
        long delay = Math.max(1L, interval.toMillis());
        return now + delay;
    }

    private Long normalizeNextChange(ShopRotationDefinition definition, Long currentValue, long now) {
        Duration interval = definition.interval();
        if (interval == null || interval.isZero() || interval.isNegative()) {
            return null;
        }
        if (currentValue != null && currentValue > now) {
            return currentValue;
        }
        return computeNextChange(definition, now);
    }

    private void loadState() {
        rotationStates.clear();
        if (!stateFile.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(stateFile);
        ConfigurationSection rotationsSection = configuration.getConfigurationSection("rotations");
        if (rotationsSection == null) {
            return;
        }

        for (String rotationId : rotationsSection.getKeys(false)) {
            ConfigurationSection section = rotationsSection.getConfigurationSection(rotationId);
            if (section == null) {
                continue;
            }

            String activeOption = section.getString("active-option");
            Long nextChange = null;
            if (section.contains("next-change")) {
                long stored = section.getLong("next-change");
                if (stored > 0L) {
                    nextChange = stored;
                }
            }
            rotationStates.put(rotationId, new RotationState(activeOption, nextChange));
        }
    }

    private void saveState() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for shop rotation state.");
            return;
        }

        YamlConfiguration configuration = new YamlConfiguration();
        if (!rotationStates.isEmpty()) {
            ConfigurationSection rotationsSection = configuration.createSection("rotations");
            for (Map.Entry<String, RotationState> entry : rotationStates.entrySet()) {
                RotationState state = entry.getValue();
                ConfigurationSection section = rotationsSection.createSection(entry.getKey());
                if (state.activeOptionId != null && !state.activeOptionId.isBlank()) {
                    section.set("active-option", state.activeOptionId);
                }
                if (state.nextChangeMillis != null && state.nextChangeMillis > 0L) {
                    section.set("next-change", state.nextChangeMillis);
                }
            }
        } else {
            configuration.createSection("rotations");
        }

        try {
            configuration.save(stateFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save shop rotation state.", ex);
        }
    }

    private record UpdateResult(boolean stateChanged, boolean layoutChanged) {
    }

    private static final class RotationState {

        private String activeOptionId;
        private Long nextChangeMillis;

        private RotationState() {
        }

        private RotationState(String activeOptionId, Long nextChangeMillis) {
            this.activeOptionId = activeOptionId;
            this.nextChangeMillis = nextChangeMillis;
        }
    }
}

