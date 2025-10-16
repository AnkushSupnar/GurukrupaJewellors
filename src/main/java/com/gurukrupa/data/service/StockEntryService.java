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

@Service
@RequiredArgsConstructor
@Slf4j
public class StockEntryService {

    private final StockEntryMasterRepository stockEntryMasterRepository;
    private final StockEntryItemRepository stockEntryItemRepository;

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
     * Returns map: "MetalType Purity" -> consumed weight
     */
    public java.util.Map<String, java.math.BigDecimal> getConsumedMetalForInvoice(Long purchaseInvoiceId) {
        java.util.Map<String, java.math.BigDecimal> consumedMetal = new java.util.HashMap<>();

        List<StockEntryMaster> entries = stockEntryMasterRepository.findByPurchaseInvoiceId(purchaseInvoiceId);

        for (StockEntryMaster entry : entries) {
            if (entry.getStockEntryItems() == null) continue;

            for (StockEntryItem item : entry.getStockEntryItems()) {
                JewelryItem jewelry = item.getJewelryItem();
                if (jewelry == null) continue;

                String key = jewelry.getMetalType() + " " + jewelry.getPurity();
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
     * Returns map: "MetalType Purity" -> remaining weight
     */
    public java.util.Map<String, java.math.BigDecimal> getRemainingMetalForInvoice(PurchaseInvoice invoice) {
        java.util.Map<String, java.math.BigDecimal> remainingMetal = new java.util.HashMap<>();

        if (invoice == null || invoice.getPurchaseMetalTransactions() == null) {
            return remainingMetal;
        }

        // Calculate total available metal from invoice
        java.util.Map<String, java.math.BigDecimal> availableMetal = new java.util.HashMap<>();
        for (PurchaseMetalTransaction transaction : invoice.getPurchaseMetalTransactions()) {
            String key = transaction.getMetalType() + " " + transaction.getPurity();
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

        // If new entry, validate invoice not already used
        if (stockEntry.getId() == null) {
            String validationError = validatePurchaseInvoice(stockEntry.getPurchaseInvoice());
            if (validationError != null) {
                throw new IllegalArgumentException(validationError);
            }
        }

        log.info("Saving stock entry: {}", stockEntry.getEntryNumber());
        return stockEntryMasterRepository.save(stockEntry);
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
     */
    @Transactional
    public void delete(StockEntryMaster stockEntry) {
        log.info("Deleting stock entry: {}", stockEntry.getEntryNumber());
        stockEntryMasterRepository.delete(stockEntry);
    }

    /**
     * Cancel stock entry (soft delete)
     */
    @Transactional
    public void cancel(StockEntryMaster stockEntry) {
        stockEntry.setStatus(StockEntryMaster.EntryStatus.CANCELLED);
        save(stockEntry);
        log.info("Cancelled stock entry: {}", stockEntry.getEntryNumber());
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
