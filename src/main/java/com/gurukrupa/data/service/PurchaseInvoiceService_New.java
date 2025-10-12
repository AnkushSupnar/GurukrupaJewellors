package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * New Purchase Invoice Service for metal-based purchase system
 * This service handles purchases of metal (by weight) from suppliers,
 * not specific jewelry items.
 */
@Service
@Transactional
public class PurchaseInvoiceService_New {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseInvoiceService_New.class);

    @Autowired
    private PurchaseInvoiceRepository purchaseInvoiceRepository;

    @Autowired
    private PurchaseMetalTransactionRepository purchaseMetalTransactionRepository;

    @Autowired
    private PurchaseExchangeTransactionRepository purchaseExchangeTransactionRepository;

    @Autowired
    private PurchaseMetalStockService purchaseMetalStockService;

    @Autowired
    private ExchangeMetalStockService exchangeMetalStockService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private BankTransactionService bankTransactionService;

    /**
     * Save a new purchase invoice with metal transactions
     */
    public PurchaseInvoice savePurchaseInvoice(PurchaseInvoice invoice) {
        // Generate invoice number if not set
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }

        // Set invoice reference in all metal transactions
        if (invoice.getPurchaseMetalTransactions() != null) {
            for (PurchaseMetalTransaction transaction : invoice.getPurchaseMetalTransactions()) {
                transaction.setPurchaseInvoice(invoice);
            }
        }

        // Set invoice reference in all exchange transactions
        if (invoice.getPurchaseExchangeTransactions() != null) {
            for (PurchaseExchangeTransaction transaction : invoice.getPurchaseExchangeTransactions()) {
                transaction.setPurchaseInvoice(invoice);
            }
        }

        // Calculate totals before saving
        invoice.calculateTotals();

        // Check if this is a new invoice
        boolean isNewInvoice = invoice.getId() == null;

        // Save the invoice
        PurchaseInvoice savedInvoice = purchaseInvoiceRepository.save(invoice);
        LOG.info("Saved purchase invoice: {}", savedInvoice.getInvoiceNumber());

        // Process metal stock and transactions for new invoices only
        if (isNewInvoice && savedInvoice.getStatus() != PurchaseInvoice.InvoiceStatus.CANCELLED) {
            processNewPurchaseInvoice(savedInvoice);
        }

        return savedInvoice;
    }

    /**
     * Process a new purchase invoice (add to stock, deduct bank, etc.)
     */
    private void processNewPurchaseInvoice(PurchaseInvoice invoice) {
        String supplierName = invoice.getSupplier() != null ?
                invoice.getSupplier().getSupplierFullName() : "Supplier";

        LOG.info("Processing new purchase invoice: {}", invoice.getInvoiceNumber());

        try {
            // 1. Add purchased metal to purchase metal stock
            processPurchaseMetalStockAddition(invoice);

            // 2. Reduce exchange metal stock (metal given to supplier)
            processExchangeMetalStockReduction(invoice);

            // 3. Process bank transaction for payment
            if (invoice.getPaidAmount() != null &&
                invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 &&
                invoice.getPaymentMethod() != PurchaseInvoice.PaymentMethod.CASH &&
                invoice.getPaymentMethod() != PurchaseInvoice.PaymentMethod.CREDIT) {
                processBankTransaction(invoice);
            }

            LOG.info("Successfully processed purchase invoice: {}", invoice.getInvoiceNumber());

        } catch (Exception e) {
            LOG.error("Error processing purchase invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to process purchase invoice: " + e.getMessage(), e);
        }
    }

    /**
     * Add purchased metal to purchase metal stock
     */
    private void processPurchaseMetalStockAddition(PurchaseInvoice invoice) {
        if (invoice.getPurchaseMetalTransactions() == null ||
            invoice.getPurchaseMetalTransactions().isEmpty()) {
            LOG.info("No metal purchases to add to stock");
            return;
        }

        LOG.info("Adding {} metal purchases to stock", invoice.getPurchaseMetalTransactions().size());

        for (PurchaseMetalTransaction transaction : invoice.getPurchaseMetalTransactions()) {
            try {
                // Use Metal entity reference if available, otherwise fallback to string metalType
                if (transaction.getMetal() != null) {
                    purchaseMetalStockService.addPurchasedMetal(
                        transaction.getMetal(),
                        transaction.getGrossWeight(),
                        transaction.getNetWeightCharged(),
                        invoice.getInvoiceNumber(),
                        "Purchase from " + invoice.getSupplier().getSupplierFullName()
                    );
                    LOG.info("Added to stock: {} (ID: {}) - Gross: {}g, Net: {}g",
                            transaction.getMetal().getMetalName(), transaction.getMetal().getId(),
                            transaction.getGrossWeight(), transaction.getNetWeightCharged());
                } else {
                    // Fallback to old method for backward compatibility
                    purchaseMetalStockService.addPurchasedMetal(
                        transaction.getMetalType(),
                        transaction.getPurity(),
                        transaction.getGrossWeight(),
                        transaction.getNetWeightCharged(),
                        invoice.getInvoiceNumber(),
                        "Purchase from " + invoice.getSupplier().getSupplierFullName()
                    );
                    LOG.info("Added to stock: {} {} - Gross: {}g, Net: {}g",
                            transaction.getMetalType(), transaction.getPurity(),
                            transaction.getGrossWeight(), transaction.getNetWeightCharged());
                }

            } catch (Exception e) {
                String metalInfo = transaction.getMetal() != null ?
                    transaction.getMetal().getMetalName() :
                    transaction.getMetalType() + " " + transaction.getPurity();
                LOG.error("Error adding metal to stock: {}", metalInfo, e);
                throw e;
            }
        }
    }

    /**
     * Reduce exchange metal stock (metal given to supplier in exchange)
     */
    private void processExchangeMetalStockReduction(PurchaseInvoice invoice) {
        if (invoice.getPurchaseExchangeTransactions() == null ||
            invoice.getPurchaseExchangeTransactions().isEmpty()) {
            LOG.info("No exchange items to process");
            return;
        }

        String supplierName = invoice.getSupplier() != null ?
                invoice.getSupplier().getSupplierFullName() : "Supplier";

        LOG.info("Processing {} exchange items", invoice.getPurchaseExchangeTransactions().size());

        for (PurchaseExchangeTransaction transaction : invoice.getPurchaseExchangeTransactions()) {
            try {
                BigDecimal netWeight = transaction.getNetWeight();
                if (netWeight != null && netWeight.compareTo(BigDecimal.ZERO) > 0) {
                    // Use purity 0 to match exchange metal stock system
                    BigDecimal stockPurity = new BigDecimal("0");
                    exchangeMetalStockService.sellExchangeMetalWeight(
                        transaction.getMetalType(),
                        stockPurity,
                        netWeight,
                        "PURCHASE_INVOICE",
                        invoice.getId(),
                        invoice.getInvoiceNumber(),
                        supplierName
                    );

                    LOG.info("Reduced exchange stock: {} - {}g (fine: {}%)",
                            transaction.getMetalType(), netWeight, transaction.getFinePercentage());
                }
            } catch (Exception e) {
                LOG.error("Error reducing exchange metal stock for {}",
                         transaction.getItemName(), e);
                throw e;
            }
        }
    }

    /**
     * Process bank transaction for purchase payment
     */
    private void processBankTransaction(PurchaseInvoice invoice) {
        String supplierName = invoice.getSupplier() != null ?
                invoice.getSupplier().getSupplierFullName() : "Supplier";

        LOG.info("Processing bank transaction for purchase invoice {}", invoice.getInvoiceNumber());

        try {
            // Get active bank accounts
            List<BankAccount> activeBankAccounts = bankAccountService.getAllActiveBankAccounts();
            if (activeBankAccounts.isEmpty()) {
                LOG.warn("No active bank accounts found");
                return;
            }

            // Use first active bank account
            BankAccount bankAccount = activeBankAccounts.get(0);

            // Record purchase payment (debit from bank)
            bankTransactionService.recordPurchasePayment(
                bankAccount,
                invoice.getPaidAmount(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getPaymentReference(),
                supplierName
            );

            LOG.info("Recorded bank transaction - Amount: {}", invoice.getPaidAmount());

        } catch (Exception e) {
            LOG.error("Error recording bank transaction for invoice {}",
                     invoice.getInvoiceNumber(), e);
            // Don't throw - bank transaction failure shouldn't rollback entire purchase
        }
    }

    /**
     * Generate invoice number
     */
    public String generateInvoiceNumber() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = now.format(formatter);

        Long todayCount = purchaseInvoiceRepository.countTodaysInvoices();
        String counterPart = String.format("%04d", todayCount + 1);

        return "PI-" + datePart + "-" + counterPart;
    }

    /**
     * Find invoice by ID
     */
    public Optional<PurchaseInvoice> findById(Long id) {
        return purchaseInvoiceRepository.findById(id);
    }

    /**
     * Find invoice by invoice number
     */
    public Optional<PurchaseInvoice> findByInvoiceNumber(String invoiceNumber) {
        return purchaseInvoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    /**
     * Get all invoices
     */
    public List<PurchaseInvoice> getAllInvoices() {
        return purchaseInvoiceRepository.findAllOrderByInvoiceDateDesc();
    }

    /**
     * Get today's invoices
     */
    public List<PurchaseInvoice> findTodaysInvoices() {
        return purchaseInvoiceRepository.findInvoicesByDate(LocalDateTime.now());
    }

    /**
     * Get invoices by date range
     */
    public List<PurchaseInvoice> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseInvoiceRepository.findByInvoiceDateBetween(startDate, endDate);
    }

    /**
     * Get invoices by supplier
     */
    public List<PurchaseInvoice> findBySupplier(Long supplierId) {
        return purchaseInvoiceRepository.findBySupplierIdOrderByInvoiceDateDesc(supplierId);
    }

    /**
     * Update invoice status
     */
    public PurchaseInvoice updateStatus(Long invoiceId, PurchaseInvoice.InvoiceStatus status) {
        Optional<PurchaseInvoice> invoiceOpt = purchaseInvoiceRepository.findById(invoiceId);
        if (invoiceOpt.isPresent()) {
            PurchaseInvoice invoice = invoiceOpt.get();
            invoice.setStatus(status);
            return purchaseInvoiceRepository.save(invoice);
        }
        return null;
    }

    /**
     * Cancel invoice
     */
    public PurchaseInvoice cancelInvoice(Long invoiceId) {
        return updateStatus(invoiceId, PurchaseInvoice.InvoiceStatus.CANCELLED);
    }

    /**
     * Delete invoice
     */
    public void deleteInvoice(Long invoiceId) {
        purchaseInvoiceRepository.deleteById(invoiceId);
    }

    /**
     * Get total purchases for today
     */
    public Double getTodaysTotalPurchases() {
        return purchaseInvoiceRepository.getTodaysTotalPurchases();
    }

    /**
     * Get total purchases for date range
     */
    public Double getPurchasesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseInvoiceRepository.getPurchasesByDateRange(startDate, endDate);
    }

    /**
     * Get total pending amount for supplier
     */
    public BigDecimal getTotalPendingAmountForSupplier(Long supplierId) {
        return purchaseInvoiceRepository.getTotalPendingAmountBySupplierId(supplierId);
    }
}
