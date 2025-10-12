package com.gurukrupa.data.entities;

import com.gurukrupa.utility.PurityCalculator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to track metal purchased from suppliers
 * This is separate from ExchangeMetalStock which tracks customer exchange metal
 */
@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "purchase_metal_stock",
       uniqueConstraints = @UniqueConstraint(columnNames = {"metal_id"}))
public class PurchaseMetalStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to Metal master data
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metal_id", nullable = false)
    private Metal metal; // Reference to the metal from metals table

    // Keep these for backward compatibility and quick access (denormalized)
    @Column(name = "metal_type", nullable = false, length = 50)
    private String metalType; // GOLD, SILVER, PLATINUM, etc. (denormalized from Metal)

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal purity; // 1000, 916, 750, 585 for gold (24k, 22k, 18k, 14k) or 999, 925 for silver (denormalized from Metal)

    @Column(name = "total_gross_weight", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal totalGrossWeight = BigDecimal.ZERO; // Total gross weight purchased (before seller %)

    @Column(name = "total_net_weight", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal totalNetWeight = BigDecimal.ZERO; // Total net weight charged (after seller %)

    @Column(name = "available_weight", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal availableWeight = BigDecimal.ZERO; // Available weight for use

    @Column(name = "used_weight", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal usedWeight = BigDecimal.ZERO; // Weight used in manufacturing/sales

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    /**
     * Add purchased metal to stock
     * @param grossWeight Gross weight of metal
     * @param netWeight Net weight charged (after seller percentage)
     */
    public void addMetal(BigDecimal grossWeight, BigDecimal netWeight) {
        if (grossWeight != null && grossWeight.compareTo(BigDecimal.ZERO) > 0) {
            this.totalGrossWeight = this.totalGrossWeight.add(grossWeight);
        }
        if (netWeight != null && netWeight.compareTo(BigDecimal.ZERO) > 0) {
            this.totalNetWeight = this.totalNetWeight.add(netWeight);
            this.availableWeight = this.availableWeight.add(netWeight);
        }
    }

    /**
     * Use metal from stock (for manufacturing/sales)
     * @param weight Weight to be used
     */
    public void useMetal(BigDecimal weight) {
        if (weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
            if (this.availableWeight.compareTo(weight) >= 0) {
                this.availableWeight = this.availableWeight.subtract(weight);
                this.usedWeight = this.usedWeight.add(weight);
            } else {
                throw new IllegalArgumentException("Insufficient available metal. Available: " +
                    this.availableWeight + " grams, Requested: " + weight + " grams");
            }
        }
    }

    /**
     * Return metal to stock (from cancelled orders, etc.)
     * @param weight Weight to be returned
     */
    public void returnMetal(BigDecimal weight) {
        if (weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
            this.availableWeight = this.availableWeight.add(weight);
            this.usedWeight = this.usedWeight.subtract(weight);
        }
    }

    /**
     * Get unique key for this metal type and purity
     */
    public String getMetalKey() {
        return metalType + "_" + purity;
    }

    /**
     * Get display name for this metal stock
     */
    public String getDisplayName() {
        return metalType + " " + purity + " - " + availableWeight + "g available";
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
     * Calculate pure metal weight from available weight
     * @return Pure metal weight based on purity
     */
    public BigDecimal getAvailablePureWeight() {
        BigDecimal karat = getPurityAsKarat();
        return PurityCalculator.getPureMetalWeight(availableWeight, karat);
    }

    /**
     * Calculate pure metal weight from total net weight
     * @return Pure metal weight based on purity
     */
    public BigDecimal getTotalPureWeight() {
        BigDecimal karat = getPurityAsKarat();
        return PurityCalculator.getPureMetalWeight(totalNetWeight, karat);
    }

    /**
     * Get formatted purity display string
     */
    public String getPurityDisplay() {
        BigDecimal karat = getPurityAsKarat();
        return PurityCalculator.formatPurityDisplay(karat);
    }
}
