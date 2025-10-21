package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to track supplier payment receipts
 * Records payments made to suppliers for their credit purchase invoices
 */
@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "supplier_payments")
public class SupplierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Payment Receipt Number
    @Column(unique = true, nullable = false, length = 50)
    private String receiptNumber;

    // Supplier reference
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    // Payment date
    @Column(nullable = false)
    private LocalDateTime paymentDate;

    // Payment amount
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    // Bank account used for payment
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bank_account_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BankAccount bankAccount;

    // Bank transaction reference
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id")
    @ToString.Exclude
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BankTransaction bankTransaction;

    // Payment mode
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode paymentMode;

    // Transaction reference (cheque no, UPI ref, etc.)
    @Column(length = 100)
    private String transactionReference;

    // Notes
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Amount before this payment (for tracking)
    @Column(precision = 15, scale = 2)
    private BigDecimal previousPendingAmount;

    // Amount after this payment (for tracking)
    @Column(precision = 15, scale = 2)
    private BigDecimal remainingPendingAmount;

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

    // Enums
    public enum PaymentMode {
        CASH,
        CHEQUE,
        BANK_TRANSFER,
        UPI,
        NEFT,
        RTGS,
        IMPS,
        OTHER
    }
}
