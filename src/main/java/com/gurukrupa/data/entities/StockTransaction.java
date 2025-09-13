package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "stock_transactions")
public class StockTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to the jewelry item
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "jewelry_item_id", nullable = false)
    @JsonIgnoreProperties({"stockTransactions"})
    private JewelryItem jewelryItem;
    
    // Transaction type
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;
    
    // Transaction source
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_source", nullable = false, length = 30)
    private TransactionSource transactionSource;
    
    // Quantity involved in transaction
    @Column(nullable = false)
    private Integer quantity;
    
    // Stock levels
    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;
    
    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;
    
    // Reference details
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    
    @Column(name = "reference_id")
    private Long referenceId;
    
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    // Additional details
    @Column(length = 500)
    private String description;
    
    @Column(length = 255)
    private String remarks;
    
    // Transaction date
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;
    
    // User who performed the transaction
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    // Timestamps
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
    
    // Enums
    public enum TransactionType {
        IN,       // Stock coming in
        OUT,      // Stock going out
        ADJUSTMENT // Stock adjustment (correction)
    }
    
    public enum TransactionSource {
        SALE,           // From bill/sale
        PURCHASE,       // From purchase
        SALE_RETURN,    // Customer return
        PURCHASE_RETURN,// Return to supplier
        OPENING_STOCK,  // Initial stock
        ADJUSTMENT,     // Manual adjustment
        DAMAGE,         // Damaged goods
        LOSS,           // Lost items
        TRANSFER,       // Transfer between locations
        PRODUCTION,     // Manufacturing/production
        OTHER           // Other sources
    }
}