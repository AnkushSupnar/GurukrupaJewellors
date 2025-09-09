package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@Table(name = "exchanges")
public class Exchange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String exchangeNumber;
    
    // Customer Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    // Exchange Details
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalExchangeAmount = BigDecimal.ZERO;
    
    // Exchange Status
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExchangeStatus status;
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime exchangeDate;
    
    @Column(nullable = false)
    private LocalDateTime createdDate;
    
    @Column
    private LocalDateTime updatedDate;
    
    // Notes or description
    @Column(length = 500)
    private String notes;
    
    // Reference to the bill (if exchange happened during billing)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = true)
    @JsonIgnoreProperties({"exchange", "billTransactions", "paymentModes", "customer"})
    private Bill bill;
    
    // Relationships
    @OneToMany(mappedBy = "exchange", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @ToString.Exclude
    @Builder.Default
    private List<ExchangeTransaction> exchangeTransactions = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (exchangeDate == null) {
            exchangeDate = LocalDateTime.now();
        }
        if (status == null) {
            status = ExchangeStatus.ACTIVE;
        }
        generateExchangeNumber();
        // Don't calculate here - let service handle it
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        // Don't calculate here - let service handle it
    }
    
    private void generateExchangeNumber() {
        // Exchange number generation is now handled by the service layer
        // This method exists for compatibility but doesn't generate the number
        if (exchangeNumber == null) {
            exchangeNumber = "TEMP-EX-" + System.currentTimeMillis();
        }
    }
    
    public void calculateTotalAmount() {
        // Calculate total exchange amount from exchange transactions
        totalExchangeAmount = exchangeTransactions.stream()
            .map(ExchangeTransaction::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public enum ExchangeStatus {
        ACTIVE, USED_IN_BILL, CANCELLED
    }
}