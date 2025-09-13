package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "purchase_invoices")
public class PurchaseInvoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String invoiceNumber;
    
    @Column(length = 50)
    private String supplierInvoiceNumber; // Supplier's invoice reference
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;
    
    @Column(nullable = false)
    private LocalDateTime invoiceDate;
    
    // Purchase type
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseType purchaseType;
    
    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;
    
    // Payment details
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    
    @Column(length = 100)
    private String paymentReference;
    
    // Transactions
    @OneToMany(mappedBy = "purchaseInvoice", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"purchaseInvoice"})
    @Builder.Default
    private List<PurchaseTransaction> purchaseTransactions = new ArrayList<>();
    
    // Financial details
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal netTotal = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstRate = new BigDecimal("3.00");
    
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    
    // Additional charges
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal transportCharges = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otherCharges = BigDecimal.ZERO;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (invoiceDate == null) {
            invoiceDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
    
    // Enums
    public enum PurchaseType {
        NEW_STOCK,      // New jewelry items
        EXCHANGE_ITEMS, // Old/exchange items from customers
        RAW_MATERIAL,   // Gold, silver bars
        OTHER           // Other purchases
    }
    
    public enum InvoiceStatus {
        DRAFT,
        CONFIRMED,
        PAID,
        PARTIAL,
        CANCELLED
    }
    
    public enum PaymentMethod {
        CASH,
        BANK_TRANSFER,
        CHEQUE,
        UPI,
        CARD,
        CREDIT,
        PARTIAL,
        OTHER
    }
    
    // Calculate totals
    public void calculateTotals() {
        // Calculate subtotal from transactions
        subtotal = purchaseTransactions.stream()
                .map(PurchaseTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate net total (subtotal - discount + additional charges)
        netTotal = subtotal.subtract(discount != null ? discount : BigDecimal.ZERO)
                          .add(transportCharges != null ? transportCharges : BigDecimal.ZERO)
                          .add(otherCharges != null ? otherCharges : BigDecimal.ZERO);
        
        // Calculate GST
        if (gstRate != null && gstRate.compareTo(BigDecimal.ZERO) > 0) {
            gstAmount = netTotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            gstAmount = BigDecimal.ZERO;
        }
        
        // Calculate grand total
        grandTotal = netTotal.add(gstAmount);
        
        // Calculate pending amount
        pendingAmount = grandTotal.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }
}