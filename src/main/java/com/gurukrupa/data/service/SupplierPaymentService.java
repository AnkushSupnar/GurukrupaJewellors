package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.repository.SupplierPaymentRepository;
import com.gurukrupa.data.repository.PurchaseInvoiceRepository;
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

@Service
@Transactional
public class SupplierPaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(SupplierPaymentService.class);
    private static final DateTimeFormatter RECEIPT_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private SupplierPaymentRepository supplierPaymentRepository;

    @Autowired
    private PurchaseInvoiceRepository purchaseInvoiceRepository;

    @Autowired
    private BankTransactionService bankTransactionService;

    /**
     * Generate next receipt number in format: SPR-YYYYMMDD-XXXX
     */
    public String generateReceiptNumber() {
        String dateStr = LocalDateTime.now().format(RECEIPT_NUMBER_FORMAT);
        String prefix = "SPR-" + dateStr + "-";

        Optional<String> latestNumber = supplierPaymentRepository.findLatestReceiptNumber();

        if (latestNumber.isPresent() && latestNumber.get().startsWith(prefix)) {
            // Extract sequence number and increment
            String lastNumber = latestNumber.get();
            int sequenceNumber = Integer.parseInt(lastNumber.substring(prefix.length())) + 1;
            return String.format("%s%04d", prefix, sequenceNumber);
        } else {
            // First receipt of the day
            return prefix + "0001";
        }
    }

    /**
     * Get total pending amount for a supplier
     */
    @Transactional(readOnly = true)
    public BigDecimal getSupplierPendingAmount(Long supplierId) {
        List<PurchaseInvoice> invoices = purchaseInvoiceRepository
                .findBySupplierIdOrderByInvoiceDateDesc(supplierId);

        BigDecimal totalPending = BigDecimal.ZERO;
        for (PurchaseInvoice invoice : invoices) {
            if (invoice.getPendingAmount() != null) {
                totalPending = totalPending.add(invoice.getPendingAmount());
            }
        }

        return totalPending;
    }

    /**
     * Get list of pending invoices for a supplier
     */
    @Transactional(readOnly = true)
    public List<PurchaseInvoice> getSupplierPendingInvoices(Long supplierId) {
        List<PurchaseInvoice> allInvoices = purchaseInvoiceRepository
                .findBySupplierIdOrderByInvoiceDateDesc(supplierId);

        // Filter invoices with pending amount
        return allInvoices.stream()
                .filter(invoice -> invoice.getPendingAmount() != null &&
                        invoice.getPendingAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    /**
     * Record a supplier payment
     */
    public SupplierPayment recordPayment(Supplier supplier, BankAccount bankAccount,
                                        BigDecimal paymentAmount, SupplierPayment.PaymentMode paymentMode,
                                        String transactionReference, String notes) {

        // Get current pending amount
        BigDecimal previousPending = getSupplierPendingAmount(supplier.getId());

        // Create payment record
        SupplierPayment payment = SupplierPayment.builder()
                .receiptNumber(generateReceiptNumber())
                .supplier(supplier)
                .paymentDate(LocalDateTime.now())
                .paymentAmount(paymentAmount)
                .bankAccount(bankAccount)
                .paymentMode(paymentMode)
                .transactionReference(transactionReference)
                .notes(notes)
                .previousPendingAmount(previousPending)
                .remainingPendingAmount(previousPending.subtract(paymentAmount))
                .build();

        // Save payment record
        payment = supplierPaymentRepository.save(payment);

        // Record bank transaction (debit from bank)
        BankTransaction bankTxn = bankTransactionService.recordPurchasePayment(
                bankAccount,
                paymentAmount,
                null, // No specific purchase ID, this is a general payment
                payment.getReceiptNumber(),
                transactionReference,
                supplier.getSupplierFullName()
        );

        // Link bank transaction to payment
        payment.setBankTransaction(bankTxn);
        payment = supplierPaymentRepository.save(payment);

        // Update purchase invoices to allocate this payment
        allocatePaymentToInvoices(supplier.getId(), paymentAmount);

        LOG.info("Recorded supplier payment: {} - Amount: {} to {}",
                payment.getReceiptNumber(), paymentAmount, supplier.getSupplierFullName());

        return payment;
    }

    /**
     * Allocate payment amount to pending invoices (FIFO - oldest first)
     */
    private void allocatePaymentToInvoices(Long supplierId, BigDecimal paymentAmount) {
        List<PurchaseInvoice> pendingInvoices = getSupplierPendingInvoices(supplierId);

        BigDecimal remainingPayment = paymentAmount;

        for (PurchaseInvoice invoice : pendingInvoices) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal invoicePending = invoice.getPendingAmount();
            BigDecimal amountToAllocate = remainingPayment.min(invoicePending);

            // Update invoice amounts
            invoice.setPaidAmount(invoice.getPaidAmount().add(amountToAllocate));
            invoice.setPendingAmount(invoice.getPendingAmount().subtract(amountToAllocate));

            purchaseInvoiceRepository.save(invoice);

            remainingPayment = remainingPayment.subtract(amountToAllocate);

            LOG.info("Allocated {} to invoice {}, remaining pending: {}",
                    amountToAllocate, invoice.getInvoiceNumber(), invoice.getPendingAmount());
        }

        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
            LOG.warn("Payment amount {} exceeds total pending amount. Excess: {}",
                    paymentAmount, remainingPayment);
        }
    }

    /**
     * Get all payments for a supplier
     */
    @Transactional(readOnly = true)
    public List<SupplierPayment> getPaymentsBySupplier(Long supplierId) {
        return supplierPaymentRepository.findBySupplierIdOrderByPaymentDateDesc(supplierId);
    }

    /**
     * Get all payments
     */
    @Transactional(readOnly = true)
    public List<SupplierPayment> getAllPayments() {
        return supplierPaymentRepository.findAllByOrderByPaymentDateDesc();
    }

    /**
     * Get payments by date range
     */
    @Transactional(readOnly = true)
    public List<SupplierPayment> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return supplierPaymentRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Get payments by supplier and date range
     */
    @Transactional(readOnly = true)
    public List<SupplierPayment> getPaymentsBySupplierAndDateRange(Long supplierId, LocalDateTime startDate, LocalDateTime endDate) {
        return supplierPaymentRepository.findAll().stream()
            .filter(payment -> payment.getSupplier().getId().equals(supplierId))
            .filter(payment -> !payment.getPaymentDate().isBefore(startDate) &&
                              !payment.getPaymentDate().isAfter(endDate))
            .sorted((p1, p2) -> p2.getPaymentDate().compareTo(p1.getPaymentDate()))
            .toList();
    }

    /**
     * Get payment by receipt number
     */
    @Transactional(readOnly = true)
    public Optional<SupplierPayment> getPaymentByReceiptNumber(String receiptNumber) {
        return supplierPaymentRepository.findByReceiptNumber(receiptNumber);
    }

    /**
     * Get payment by ID
     */
    @Transactional(readOnly = true)
    public Optional<SupplierPayment> getPaymentById(Long id) {
        return supplierPaymentRepository.findById(id);
    }

    /**
     * Get total payments made to a supplier
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPaymentsBySupplier(Long supplierId) {
        return supplierPaymentRepository.getTotalPaymentsBySupplier(supplierId);
    }

    /**
     * Delete a payment (with proper cleanup)
     */
    public void deletePayment(Long paymentId) {
        Optional<SupplierPayment> paymentOpt = supplierPaymentRepository.findById(paymentId);

        if (paymentOpt.isPresent()) {
            SupplierPayment payment = paymentOpt.get();

            // Reverse the payment allocation from invoices
            reversePaymentAllocation(payment.getSupplier().getId(), payment.getPaymentAmount());

            // Note: Bank transaction reversal should be handled separately if needed
            // For audit purposes, you might want to keep the bank transaction

            supplierPaymentRepository.delete(payment);

            LOG.info("Deleted supplier payment: {}", payment.getReceiptNumber());
        }
    }

    /**
     * Reverse payment allocation (add back to pending amounts)
     */
    private void reversePaymentAllocation(Long supplierId, BigDecimal paymentAmount) {
        List<PurchaseInvoice> paidInvoices = purchaseInvoiceRepository
                .findBySupplierIdOrderByInvoiceDateDesc(supplierId)
                .stream()
                .filter(inv -> inv.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal remainingToReverse = paymentAmount;

        // Reverse in reverse order (LIFO for reversal)
        for (int i = paidInvoices.size() - 1; i >= 0 && remainingToReverse.compareTo(BigDecimal.ZERO) > 0; i--) {
            PurchaseInvoice invoice = paidInvoices.get(i);

            BigDecimal amountToReverse = remainingToReverse.min(invoice.getPaidAmount());

            invoice.setPaidAmount(invoice.getPaidAmount().subtract(amountToReverse));
            invoice.setPendingAmount(invoice.getPendingAmount().add(amountToReverse));

            purchaseInvoiceRepository.save(invoice);

            remainingToReverse = remainingToReverse.subtract(amountToReverse);
        }
    }
}
