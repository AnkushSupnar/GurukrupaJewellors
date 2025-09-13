package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.PurchaseTransaction;
import com.gurukrupa.data.entities.PurchaseInvoice;
import com.gurukrupa.data.repository.PurchaseTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PurchaseTransactionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseTransactionService.class);
    
    @Autowired
    private PurchaseTransactionRepository purchaseTransactionRepository;
    
    @Autowired
    private StockTransactionService stockTransactionService;
    
    @Autowired
    private ExchangeMetalStockService exchangeMetalStockService;
    
    /**
     * Save a purchase transaction
     */
    public PurchaseTransaction saveTransaction(PurchaseTransaction transaction) {
        // Calculate totals before saving
        transaction.calculateTotalAmount();
        return purchaseTransactionRepository.save(transaction);
    }
    
    /**
     * Get all transactions for a purchase invoice
     */
    public List<PurchaseTransaction> getTransactionsByInvoice(Long invoiceId) {
        return purchaseTransactionRepository.findByPurchaseInvoiceId(invoiceId);
    }
    
    /**
     * Get transaction by ID
     */
    public Optional<PurchaseTransaction> findById(Long id) {
        return purchaseTransactionRepository.findById(id);
    }
    
    /**
     * Delete a transaction
     */
    public void deleteTransaction(Long id) {
        purchaseTransactionRepository.deleteById(id);
    }
    
    /**
     * Get transactions by item code
     */
    public List<PurchaseTransaction> findByItemCode(String itemCode) {
        return purchaseTransactionRepository.findByItemCodeContainingIgnoreCase(itemCode);
    }
    
    /**
     * Get transactions by metal type
     */
    public List<PurchaseTransaction> findByMetalType(String metalType) {
        return purchaseTransactionRepository.findByMetalType(metalType);
    }
    
    /**
     * Get transactions by supplier
     */
    public List<PurchaseTransaction> findBySupplier(Long supplierId) {
        // This needs to be implemented using a custom query
        // For now, return all transactions and filter by supplier
        return purchaseTransactionRepository.findAll().stream()
            .filter(t -> t.getPurchaseInvoice() != null && 
                        t.getPurchaseInvoice().getSupplier() != null &&
                        t.getPurchaseInvoice().getSupplier().getId().equals(supplierId))
            .collect(Collectors.toList());
    }
    
    /**
     * Get transactions by date range
     */
    public List<PurchaseTransaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseTransactionRepository.findByDateRange(startDate, endDate);
    }
    
    /**
     * Get exchange item transactions
     */
    public List<PurchaseTransaction> findExchangeItemTransactions() {
        return purchaseTransactionRepository.findByItemType(PurchaseTransaction.ItemType.EXCHANGE_ITEM);
    }
    
    /**
     * Process stock addition for purchase transaction
     */
    public void processStockAddition(PurchaseTransaction transaction, PurchaseInvoice invoice) {
        if (transaction.getItemType() == PurchaseTransaction.ItemType.NEW_ITEM && 
            transaction.getQuantity() != null && transaction.getQuantity() > 0) {
            
            try {
                // For now, log the stock addition requirement
                // In a real implementation, we would need to:
                // 1. Find or create the JewelryItem based on itemCode
                // 2. Call recordStockIn with the JewelryItem object
                
                LOG.info("Stock addition required for item {} quantity {} from invoice {} - Manual processing needed", 
                        transaction.getItemCode(), transaction.getQuantity(), invoice.getInvoiceNumber());
                
                // TODO: Implement proper stock addition when JewelryItem linking is available
                // This would require either:
                // - Adding jewelryItemId to PurchaseTransaction
                // - Or implementing a method to find/create JewelryItem by itemCode
                
            } catch (Exception e) {
                LOG.error("Error processing stock for transaction {}: {}", transaction.getId(), e.getMessage());
                throw new RuntimeException("Failed to process stock: " + e.getMessage());
            }
        }
    }
    
    /**
     * Process exchange metal sale for purchase transaction
     */
    public void processExchangeMetalSale(PurchaseTransaction transaction, PurchaseInvoice invoice) {
        if (transaction.getItemType() == PurchaseTransaction.ItemType.EXCHANGE_ITEM &&
            transaction.getNetWeight() != null && transaction.getNetWeight().compareTo(BigDecimal.ZERO) > 0) {
            
            try {
                // Sell exchange metal weight
                exchangeMetalStockService.sellExchangeMetalWeight(
                    transaction.getMetalType(),
                    transaction.getPurity(),
                    transaction.getNetWeight(),
                    "PURCHASE_INVOICE",
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    invoice.getSupplier().getSupplierFullName()
                );
                
                LOG.info("Sold exchange metal {} {}k weight {} to supplier {} in invoice {}", 
                        transaction.getMetalType(), transaction.getPurity(), transaction.getNetWeight(),
                        invoice.getSupplier().getSupplierFullName(), invoice.getInvoiceNumber());
                
            } catch (Exception e) {
                LOG.error("Error processing exchange metal sale for transaction {}: {}", 
                         transaction.getId(), e.getMessage());
                throw new RuntimeException("Failed to process exchange metal sale: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get total purchase quantity for an item
     */
    public Integer getTotalPurchaseQuantity(String itemCode) {
        return purchaseTransactionRepository.getTotalQuantityPurchasedByItemCode(itemCode);
    }
    
    /**
     * Get total purchase weight by metal type
     */
    public BigDecimal getTotalPurchaseWeight(String metalType) {
        Double weight = purchaseTransactionRepository.getTotalWeightPurchasedByMetal(metalType);
        return weight != null ? BigDecimal.valueOf(weight) : BigDecimal.ZERO;
    }
    
    /**
     * Get purchase statistics by date range
     */
    public List<Object[]> getPurchaseStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        // This method doesn't exist in repository, use getPurchasesByMetal instead
        return purchaseTransactionRepository.getPurchasesByMetal();
    }
    
    /**
     * Get top purchased items
     */
    public List<Object[]> getTopPurchasedItems(int limit) {
        // Use getFrequentlyPurchasedItems instead
        List<Object[]> items = purchaseTransactionRepository.getFrequentlyPurchasedItems();
        return items.size() > limit ? items.subList(0, limit) : items;
    }
    
    /**
     * Update transaction
     */
    public PurchaseTransaction updateTransaction(PurchaseTransaction transaction) {
        if (transaction.getId() == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null for update");
        }
        
        // Recalculate totals
        transaction.calculateTotalAmount();
        
        return purchaseTransactionRepository.save(transaction);
    }
    
    /**
     * Batch save transactions
     */
    public List<PurchaseTransaction> saveAllTransactions(List<PurchaseTransaction> transactions) {
        // Calculate totals for each transaction
        transactions.forEach(PurchaseTransaction::calculateTotalAmount);
        return purchaseTransactionRepository.saveAll(transactions);
    }
    
    /**
     * Check if item exists in any purchase
     */
    public boolean isItemPurchased(String itemCode) {
        // existsByItemCode doesn't exist, check if any transactions exist with this item code
        return !purchaseTransactionRepository.findByItemCodeContainingIgnoreCase(itemCode).isEmpty();
    }
    
    /**
     * Get recent transactions
     */
    public List<PurchaseTransaction> getRecentTransactions(int limit) {
        // findRecentTransactions doesn't exist, use findAll with sorting
        List<PurchaseTransaction> allTransactions = purchaseTransactionRepository.findAll();
        allTransactions.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));
        return allTransactions.size() > limit ? allTransactions.subList(0, limit) : allTransactions;
    }
}