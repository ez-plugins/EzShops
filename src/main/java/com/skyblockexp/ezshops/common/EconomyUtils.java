package com.skyblockexp.ezshops.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility helpers for working with economy amounts.
 */
public final class EconomyUtils {

    private static final int MAX_DECIMALS = 2;

    private EconomyUtils() {
    }

    /**
     * Normalizes a currency amount to the maximum number of decimal places supported by the
     * economy provider. Values are rounded using {@link RoundingMode#HALF_UP} so that rounding
     * errors from floating point math do not trigger provider validation errors.
     *
     * @param amount raw value calculated by plugin logic
     * @return the amount rounded to two decimal places
     */
    public static double normalizeCurrency(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return 0.0D;
        }
        BigDecimal decimal = BigDecimal.valueOf(amount);
        decimal = decimal.setScale(MAX_DECIMALS, RoundingMode.HALF_UP);
        return decimal.doubleValue();
    }
}
