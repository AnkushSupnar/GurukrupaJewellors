package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.PurchaseInvoice;
import com.gurukrupa.data.entities.PurchaseTransaction;
import com.gurukrupa.data.entities.PurchaseExchangeTransaction;
import com.gurukrupa.data.entities.Supplier;
import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.entities.StockTransaction;
import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.BankTransaction;
import com.gurukrupa.data.entities.ExchangeMetalStock;
import com.gurukrupa.data.repository.PurchaseInvoiceRepository;
import com.gurukrupa.data.repository.PurchaseTransactionRepository;
import com.gurukrupa.data.repository.PurchaseExchangeTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PurchaseInvoiceService {
    
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseInvoiceService.class);
    
    @Autowired
    private PurchaseInvoiceRepository purchaseInvoiceRepository;
    
    @Autowired
    private PurchaseTransactionRepository purchaseTransactionRepository;
    
    @Autowired
    private PurchaseExchangeTransactionRepository purchaseExchangeTransactionRepository;
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private AppSettingsService appSettingsService;
    
    @Autowired
    private JewelryItemService jewelryItemService;
    
    @Autowired
    private StockTransactionService stockTransactionService;
    
    @Autowired
    private ExchangeMetalStockService exchangeMetalStockService;
    
    @Autowired
    private BankAccountService bankAccountService;
    
    @Autowired
    private BankTransactionService bankTransactionService;
    
    public PurchaseInvoice savePurchaseInvoice(PurchaseInvoice invoice) {
        // Generate invoice number if not set
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }
        
        // Set invoice reference in all transactions before saving
        for (PurchaseTransaction transaction : invoice.getPurchaseTransactions()) {
            transaction.setPurchaseInvoice(invoice);
        }
        
        // Calculate totals before saving
        invoice.calculateTotals();
        
        // Check if this is a new invoice (for stock addition)
        boolean isNewInvoice = invoice.getId() == null;
        
        // Save the invoice (this will cascade save the transactions)
        PurchaseInvoice savedInvoice = purchaseInvoiceRepository.save(invoice);
        
        // Add stock for new invoices only (not for updates)
        if (isNewInvoice && savedInvoice.getStatus() != PurchaseInvoice.InvoiceStatus.CANCELLED) {
            processStockAddition(savedInvoice);
        }
        
        return savedInvoice;
    }
    
    public PurchaseInvoice savePurchaseInvoiceWithStockUpdate(PurchaseInvoice invoice) {
        try {
            LOG.info("Starting to save purchase invoice with stock update");
            
            // Generate invoice number if not set
            if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
                invoice.setInvoiceNumber(generateInvoiceNumber());
            }
            LOG.info("Invoice number: {}", invoice.getInvoiceNumber());
            
            // Set invoice reference in all transactions before saving
            for (PurchaseTransaction transaction : invoice.getPurchaseTransactions()) {
                transaction.setPurchaseInvoice(invoice);
            }
            LOG.info("Set invoice reference for {} purchase transactions", invoice.getPurchaseTransactions().size());
            
            // Set invoice reference in all exchange transactions before saving
            if (invoice.getPurchaseExchangeTransactions() != null) {
                for (PurchaseExchangeTransaction exchangeTransaction : invoice.getPurchaseExchangeTransactions()) {
                    exchangeTransaction.setPurchaseInvoice(invoice);
                }
                LOG.info("Set invoice reference for {} exchange transactions", invoice.getPurchaseExchangeTransactions().size());
            }
            
            // Calculate totals before saving
            invoice.calculateTotals();
            LOG.info("Calculated totals - Grand Total: {}", invoice.getGrandTotal());
            
            // Check if this is a new invoice
            boolean isNewInvoice = invoice.getId() == null;
            
            // Save the invoice (this will cascade save the transactions)
            LOG.info("Saving invoice to database");
            PurchaseInvoice savedInvoice = purchaseInvoiceRepository.save(invoice);
            LOG.info("Invoice saved successfully with ID: {}", savedInvoice.getId());
            
            // Process stock updates for new invoices only (not for updates)
            if (isNewInvoice && savedInvoice.getStatus() != PurchaseInvoice.InvoiceStatus.CANCELLED) {
                try {
                    // Process jewelry item stock addition
                    LOG.info("Processing stock addition");
                    processStockAddition(savedInvoice);
                } catch (Exception e) {
                    LOG.error("Error in processStockAddition", e);
                    throw new RuntimeException("Failed to process stock addition: " + e.getMessage(), e);
                }
                
                try {
                    // Process exchange metal stock reduction
                    if (savedInvoice.getPurchaseExchangeTransactions() != null && !savedInvoice.getPurchaseExchangeTransactions().isEmpty()) {
                        LOG.info("Processing exchange metal stock reduction");
                        processExchangeMetalStockReduction(savedInvoice);
                    }
                } catch (Exception e) {
                    LOG.error("Error in processExchangeMetalStockReduction", e);
                    throw new RuntimeException("Failed to process exchange metal stock: " + e.getMessage(), e);
                }
                
                try {
                    // Process bank transaction if payment method is not CASH
                    if (savedInvoice.getPaymentMethod() != PurchaseInvoice.PaymentMethod.CASH && 
                        savedInvoice.getPaidAmount() != null && 
                        savedInvoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                        LOG.info("Processing bank transaction");
                        processBankTransaction(savedInvoice);
                    }
                } catch (Exception e) {
                    LOG.error("Error in processBankTransaction", e);
                    // Bank transaction failure should not rollback the entire transaction
                    // Just log the error
                }
            }
            
            LOG.info("Purchase invoice saved successfully");
            return savedInvoice;
            
        } catch (Exception e) {
            LOG.error("Error saving purchase invoice with stock update", e);
            throw new RuntimeException("Failed to save purchase invoice: " + e.getMessage(), e);
        }
    }
    
    public PurchaseInvoice createPurchaseInvoice(Supplier supplier,
                                               List<PurchaseTransaction> purchaseTransactions,
                                               String supplierInvoiceNumber,
                                               PurchaseInvoice.PurchaseType purchaseType,
                                               BigDecimal discount,
                                               BigDecimal gstRate,
                                               BigDecimal transportCharges,
                                               BigDecimal otherCharges,
                                               PurchaseInvoice.PaymentMethod paymentMethod,
                                               String paymentReference,
                                               String notes) {
        
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier cannot be null");
        }
        
        // Check if supplier invoice number already exists
        if (supplierInvoiceNumber != null && !supplierInvoiceNumber.isEmpty() &&
            purchaseInvoiceRepository.existsBySupplierInvoiceNumberAndSupplierId(supplierInvoiceNumber, supplier.getId())) {
            throw new IllegalArgumentException("Invoice with this supplier invoice number already exists");
        }
        
        // Create new invoice
        PurchaseInvoice invoice = PurchaseInvoice.builder()
                .supplier(supplier)
                .supplierInvoiceNumber(supplierInvoiceNumber)
                .purchaseType(purchaseType != null ? purchaseType : PurchaseInvoice.PurchaseType.NEW_STOCK)
                .discount(discount != null ? discount : BigDecimal.ZERO)
                .gstRate(gstRate != null ? gstRate : new BigDecimal("3.00"))
                .transportCharges(transportCharges != null ? transportCharges : BigDecimal.ZERO)
                .otherCharges(otherCharges != null ? otherCharges : BigDecimal.ZERO)
                .paymentMethod(paymentMethod != null ? paymentMethod : PurchaseInvoice.PaymentMethod.CASH)
                .paymentReference(paymentReference)
                .status(PurchaseInvoice.InvoiceStatus.DRAFT)
                .invoiceDate(LocalDateTime.now())
                .paidAmount(BigDecimal.ZERO)
                .pendingAmount(BigDecimal.ZERO)
                .purchaseTransactions(new ArrayList<>())
                .notes(notes)
                .build();
        
        // Add all purchase transactions with proper invoice reference
        purchaseTransactions.forEach(transaction -> {
            transaction.setPurchaseInvoice(invoice);
            invoice.getPurchaseTransactions().add(transaction);
        });
        
        // Save the invoice
        return savePurchaseInvoice(invoice);
    }
    
    public String generateInvoiceNumber() {
        // Use current date and time for invoice number
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = now.format(formatter);
        
        // Get count of invoices today
        Long todayCount = purchaseInvoiceRepository.countTodaysInvoices();
        String counterPart = String.format("%04d", todayCount + 1);
        
        return "PI-" + datePart + "-" + counterPart;
    }
    
    public Optional<PurchaseInvoice> findById(Long id) {
        return purchaseInvoiceRepository.findById(id);
    }
    
    public Optional<PurchaseInvoice> findByInvoiceNumber(String invoiceNumber) {
        return purchaseInvoiceRepository.findByInvoiceNumber(invoiceNumber);
    }
    
    public List<PurchaseInvoice> findAllInvoices() {
        return purchaseInvoiceRepository.findAllOrderByCreatedDateDesc();
    }
    
    public List<PurchaseInvoice> findInvoicesBySupplier(Long supplierId) {
        return purchaseInvoiceRepository.findBySupplierIdOrderByInvoiceDateDesc(supplierId);
    }
    
    public List<PurchaseInvoice> findInvoicesBySupplierName(String supplierName) {
        return purchaseInvoiceRepository.findBySupplierNameContainingIgnoreCase(supplierName);
    }
    
    public List<PurchaseInvoice> findInvoicesByStatus(PurchaseInvoice.InvoiceStatus status) {
        return purchaseInvoiceRepository.findByStatus(status);
    }
    
    public List<PurchaseInvoice> findInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseInvoiceRepository.findInvoicesByDateRange(startDate, endDate);
    }
    
    public List<PurchaseInvoice> getAllInvoices() {
        return purchaseInvoiceRepository.findAllOrderByInvoiceDateDesc();
    }
    
    public List<PurchaseInvoice> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseInvoiceRepository.findByInvoiceDateBetween(startDate, endDate);
    }
    
    public List<PurchaseInvoice> findTodaysInvoices() {
        return purchaseInvoiceRepository.findInvoicesByDate(LocalDateTime.now());
    }
    
    public Double getTodaysTotalPurchases() {
        return purchaseInvoiceRepository.getTodaysTotalPurchases();
    }
    
    public Double getPurchasesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseInvoiceRepository.getPurchasesByDateRange(startDate, endDate);
    }
    
    public PurchaseInvoice updateInvoiceStatus(Long invoiceId, PurchaseInvoice.InvoiceStatus status) {
        Optional<PurchaseInvoice> invoiceOpt = purchaseInvoiceRepository.findById(invoiceId);
        if (invoiceOpt.isPresent()) {
            PurchaseInvoice invoice = invoiceOpt.get();
            invoice.setStatus(status);
            return purchaseInvoiceRepository.save(invoice);
        }
        return null;
    }
    
    public PurchaseInvoice markInvoiceAsPaid(Long invoiceId) {
        return updateInvoiceStatus(invoiceId, PurchaseInvoice.InvoiceStatus.PAID);
    }
    
    public PurchaseInvoice cancelInvoice(Long invoiceId) {
        return updateInvoiceStatus(invoiceId, PurchaseInvoice.InvoiceStatus.CANCELLED);
    }
    
    public void deleteInvoice(Long invoiceId) {
        purchaseInvoiceRepository.deleteById(invoiceId);
    }
    
    // Analytics methods
    public Long getTodaysInvoiceCount() {
        return purchaseInvoiceRepository.countTodaysInvoices();
    }
    
    public List<PurchaseTransaction> getInvoiceTransactions(Long invoiceId) {
        return purchaseTransactionRepository.findByPurchaseInvoiceId(invoiceId);
    }
    
    public List<PurchaseInvoice> findBySupplierId(Long supplierId) {
        return purchaseInvoiceRepository.findBySupplierIdOrderByInvoiceDateDesc(supplierId);
    }
    
    public List<PurchaseInvoice> findBySupplierIdAndDateRange(Long supplierId, LocalDateTime fromDate, LocalDateTime toDate) {
        return purchaseInvoiceRepository.findBySupplierIdAndInvoiceDateBetween(supplierId, fromDate, toDate);
    }
    
    public BigDecimal getTotalPendingAmountForSupplier(Long supplierId) {
        return purchaseInvoiceRepository.getTotalPendingAmountBySupplierId(supplierId);
    }
    
    public Double getTodaysPaidAmount() {
        return purchaseInvoiceRepository.getTodaysPaidAmount();
    }
    
    public Double getPaidAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseInvoiceRepository.getPaidAmountByDateRange(startDate, endDate);
    }
    
    public List<PurchaseInvoice> findExchangePurchaseInvoices() {
        return purchaseInvoiceRepository.findExchangePurchaseInvoices();
    }
    
    /**
     * Revert the effects of a purchase invoice before editing
     * This includes stock adjustments, bank transactions, and exchange metal stock
     */
    @Transactional
    public void revertPurchaseInvoiceEffects(PurchaseInvoice originalInvoice) {
        String supplierName = originalInvoice.getSupplier() != null ? 
                            originalInvoice.getSupplier().getSupplierFullName() : "Supplier";
        
        LOG.info("Reverting purchase invoice effects for: {}", originalInvoice.getInvoiceNumber());
        
        try {
            // 1. Revert stock additions from original purchase transactions
            for (PurchaseTransaction transaction : originalInvoice.getPurchaseTransactions()) {
                if (transaction.getItemType() == PurchaseTransaction.ItemType.NEW_ITEM) {
                    Integer quantity = transaction.getQuantity() != null ? transaction.getQuantity() : 1;
                    
                    if (quantity > 0) {
                        // Find the jewelry item and reduce stock
                        Optional<JewelryItem> existingItem = jewelryItemService.findByItemCode(transaction.getItemCode());
                        
                        if (existingItem.isPresent()) {
                            // Reduce stock (this is reverse of the original purchase)
                            jewelryItemService.reduceStock(
                                existingItem.get().getId(),
                                quantity,
                                StockTransaction.TransactionSource.ADJUSTMENT,
                                "PURCHASE_EDIT_REVERSAL",
                                originalInvoice.getId(),
                                originalInvoice.getInvoiceNumber(),
                                "Stock reduction due to purchase invoice edit - " + supplierName,
                                "System"
                            );
                            LOG.info("Reverted stock for item {} quantity {} from invoice {}", 
                                    transaction.getItemCode(), quantity, originalInvoice.getInvoiceNumber());
                        }
                    }
                }
            }
            
            // 2. Revert exchange metal stock reductions (add back the metal stock)
            if (originalInvoice.getPurchaseExchangeTransactions() != null && 
                !originalInvoice.getPurchaseExchangeTransactions().isEmpty()) {
                
                for (PurchaseExchangeTransaction exchangeTransaction : originalInvoice.getPurchaseExchangeTransactions()) {
                    BigDecimal netWeight = exchangeTransaction.getNetWeight();
                    
                    if (netWeight != null && netWeight.compareTo(BigDecimal.ZERO) > 0) {
                        // Add back the exchange metal stock (reverse of original reduction)
                        BigDecimal stockPurity = new BigDecimal("0"); // Use purity 0 to match billing system
                        exchangeMetalStockService.addExchangeMetalWeight(
                            exchangeTransaction.getMetalType(),
                            stockPurity,
                            netWeight,
                            "PURCHASE_EDIT_REVERSAL",
                            originalInvoice.getId(),
                            originalInvoice.getInvoiceNumber(),
                            "Exchange metal stock reversal due to purchase invoice edit - " + supplierName
                        );
                        
                        LOG.info("Reverted exchange metal stock: {} {} weight {} for invoice {}", 
                                exchangeTransaction.getMetalType(), exchangeTransaction.getPurity(), 
                                netWeight, originalInvoice.getInvoiceNumber());
                    }
                }
            }
            
            // 3. Revert bank transaction (add back the paid amount to bank balance)
            if (originalInvoice.getPaidAmount() != null && 
                originalInvoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 &&
                originalInvoice.getPaymentMethod() != PurchaseInvoice.PaymentMethod.CASH &&
                originalInvoice.getPaymentMethod() != PurchaseInvoice.PaymentMethod.CREDIT) {
                
                // Get the first active bank account (in a real implementation, you'd store which account was used)
                List<BankAccount> activeBankAccounts = bankAccountService.getAllActiveBankAccounts();
                if (!activeBankAccounts.isEmpty()) {
                    BankAccount bankAccount = activeBankAccounts.get(0);
                    
                    // Record credit transaction (reverse of original debit)
                    bankTransactionService.recordCredit(
                        bankAccount,
                        originalInvoice.getPaidAmount(),
                        BankTransaction.TransactionSource.MANUAL_ENTRY,
                        "PURCHASE_EDIT_REVERSAL",
                        originalInvoice.getId(),
                        originalInvoice.getInvoiceNumber(),
                        originalInvoice.getPaymentReference(),
                        supplierName,
                        String.format("Purchase payment reversal due to invoice edit - %s to %s", 
                            originalInvoice.getInvoiceNumber(), supplierName)
                    );
                    
                    LOG.info("Reverted bank transaction for purchase invoice {} - Amount: {}", 
                            originalInvoice.getInvoiceNumber(), originalInvoice.getPaidAmount());
                }
            }
            
            LOG.info("Successfully reverted all effects for purchase invoice: {}", originalInvoice.getInvoiceNumber());
            
        } catch (Exception e) {
            LOG.error("Error reverting purchase invoice effects for: {}", originalInvoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to revert purchase invoice effects: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update an existing purchase invoice with new transaction data
     * This method properly handles stock and financial adjustments
     */
    @Transactional
    public PurchaseInvoice updatePurchaseInvoice(Long invoiceId, 
                                               List<PurchaseTransaction> newPurchaseTransactions,
                                               List<PurchaseExchangeTransaction> newExchangeTransactions,
                                               PurchaseInvoice updatedInvoiceData) {
        try {
            // Load fresh entity from database
            Optional<PurchaseInvoice> existingInvoiceOpt = findById(invoiceId);
            if (existingInvoiceOpt.isEmpty()) {
                throw new RuntimeException("Purchase invoice not found with ID: " + invoiceId);
            }
            
            PurchaseInvoice existingInvoice = existingInvoiceOpt.get();
            
            LOG.info("Starting purchase invoice update for: {}", existingInvoice.getInvoiceNumber());
            
            // Step 1: Revert the effects of the original invoice
            revertPurchaseInvoiceEffects(existingInvoice);
            
            // Step 2: Update invoice fields
            existingInvoice.setSupplier(updatedInvoiceData.getSupplier());
            existingInvoice.setSupplierInvoiceNumber(updatedInvoiceData.getSupplierInvoiceNumber());
            existingInvoice.setPurchaseType(updatedInvoiceData.getPurchaseType());
            existingInvoice.setPaymentMethod(updatedInvoiceData.getPaymentMethod());
            existingInvoice.setPaymentReference(updatedInvoiceData.getPaymentReference());
            existingInvoice.setStatus(updatedInvoiceData.getStatus());
            existingInvoice.setDiscount(updatedInvoiceData.getDiscount());
            existingInvoice.setGstRate(updatedInvoiceData.getGstRate());
            existingInvoice.setTransportCharges(updatedInvoiceData.getTransportCharges());
            existingInvoice.setOtherCharges(updatedInvoiceData.getOtherCharges());
            existingInvoice.setPaidAmount(updatedInvoiceData.getPaidAmount());
            existingInvoice.setPendingAmount(updatedInvoiceData.getPendingAmount());
            existingInvoice.setNotes(updatedInvoiceData.getNotes());
            
            // Step 3: Delete old purchase transactions from database
            List<PurchaseTransaction> oldPurchaseTransactions = existingInvoice.getPurchaseTransactions();
            if (!oldPurchaseTransactions.isEmpty()) {
                LOG.info("Deleting {} old purchase transactions for invoice {}", 
                        oldPurchaseTransactions.size(), existingInvoice.getInvoiceNumber());
                purchaseTransactionRepository.deleteAll(oldPurchaseTransactions);
            }
            existingInvoice.getPurchaseTransactions().clear();
            
            // Add new purchase transactions
            for (PurchaseTransaction transaction : newPurchaseTransactions) {
                transaction.setId(null); // Ensure new entity
                transaction.setPurchaseInvoice(existingInvoice);
                existingInvoice.getPurchaseTransactions().add(transaction);
            }
            
            // Step 4: Delete old exchange transactions from database
            List<PurchaseExchangeTransaction> oldExchangeTransactions = existingInvoice.getPurchaseExchangeTransactions();
            if (!oldExchangeTransactions.isEmpty()) {
                LOG.info("Deleting {} old exchange transactions for invoice {}", 
                        oldExchangeTransactions.size(), existingInvoice.getInvoiceNumber());
                purchaseExchangeTransactionRepository.deleteAll(oldExchangeTransactions);
            }
            existingInvoice.getPurchaseExchangeTransactions().clear();
            
            // Add new exchange transactions
            for (PurchaseExchangeTransaction exchangeTransaction : newExchangeTransactions) {
                exchangeTransaction.setId(null); // Ensure new entity
                exchangeTransaction.setPurchaseInvoice(existingInvoice);
                existingInvoice.getPurchaseExchangeTransactions().add(exchangeTransaction);
            }
            
            // Step 5: Calculate totals
            existingInvoice.calculateTotals();
            
            // Step 6: Save the updated invoice first
            PurchaseInvoice savedInvoice = purchaseInvoiceRepository.save(existingInvoice);
            LOG.info("Updated invoice saved with ID: {}", savedInvoice.getId());
            
            // Step 7: Apply the effects of the new invoice (stock addition, bank deduction, etc.)
            // But only if the invoice status indicates it should affect stock/bank
            if (savedInvoice.getStatus() != PurchaseInvoice.InvoiceStatus.CANCELLED) {
                applyUpdatedInvoiceEffects(savedInvoice);
            }
            
            LOG.info("Purchase invoice update completed successfully for: {}", savedInvoice.getInvoiceNumber());
            return savedInvoice;
            
        } catch (Exception e) {
            LOG.error("Error updating purchase invoice with ID: {}", invoiceId, e);
            throw new RuntimeException("Failed to update purchase invoice: " + e.getMessage(), e);
        }
    }
    
    /**
     * Apply the effects of an updated purchase invoice
     * This includes stock additions, bank deductions, and exchange metal stock reductions
     */
    @Transactional
    public void applyUpdatedInvoiceEffects(PurchaseInvoice updatedInvoice) {
        String supplierName = updatedInvoice.getSupplier() != null ? 
                            updatedInvoice.getSupplier().getSupplierFullName() : "Supplier";
        
        LOG.info("Applying updated purchase invoice effects for: {}", updatedInvoice.getInvoiceNumber());
        
        try {
            // 1. Process jewelry item stock addition (same as original but with "EDITED" context)
            for (PurchaseTransaction transaction : updatedInvoice.getPurchaseTransactions()) {
                if (transaction.getItemType() == PurchaseTransaction.ItemType.NEW_ITEM) {
                    Integer quantity = transaction.getQuantity() != null ? transaction.getQuantity() : 1;
                    
                    if (quantity > 0) {
                        // Find or create jewelry item
                        Optional<JewelryItem> existingItem = jewelryItemService.findByItemCode(transaction.getItemCode());
                        
                        if (existingItem.isPresent()) {
                            // Update existing item stock
                            jewelryItemService.addStock(
                                existingItem.get().getId(),
                                quantity,
                                StockTransaction.TransactionSource.PURCHASE,
                                "PURCHASE_INVOICE_EDITED",
                                updatedInvoice.getId(),
                                updatedInvoice.getInvoiceNumber(),
                                "Purchase from " + supplierName + " (Invoice Edited)",
                                "System"
                            );
                            LOG.info("Added stock for existing item {} quantity {} from edited invoice {}", 
                                    transaction.getItemCode(), quantity, updatedInvoice.getInvoiceNumber());
                        } else {
                            // Create new jewelry item (same logic as original purchase)
                            BigDecimal grossWeight = transaction.getGrossWeight() != null ? transaction.getGrossWeight() : BigDecimal.ZERO;
                            BigDecimal netWeight = transaction.getNetWeight() != null ? transaction.getNetWeight() : BigDecimal.ZERO;
                            BigDecimal stoneWeight = grossWeight.subtract(netWeight);
                            if (stoneWeight.compareTo(BigDecimal.ZERO) < 0) {
                                stoneWeight = BigDecimal.ZERO;
                            }
                            
                            BigDecimal ratePerGram = transaction.getRatePerGram() != null ? transaction.getRatePerGram() : BigDecimal.ZERO;
                            BigDecimal labourCharges = transaction.getMakingCharges() != null ? transaction.getMakingCharges() : BigDecimal.ZERO;
                            
                            JewelryItem newItem = JewelryItem.builder()
                                .itemCode(transaction.getItemCode())
                                .itemName(transaction.getItemName())
                                .category("General")
                                .metalType(transaction.getMetalType())
                                .purity(transaction.getPurity() != null ? transaction.getPurity() : new BigDecimal("916"))
                                .grossWeight(grossWeight)
                                .netWeight(netWeight)
                                .stoneWeight(stoneWeight)
                                .quantity(quantity)
                                .goldRate(ratePerGram.multiply(BigDecimal.TEN))
                                .labourCharges(labourCharges)
                                .totalAmount(transaction.getTotalAmount() != null ? transaction.getTotalAmount() : BigDecimal.ZERO)
                                .isActive(true)
                                .createdDate(LocalDateTime.now())
                                .build();
                            
                            JewelryItem savedItem = jewelryItemService.saveJewelryItem(newItem);
                            
                            // Record stock transaction
                            stockTransactionService.recordStockIn(
                                savedItem,
                                quantity,
                                StockTransaction.TransactionSource.PURCHASE,
                                "PURCHASE_INVOICE_EDITED",
                                updatedInvoice.getId(),
                                updatedInvoice.getInvoiceNumber(),
                                "Initial purchase from " + supplierName + " (Invoice Edited)",
                                "System"
                            );
                            
                            LOG.info("Created new item {} with quantity {} from edited invoice {}", 
                                    transaction.getItemCode(), quantity, updatedInvoice.getInvoiceNumber());
                        }
                    }
                }
            }
            
            // 2. Process exchange metal stock reduction
            if (updatedInvoice.getPurchaseExchangeTransactions() != null && 
                !updatedInvoice.getPurchaseExchangeTransactions().isEmpty()) {
                
                for (PurchaseExchangeTransaction exchangeTransaction : updatedInvoice.getPurchaseExchangeTransactions()) {
                    BigDecimal netWeight = exchangeTransaction.getNetWeight();
                    
                    if (netWeight != null && netWeight.compareTo(BigDecimal.ZERO) > 0) {
                        // Reduce exchange metal stock
                        BigDecimal stockPurity = new BigDecimal("0");
                        exchangeMetalStockService.sellExchangeMetalWeight(
                            exchangeTransaction.getMetalType(),
                            stockPurity,
                            netWeight,
                            "PURCHASE_INVOICE_EDITED",
                            updatedInvoice.getId(),
                            updatedInvoice.getInvoiceNumber(),
                            supplierName + " (Invoice Edited)"
                        );
                        
                        LOG.info("Reduced exchange metal stock: {} {} weight {} for edited invoice {}", 
                                exchangeTransaction.getMetalType(), exchangeTransaction.getPurity(), 
                                netWeight, updatedInvoice.getInvoiceNumber());
                    }
                }
            }
            
            // 3. Process bank transaction for the updated payment
            if (updatedInvoice.getPaidAmount() != null && 
                updatedInvoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 &&
                updatedInvoice.getPaymentMethod() != PurchaseInvoice.PaymentMethod.CASH &&
                updatedInvoice.getPaymentMethod() != PurchaseInvoice.PaymentMethod.CREDIT) {
                
                // Get the first active bank account
                List<BankAccount> activeBankAccounts = bankAccountService.getAllActiveBankAccounts();
                if (!activeBankAccounts.isEmpty()) {
                    BankAccount bankAccount = activeBankAccounts.get(0);
                    
                    // Record debit transaction for the updated payment
                    bankTransactionService.recordDebit(
                        bankAccount,
                        updatedInvoice.getPaidAmount(),
                        BankTransaction.TransactionSource.PURCHASE_PAYMENT,
                        "PURCHASE_INVOICE_EDITED",
                        updatedInvoice.getId(),
                        updatedInvoice.getInvoiceNumber(),
                        updatedInvoice.getPaymentReference(),
                        supplierName,
                        String.format("Updated payment for purchase invoice %s to %s (Invoice Edited)", 
                            updatedInvoice.getInvoiceNumber(), supplierName)
                    );
                    
                    LOG.info("Recorded updated bank transaction for purchase invoice {} - Amount: {}", 
                            updatedInvoice.getInvoiceNumber(), updatedInvoice.getPaidAmount());
                }
            }
            
            LOG.info("Successfully applied all effects for updated purchase invoice: {}", updatedInvoice.getInvoiceNumber());
            
        } catch (Exception e) {
            LOG.error("Error applying updated purchase invoice effects for: {}", updatedInvoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to apply updated purchase invoice effects: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process stock addition for purchase invoice items
     */
    private void processStockAddition(PurchaseInvoice invoice) {
        String supplierName = invoice.getSupplier() != null ? 
                            invoice.getSupplier().getSupplierFullName() : "Supplier";
        
        LOG.info("Processing stock addition for purchase invoice {}", invoice.getInvoiceNumber());
        
        // Process each transaction to add stock
        for (PurchaseTransaction transaction : invoice.getPurchaseTransactions()) {
            try {
                // Only process NEW_ITEM type for stock addition
                if (transaction.getItemType() == PurchaseTransaction.ItemType.NEW_ITEM) {
                    // Get quantity from transaction (default to 1 if not set)
                    Integer quantity = transaction.getQuantity() != null ? transaction.getQuantity() : 1;
                    
                    // Skip if quantity is 0 or negative
                    if (quantity <= 0) {
                        LOG.warn("Skipping stock addition for item {} with quantity {} in invoice {}", 
                                transaction.getItemCode(), quantity, invoice.getInvoiceNumber());
                        continue;
                    }
                    
                    // Find or create jewelry item
                    Optional<JewelryItem> existingItem = jewelryItemService.findByItemCode(transaction.getItemCode());
                    
                    if (existingItem.isPresent()) {
                        // Update existing item stock
                        jewelryItemService.addStock(
                            existingItem.get().getId(),
                            quantity,
                            StockTransaction.TransactionSource.PURCHASE,
                            "PURCHASE_INVOICE",
                            invoice.getId(),
                            invoice.getInvoiceNumber(),
                            "Purchase from " + supplierName,
                            "System"
                        );
                        LOG.info("Added stock for existing item {} quantity {} from invoice {}", 
                                transaction.getItemCode(), quantity, invoice.getInvoiceNumber());
                    } else {
                        // Create new jewelry item
                        BigDecimal grossWeight = transaction.getGrossWeight() != null ? transaction.getGrossWeight() : BigDecimal.ZERO;
                        BigDecimal netWeight = transaction.getNetWeight() != null ? transaction.getNetWeight() : BigDecimal.ZERO;
                        BigDecimal stoneWeight = grossWeight.subtract(netWeight);
                        if (stoneWeight.compareTo(BigDecimal.ZERO) < 0) {
                            stoneWeight = BigDecimal.ZERO;
                        }
                        
                        BigDecimal ratePerGram = transaction.getRatePerGram() != null ? transaction.getRatePerGram() : BigDecimal.ZERO;
                        BigDecimal labourCharges = transaction.getMakingCharges() != null ? transaction.getMakingCharges() : BigDecimal.ZERO;
                        
                        JewelryItem newItem = JewelryItem.builder()
                            .itemCode(transaction.getItemCode())
                            .itemName(transaction.getItemName())
                            .category("General") // You might want to determine this from the item name
                            .metalType(transaction.getMetalType())
                            .purity(transaction.getPurity() != null ? transaction.getPurity() : new BigDecimal("916"))
                            .grossWeight(grossWeight)
                            .netWeight(netWeight)
                            .stoneWeight(stoneWeight)
                            .quantity(quantity)
                            .goldRate(ratePerGram.multiply(BigDecimal.TEN))
                            .labourCharges(labourCharges)
                            .totalAmount(transaction.getTotalAmount() != null ? transaction.getTotalAmount() : BigDecimal.ZERO)
                            .isActive(true)
                            .createdDate(LocalDateTime.now())
                            .build();
                        
                        JewelryItem savedItem = jewelryItemService.saveJewelryItem(newItem);
                        
                        // Record stock transaction
                        stockTransactionService.recordStockIn(
                            savedItem,
                            quantity,
                            StockTransaction.TransactionSource.PURCHASE,
                            "PURCHASE_INVOICE",
                            invoice.getId(),
                            invoice.getInvoiceNumber(),
                            "Initial purchase from " + supplierName,
                            "System"
                        );
                        
                        LOG.info("Created new item {} with quantity {} from invoice {}", 
                                transaction.getItemCode(), quantity, invoice.getInvoiceNumber());
                    }
                }
                
            } catch (Exception e) {
                // Log the error but continue with other items
                LOG.error("Error adding stock for item {} in invoice {}: {}", 
                         transaction.getItemCode(), invoice.getInvoiceNumber(), e.getMessage(), e);
            }
        }
        
        LOG.info("Completed stock addition processing for invoice {}", invoice.getInvoiceNumber());
    }
    
    /**
     * Process exchange metal stock reduction for purchase invoice exchange items
     */
    private void processExchangeMetalStockReduction(PurchaseInvoice invoice) {
        String supplierName = invoice.getSupplier() != null ? 
                            invoice.getSupplier().getSupplierFullName() : "Supplier";
        
        LOG.info("Processing exchange metal stock reduction for purchase invoice {}", invoice.getInvoiceNumber());
        
        // Process each exchange transaction to reduce metal stock
        for (PurchaseExchangeTransaction exchangeTransaction : invoice.getPurchaseExchangeTransactions()) {
            try {
                BigDecimal netWeight = exchangeTransaction.getNetWeight();
                
                // Skip if net weight is 0 or negative
                if (netWeight == null || netWeight.compareTo(BigDecimal.ZERO) <= 0) {
                    LOG.warn("Skipping exchange metal reduction for {} with weight {} in invoice {}", 
                            exchangeTransaction.getItemName(), netWeight, invoice.getInvoiceNumber());
                    continue;
                }
                
                // Reduce exchange metal stock
                // Use purity 0 to match how billing system stores exchange metal
                BigDecimal stockPurity = new BigDecimal("0");
                exchangeMetalStockService.sellExchangeMetalWeight(
                    exchangeTransaction.getMetalType(),
                    stockPurity,
                    netWeight,
                    "PURCHASE_INVOICE",
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    supplierName
                );
                
                LOG.info("Reduced exchange metal stock: {} {} weight {} for invoice {}", 
                        exchangeTransaction.getMetalType(), exchangeTransaction.getPurity(), 
                        netWeight, invoice.getInvoiceNumber());
                
            } catch (Exception e) {
                // Log the error but continue with other items
                LOG.error("Error reducing exchange metal stock for {} in invoice {}: {}", 
                         exchangeTransaction.getItemName(), invoice.getInvoiceNumber(), e.getMessage(), e);
            }
        }
        
        LOG.info("Completed exchange metal stock reduction for invoice {}", invoice.getInvoiceNumber());
    }
    
    /**
     * Process bank transaction for purchase invoice payment
     */
    private void processBankTransaction(PurchaseInvoice invoice) {
        String supplierName = invoice.getSupplier() != null ? 
                            invoice.getSupplier().getSupplierFullName() : "Supplier";
        
        LOG.info("Processing bank transaction for purchase invoice {}", invoice.getInvoiceNumber());
        
        try {
            // Get the first active bank account (you might want to make this configurable)
            List<BankAccount> activeBankAccounts = bankAccountService.getAllActiveBankAccounts();
            if (activeBankAccounts.isEmpty()) {
                LOG.warn("No active bank accounts found for recording purchase payment transaction");
                return;
            }
            
            // Use the first active bank account
            // In a real implementation, you might want to:
            // 1. Allow user to select which bank account to use
            // 2. Have a default bank account in app settings
            // 3. Store bank account ID in the invoice
            BankAccount bankAccount = activeBankAccounts.get(0);
            
            // Check if bank account has sufficient balance
            if (bankAccount.getCurrentBalance().compareTo(invoice.getPaidAmount()) < 0) {
                LOG.warn("Insufficient bank balance for purchase payment. Available: {}, Required: {}", 
                        bankAccount.getCurrentBalance(), invoice.getPaidAmount());
                // You might want to throw an exception here in production
                // For now, we'll continue to record the transaction (allowing negative balance)
            }
            
            // Record the bank transaction
            bankTransactionService.recordPurchasePayment(
                bankAccount,
                invoice.getPaidAmount(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getPaymentReference(),
                supplierName
            );
            
            LOG.info("Recorded bank transaction for purchase invoice {} - Amount: {}", 
                    invoice.getInvoiceNumber(), invoice.getPaidAmount());
            
        } catch (Exception e) {
            LOG.error("Error recording bank transaction for purchase invoice {}: {}", 
                     invoice.getInvoiceNumber(), e.getMessage(), e);
            // Depending on business requirements, you might want to throw the exception
            // to rollback the entire transaction, or just log it and continue
        }
    }
}