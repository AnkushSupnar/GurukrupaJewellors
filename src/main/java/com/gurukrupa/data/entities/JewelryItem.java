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
            BigDecimal goldValue = netWeight.multiply(goldRate).divide(BigDecimal.valueOf(10));
            
            // Calculate labour charges as percentage of gold value
            // Note: labourCharges field now stores percentage value (e.g., 10 for 10%)
            BigDecimal labourChargesAmount = goldValue.multiply(labourCharges).divide(BigDecimal.valueOf(100));
            
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
            
            this.totalAmount = total;
        }
    }
    
}