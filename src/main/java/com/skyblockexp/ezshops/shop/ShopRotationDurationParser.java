package com.skyblockexp.ezshops.shop;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;

final class ShopRotationDurationParser {

    private ShopRotationDurationParser() {}

    static Duration parse(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Duration.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // handled below
        }
        return parseCompact(trimmed);
    }

    private static Duration parseCompact(String input) {
        String normalized = input.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.isEmpty()) {
            return null;
        }
        double multiplier = 60.0D; // minutes expressed in seconds
        String numeric = normalized;
        if (normalized.endsWith("days")) {
            multiplier = 86400.0D;
            numeric = normalized.substring(0, normalized.length() - 4);
        } else if (normalized.endsWith("day")) {
            multiplier = 86400.0D;
            numeric = normalized.substring(0, normalized.length() - 3);
        } else if (normalized.endsWith("d")) {
            multiplier = 86400.0D;
            numeric = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("hours")) {
            multiplier = 3600.0D;
            numeric = normalized.substring(0, normalized.length() - 5);
        } else if (normalized.endsWith("hour")) {
            multiplier = 3600.0D;
            numeric = normalized.substring(0, normalized.length() - 4);
        } else if (normalized.endsWith("hrs")) {
            multiplier = 3600.0D;
            numeric = normalized.substring(0, normalized.length() - 3);
        } else if (normalized.endsWith("hr")) {
            multiplier = 3600.0D;
            numeric = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("h")) {
            multiplier = 3600.0D;
            numeric = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("minutes")) {
            multiplier = 60.0D;
            numeric = normalized.substring(0, normalized.length() - 7);
        } else if (normalized.endsWith("minute")) {
            multiplier = 60.0D;
            numeric = normalized.substring(0, normalized.length() - 6);
        } else if (normalized.endsWith("mins")) {
            multiplier = 60.0D;
            numeric = normalized.substring(0, normalized.length() - 4);
        } else if (normalized.endsWith("min")) {
            multiplier = 60.0D;
            numeric = normalized.substring(0, normalized.length() - 3);
        } else if (normalized.endsWith("m")) {
            multiplier = 60.0D;
            numeric = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("seconds")) {
            multiplier = 1.0D;
            numeric = normalized.substring(0, normalized.length() - 7);
        } else if (normalized.endsWith("second")) {
            multiplier = 1.0D;
            numeric = normalized.substring(0, normalized.length() - 6);
        } else if (normalized.endsWith("secs")) {
            multiplier = 1.0D;
            numeric = normalized.substring(0, normalized.length() - 4);
        } else if (normalized.endsWith("sec")) {
            multiplier = 1.0D;
            numeric = normalized.substring(0, normalized.length() - 3);
        } else if (normalized.endsWith("s")) {
            multiplier = 1.0D;
            numeric = normalized.substring(0, normalized.length() - 1);
        }

        double amount;
        try {
            amount = Double.parseDouble(numeric.trim());
        } catch (NumberFormatException ex) {
            return null;
        }

        double seconds = amount * multiplier;
        if (!Double.isFinite(seconds) || seconds <= 0.0D) {
            return null;
        }
        long rounded = Math.max(1L, Math.round(seconds));
        try {
            return Duration.ofSeconds(rounded);
        } catch (ArithmeticException ex) {
            return null;
        }
    }
}

