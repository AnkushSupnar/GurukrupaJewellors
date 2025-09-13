package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.ExchangeMetalStock;
import com.gurukrupa.data.entities.ExchangeMetalTransaction;
import com.gurukrupa.data.entities.ExchangeMetalTransaction.TransactionType;
import com.gurukrupa.data.entities.ExchangeMetalTransaction.TransactionSource;
import com.gurukrupa.data.repository.ExchangeMetalStockRepository;
import com.gurukrupa.data.repository.ExchangeMetalTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExchangeMetalStockService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeMetalStockService.class);
    
    @Autowired
    private ExchangeMetalStockRepository metalStockRepository;
    
    @Autowired
    private ExchangeMetalTransactionRepository metalTransactionRepository;
    
    /**
     * Add exchange metal weight from customer exchange
     */
    public ExchangeMetalStock addExchangeMetalWeight(String metalType, BigDecimal purity, BigDecimal weight,
                                                    String referenceType, Long referenceId, 
                                                    String referenceNumber, String customerName) {
        
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be greater than zero");
        }
        
        // Find or create metal stock entry
        Optional<ExchangeMetalStock> stockOpt = metalStockRepository.findByMetalTypeAndPurity(metalType, purity);
        ExchangeMetalStock metalStock;
        
        if (stockOpt.isPresent()) {
            metalStock = stockOpt.get();
        } else {
            // Create new stock entry
            metalStock = ExchangeMetalStock.builder()
                    .metalType(metalType)
                    .purity(purity)
                    .totalWeight(BigDecimal.ZERO)
                    .availableWeight(BigDecimal.ZERO)
                    .soldWeight(BigDecimal.ZERO)
                    .build();
        }
        
        // Record weight before transaction
        BigDecimal weightBefore = metalStock.getTotalWeight();
        
        // Add weight to stock
        metalStock.addWeight(weight);
        
        // Save stock
        metalStock = metalStockRepository.save(metalStock);
        
        // Create transaction record
        ExchangeMetalTransaction transaction = ExchangeMetalTransaction.builder()
                .metalStock(metalStock)
                .transactionType(TransactionType.IN)
                .transactionSource(TransactionSource.CUSTOMER_EXCHANGE)
                .weight(weight)
                .weightBefore(weightBefore)
                .weightAfter(metalStock.getTotalWeight())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .party(customerName)
                .description(String.format("Exchange metal from %s", customerName != null ? customerName : "Customer"))
                .transactionDate(LocalDateTime.now())
                .createdBy("System")
                .build();
        
        metalTransactionRepository.save(transaction);
        
        LOG.info("Added exchange metal: Type={}, Purity={}, Weight={}, New Total={}", 
                metalType, purity, weight, metalStock.getTotalWeight());
        
        return metalStock;
    }
    
    /**
     * Sell exchange metal weight to supplier
     */
    public ExchangeMetalStock sellExchangeMetalWeight(String metalType, BigDecimal purity, BigDecimal weight,
                                                     String referenceType, Long referenceId,
                                                     String referenceNumber, String supplierName) {
        
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be greater than zero");
        }
        
        // Find metal stock entry
        Optional<ExchangeMetalStock> stockOpt = metalStockRepository.findByMetalTypeAndPurity(metalType, purity);
        if (stockOpt.isEmpty()) {
            throw new IllegalArgumentException(String.format("No stock found for %s %s", metalType, purity));
        }
        
        ExchangeMetalStock metalStock = stockOpt.get();
        
        // Check available weight
        if (metalStock.getAvailableWeight().compareTo(weight) < 0) {
            throw new IllegalArgumentException(String.format(
                "Insufficient stock for %s %s. Available: %s, Requested: %s",
                metalType, purity, metalStock.getAvailableWeight(), weight));
        }
        
        // Record weight before transaction
        BigDecimal weightBefore = metalStock.getTotalWeight();
        
        // Sell weight from stock
        metalStock.sellWeight(weight);
        
        // Save stock
        metalStock = metalStockRepository.save(metalStock);
        
        // Create transaction record
        ExchangeMetalTransaction transaction = ExchangeMetalTransaction.builder()
                .metalStock(metalStock)
                .transactionType(TransactionType.OUT)
                .transactionSource(TransactionSource.SUPPLIER_SALE)
                .weight(weight)
                .weightBefore(weightBefore)
                .weightAfter(metalStock.getTotalWeight())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .party(supplierName)
                .description(String.format("Sold to %s", supplierName != null ? supplierName : "Supplier"))
                .transactionDate(LocalDateTime.now())
                .createdBy("System")
                .build();
        
        metalTransactionRepository.save(transaction);
        
        LOG.info("Sold exchange metal: Type={}, Purity={}, Weight={}, Available={}", 
                metalType, purity, weight, metalStock.getAvailableWeight());
        
        return metalStock;
    }
    
    /**
     * Adjust metal stock (for corrections)
     */
    public ExchangeMetalStock adjustMetalStock(String metalType, BigDecimal purity, 
                                             BigDecimal newTotalWeight, String reason, String createdBy) {
        
        // Find or create metal stock entry
        Optional<ExchangeMetalStock> stockOpt = metalStockRepository.findByMetalTypeAndPurity(metalType, purity);
        ExchangeMetalStock metalStock;
        
        if (stockOpt.isPresent()) {
            metalStock = stockOpt.get();
        } else {
            metalStock = ExchangeMetalStock.builder()
                    .metalType(metalType)
                    .purity(purity)
                    .totalWeight(BigDecimal.ZERO)
                    .availableWeight(BigDecimal.ZERO)
                    .soldWeight(BigDecimal.ZERO)
                    .build();
        }
        
        // Calculate adjustment
        BigDecimal currentWeight = metalStock.getTotalWeight();
        BigDecimal adjustmentWeight = newTotalWeight.subtract(currentWeight);
        TransactionType transactionType = adjustmentWeight.compareTo(BigDecimal.ZERO) >= 0 ? 
                                         TransactionType.IN : TransactionType.OUT;
        
        // Update stock
        metalStock.setTotalWeight(newTotalWeight);
        metalStock.setAvailableWeight(newTotalWeight.subtract(metalStock.getSoldWeight()));
        
        // Save stock
        metalStock = metalStockRepository.save(metalStock);
        
        // Create transaction record
        ExchangeMetalTransaction transaction = ExchangeMetalTransaction.builder()
                .metalStock(metalStock)
                .transactionType(transactionType)
                .transactionSource(TransactionSource.ADJUSTMENT)
                .weight(adjustmentWeight.abs())
                .weightBefore(currentWeight)
                .weightAfter(newTotalWeight)
                .referenceType("ADJUSTMENT")
                .description("Stock adjustment: " + reason)
                .transactionDate(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
        
        metalTransactionRepository.save(transaction);
        
        LOG.info("Adjusted metal stock: Type={}, Purity={}, From={}, To={}, Reason={}", 
                metalType, purity, currentWeight, newTotalWeight, reason);
        
        return metalStock;
    }
    
    /**
     * Get metal stock by type and purity
     */
    public Optional<ExchangeMetalStock> getMetalStock(String metalType, BigDecimal purity) {
        return metalStockRepository.findByMetalTypeAndPurity(metalType, purity);
    }
    
    /**
     * Get all metal stocks
     */
    public List<ExchangeMetalStock> getAllMetalStocks() {
        return metalStockRepository.findAllOrderByMetalTypeAndPurity();
    }
    
    /**
     * Get available metal stocks
     */
    public List<ExchangeMetalStock> getAvailableMetalStocks() {
        return metalStockRepository.findAllWithAvailableStock();
    }
    
    /**
     * Get metal stocks by metal type
     */
    public List<ExchangeMetalStock> getMetalStocksByType(String metalType) {
        return metalStockRepository.findByMetalType(metalType);
    }
    
    /**
     * Get transaction history for a metal stock
     */
    public List<ExchangeMetalTransaction> getMetalStockTransactions(Long metalStockId) {
        return metalTransactionRepository.findByMetalStockIdOrderByTransactionDateDesc(metalStockId);
    }
    
    /**
     * Get transactions by reference
     */
    public List<ExchangeMetalTransaction> getTransactionsByReference(String referenceType, Long referenceId) {
        return metalTransactionRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }
    
    /**
     * Check if exchange has been processed
     */
    public boolean isExchangeProcessed(Long exchangeId) {
        return metalTransactionRepository.existsByExchangeId(exchangeId);
    }
    
    /**
     * Check if purchase invoice has been processed
     */
    public boolean isPurchaseInvoiceProcessed(Long invoiceId) {
        return metalTransactionRepository.existsByPurchaseInvoiceId(invoiceId);
    }
    
    /**
     * Get total weight by metal type
     */
    public BigDecimal getTotalWeightByMetalType(String metalType) {
        return metalStockRepository.getTotalWeightByMetalType(metalType);
    }
    
    /**
     * Get available weight by metal type
     */
    public BigDecimal getAvailableWeightByMetalType(String metalType) {
        return metalStockRepository.getAvailableWeightByMetalType(metalType);
    }
    
    /**
     * Check if sufficient stock is available
     */
    public boolean hasAvailableStock(String metalType, BigDecimal purity, BigDecimal requiredWeight) {
        return metalStockRepository.hasAvailableStock(metalType, purity, requiredWeight);
    }
    
    /**
     * Get stock summary by metal
     */
    public List<Object[]> getStockSummaryByMetal() {
        return metalStockRepository.getStockSummaryByMetal();
    }
}