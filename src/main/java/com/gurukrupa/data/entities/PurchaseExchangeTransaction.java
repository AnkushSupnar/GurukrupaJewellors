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
    private BigDecimal grossWeight = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal deduction = BigDecimal.ZERO; // Percentage deduction
    
    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal netWeight = BigDecimal.ZERO;
    
    // Pricing
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ratePerTenGrams = BigDecimal.ZERO;
    
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
    
    public void calculateNetWeightAndAmount() {
        // Calculate net weight after deduction
        if (grossWeight != null && deduction != null) {
            // deduction is a percentage
            BigDecimal deductionAmount = grossWeight.multiply(deduction)
                .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP);
            netWeight = grossWeight.subtract(deductionAmount);
        } else if (grossWeight != null) {
            netWeight = grossWeight;
        } else {
            netWeight = BigDecimal.ZERO;
        }
        
        // Calculate total amount: (netWeight * ratePerTenGrams) / 10
        if (netWeight != null && ratePerTenGrams != null) {
            totalAmount = netWeight.multiply(ratePerTenGrams)
                .divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        } else {
            totalAmount = BigDecimal.ZERO;
        }
    }
}