package com.gurukrupa.utility;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class WeightFormatter {
    
    private static final DecimalFormat WEIGHT_FORMATTER = new DecimalFormat("#,##0.000");
    private static final DecimalFormat SIMPLE_FORMATTER = new DecimalFormat("0.000");
    
    public static String format(BigDecimal weight) {
        if (weight == null) {
            return "0.000";
        }
        return WEIGHT_FORMATTER.format(weight);
    }
    
    public static String format(Double weight) {
        if (weight == null) {
            return "0.000";
        }
        return WEIGHT_FORMATTER.format(weight);
    }
    
    public static String formatSimple(BigDecimal weight) {
        if (weight == null) {
            return "0.000";
        }
        return SIMPLE_FORMATTER.format(weight);
    }
    
    public static String formatSimple(Double weight) {
        if (weight == null) {
            return "0.000";
        }
        return SIMPLE_FORMATTER.format(weight);
    }
    
    public static String formatWithUnit(BigDecimal weight) {
        if (weight == null) {
            return "0.000 g";
        }
        return WEIGHT_FORMATTER.format(weight) + " g";
    }
    
    public static String formatWithUnit(Double weight) {
        if (weight == null) {
            return "0.000 g";
        }
        return WEIGHT_FORMATTER.format(weight) + " g";
    }
}