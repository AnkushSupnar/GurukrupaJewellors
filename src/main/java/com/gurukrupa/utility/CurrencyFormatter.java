package com.gurukrupa.utility;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Utility class for formatting currency amounts in Indian numbering system
 * Indian format: 1,00,00,000.00 (lakhs and crores)
 */
public class CurrencyFormatter {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    /**
     * Format amount with rupee symbol in Indian numbering system
     * Example: 992000.00 → ₹ 9,92,000.00
     */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "₹ 0.00";
        }
        return "₹ " + formatIndianNumber(amount);
    }

    /**
     * Format amount with rupee symbol in Indian numbering system
     * Example: 2405600.00 → ₹ 24,05,600.00
     */
    public static String format(Double amount) {
        if (amount == null) {
            return "₹ 0.00";
        }
        return "₹ " + formatIndianNumber(BigDecimal.valueOf(amount));
    }

    /**
     * Format amount without rupee symbol in Indian numbering system
     * Example: 992000.00 → 9,92,000.00
     */
    public static String formatWithoutSymbol(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return formatIndianNumber(amount);
    }

    /**
     * Format amount without rupee symbol in Indian numbering system
     * Example: 2405600.00 → 24,05,600.00
     */
    public static String formatWithoutSymbol(Double amount) {
        if (amount == null) {
            return "0.00";
        }
        return formatIndianNumber(BigDecimal.valueOf(amount));
    }

    /**
     * Format number in Indian numbering system
     * Indian format: First comma after 3 digits from right, then every 2 digits
     * Examples:
     *   1000 → 1,000
     *   10000 → 10,000
     *   100000 → 1,00,000
     *   1000000 → 10,00,000
     *   10000000 → 1,00,00,000
     */
    private static String formatIndianNumber(BigDecimal number) {
        // Format decimal part (always 2 decimal places)
        String formatted = DECIMAL_FORMAT.format(number);

        // Split into integer and decimal parts
        String[] parts = formatted.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "00";

        // Handle negative numbers
        boolean isNegative = integerPart.startsWith("-");
        if (isNegative) {
            integerPart = integerPart.substring(1);
        }

        // Apply Indian numbering format to integer part
        String formattedInteger = applyIndianGrouping(integerPart);

        // Reconstruct the number
        return (isNegative ? "-" : "") + formattedInteger + "." + decimalPart;
    }

    /**
     * Apply Indian grouping to integer part
     * Pattern: Last 3 digits, then groups of 2 from right to left
     */
    private static String applyIndianGrouping(String number) {
        int length = number.length();

        // If number is 3 digits or less, no grouping needed
        if (length <= 3) {
            return number;
        }

        StringBuilder result = new StringBuilder();

        // Get last 3 digits
        String lastThree = number.substring(length - 3);
        result.insert(0, lastThree);

        // Get remaining digits
        String remaining = number.substring(0, length - 3);

        // Add groups of 2 from right to left
        while (remaining.length() > 0) {
            result.insert(0, ",");

            if (remaining.length() <= 2) {
                result.insert(0, remaining);
                break;
            } else {
                String group = remaining.substring(remaining.length() - 2);
                result.insert(0, group);
                remaining = remaining.substring(0, remaining.length() - 2);
            }
        }

        return result.toString();
    }
}
