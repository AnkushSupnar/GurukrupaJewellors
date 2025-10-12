package com.gurukrupa.utility;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility class for formatting currency amounts in Indian numbering system
 * Indian format: 1,00,00,000.00 (lakhs and crores)
 */
public class CurrencyFormatter {

    // Indian numbering system: First comma after 3 digits, then every 2 digits
    private static final DecimalFormat INDIAN_CURRENCY_FORMAT;
    private static final DecimalFormat INDIAN_DECIMAL_FORMAT;

    static {
        // Create decimal format symbols for Indian locale
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "IN"));
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        // Format: #,##,##0.00 (Indian numbering system)
        INDIAN_CURRENCY_FORMAT = new DecimalFormat("₹ #,##,##0.00", symbols);
        INDIAN_DECIMAL_FORMAT = new DecimalFormat("#,##,##0.00", symbols);
    }

    /**
     * Format amount with rupee symbol in Indian numbering system
     * Example: 992000.00 → ₹ 9,92,000.00
     */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "₹ 0.00";
        }
        return INDIAN_CURRENCY_FORMAT.format(amount);
    }

    /**
     * Format amount with rupee symbol in Indian numbering system
     * Example: 2405600.00 → ₹ 24,05,600.00
     */
    public static String format(Double amount) {
        if (amount == null) {
            return "₹ 0.00";
        }
        return INDIAN_CURRENCY_FORMAT.format(amount);
    }

    /**
     * Format amount without rupee symbol in Indian numbering system
     * Example: 992000.00 → 9,92,000.00
     */
    public static String formatWithoutSymbol(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return INDIAN_DECIMAL_FORMAT.format(amount);
    }

    /**
     * Format amount without rupee symbol in Indian numbering system
     * Example: 2405600.00 → 24,05,600.00
     */
    public static String formatWithoutSymbol(Double amount) {
        if (amount == null) {
            return "0.00";
        }
        return INDIAN_DECIMAL_FORMAT.format(amount);
    }
}