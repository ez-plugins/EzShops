package com.skyblockexp.ezshops.shop;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Captures the metadata for a rotation group including its options.
 */
public final class ShopRotationDefinition {

    private final String id;
    private final Duration interval;
    private final ShopRotationMode mode;
    private final List<ShopRotationOption> options;
    private final Map<String, ShopRotationOption> optionLookup;
    private final String defaultOptionId;

    public ShopRotationDefinition(String id, Duration interval, ShopRotationMode mode,
            List<ShopRotationOption> options, String defaultOptionId) {
        this.id = Objects.requireNonNull(id, "id");
        this.interval = interval;
        this.mode = mode == null ? ShopRotationMode.SEQUENTIAL : mode;
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Rotation definitions require at least one option.");
        }
        this.options = List.copyOf(options);
        Map<String, ShopRotationOption> lookup = new LinkedHashMap<>();
        for (ShopRotationOption option : options) {
            lookup.put(option.id(), option);
        }
        this.optionLookup = Collections.unmodifiableMap(lookup);
        String resolvedDefault = defaultOptionId;
        if (resolvedDefault == null || !lookup.containsKey(resolvedDefault)) {
            resolvedDefault = options.get(0).id();
        }
        this.defaultOptionId = resolvedDefault;
    }

    public String id() {
        return id;
    }

    public Duration interval() {
        return interval;
    }

    public ShopRotationMode mode() {
        return mode;
    }

    public List<ShopRotationOption> options() {
        return options;
    }

    public Optional<ShopRotationOption> option(String optionId) {
        if (optionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(optionLookup.get(optionId));
    }

    public boolean containsOption(String optionId) {
        return optionLookup.containsKey(optionId);
    }

    public String defaultOptionId() {
        return defaultOptionId;
    }
}

