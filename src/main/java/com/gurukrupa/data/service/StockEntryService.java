package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.repository.StockEntryMasterRepository;
import com.gurukrupa.data.repository.StockEntryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockEntryService {

    private final StockEntryMasterRepository stockEntryMasterRepository;
    private final StockEntryItemRepository stockEntryItemRepository;

    @Autowired
    private MetalService metalService;

    @Autowired
    private StockTransactionService stockTransactionService;

    @Autowired
    private PurchaseMetalStockService purchaseMetalStockService;

    /**
     * Generate next entry number
     * Format: SE-YYYYMMDD-XXXX
     */
    public String generateEntryNumber() {
        String datePrefix = "SE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<String> latestNumbers = stockEntryMasterRepository.findLatestEntryNumber();

        int nextSequence = 1;
        if (!latestNumbers.isEmpty()) {
            String latestNumber = latestNumbers.get(0);
            if (latestNumber != null && latestNumber.startsWith(datePrefix)) {
                try {
                    String sequencePart = latestNumber.substring(latestNumber.lastIndexOf("-") + 1);
                    nextSequence = Integer.parseInt(sequencePart) + 1;
                } catch (Exception e) {
                    log.warn("Error parsing latest entry number: {}", latestNumber, e);
                }
            }
        }

        return String.format("%s-%04d", datePrefix, nextSequence);
    }

    /**
     * Calculate consumed metal from all stock entries for a given purchase invoice
     * Returns map: "MetalID" or "MetalType Purity" -> consumed weight
     * Uses Metal entity reference if available, otherwise falls back to denormalized fields
     */
    public java.util.Map<String, java.math.BigDecimal> getConsumedMetalForInvoice(Long purchaseInvoiceId) {
        java.util.Map<String, java.math.BigDecimal> consumedMetal = new java.util.HashMap<>();

        List<StockEntryMaster> entries = stockEntryMasterRepository.findByPurchaseInvoiceId(purchaseInvoiceId);

        for (StockEntryMaster entry : entries) {
            if (entry.getStockEntryItems() == null) continue;

            for (StockEntryItem item : entry.getStockEntryItems()) {
                JewelryItem jewelry = item.getJewelryItem();
                if (jewelry == null) continue;

                // Generate key using Metal entity if available, otherwise use denormalized fields
                String key = getMetalKey(jewelry.getMetal(), jewelry.getMetalType(), jewelry.getPurity());
                java.math.BigDecimal consumed = consumedMetal.getOrDefault(key, java.math.BigDecimal.ZERO);

                // Calculate total consumed weight (net weight * quantity)
                java.math.BigDecimal itemWeight = jewelry.getNetWeight().multiply(
                    java.math.BigDecimal.valueOf(item.getQuantity())
                );

                consumedMetal.put(key, consumed.add(itemWeight));
            }
        }

        return consumedMetal;
    }

    /**
     * Calculate remaining metal available in a purchase invoice
     * Returns map: "MetalID" or "MetalType Purity" -> remaining weight
     * Uses Metal entity reference if available, otherwise falls back to denormalized fields
     */
    public java.util.Map<String, java.math.BigDecimal> getRemainingMetalForInvoice(PurchaseInvoice invoice) {
        java.util.Map<String, java.math.BigDecimal> remainingMetal = new java.util.HashMap<>();

        if (invoice == null || invoice.getPurchaseMetalTransactions() == null) {
            return remainingMetal;
        }

        // Calculate total available metal from invoice
        java.util.Map<String, java.math.BigDecimal> availableMetal = new java.util.HashMap<>();
        for (PurchaseMetalTransaction transaction : invoice.getPurchaseMetalTransactions()) {
            // Generate key using Metal entity if available, otherwise use denormalized fields
            String key = getMetalKey(transaction.getMetal(), transaction.getMetalType(), transaction.getPurity());
            java.math.BigDecimal current = availableMetal.getOrDefault(key, java.math.BigDecimal.ZERO);
            availableMetal.put(key, current.add(transaction.getGrossWeight()));
        }

        // Get consumed metal from all stock entries
        java.util.Map<String, java.math.BigDecimal> consumedMetal = getConsumedMetalForInvoice(invoice.getId());

        // Calculate remaining = available - consumed
        for (String key : availableMetal.keySet()) {
            java.math.BigDecimal available = availableMetal.get(key);
            java.math.BigDecimal consumed = consumedMetal.getOrDefault(key, java.math.BigDecimal.ZERO);
            java.math.BigDecimal remaining = available.subtract(consumed);

            if (remaining.compareTo(java.math.BigDecimal.ZERO) > 0) {
                remainingMetal.put(key, remaining);
            }
        }

        return remainingMetal;
    }

    /**
     * Generate consistent metal key for matching
     * Uses Metal entity ID if available, otherwise creates key from metalType and purity
     * @param metal Metal entity (can be null)
     * @param metalType Denormalized metal type (fallback)
     * @param purity Denormalized purity (fallback)
     * @return Consistent metal key for matching
     */
    private String getMetalKey(Metal metal, String metalType, java.math.BigDecimal purity) {
        if (metal != null && metal.getId() != null) {
            // Use Metal ID as key for exact matching
            return "M-" + metal.getId();
        } else {
            // Fallback to denormalized fields with normalized purity
            return metalType + " " + purity.stripTrailingZeros().toPlainString();
        }
    }

    /**
     * Check if purchase invoice has any remaining metal available
     */
    public boolean hasRemainingMetal(Long purchaseInvoiceId) {
        PurchaseInvoice invoice = new PurchaseInvoice();
        invoice.setId(purchaseInvoiceId);

        // Find the actual invoice from database
        // We need the full invoice with transactions
        List<StockEntryMaster> entries = stockEntryMasterRepository.findByPurchaseInvoiceId(purchaseInvoiceId);
        if (!entries.isEmpty()) {
            invoice = entries.get(0).getPurchaseInvoice();
        }

        java.util.Map<String, java.math.BigDecimal> remaining = getRemainingMetalForInvoice(invoice);
        return !remaining.isEmpty();
    }

    /**
     * Validate purchase invoice for stock entry
     * Returns error message if invalid, null if valid
     */
    public String validatePurchaseInvoice(PurchaseInvoice purchaseInvoice) {
        if (purchaseInvoice == null) {
            return "Purchase invoice is required";
        }

        // Check if invoice has metal transactions
        if (purchaseInvoice.getPurchaseMetalTransactions() == null ||
            purchaseInvoice.getPurchaseMetalTransactions().isEmpty()) {
            return "Selected purchase invoice has no metal transactions. Cannot create stock entry.";
        }

        // Check if there's any remaining metal available
        java.util.Map<String, java.math.BigDecimal> remainingMetal = getRemainingMetalForInvoice(purchaseInvoice);
        if (remainingMetal.isEmpty()) {
            return String.format("Purchase invoice %s has no remaining metal available. All metal from this invoice has been consumed in previous stock entries.",
                    purchaseInvoice.getInvoiceNumber());
        }

        return null; // Valid
    }

    /**
     * Get all stock entries for a purchase invoice
     */
    public List<StockEntryMaster> findByPurchaseInvoice(PurchaseInvoice purchaseInvoice) {
        return stockEntryMasterRepository.findByPurchaseInvoiceAndStatus(
            purchaseInvoice, StockEntryMaster.EntryStatus.ACTIVE);
    }

    /**
     * Save stock entry (create or update)
     */
    @Transactional
    public StockEntryMaster save(StockEntryMaster stockEntry) {
        // Calculate totals before saving
        stockEntry.calculateTotals();

        // Determine if this is a new entry
        boolean isNewEntry = (stockEntry.getId() == null);

        // If new entry, validate invoice not already used
        if (isNewEntry) {
            String validationError = validatePurchaseInvoice(stockEntry.getPurchaseInvoice());
            if (validationError != null) {
                throw new IllegalArgumentException(validationError);
            }
        }

        log.info("Saving stock entry: {}", stockEntry.getEntryNumber());
        StockEntryMaster savedEntry = stockEntryMasterRepository.save(stockEntry);

        // Record stock transactions for new entries only
        if (isNewEntry) {
            recordStockTransactions(savedEntry);
        }

        return savedEntry;
    }

    /**
     * Record stock transactions for all items in the stock entry
     * This creates a STOCK IN transaction for each jewelry item
     * Also updates purchase metal stock to track metal consumption
     */
    private void recordStockTransactions(StockEntryMaster stockEntry) {
        if (stockEntry.getStockEntryItems() == null || stockEntry.getStockEntryItems().isEmpty()) {
            log.warn("No items to record transactions for stock entry: {}", stockEntry.getEntryNumber());
            return;
        }

        String purchaseInvoiceNumber = stockEntry.getPurchaseInvoice() != null ?
                stockEntry.getPurchaseInvoice().getInvoiceNumber() : "Unknown";

        log.info("Recording stock transactions for {} items in stock entry: {}",
                stockEntry.getStockEntryItems().size(), stockEntry.getEntryNumber());

        for (StockEntryItem item : stockEntry.getStockEntryItems()) {
            try {
                JewelryItem jewelryItem = item.getJewelryItem();
                if (jewelryItem == null) {
                    log.warn("Skipping item with null jewelryItem in stock entry: {}", stockEntry.getEntryNumber());
                    continue;
                }

                int quantity = item.getQuantity();
                if (quantity <= 0) {
                    log.warn("Skipping item {} with zero or negative quantity", jewelryItem.getItemCode());
                    continue;
                }

                String description = String.format("Stock Entry from Purchase Invoice %s - %s",
                        purchaseInvoiceNumber,
                        item.getRemarks() != null ? item.getRemarks() : "");

                // Record stock IN transaction
                stockTransactionService.recordStockIn(
                        jewelryItem,
                        quantity,
                        StockTransaction.TransactionSource.PRODUCTION, // Using PRODUCTION as it's manufactured items
                        "STOCK_ENTRY",
                        stockEntry.getId(),
                        stockEntry.getEntryNumber(),
                        description.trim(),
                        "System"
                );

                log.info("Recorded stock IN transaction: Item={}, Quantity={}, Entry={}",
                        jewelryItem.getItemCode(), quantity, stockEntry.getEntryNumber());

                // Update purchase metal stock - consume metal used for this jewelry item
                updatePurchaseMetalStock(stockEntry, jewelryItem, quantity);

            } catch (Exception e) {
                log.error("Error recording stock transaction for item in entry {}: {}",
                        stockEntry.getEntryNumber(), e.getMessage(), e);
                // Continue with next item rather than failing entire entry
            }
        }

        log.info("Completed recording stock transactions for stock entry: {}", stockEntry.getEntryNumber());
    }

    /**
     * Update purchase metal stock to track metal consumption for a jewelry item
     * This updates the used_weight and available_weight in purchase_metal_stock table
     */
    private void updatePurchaseMetalStock(StockEntryMaster stockEntry, JewelryItem jewelryItem, int quantity) {
        try {
            if (jewelryItem.getMetal() == null) {
                log.warn("Cannot update metal stock - jewelry item {} has no metal reference",
                        jewelryItem.getItemCode());
                return;
            }

            // Calculate total metal weight consumed (net weight × quantity)
            java.math.BigDecimal metalWeightConsumed = jewelryItem.getNetWeight()
                    .multiply(java.math.BigDecimal.valueOf(quantity));

            String reference = "STOCK_ENTRY: " + stockEntry.getEntryNumber();
            String description = String.format("Metal consumed for %s (Qty: %d) - %s",
                    jewelryItem.getItemCode(),
                    quantity,
                    jewelryItem.getItemName());

            // Use Metal entity reference for accurate tracking
            purchaseMetalStockService.useMetal(
                    jewelryItem.getMetal(),
                    metalWeightConsumed,
                    reference,
                    description
            );

            log.info("Updated metal stock: {} (ID: {}) - consumed {} for item {}",
                    jewelryItem.getMetal().getMetalName(),
                    jewelryItem.getMetal().getId(),
                    metalWeightConsumed,
                    jewelryItem.getItemCode());

        } catch (Exception e) {
            log.error("Error updating purchase metal stock for item {} in entry {}: {}",
                    jewelryItem.getItemCode(), stockEntry.getEntryNumber(), e.getMessage(), e);
        }
    }

    /**
     * Find stock entry by ID
     */
    public Optional<StockEntryMaster> findById(Long id) {
        return stockEntryMasterRepository.findById(id);
    }

    /**
     * Find stock entry by entry number
     */
    public Optional<StockEntryMaster> findByEntryNumber(String entryNumber) {
        return stockEntryMasterRepository.findByEntryNumber(entryNumber);
    }

    /**
     * Find all active stock entries
     */
    public List<StockEntryMaster> findAllActive() {
        return stockEntryMasterRepository.findByStatusOrderByEntryDateDesc(StockEntryMaster.EntryStatus.ACTIVE);
    }

    /**
     * Find stock entries by date range
     */
    public List<StockEntryMaster> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stockEntryMasterRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Search stock entries
     */
    public List<StockEntryMaster> searchEntries(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAllActive();
        }
        return stockEntryMasterRepository.searchEntries(searchTerm);
    }

    /**
     * Delete stock entry
     * Also reverses all stock transactions associated with this entry
     */
    @Transactional
    public void delete(StockEntryMaster stockEntry) {
        log.info("Deleting stock entry: {}", stockEntry.getEntryNumber());

        // Reverse stock transactions before deleting
        reverseStockTransactions(stockEntry);

        stockEntryMasterRepository.delete(stockEntry);
    }

    /**
     * Cancel stock entry (soft delete)
     * Also reverses all stock transactions associated with this entry
     */
    @Transactional
    public void cancel(StockEntryMaster stockEntry) {
        log.info("Cancelling stock entry: {}", stockEntry.getEntryNumber());

        // Reverse stock transactions for cancelled entry
        reverseStockTransactions(stockEntry);

        stockEntry.setStatus(StockEntryMaster.EntryStatus.CANCELLED);
        // Note: We don't call save() here as it would trigger recordStockTransactions again
        stockEntryMasterRepository.save(stockEntry);

        log.info("Cancelled stock entry: {}", stockEntry.getEntryNumber());
    }

    /**
     * Reverse stock transactions for a stock entry
     * Creates STOCK OUT transactions to reverse the original STOCK IN transactions
     * Also returns metal back to purchase metal stock
     */
    private void reverseStockTransactions(StockEntryMaster stockEntry) {
        if (stockEntry.getStockEntryItems() == null || stockEntry.getStockEntryItems().isEmpty()) {
            log.warn("No items to reverse transactions for stock entry: {}", stockEntry.getEntryNumber());
            return;
        }

        log.info("Reversing stock transactions for {} items in stock entry: {}",
                stockEntry.getStockEntryItems().size(), stockEntry.getEntryNumber());

        for (StockEntryItem item : stockEntry.getStockEntryItems()) {
            try {
                JewelryItem jewelryItem = item.getJewelryItem();
                if (jewelryItem == null) {
                    log.warn("Skipping item with null jewelryItem in stock entry: {}", stockEntry.getEntryNumber());
                    continue;
                }

                int quantity = item.getQuantity();
                if (quantity <= 0) {
                    log.warn("Skipping item {} with zero or negative quantity", jewelryItem.getItemCode());
                    continue;
                }

                String description = String.format("Reversal of Stock Entry %s - %s",
                        stockEntry.getEntryNumber(),
                        stockEntry.getStatus() == StockEntryMaster.EntryStatus.CANCELLED ? "Entry Cancelled" : "Entry Deleted");

                // Record stock OUT transaction to reverse the original stock IN
                stockTransactionService.recordStockOut(
                        jewelryItem,
                        quantity,
                        StockTransaction.TransactionSource.ADJUSTMENT,
                        "STOCK_ENTRY_REVERSAL",
                        stockEntry.getId(),
                        "REV-" + stockEntry.getEntryNumber(),
                        description,
                        "System"
                );

                log.info("Reversed stock transaction: Item={}, Quantity={}, Entry={}",
                        jewelryItem.getItemCode(), quantity, stockEntry.getEntryNumber());

                // Return metal back to purchase metal stock
                returnPurchaseMetalStock(stockEntry, jewelryItem, quantity);

            } catch (Exception e) {
                log.error("Error reversing stock transaction for item in entry {}: {}",
                        stockEntry.getEntryNumber(), e.getMessage(), e);
                // Continue with next item rather than failing entire reversal
            }
        }

        log.info("Completed reversing stock transactions for stock entry: {}", stockEntry.getEntryNumber());
    }

    /**
     * Return metal back to purchase metal stock when stock entry is cancelled/deleted
     * This updates the used_weight and available_weight in purchase_metal_stock table
     */
    private void returnPurchaseMetalStock(StockEntryMaster stockEntry, JewelryItem jewelryItem, int quantity) {
        try {
            if (jewelryItem.getMetal() == null) {
                log.warn("Cannot return metal stock - jewelry item {} has no metal reference",
                        jewelryItem.getItemCode());
                return;
            }

            // Calculate total metal weight to return (net weight × quantity)
            java.math.BigDecimal metalWeightToReturn = jewelryItem.getNetWeight()
                    .multiply(java.math.BigDecimal.valueOf(quantity));

            String reference = "STOCK_ENTRY_REVERSAL: " + stockEntry.getEntryNumber();
            String description = String.format("Metal returned from %s (Qty: %d) - Entry %s",
                    jewelryItem.getItemCode(),
                    quantity,
                    stockEntry.getStatus() == StockEntryMaster.EntryStatus.CANCELLED ? "Cancelled" : "Deleted");

            // Use Metal entity reference for accurate tracking
            purchaseMetalStockService.returnMetal(
                    jewelryItem.getMetal(),
                    metalWeightToReturn,
                    reference,
                    description
            );

            log.info("Returned metal stock: {} (ID: {}) - returned {} for item {}",
                    jewelryItem.getMetal().getMetalName(),
                    jewelryItem.getMetal().getId(),
                    metalWeightToReturn,
                    jewelryItem.getItemCode());

        } catch (Exception e) {
            log.error("Error returning purchase metal stock for item {} in entry {}: {}",
                    jewelryItem.getItemCode(), stockEntry.getEntryNumber(), e.getMessage(), e);
        }
    }

    /**
     * Get count of active stock entries
     */
    public long getActiveCount() {
        return stockEntryMasterRepository.countByStatus(StockEntryMaster.EntryStatus.ACTIVE);
    }

    // ===== Stock Entry Item Operations =====

    /**
     * Add item to stock entry
     */
    @Transactional
    public StockEntryItem addItem(StockEntryMaster stockEntry, JewelryItem jewelryItem, int quantity, String remarks) {
        StockEntryItem item = StockEntryItem.builder()
                .stockEntry(stockEntry)
                .jewelryItem(jewelryItem)
                .quantity(quantity)
                .remarks(remarks)
                .build();

        stockEntry.addItem(item);
        stockEntryItemRepository.save(item);
        stockEntryMasterRepository.save(stockEntry);

        log.info("Added item {} to stock entry {}", jewelryItem.getItemCode(), stockEntry.getEntryNumber());
        return item;
    }

    /**
     * Remove item from stock entry
     */
    @Transactional
    public void removeItem(StockEntryMaster stockEntry, StockEntryItem item) {
        stockEntry.removeItem(item);
        stockEntryItemRepository.delete(item);
        stockEntryMasterRepository.save(stockEntry);

        log.info("Removed item from stock entry {}", stockEntry.getEntryNumber());
    }

    /**
     * Find items by stock entry
     */
    public List<StockEntryItem> findItemsByStockEntry(StockEntryMaster stockEntry) {
        return stockEntryItemRepository.findByStockEntry(stockEntry);
    }

    /**
     * Get total quantity of a jewelry item in stock
     */
    public Integer getTotalQuantityInStock(Long jewelryItemId) {
        return stockEntryItemRepository.getTotalQuantityByJewelryItem(jewelryItemId);
    }
}
