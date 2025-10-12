package com.gurukrupa.utility;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for calculating metal purity conversions for Gold, Silver, and other precious metals.
 * Supports three purity formats:
 * 1. Karat (K) - typically 0-24 for gold (e.g., 22K, 18K)
 * 2. Fineness - parts per thousand (e.g., 916, 750)
 * 3. Percentage - 0-100% (e.g., 91.67%)
 */
public class PurityCalculator {

    private static final BigDecimal MAX_KARAT = new BigDecimal("24");
    private static final BigDecimal MAX_FINENESS = new BigDecimal("1000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 3; // Decimal places for calculations

    /**
     * Parse purity string to numeric value
     * Handles formats like "24K", "22K", "916", "92.5", etc.
     * @param purityStr Purity string (e.g., "22K", "916", "92.5")
     * @return Numeric purity value
     */
    public static BigDecimal parsePurityString(String purityStr) {
        if (purityStr == null || purityStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Remove common suffixes and whitespace
        String cleaned = purityStr.trim()
            .replaceAll("(?i)K", "")  // Remove 'K' or 'k'
            .replaceAll("%", "")       // Remove '%'
            .trim();

        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Check if value is in karat format (typically <= 24)
     * @param value Numeric purity value
     * @return true if value appears to be in karat format
     */
    public static boolean isKaratFormat(BigDecimal value) {
        return value != null &&
               value.compareTo(BigDecimal.ZERO) > 0 &&
               value.compareTo(MAX_KARAT) <= 0;
    }

    /**
     * Check if value is in percentage format (0-100)
     * @param value Numeric purity value
     * @return true if value appears to be in percentage format
     */
    public static boolean isPercentageFormat(BigDecimal value) {
        return value != null &&
               value.compareTo(MAX_KARAT) > 0 &&
               value.compareTo(HUNDRED) <= 0;
    }

    /**
     * Check if value is in fineness format (parts per thousand, typically > 100)
     * @param value Numeric purity value
     * @return true if value appears to be in fineness format
     */
    public static boolean isFivenessFormat(BigDecimal value) {
        return value != null &&
               value.compareTo(HUNDRED) > 0 &&
               value.compareTo(MAX_FINENESS) <= 0;
    }

    /**
     * Convert karat to percentage
     * Formula: (karat / 24) * 100
     * Example: 22K -> 91.67%
     * @param karat Karat value (0-24)
     * @return Percentage value
     */
    public static BigDecimal getPurityPercentage(BigDecimal karat) {
        if (karat == null || karat.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return karat.divide(MAX_KARAT, SCALE, RoundingMode.HALF_UP)
                   .multiply(HUNDRED)
                   .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert karat to fineness (parts per thousand)
     * Formula: (karat / 24) * 1000
     * Example: 22K -> 916
     * @param karat Karat value (0-24)
     * @return Fineness value
     */
    public static BigDecimal getFineness(BigDecimal karat) {
        if (karat == null || karat.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return karat.divide(MAX_KARAT, SCALE, RoundingMode.HALF_UP)
                   .multiply(MAX_FINENESS)
                   .setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Convert fineness to karat
     * Formula: (fineness / 1000) * 24
     * Example: 916 -> 22K
     * @param fineness Fineness value (0-1000)
     * @return Karat value
     */
    public static BigDecimal getKaratFromFineness(BigDecimal fineness) {
        if (fineness == null || fineness.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return fineness.divide(MAX_FINENESS, SCALE, RoundingMode.HALF_UP)
                      .multiply(MAX_KARAT)
                      .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert percentage to karat
     * Formula: (percentage / 100) * 24
     * Example: 91.67% -> 22K
     * @param percentage Percentage value (0-100)
     * @return Karat value
     */
    public static BigDecimal getKaratFromPercentage(BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return percentage.divide(HUNDRED, SCALE, RoundingMode.HALF_UP)
                        .multiply(MAX_KARAT)
                        .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate pure metal weight from gross/net weight and karat
     * Formula: weight * (karat / 24)
     * Example: 10g of 22K = 10 * (22/24) = 9.167g pure gold
     * @param weight Total weight in grams
     * @param karat Karat value (0-24)
     * @return Pure metal weight in grams
     */
    public static BigDecimal getPureMetalWeight(BigDecimal weight, BigDecimal karat) {
        if (weight == null || karat == null ||
            weight.compareTo(BigDecimal.ZERO) <= 0 ||
            karat.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal purityRatio = karat.divide(MAX_KARAT, SCALE, RoundingMode.HALF_UP);
        return weight.multiply(purityRatio)
                    .setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Calculate gross weight from pure metal weight and karat
     * Formula: pureWeight / (karat / 24)
     * Example: 9.167g pure gold at 22K = 9.167 / (22/24) = 10g
     * @param pureWeight Pure metal weight in grams
     * @param karat Karat value (0-24)
     * @return Gross weight in grams
     */
    public static BigDecimal getGrossWeightFromPure(BigDecimal pureWeight, BigDecimal karat) {
        if (pureWeight == null || karat == null ||
            pureWeight.compareTo(BigDecimal.ZERO) <= 0 ||
            karat.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal purityRatio = karat.divide(MAX_KARAT, SCALE, RoundingMode.HALF_UP);
        return pureWeight.divide(purityRatio, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Format purity display string showing karat, fineness, and percentage
     * Example: 22K -> "22K (916) - 91.67%"
     * @param karat Karat value
     * @return Formatted display string
     */
    public static String formatPurityDisplay(BigDecimal karat) {
        if (karat == null || karat.compareTo(BigDecimal.ZERO) <= 0) {
            return "N/A";
        }

        BigDecimal fineness = getFineness(karat);
        BigDecimal percentage = getPurityPercentage(karat);

        return String.format("%sK (%s) - %s%%",
            karat.setScale(0, RoundingMode.HALF_UP),
            fineness.setScale(0, RoundingMode.HALF_UP),
            percentage.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Convert any purity format to karat
     * Automatically detects format and converts
     * @param purity Purity value in any format
     * @return Karat value
     */
    public static BigDecimal convertToKarat(BigDecimal purity) {
        if (purity == null || purity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (isKaratFormat(purity)) {
            return purity;
        } else if (isFivenessFormat(purity)) {
            return getKaratFromFineness(purity);
        } else if (isPercentageFormat(purity)) {
            return getKaratFromPercentage(purity);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Convert any purity format to fineness
     * Automatically detects format and converts
     * @param purity Purity value in any format
     * @return Fineness value
     */
    public static BigDecimal convertToFineness(BigDecimal purity) {
        BigDecimal karat = convertToKarat(purity);
        return getFineness(karat);
    }

    /**
     * Convert any purity format to percentage
     * Automatically detects format and converts
     * @param purity Purity value in any format
     * @return Percentage value
     */
    public static BigDecimal convertToPercentage(BigDecimal purity) {
        BigDecimal karat = convertToKarat(purity);
        return getPurityPercentage(karat);
    }

    /**
     * Validate purity value is within acceptable range
     * @param purity Purity value
     * @return true if valid
     */
    public static boolean isValidPurity(BigDecimal purity) {
        if (purity == null || purity.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        return isKaratFormat(purity) ||
               isPercentageFormat(purity) ||
               isFivenessFormat(purity);
    }

    /**
     * Get common gold purity values in karat
     * @return Array of common karat values
     */
    public static BigDecimal[] getCommonGoldPurities() {
        return new BigDecimal[] {
            new BigDecimal("24"),  // 24K - 100% pure gold
            new BigDecimal("22"),  // 22K - 91.67% pure gold (916)
            new BigDecimal("21"),  // 21K - 87.5% pure gold (875)
            new BigDecimal("18"),  // 18K - 75% pure gold (750)
            new BigDecimal("14"),  // 14K - 58.33% pure gold (583)
            new BigDecimal("10")   // 10K - 41.67% pure gold (417)
        };
    }

    /**
     * Get common silver purity values in fineness
     * @return Array of common fineness values for silver
     */
    public static BigDecimal[] getCommonSilverPurities() {
        return new BigDecimal[] {
            new BigDecimal("999"),  // 99.9% pure silver (fine silver)
            new BigDecimal("958"),  // 95.8% pure silver (Britannia silver)
            new BigDecimal("925"),  // 92.5% pure silver (Sterling silver)
            new BigDecimal("900"),  // 90% pure silver (Coin silver)
            new BigDecimal("800")   // 80% pure silver
        };
    }
}
