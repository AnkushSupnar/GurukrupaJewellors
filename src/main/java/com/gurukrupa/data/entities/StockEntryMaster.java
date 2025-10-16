package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Master table for stock entry
 * Links purchased metal to manufactured jewelry items
 */
@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "stock_entry_master")
public class StockEntryMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String entryNumber; // Format: SE-YYYYMMDD-0001

    @Column(nullable = false)
    private LocalDateTime entryDate;

    // Reference to purchase invoice (multiple stock entries can use the same invoice until metal is exhausted)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_invoice_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private PurchaseInvoice purchaseInvoice;

    // Summary fields
    @Column(nullable = false)
    @Builder.Default
    private Integer totalItems = 0;

    @Column(precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal totalGrossWeight = BigDecimal.ZERO;

    @Column(precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal totalNetWeight = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EntryStatus status = EntryStatus.ACTIVE;

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column
    private LocalDateTime updatedDate;

    // One-to-many relationship with stock entry items
    @OneToMany(mappedBy = "stockEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JsonIgnoreProperties({"stockEntry"})
    @Builder.Default
    private List<StockEntryItem> stockEntryItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (entryDate == null) {
            entryDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

    // Enums
    public enum EntryStatus {
        ACTIVE,
        CANCELLED
    }

    /**
     * Calculate and update summary totals
     */
    public void calculateTotals() {
        if (stockEntryItems == null || stockEntryItems.isEmpty()) {
            totalItems = 0;
            totalGrossWeight = BigDecimal.ZERO;
            totalNetWeight = BigDecimal.ZERO;
            return;
        }

        totalItems = stockEntryItems.stream()
            .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
            .sum();

        totalGrossWeight = stockEntryItems.stream()
            .map(item -> {
                if (item.getJewelryItem() != null && item.getJewelryItem().getGrossWeight() != null) {
                    BigDecimal weight = item.getJewelryItem().getGrossWeight();
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    return weight.multiply(BigDecimal.valueOf(qty));
                }
                return BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalNetWeight = stockEntryItems.stream()
            .map(item -> {
                if (item.getJewelryItem() != null && item.getJewelryItem().getNetWeight() != null) {
                    BigDecimal weight = item.getJewelryItem().getNetWeight();
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    return weight.multiply(BigDecimal.valueOf(qty));
                }
                return BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Add an item to this stock entry
     */
    public void addItem(StockEntryItem item) {
        if (stockEntryItems == null) {
            stockEntryItems = new ArrayList<>();
        }
        stockEntryItems.add(item);
        item.setStockEntry(this);
        calculateTotals();
    }

    /**
     * Remove an item from this stock entry
     */
    public void removeItem(StockEntryItem item) {
        if (stockEntryItems != null) {
            stockEntryItems.remove(item);
            item.setStockEntry(null);
            calculateTotals();
        }
    }

    /**
     * Get display string for this entry
     */
    public String getDisplayString() {
        return String.format("%s - %s - %d items, %.3fg",
            entryNumber,
            purchaseInvoice != null ? purchaseInvoice.getInvoiceNumber() : "N/A",
            totalItems,
            totalGrossWeight);
    }
}
