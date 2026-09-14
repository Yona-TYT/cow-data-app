package com.example.cow_data.utls;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for general mathematical and financial calculations.
 */
public final class MathUtls {

    // Constructor privado para evitar que la clase se instancie
    private MathUtls() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Adds a percentage to a base value.
     */
    public static double addPercentage(double baseValue, double percentage) {
        return baseValue + (baseValue * (percentage / 100.0));
    }

    /**
     * Subtracts a percentage from a base value (useful for discounts).
     */
    public static double subtractPercentage(double baseValue, double percentage) {
        return baseValue - (baseValue * (percentage / 100.0));
    }

    /**
     * Calculates the exact value of a percentage from a total.
     * e.g., 10% of 25 returns 2.5
     */
    public static double getPercentageValue(double totalValue, double percentage) {
        return totalValue * (percentage / 100.0);
    }

    /**
     * Rounds a double value to a specific number of decimal places.
     * Prevents common double floating-point precision issues.
     */
    public static double round(double value, int decimalPlaces) {
        if (decimalPlaces < 0) throw new IllegalArgumentException("Decimal places cannot be negative");
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
