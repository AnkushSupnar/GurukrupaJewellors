package com.gurukrupa.data.entities;

import com.gurukrupa.utility.PurityCalculator;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entity representing a metal purchase transaction
 * This tracks metal bought from suppliers (by weight, not as specific items)
 */
@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "purchase_metal_transactions")
public class PurchaseMetalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to the purchase invoice
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_invoice_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"purchaseMetalTransactions", "purchaseExchangeTransactions", "supplier"})
    private PurchaseInvoice purchaseInvoice;

    // Metal Information - Reference to Metal master data
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metal_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Metal metal; // Reference to the metal from metals table

    // Keep these for backward compatibility and quick access
    @Column(nullable = false, length = 50)
    private String metalType; // GOLD, SILVER, PLATINUM, etc. (denormalized from Metal)

    @Column(nullable = false, precision = 7, scale = 3)
    private BigDecimal purity; // 916, 750, 585 for gold (22k, 18k, 14k) or 999, 925 for silver (denormalized from Metal)

    // Weight Details
    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal grossWeight = BigDecimal.ZERO; // Gross weight purchased (e.g., 10 grams)

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal sellerPercentage = new BigDecimal("97.00"); // Seller percentage (e.g., 97%)

    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal netWeightCharged = BigDecimal.ZERO; // Net weight charged = grossWeight * sellerPercentage / 100

    // Pricing
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ratePerGram = BigDecimal.ZERO; // Rate per gram

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO; // Total amount = netWeightCharged * ratePerGram

    @Column(length = 500)
    private String description;

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        calculateNetWeightAndAmount();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        calculateNetWeightAndAmount();
    }

    /**
     * Calculate net weight charged and total amount
     * Net Weight = Gross Weight * Seller Percentage / 100
     * Total Amount = Net Weight * Rate Per Gram
     */
    public void calculateNetWeightAndAmount() {
        // Calculate net weight charged based on seller percentage
        if (grossWeight != null && sellerPercentage != null) {
            netWeightCharged = grossWeight
                .multiply(sellerPercentage)
                .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP);
        } else {
            netWeightCharged = BigDecimal.ZERO;
        }

        // Calculate total amount
        if (netWeightCharged != null && ratePerGram != null) {
            totalAmount = netWeightCharged
                .multiply(ratePerGram)
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            totalAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Get display string for this transaction
     */
    public String getDisplayString() {
        return String.format("%s %s - Gross: %.3fg, Net: %.3fg @ ₹%.2f/g = ₹%.2f",
            metalType, purity, grossWeight, netWeightCharged, ratePerGram, totalAmount);
    }

    /**
     * Get purity as karat value
     * Converts fineness (916, 750) to karat (22, 18)
     */
    public BigDecimal getPurityAsKarat() {
        if (PurityCalculator.isFivenessFormat(purity)) {
            return PurityCalculator.getKaratFromFineness(purity);
        }
        return purity;
    }

    /**
     * Get purity percentage
     * E.g., 916 fineness -> 91.67%
     */
    public BigDecimal getPurityPercentage() {
        BigDecimal karat = getPurityAsKarat();
        return PurityCalculator.getPurityPercentage(karat);
    }

    /**
     * Get fineness value
     * E.g., 22K -> 916
     */
    public BigDecimal getFinenessValue() {
        if (PurityCalculator.isFivenessFormat(purity)) {
            return purity;
        }
        return PurityCalculator.getFineness(purity);
    }

    /**
     * Calculate pure metal weight from gross weight
     * @return Pure metal weight based on purity
     */
    public BigDecimal getGrossPureWeight() {
        BigDecimal karat = getPurityAsKarat();
        return PurityCalculator.getPureMetalWeight(grossWeight, karat);
    }

    /**
     * Calculate pure metal weight from net weight charged
     * @return Pure metal weight based on purity and seller percentage
     */
    public BigDecimal getNetPureWeight() {
        BigDecimal karat = getPurityAsKarat();
        return PurityCalculator.getPureMetalWeight(netWeightCharged, karat);
    }

    /**
     * Get formatted purity display string
     */
    public String getPurityDisplay() {
        BigDecimal karat = getPurityAsKarat();
        return PurityCalculator.formatPurityDisplay(karat);
    }

    /**
     * Get display string with purity details
     */
    public String getDetailedDisplayString() {
        return String.format("%s %s - Gross: %.3fg (%.3fg pure), Net: %.3fg (%.3fg pure) @ ₹%.2f/g = ₹%.2f",
            metalType,
            getPurityDisplay(),
            grossWeight.doubleValue(),
            getGrossPureWeight().doubleValue(),
            netWeightCharged.doubleValue(),
            getNetPureWeight().doubleValue(),
            ratePerGram.doubleValue(),
            totalAmount.doubleValue());
    }
}
