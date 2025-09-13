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
@Table(name = "exchange_metal_stock", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"metal_type", "purity"}))
public class ExchangeMetalStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "metal_type", nullable = false, length = 50)
    private String metalType; // GOLD, SILVER, etc.
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal purity; // 22k, 18k, 14k, etc.
    
    @Column(name = "total_weight", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal totalWeight = BigDecimal.ZERO; // Total weight in grams
    
    @Column(name = "available_weight", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal availableWeight = BigDecimal.ZERO; // Available weight (not sold to suppliers)
    
    @Column(name = "sold_weight", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal soldWeight = BigDecimal.ZERO; // Weight sold to suppliers
    
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
    
    // Helper methods
    public void addWeight(BigDecimal weight) {
        if (weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
            this.totalWeight = this.totalWeight.add(weight);
            this.availableWeight = this.availableWeight.add(weight);
        }
    }
    
    public void sellWeight(BigDecimal weight) {
        if (weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
            if (this.availableWeight.compareTo(weight) >= 0) {
                this.availableWeight = this.availableWeight.subtract(weight);
                this.soldWeight = this.soldWeight.add(weight);
            } else {
                throw new IllegalArgumentException("Insufficient available weight. Available: " + 
                    this.availableWeight + ", Requested: " + weight);
            }
        }
    }
    
    public void returnWeight(BigDecimal weight) {
        if (weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
            this.availableWeight = this.availableWeight.add(weight);
            this.soldWeight = this.soldWeight.subtract(weight);
        }
    }
    
    public String getMetalKey() {
        return metalType + "_" + purity;
    }
}