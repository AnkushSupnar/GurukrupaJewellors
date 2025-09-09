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
@Table(name = "payment_modes")
public class PaymentMode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Payment Type (CASH, BANK, UPI, CARD, etc.)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;
    
    // Bill reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"paymentModes", "exchange", "billTransactions", "customer"})
    private Bill bill;
    
    // Amount for this payment mode
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    // Reference number (transaction ID for UPI/Card, cheque number, etc.)
    @Column(name = "reference_number")
    private String referenceNumber;
    
    // Bank details (only for BANK payment type)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    @ToString.Exclude
    private BankAccount bankAccount;
    
    // Additional bank-specific details
    @Column(name = "bank_name")
    private String bankName; // For customer's bank (if different from business bank)
    
    @Column(name = "bank_branch")
    private String bankBranch;
    
    // UPI-specific details
    @Column(name = "upi_id")
    private String upiId;
    
    @Column(name = "upi_app")
    private String upiApp; // GooglePay, PhonePe, Paytm, etc.
    
    // Card-specific details
    @Column(name = "card_last_four")
    private String cardLastFour;
    
    @Column(name = "card_type")
    private String cardType; // Credit, Debit
    
    @Column(name = "card_network")
    private String cardNetwork; // Visa, MasterCard, RuPay, etc.
    
    // Status
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.COMPLETED;
    
    // Notes
    @Column(length = 500)
    private String notes;
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime paymentDate;
    
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
    
    public enum PaymentType {
        CASH,
        BANK,
        UPI,
        CARD,
        CHEQUE,
        PARTIAL
    }
    
    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED
    }
}