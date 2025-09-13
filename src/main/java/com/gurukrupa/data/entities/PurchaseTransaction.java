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
@Table(name = "purchase_transactions")
public class PurchaseTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to the purchase invoice
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_invoice_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"purchaseTransactions", "supplier"})
    private PurchaseInvoice purchaseInvoice;
    
    // Item type
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType itemType;
    
    // Item Information
    @Column(nullable = false)
    private String itemCode;
    
    @Column(nullable = false)
    private String itemName;
    
    @Column(nullable = false)
    private String metalType;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal purity; // 22k, 18k, etc.
    
    // Quantity
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;
    
    // Weight
    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal grossWeight = BigDecimal.ZERO;
    
    @Column(precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal netWeight = BigDecimal.ZERO;
    
    // Pricing
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ratePerGram = BigDecimal.ZERO;
    
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal makingCharges = BigDecimal.ZERO;
    
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal otherCharges = BigDecimal.ZERO;
    
    // Total amount
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    // Exchange item reference (if this is an exchange item purchase)
    @Column(name = "exchange_item_id")
    private Long exchangeItemId;
    
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
        calculateTotalAmount();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        calculateTotalAmount();
    }
    
    // Enums
    public enum ItemType {
        NEW_ITEM,       // New jewelry item
        EXCHANGE_ITEM,  // Exchange/old item
        RAW_MATERIAL,   // Raw gold/silver
        OTHER           // Other items
    }
    
    public void calculateTotalAmount() {
        // Calculate metal value: (netWeight * ratePerGram)
        BigDecimal metalValue = BigDecimal.ZERO;
        if (netWeight != null && ratePerGram != null) {
            metalValue = netWeight.multiply(ratePerGram);
        }
        
        // Calculate total amount = metalValue + makingCharges + otherCharges
        totalAmount = metalValue;
        if (makingCharges != null) {
            totalAmount = totalAmount.add(makingCharges);
        }
        if (otherCharges != null) {
            totalAmount = totalAmount.add(otherCharges);
        }
        
        // Round to 2 decimal places
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
    }
}