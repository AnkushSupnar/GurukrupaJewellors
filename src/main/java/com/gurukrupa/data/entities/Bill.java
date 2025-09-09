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
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstRate = new BigDecimal("3.00");
    
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cgstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sgstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTaxAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal netTotal = BigDecimal.ZERO;
    
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal exchangeAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    
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
    @Builder.Default
    private List<BillTransaction> billTransactions = new ArrayList<>();
    
    // Note: Exchange relationship is now managed through Exchange entity's bill_id
    // To get exchange for a bill, query Exchange table with bill_id
    @Transient
    private Exchange exchange; // This is now a transient field for backward compatibility
    
    // Payment modes used for this bill
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<PaymentMode> paymentModes = new ArrayList<>();
    
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
        calculateTotals();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        calculateTotals();
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
        
        // Calculate exchange amount from linked exchange if present
        if (exchange != null && exchange.getTotalExchangeAmount() != null) {
            exchangeAmount = exchange.getTotalExchangeAmount();
            System.out.println("Bill calculateTotals - Exchange amount from exchange entity: " + exchangeAmount);
        } else if (exchangeAmount == null) {
            exchangeAmount = BigDecimal.ZERO;
            System.out.println("Bill calculateTotals - No exchange linked or amount null");
        } else {
            System.out.println("Bill calculateTotals - Using existing exchange amount: " + exchangeAmount);
        }
        
        // Calculate net total after discount (but before exchange deduction)
        netTotal = subtotal.subtract(discount);
        
        // Calculate tax amounts (GST only on billing items, not on exchange)
        BigDecimal taxableAmount = subtotal;
        totalTaxAmount = taxableAmount.multiply(gstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        cgstAmount = totalTaxAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        sgstAmount = totalTaxAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        
        // Calculate grand total: netTotal + GST - exchangeAmount
        grandTotal = netTotal.add(totalTaxAmount).subtract(exchangeAmount);
        
        System.out.println("Bill calculateTotals - Subtotal: " + subtotal + ", Discount: " + discount + 
            ", NetTotal: " + netTotal + ", GST: " + totalTaxAmount + ", Exchange: " + exchangeAmount + 
            ", GrandTotal: " + grandTotal);
        
        // Calculate pending amount
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        pendingAmount = grandTotal.subtract(paidAmount);
        
        // If paid amount equals or exceeds grand total, pending is zero
        if (pendingAmount.compareTo(BigDecimal.ZERO) < 0) {
            pendingAmount = BigDecimal.ZERO;
        }
    }
    
    public enum PaymentMethod {
        CASH, UPI, CARD, CHEQUE, BANK_TRANSFER, PARTIAL, CREDIT
    }
    
    public enum BillStatus {
        DRAFT, CONFIRMED, PAID, CANCELLED
    }
}