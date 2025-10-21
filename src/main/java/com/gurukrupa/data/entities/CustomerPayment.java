package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for customer payment receipts
 * Records payments received from customers for credit sales
 */
@Entity
@Table(name = "customer_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique receipt number (e.g., CPR-20250122-0001)
     */
    @Column(unique = true, nullable = false, length = 50)
    private String receiptNumber;

    /**
     * Customer who made the payment
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Date and time of payment
     */
    @Column(nullable = false)
    private LocalDateTime paymentDate;

    /**
     * Amount received from customer
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    /**
     * Bank account where payment was deposited
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    /**
     * Associated bank transaction (CREDIT entry)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id")
    private BankTransaction bankTransaction;

    /**
     * Mode of payment
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode paymentMode;

    /**
     * Transaction reference (cheque number, UPI ref, etc.)
     */
    @Column(length = 100)
    private String transactionReference;

    /**
     * Additional notes about the payment
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Total pending amount before this payment
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal previousPendingAmount;

    /**
     * Remaining pending amount after this payment
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal remainingPendingAmount;

    /**
     * Record creation timestamp
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * Last update timestamp
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime modifiedDate;

    /**
     * Payment modes supported
     */
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
