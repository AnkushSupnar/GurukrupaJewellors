package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.gurukrupa.utility.PurityCalculator;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "jewelry_items")
public class JewelryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String itemCode;
    
    @Column(nullable = false)
    private String itemName;
    
    @Column(nullable = false)
    private String category; // Ring, Necklace, Earrings, Bracelet, Pendant, etc.
    
    @Column(nullable = false)
    private String metalType; // This will now store the metal name from Metal table
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purity; // 22k, 18k, 14k, etc. (stored as decimal like 22.00, 18.00)
    
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal grossWeight; // Total weight in grams
    
    @Column(precision = 10, scale = 3)
    private BigDecimal stoneWeight; // Weight of stones/gems in grams
    
    @Column(precision = 10, scale = 3)
    private BigDecimal netWeight; // Net gold weight (grossWeight - stoneWeight)
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal labourCharges; // Labour charges as percentage (e.g., 10.00 for 10%)
    
    @Column(precision = 12, scale = 2)
    private BigDecimal stoneCharges; // Charges for stones/gems
    
    @Column(precision = 12, scale = 2)
    private BigDecimal otherCharges; // Any other charges
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal goldRate; // Rate per 10 grams
    
    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount; // Calculated total amount
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        isActive = true;
        calculateTotalAmount();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        calculateTotalAmount();
    }
    
    public void calculateTotalAmount() {
        if (netWeight != null && goldRate != null && labourCharges != null) {
            // Calculate gold value: (netWeight * goldRate) / 10
            BigDecimal goldValue = netWeight.multiply(goldRate).divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);

            // Calculate labour charges as percentage of gold value
            // Note: labourCharges field now stores percentage value (e.g., 10 for 10%)
            BigDecimal labourChargesAmount = goldValue.multiply(labourCharges).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Add labour charges amount
            BigDecimal total = goldValue.add(labourChargesAmount);

            // Add stone charges if present
            if (stoneCharges != null) {
                total = total.add(stoneCharges);
            }

            // Add other charges if present
            if (otherCharges != null) {
                total = total.add(otherCharges);
            }

            this.totalAmount = total.setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Get purity percentage
     * E.g., 22K -> 91.67%
     */
    public BigDecimal getPurityPercentage() {
        return PurityCalculator.getPurityPercentage(purity);
    }

    /**
     * Get purity as fineness value
     * E.g., 22K -> 916
     */
    public BigDecimal getFineness() {
        return PurityCalculator.getFineness(purity);
    }

    /**
     * Calculate pure metal weight from net weight
     * E.g., 10g of 22K = 9.167g pure gold
     */
    public BigDecimal getPureMetalWeight() {
        if (netWeight != null) {
            return PurityCalculator.getPureMetalWeight(netWeight, purity);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate pure metal weight from gross weight (including stones)
     */
    public BigDecimal getPureMetalWeightFromGross() {
        if (grossWeight != null) {
            return PurityCalculator.getPureMetalWeight(grossWeight, purity);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get formatted purity display string
     * E.g., "22K (916) - 91.67%"
     */
    public String getPurityDisplay() {
        return PurityCalculator.formatPurityDisplay(purity);
    }

    /**
     * Get gold value without labour charges
     */
    public BigDecimal getGoldValue() {
        if (netWeight != null && goldRate != null) {
            return netWeight.multiply(goldRate)
                .divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get labour charges amount (not percentage)
     */
    public BigDecimal getLabourChargesAmount() {
        BigDecimal goldValue = getGoldValue();
        if (goldValue.compareTo(BigDecimal.ZERO) > 0 && labourCharges != null) {
            return goldValue.multiply(labourCharges)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get total amount of extra charges (stone + other)
     */
    public BigDecimal getTotalExtraCharges() {
        BigDecimal total = BigDecimal.ZERO;
        if (stoneCharges != null) {
            total = total.add(stoneCharges);
        }
        if (otherCharges != null) {
            total = total.add(otherCharges);
        }
        return total;
    }

    /**
     * Get detailed display string for the item
     */
    public String getDetailedDisplay() {
        return String.format("%s (%s) - %s %.0fK - Net: %.3fg (%.3fg pure) @ ₹%.2f = ₹%.2f",
            itemCode,
            itemName,
            metalType,
            purity.doubleValue(),
            netWeight.doubleValue(),
            getPureMetalWeight().doubleValue(),
            goldRate.doubleValue(),
            totalAmount.doubleValue());
    }

}