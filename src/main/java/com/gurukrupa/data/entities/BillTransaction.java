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
@Table(name = "bill_transactions")
public class BillTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to the main bill
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"billTransactions", "exchange", "paymentModes", "customer"})
    private Bill bill;
    
    // Item Information
    @Column(nullable = false)
    private String itemCode;
    
    @Column(nullable = false)
    private String itemName;
    
    @Column(nullable = false)
    private String metalType;
    
    // Weight
    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ZERO;
    
    // Pricing
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ratePerTenGrams = BigDecimal.ZERO;
    
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal labourCharges = BigDecimal.ZERO;
    
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
        calculateTotalAmount();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        calculateTotalAmount();
    }
    
    public void calculateTotalAmount() {
        // Calculate gold value: (weight * ratePerTenGrams) / 10
        BigDecimal goldValue = BigDecimal.ZERO;
        if (weight != null && ratePerTenGrams != null) {
            goldValue = weight.multiply(ratePerTenGrams)
                .divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        }
        
        // Calculate total amount = goldValue + labourCharges
        totalAmount = goldValue;
        if (labourCharges != null) {
            totalAmount = totalAmount.add(labourCharges);
        }
    }
}