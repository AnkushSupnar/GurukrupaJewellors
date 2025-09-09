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
@Table(name = "exchange_transactions")
public class ExchangeTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to the main exchange
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"exchangeTransactions", "bill", "customer"})
    private Exchange exchange;
    
    // Item Information
    @Column(nullable = false)
    private String itemName;
    
    @Column(nullable = false)
    private String metalType;
    
    // Weight
    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal grossWeight = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal deduction = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal netWeight = BigDecimal.ZERO;
    
    // Pricing
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ratePerTenGrams = BigDecimal.ZERO;
    
    // Total amount
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
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
        // Calculate net weight = grossWeight - deduction
        if (grossWeight != null && deduction != null) {
            netWeight = grossWeight.subtract(deduction);
        } else if (grossWeight != null) {
            netWeight = grossWeight;
        } else {
            netWeight = BigDecimal.ZERO;
        }
        
        // Calculate total amount: (netWeight * ratePerTenGrams) / 10
        if (netWeight != null && ratePerTenGrams != null) {
            totalAmount = netWeight.multiply(ratePerTenGrams)
                .divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
            System.out.println("ExchangeTransaction calculated: netWeight=" + netWeight + 
                ", rate=" + ratePerTenGrams + ", totalAmount=" + totalAmount);
        } else {
            totalAmount = BigDecimal.ZERO;
            System.out.println("ExchangeTransaction calculation failed: netWeight=" + netWeight + 
                ", rate=" + ratePerTenGrams);
        }
    }
}