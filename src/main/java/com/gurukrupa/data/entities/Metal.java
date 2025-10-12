package com.gurukrupa.data.entities;

import com.gurukrupa.utility.PurityCalculator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "metals")
public class Metal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String metalName; // e.g., "Gold 24K", "Gold 22K", "Silver", etc.
    
    @Column(nullable = false)
    private String metalType; // e.g., "Gold", "Silver", "Platinum"
    
    @Column(nullable = false)
    private String purity; // e.g., "24K", "22K", "18K", "92.5"
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        isActive = true;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

    /**
     * Get purity as numeric value (extracts number from string like "24K" -> 24)
     */
    public BigDecimal getPurityNumeric() {
        return PurityCalculator.parsePurityString(this.purity);
    }

    /**
     * Get purity percentage (e.g., 22K -> 91.67%)
     */
    public BigDecimal getPurityPercentage() {
        BigDecimal numericPurity = getPurityNumeric();

        // If purity is in karat format (<=24), convert to percentage
        if (PurityCalculator.isKaratFormat(numericPurity)) {
            return PurityCalculator.getPurityPercentage(numericPurity);
        }

        // If already in percentage format or fineness, return as-is or convert
        if (PurityCalculator.isPercentageFormat(numericPurity)) {
            return numericPurity;
        }

        // If in fineness format, convert to percentage
        if (PurityCalculator.isFivenessFormat(numericPurity)) {
            BigDecimal karat = PurityCalculator.getKaratFromFineness(numericPurity);
            return PurityCalculator.getPurityPercentage(karat);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get purity as fineness (e.g., 22K -> 916)
     */
    public BigDecimal getFineness() {
        BigDecimal numericPurity = getPurityNumeric();

        // If purity is in karat format, convert to fineness
        if (PurityCalculator.isKaratFormat(numericPurity)) {
            return PurityCalculator.getFineness(numericPurity);
        }

        // If already in fineness format, return as-is
        if (PurityCalculator.isFivenessFormat(numericPurity)) {
            return numericPurity;
        }

        // If in percentage format, convert to karat first then to fineness
        if (PurityCalculator.isPercentageFormat(numericPurity)) {
            BigDecimal karat = PurityCalculator.getKaratFromPercentage(numericPurity);
            return PurityCalculator.getFineness(karat);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Get purity as karat value (e.g., "22K" -> 22, "916" -> 22)
     */
    public BigDecimal getKaratValue() {
        BigDecimal numericPurity = getPurityNumeric();

        // If already in karat format, return as-is
        if (PurityCalculator.isKaratFormat(numericPurity)) {
            return numericPurity;
        }

        // If in fineness format, convert to karat
        if (PurityCalculator.isFivenessFormat(numericPurity)) {
            return PurityCalculator.getKaratFromFineness(numericPurity);
        }

        // If in percentage format, convert to karat
        if (PurityCalculator.isPercentageFormat(numericPurity)) {
            return PurityCalculator.getKaratFromPercentage(numericPurity);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Calculate pure metal weight from gross weight
     * @param grossWeight Total weight of the item
     * @return Pure metal weight
     */
    public BigDecimal calculatePureWeight(BigDecimal grossWeight) {
        BigDecimal karat = getKaratValue();
        return PurityCalculator.getPureMetalWeight(grossWeight, karat);
    }

    /**
     * Get formatted display string for purity
     * @return Display string (e.g., "22K (916) - 91.67%")
     */
    public String getPurityDisplay() {
        BigDecimal karat = getKaratValue();
        return PurityCalculator.formatPurityDisplay(karat);
    }

    /**
     * Check if this metal is gold
     */
    public boolean isGold() {
        return "Gold".equalsIgnoreCase(this.metalType);
    }

    /**
     * Check if this metal is silver
     */
    public boolean isSilver() {
        return "Silver".equalsIgnoreCase(this.metalType);
    }

    /**
     * Check if this metal is platinum
     */
    public boolean isPlatinum() {
        return "Platinum".equalsIgnoreCase(this.metalType);
    }
}