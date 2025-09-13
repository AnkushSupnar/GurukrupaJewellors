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
@Table(name = "suppliers")
public class Supplier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String supplierName;
    
    @Column(length = 100)
    private String companyName;
    
    @Column(length = 20)
    private String gstNumber;
    
    @Column(nullable = false, length = 15)
    private String mobile;
    
    @Column(length = 15)
    private String alternateMobile;
    
    @Column(length = 100)
    private String email;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(length = 50)
    private String city;
    
    @Column(length = 50)
    private String state;
    
    @Column(length = 10)
    private String pincode;
    
    @Column(length = 100)
    private String contactPerson;
    
    @Column(length = 50)
    private String supplierType; // GOLD_SUPPLIER, SILVER_SUPPLIER, DIAMOND_SUPPLIER, etc.
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal creditLimit = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO; // Outstanding amount
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
    
    // Helper method
    public String getSupplierFullName() {
        if (companyName != null && !companyName.isEmpty()) {
            return companyName + " (" + supplierName + ")";
        }
        return supplierName;
    }
}