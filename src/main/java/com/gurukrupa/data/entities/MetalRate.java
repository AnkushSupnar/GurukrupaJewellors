package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "metal_rates", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"metal_id", "rate_date"}))
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetalRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metal_id", nullable = false)
    @ToString.Exclude
    private Metal metal;
    
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerGram;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerTenGrams; // For display purposes
    
    @Column(precision = 10, scale = 2)
    private BigDecimal buyingRate; // Optional buying rate
    
    @Column(precision = 10, scale = 2)
    private BigDecimal sellingRate; // Optional selling rate
    
    @Column(length = 200)
    private String remarks;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @ToString.Exclude
    private LoginUser createdBy;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Calculate rate per 10 grams if only per gram is set
        if (ratePerGram != null && ratePerTenGrams == null) {
            ratePerTenGrams = ratePerGram.multiply(new BigDecimal("10"));
        }
        // Calculate rate per gram if only per 10 grams is set
        else if (ratePerTenGrams != null && ratePerGram == null) {
            ratePerGram = ratePerTenGrams.divide(new BigDecimal("10"), 2, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Keep rates in sync
        if (ratePerGram != null) {
            ratePerTenGrams = ratePerGram.multiply(new BigDecimal("10"));
        }
    }
}