package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.entities.StockTransaction;
import com.gurukrupa.data.entities.StockTransaction.TransactionType;
import com.gurukrupa.data.entities.StockTransaction.TransactionSource;
import com.gurukrupa.data.repository.JewelryItemRepository;
import com.gurukrupa.data.repository.StockTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StockTransactionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(StockTransactionService.class);
    
    @Autowired
    private StockTransactionRepository stockTransactionRepository;
    
    @Autowired
    private JewelryItemRepository jewelryItemRepository;
    
    /**
     * Record a stock OUT transaction (e.g., sale)
     */
    public StockTransaction recordStockOut(JewelryItem item, Integer quantity, 
                                         TransactionSource source, String referenceType,
                                         Long referenceId, String referenceNumber,
                                         String description, String createdBy) {
        
        // Validate quantity
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        
        // Check current stock
        Integer currentStock = item.getQuantity();
        if (currentStock < quantity) {
            throw new IllegalStateException(
                String.format("Insufficient stock for item %s. Available: %d, Requested: %d", 
                    item.getItemCode(), currentStock, quantity)
            );
        }
        
        // Calculate new stock level
        Integer newStock = currentStock - quantity;
        
        // Create stock transaction
        StockTransaction transaction = StockTransaction.builder()
                .jewelryItem(item)
                .transactionType(TransactionType.OUT)
                .transactionSource(source)
                .quantity(quantity)
                .quantityBefore(currentStock)
                .quantityAfter(newStock)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .description(description)
                .transactionDate(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
        
        // Save transaction
        transaction = stockTransactionRepository.save(transaction);
        
        // Update item quantity
        item.setQuantity(newStock);
        jewelryItemRepository.save(item);
        
        LOG.info("Stock OUT recorded: Item={}, Quantity={}, New Stock={}, Reference={}", 
                item.getItemCode(), quantity, newStock, referenceNumber);
        
        return transaction;
    }
    
    /**
     * Record a stock IN transaction (e.g., purchase, return)
     */
    public StockTransaction recordStockIn(JewelryItem item, Integer quantity,
                                        TransactionSource source, String referenceType,
                                        Long referenceId, String referenceNumber,
                                        String description, String createdBy) {
        
        // Validate quantity
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        
        // Get current stock
        Integer currentStock = item.getQuantity();
        
        // Calculate new stock level
        Integer newStock = currentStock + quantity;
        
        // Create stock transaction
        StockTransaction transaction = StockTransaction.builder()
                .jewelryItem(item)
                .transactionType(TransactionType.IN)
                .transactionSource(source)
                .quantity(quantity)
                .quantityBefore(currentStock)
                .quantityAfter(newStock)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .description(description)
                .transactionDate(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
        
        // Save transaction
        transaction = stockTransactionRepository.save(transaction);
        
        // Update item quantity
        item.setQuantity(newStock);
        jewelryItemRepository.save(item);
        
        LOG.info("Stock IN recorded: Item={}, Quantity={}, New Stock={}, Reference={}", 
                item.getItemCode(), quantity, newStock, referenceNumber);
        
        return transaction;
    }
    
    /**
     * Record a stock adjustment (correction)
     */
    public StockTransaction recordStockAdjustment(JewelryItem item, Integer newQuantity,
                                                String reason, String createdBy) {
        
        // Get current stock
        Integer currentStock = item.getQuantity();
        
        // Calculate adjustment quantity
        Integer adjustmentQty = Math.abs(newQuantity - currentStock);
        
        // Determine transaction type
        TransactionType type = newQuantity > currentStock ? TransactionType.IN : TransactionType.OUT;
        
        // Create stock transaction
        StockTransaction transaction = StockTransaction.builder()
                .jewelryItem(item)
                .transactionType(type)
                .transactionSource(TransactionSource.ADJUSTMENT)
                .quantity(adjustmentQty)
                .quantityBefore(currentStock)
                .quantityAfter(newQuantity)
                .referenceType("ADJUSTMENT")
                .description("Stock adjustment: " + reason)
                .remarks(reason)
                .transactionDate(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
        
        // Save transaction
        transaction = stockTransactionRepository.save(transaction);
        
        // Update item quantity
        item.setQuantity(newQuantity);
        jewelryItemRepository.save(item);
        
        LOG.info("Stock adjustment recorded: Item={}, From={}, To={}, Reason={}", 
                item.getItemCode(), currentStock, newQuantity, reason);
        
        return transaction;
    }
    
    /**
     * Record stock out for a bill sale
     */
    public StockTransaction recordBillSale(Long itemId, String itemCode, Integer quantity,
                                         Long billId, String billNumber, String customerName) {
        
        // Find the jewelry item
        Optional<JewelryItem> itemOpt = Optional.empty();
        
        // Try to find by itemId if provided
        if (itemId != null) {
            itemOpt = jewelryItemRepository.findById(itemId);
        }
        
        // If not found by ID, try to find by item code
        if (itemOpt.isEmpty() && itemCode != null) {
            itemOpt = jewelryItemRepository.findByItemCode(itemCode);
        }
        
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Jewelry item not found: " + 
                (itemCode != null ? itemCode : "ID " + itemId));
        }
        
        JewelryItem item = itemOpt.get();
        
        String description = String.format("Sale to %s - Bill %s", 
                customerName != null ? customerName : "Customer", billNumber);
        
        return recordStockOut(item, quantity, TransactionSource.SALE, "BILL",
                            billId, billNumber, description, "System");
    }
    
    /**
     * Get stock transaction history for an item
     */
    @Transactional(readOnly = true)
    public List<StockTransaction> getItemTransactionHistory(Long itemId) {
        return stockTransactionRepository.findByJewelryItemIdOrderByTransactionDateDesc(itemId);
    }
    
    /**
     * Get transactions by reference
     */
    @Transactional(readOnly = true)
    public List<StockTransaction> getTransactionsByReference(String referenceType, Long referenceId) {
        return stockTransactionRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }
    
    /**
     * Check if stock transactions exist for a bill
     */
    @Transactional(readOnly = true)
    public boolean hasBillTransactions(Long billId) {
        return stockTransactionRepository.existsByBillId(billId);
    }
    
    /**
     * Get today's transactions
     */
    @Transactional(readOnly = true)
    public List<StockTransaction> getTodaysTransactions() {
        return stockTransactionRepository.findTodaysTransactions();
    }
    
    /**
     * Get transactions for date range
     */
    @Transactional(readOnly = true)
    public List<StockTransaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stockTransactionRepository.findByTransactionDateBetween(startDate, endDate);
    }
    
    /**
     * Calculate current stock from transactions (for verification)
     */
    @Transactional(readOnly = true)
    public Integer calculateStockFromTransactions(Long itemId) {
        Integer totalIn = stockTransactionRepository.getTotalInQuantityByItemId(itemId);
        Integer totalOut = stockTransactionRepository.getTotalOutQuantityByItemId(itemId);
        return totalIn - totalOut;
    }
    
    /**
     * Verify and fix stock discrepancies
     */
    public void verifyAndFixStock(Long itemId, String createdBy) {
        Optional<JewelryItem> itemOpt = jewelryItemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Item not found with ID: " + itemId);
        }
        
        JewelryItem item = itemOpt.get();
        Integer calculatedStock = calculateStockFromTransactions(itemId);
        Integer currentStock = item.getQuantity();
        
        if (!calculatedStock.equals(currentStock)) {
            LOG.warn("Stock discrepancy found for item {}: Database={}, Calculated={}", 
                    item.getItemCode(), currentStock, calculatedStock);
            
            // Create adjustment transaction
            recordStockAdjustment(item, calculatedStock, 
                    "System correction - Stock discrepancy fix", createdBy);
        }
    }
    
    /**
     * Get stock movement summary
     */
    @Transactional(readOnly = true)
    public List<Object[]> getStockMovementSummary(Long itemId) {
        return stockTransactionRepository.getStockMovementSummaryByItemId(itemId);
    }
    
    /**
     * Rollback a transaction (create reverse transaction)
     */
    public StockTransaction rollbackTransaction(Long transactionId, String reason, String createdBy) {
        Optional<StockTransaction> originalOpt = stockTransactionRepository.findById(transactionId);
        if (originalOpt.isEmpty()) {
            throw new IllegalArgumentException("Transaction not found with ID: " + transactionId);
        }
        
        StockTransaction original = originalOpt.get();
        JewelryItem item = original.getJewelryItem();
        
        // Create reverse transaction
        TransactionType reverseType = original.getTransactionType() == TransactionType.IN ? 
                                     TransactionType.OUT : TransactionType.IN;
        
        String description = String.format("Rollback of transaction #%d - %s", 
                                         transactionId, reason);
        
        if (reverseType == TransactionType.IN) {
            return recordStockIn(item, original.getQuantity(), TransactionSource.OTHER,
                               "ROLLBACK", transactionId, "ROLLBACK-" + transactionId,
                               description, createdBy);
        } else {
            return recordStockOut(item, original.getQuantity(), TransactionSource.OTHER,
                                "ROLLBACK", transactionId, "ROLLBACK-" + transactionId,
                                description, createdBy);
        }
    }
}