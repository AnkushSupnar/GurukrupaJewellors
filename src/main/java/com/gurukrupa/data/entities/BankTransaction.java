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
@Table(name = "bank_transactions")
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Bank Account reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"bankTransactions", "isActive"})
    private BankAccount bankAccount;
    
    // Transaction Type (DEBIT or CREDIT)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;
    
    // Amount
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    // Balance after transaction
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfterTransaction;
    
    // Transaction Source/Purpose
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionSource source;
    
    // Reference details
    @Column(name = "reference_type")
    private String referenceType; // BILL, PURCHASE, EXCHANGE, MANUAL, etc.
    
    @Column(name = "reference_id")
    private Long referenceId; // ID of the related entity
    
    @Column(name = "reference_number")
    private String referenceNumber; // Bill number, Purchase invoice number, etc.
    
    // Transaction details
    @Column(name = "transaction_reference")
    private String transactionReference; // Bank transaction ID/reference
    
    @Column(length = 500)
    private String description; // Detailed description
    
    @Column(length = 255)
    private String party; // Party name (customer, supplier, etc.)
    
    // Transaction date and time
    @Column(nullable = false)
    private LocalDateTime transactionDate;
    
    // Reconciliation fields
    @Column
    private Boolean isReconciled;
    
    @Column
    private LocalDateTime reconciledDate;
    
    @Column
    private String reconciledBy;
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (isReconciled == null) {
            isReconciled = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
    
    // Enums
    public enum TransactionType {
        DEBIT,  // Money going out
        CREDIT  // Money coming in
    }
    
    public enum TransactionSource {
        BILL_PAYMENT,        // Payment received from customer bill
        PURCHASE_PAYMENT,    // Payment made to supplier
        EXCHANGE_PAYMENT,    // Payment related to exchange
        MANUAL_ENTRY,        // Manual adjustment
        BANK_CHARGES,        // Bank fees/charges
        INTEREST_CREDIT,     // Interest earned
        TRANSFER_IN,         // Transfer from another account
        TRANSFER_OUT,        // Transfer to another account
        OPENING_BALANCE,     // Initial balance
        OTHER               // Other transactions
    }
    
    // Helper method to build description
    public String buildDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(transactionType == TransactionType.CREDIT ? "Received " : "Paid ");
        desc.append("₹").append(amount).append(" ");
        
        switch (source) {
            case BILL_PAYMENT:
                desc.append("from bill ").append(referenceNumber);
                if (party != null) desc.append(" - ").append(party);
                break;
            case PURCHASE_PAYMENT:
                desc.append("to ").append(party != null ? party : "supplier");
                desc.append(" for purchase ").append(referenceNumber);
                break;
            case EXCHANGE_PAYMENT:
                desc.append("for exchange ").append(referenceNumber);
                if (party != null) desc.append(" - ").append(party);
                break;
            default:
                if (description != null) {
                    return description;
                }
        }
        
        return desc.toString();
    }
}