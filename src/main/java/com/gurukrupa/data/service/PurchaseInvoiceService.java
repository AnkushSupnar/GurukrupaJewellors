package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.PurchaseInvoice;
import com.gurukrupa.data.entities.PurchaseTransaction;
import com.gurukrupa.data.entities.Supplier;
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
                    // TODO: Implement logic to find existing item or create new one
                    // For now, we'll just log the action
                    LOG.info("Need to add stock for item {} quantity {} from invoice {}", 
                            transaction.getItemCode(), quantity, invoice.getInvoiceNumber());
                }
                
            } catch (Exception e) {
                // Log the error but continue with other items
                LOG.error("Error adding stock for item {} in invoice {}: {}", 
                         transaction.getItemCode(), invoice.getInvoiceNumber(), e.getMessage());
            }
        }
        
        LOG.info("Completed stock addition processing for invoice {}", invoice.getInvoiceNumber());
    }
}