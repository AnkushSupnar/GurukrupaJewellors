package com.gurukrupa.utility;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#,##,##0.00");
    
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "₹ 0.00";
        }
        return CURRENCY_FORMATTER.format(amount);
    }
    
    public static String format(Double amount) {
        if (amount == null) {
            return "₹ 0.00";
        }
        return CURRENCY_FORMATTER.format(amount);
    }
    
    public static String formatWithoutSymbol(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return DECIMAL_FORMATTER.format(amount);
    }
    
    public static String formatWithoutSymbol(Double amount) {
        if (amount == null) {
            return "0.00";
        }
        return DECIMAL_FORMATTER.format(amount);
    }
}