package com.gurukrupa.data.entities;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Transaction/Link table for stock entry items
 * Links stock entry master to jewelry items
 */
@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "stock_entry_items")
public class StockEntryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to stock entry master
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_entry_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"stockEntryItems"})
    private StockEntryMaster stockEntry;

    // Reference to jewelry item
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "jewelry_item_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private JewelryItem jewelryItem;

    // Quantity of this item
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(length = 500)
    private String remarks;

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    /**
     * Get display string for this item
     */
    public String getDisplayString() {
        if (jewelryItem != null) {
            return String.format("%s - %s (Qty: %d)",
                jewelryItem.getItemCode(),
                jewelryItem.getItemName(),
                quantity);
        }
        return "Unknown Item";
    }

    /**
     * Get total gross weight for this entry (item gross weight * quantity)
     */
    public java.math.BigDecimal getTotalGrossWeight() {
        if (jewelryItem != null && jewelryItem.getGrossWeight() != null) {
            return jewelryItem.getGrossWeight().multiply(java.math.BigDecimal.valueOf(quantity));
        }
        return java.math.BigDecimal.ZERO;
    }

    /**
     * Get total net weight for this entry (item net weight * quantity)
     */
    public java.math.BigDecimal getTotalNetWeight() {
        if (jewelryItem != null && jewelryItem.getNetWeight() != null) {
            return jewelryItem.getNetWeight().multiply(java.math.BigDecimal.valueOf(quantity));
        }
        return java.math.BigDecimal.ZERO;
    }
}
