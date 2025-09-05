package com.gurukrupa.data.entities;

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
@Table(name = "bill_transactions")
public class BillTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to the main bill
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    @ToString.Exclude
    private Bill bill;
    
    // Item Information
    @Column(name = "jewelry_item_id")
    private Long jewelryItemId; // Reference to JewelryItem table
    
    @Column(nullable = false)
    private String itemCode;
    
    @Column(nullable = false)
    private String itemName;
    
    @Column(nullable = false)
    private String metalType;
    
    // Quantity and Weight
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal weightPerUnit; // Weight per single unit
    
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal totalWeight; // Total weight (quantity * weightPerUnit)
    
    // Pricing
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerTenGrams;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal labourCharges = BigDecimal.ZERO;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal stoneCharges = BigDecimal.ZERO;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal otherCharges = BigDecimal.ZERO;
    
    // Calculated amounts
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal goldValue; // (totalWeight * ratePerTenGrams) / 10
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount; // goldValue + labourCharges + stoneCharges + otherCharges
    
    // Transaction type
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    // For exchange items
    @Column(precision = 10, scale = 3)
    private BigDecimal deductionWeight; // For exchange items only
    
    @Column(precision = 10, scale = 3)
    private BigDecimal netWeight; // For exchange: totalWeight - deductionWeight
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        calculateAmounts();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        calculateAmounts();
    }
    
    public void calculateAmounts() {
        if (transactionType == TransactionType.SALE) {
            // For sale items: use total weight
            calculateSaleAmounts();
        } else if (transactionType == TransactionType.EXCHANGE) {
            // For exchange items: use net weight (after deductions)
            calculateExchangeAmounts();
        }
    }
    
    private void calculateSaleAmounts() {
        // Calculate total weight
        if (quantity != null && weightPerUnit != null) {
            totalWeight = weightPerUnit.multiply(BigDecimal.valueOf(quantity));
        }
        
        // Calculate gold value: (totalWeight * ratePerTenGrams) / 10
        if (totalWeight != null && ratePerTenGrams != null) {
            goldValue = totalWeight.multiply(ratePerTenGrams).divide(BigDecimal.valueOf(10));
        } else {
            goldValue = BigDecimal.ZERO;
        }
        
        // Calculate total amount
        totalAmount = goldValue;
        if (labourCharges != null) {
            totalAmount = totalAmount.add(labourCharges);
        }
        if (stoneCharges != null) {
            totalAmount = totalAmount.add(stoneCharges);
        }
        if (otherCharges != null) {
            totalAmount = totalAmount.add(otherCharges);
        }
    }
    
    private void calculateExchangeAmounts() {
        // For exchange items, calculate net weight first
        if (totalWeight != null && deductionWeight != null) {
            netWeight = totalWeight.subtract(deductionWeight);
        } else {
            netWeight = totalWeight;
        }
        
        // Calculate gold value based on net weight
        if (netWeight != null && ratePerTenGrams != null) {
            goldValue = netWeight.multiply(ratePerTenGrams).divide(BigDecimal.valueOf(10));
        } else {
            goldValue = BigDecimal.ZERO;
        }
        
        // For exchange items, typically only gold value is considered
        totalAmount = goldValue;
    }
    
    public enum TransactionType {
        SALE,       // Items being sold
        EXCHANGE    // Items being taken in exchange
    }
}