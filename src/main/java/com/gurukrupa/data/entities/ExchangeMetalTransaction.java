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
@Table(name = "exchange_metal_transactions")
public class ExchangeMetalTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "metal_stock_id", nullable = false)
    @JsonIgnoreProperties({"transactions"})
    private ExchangeMetalStock metalStock;
    
    // Transaction type
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;
    
    // Transaction source
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_source", nullable = false, length = 30)
    private TransactionSource transactionSource;
    
    // Weight involved in transaction
    @Column(nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ZERO;
    
    // Weight levels
    @Column(name = "weight_before", nullable = false, precision = 15, scale = 3)
    private BigDecimal weightBefore;
    
    @Column(name = "weight_after", nullable = false, precision = 15, scale = 3)
    private BigDecimal weightAfter;
    
    // Reference details
    @Column(name = "reference_type", length = 50)
    private String referenceType; // EXCHANGE, PURCHASE_INVOICE, etc.
    
    @Column(name = "reference_id")
    private Long referenceId;
    
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    // Additional details
    @Column(length = 500)
    private String description;
    
    @Column(length = 255)
    private String party; // Customer or Supplier name
    
    // Transaction date
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;
    
    // User who performed the transaction
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    // Timestamps
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
    
    // Enums
    public enum TransactionType {
        IN,         // Metal coming in (from exchange)
        OUT,        // Metal going out (sold to supplier)
        ADJUSTMENT  // Adjustment (correction)
    }
    
    public enum TransactionSource {
        CUSTOMER_EXCHANGE,    // From customer exchange
        SUPPLIER_SALE,        // Sold to supplier
        ADJUSTMENT,           // Manual adjustment
        RETURN,               // Returned from supplier
        OTHER                 // Other sources
    }
}