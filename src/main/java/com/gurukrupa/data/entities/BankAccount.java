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
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String bankName;
    
    @Column(nullable = false, unique = true)
    private String accountNumber;
    
    @Column(nullable = false)
    private String ifscCode;
    
    @Column(nullable = false)
    private String accountHolderName;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    
    @Column
    private String branchName;
    
    @Column
    private String branchAddress;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal currentBalance;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BalanceType balanceType;
    
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
        if (currentBalance == null) {
            currentBalance = openingBalance;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
    
    public enum AccountType {
        SAVINGS("Savings Account"),
        CURRENT("Current Account");
      //  FIXED_DEPOSIT("Fixed Deposit"),
      //  RECURRING_DEPOSIT("Recurring Deposit"),
      //  CASH_CREDIT("Cash Credit"),
      //  OVERDRAFT("Overdraft");
        
        private final String displayName;
        
        AccountType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum BalanceType {
        CREDIT("Credit"),
        DEBIT("Debit");
        
        private final String displayName;
        
        BalanceType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}