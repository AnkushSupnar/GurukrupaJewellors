package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
@Table(name = "bills")
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String billNumber;
    
    // Customer Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    // Bill Details
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cgstAmount;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sgstAmount;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTaxAmount;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netTotal;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal exchangeAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;
    
    // Payment Information
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BillStatus status;
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime billDate;
    
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    // Relationships
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<BillTransaction> billTransactions = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (billDate == null) {
            billDate = LocalDateTime.now();
        }
        if (status == null) {
            status = BillStatus.DRAFT;
        }
        generateBillNumber();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
    
    private void generateBillNumber() {
        // Bill number generation is now handled by the service layer
        // This method exists for compatibility but doesn't generate the number
        if (billNumber == null) {
            billNumber = "TEMP-" + System.currentTimeMillis();
        }
    }
    
    public void calculateTotals() {
        // Calculate subtotal from bill transactions
        subtotal = billTransactions.stream()
            .map(BillTransaction::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate net total after discount
        netTotal = subtotal.subtract(discount);
        
        // Calculate tax amounts
        totalTaxAmount = netTotal.multiply(gstRate).divide(BigDecimal.valueOf(100));
        cgstAmount = totalTaxAmount.divide(BigDecimal.valueOf(2));
        sgstAmount = totalTaxAmount.divide(BigDecimal.valueOf(2));
        
        // Calculate grand total
        grandTotal = netTotal.add(totalTaxAmount).subtract(exchangeAmount);
    }
    
    public enum PaymentMethod {
        CASH, UPI, CARD, CHEQUE, BANK_TRANSFER
    }
    
    public enum BillStatus {
        DRAFT, CONFIRMED, PAID, CANCELLED
    }
}