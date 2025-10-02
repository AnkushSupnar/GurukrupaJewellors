package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.PurchaseInvoice;
import com.gurukrupa.data.entities.PurchaseTransaction;
import com.gurukrupa.data.entities.PurchaseExchangeTransaction;
import com.gurukrupa.data.entities.Supplier;
import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.entities.StockTransaction;
import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.ExchangeMetalStock;
import com.gurukrupa.data.repository.PurchaseInvoiceRepository;
import com.gurukrupa.data.repository.PurchaseTransactionRepository;
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