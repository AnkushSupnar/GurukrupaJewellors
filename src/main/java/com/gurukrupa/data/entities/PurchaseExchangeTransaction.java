package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "purchase_exchange_transactions")
public class PurchaseExchangeTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to the purchase invoice
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_invoice_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"purchaseExchangeTransactions", "purchaseTransactions", "supplier"})
    private PurchaseInvoice purchaseInvoice;
    
    // Item Information
    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String metalType;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal purity = new BigDecimal("916"); // Default to 91.6%

    // Weight
    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal grossWeight = BigDecimal.ZERO; // Weight sent to supplier

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal finePercentage = new BigDecimal("80.00"); // Fine % assessed by supplier (usable metal %)

    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal netWeight = BigDecimal.ZERO; // Net weight = grossWeight * finePercentage / 100

    // Pricing
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ratePerGram = BigDecimal.ZERO; // Rate per gram (not per 10g)
    
    // Total amount (exchange value to be deducted from purchase)
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
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
     * Calculate net weight and total amount
     * Net Weight = Gross Weight * Fine Percentage / 100
     * (Fine percentage is the usable metal percentage assessed by supplier)
     * Total Amount = Net Weight * Rate Per Gram
     */
    public void calculateNetWeightAndAmount() {
        // Calculate net weight based on fine percentage
        if (grossWeight != null && finePercentage != null) {
            // finePercentage is the usable metal percentage (e.g., 80% means 80% is good quality)
            netWeight = grossWeight.multiply(finePercentage)
                .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP);
        } else if (grossWeight != null) {
            netWeight = grossWeight;
        } else {
            netWeight = BigDecimal.ZERO;
        }

        // Calculate total amount: netWeight * ratePerGram
        if (netWeight != null && ratePerGram != null) {
            totalAmount = netWeight.multiply(ratePerGram)
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            totalAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use finePercentage instead
     */
    @Deprecated
    public BigDecimal getDeduction() {
        // Convert fine percentage to deduction (100 - fine)
        if (finePercentage != null) {
            return new BigDecimal("100").subtract(finePercentage);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use finePercentage instead
     */
    @Deprecated
    public void setDeduction(BigDecimal deduction) {
        // Convert deduction to fine percentage (100 - deduction)
        if (deduction != null) {
            this.finePercentage = new BigDecimal("100").subtract(deduction);
        }
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use ratePerGram instead
     */
    @Deprecated
    public BigDecimal getRatePerTenGrams() {
        if (ratePerGram != null) {
            return ratePerGram.multiply(BigDecimal.TEN);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use ratePerGram instead
     */
    @Deprecated
    public void setRatePerTenGrams(BigDecimal ratePerTenGrams) {
        if (ratePerTenGrams != null) {
            this.ratePerGram = ratePerTenGrams.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
        }
    }
}