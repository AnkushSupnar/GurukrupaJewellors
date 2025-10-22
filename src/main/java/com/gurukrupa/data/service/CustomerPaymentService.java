package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.*;
import com.gurukrupa.data.repository.BankAccountRepository;
import com.gurukrupa.data.repository.BankTransactionRepository;
import com.gurukrupa.data.repository.BillRepository;
import com.gurukrupa.data.repository.CustomerPaymentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerPaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerPaymentService.class);
    private static final DateTimeFormatter RECEIPT_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private CustomerPaymentRepository customerPaymentRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    /**
     * Generate unique receipt number (CPR-YYYYMMDD-XXXX)
     */
    public String generateReceiptNumber() {
        String datePrefix = "CPR-" + LocalDateTime.now().format(RECEIPT_NUMBER_FORMAT);
        String lastReceiptNumber = customerPaymentRepository.findAllByOrderByPaymentDateDesc()
                .stream()
                .filter(payment -> payment.getReceiptNumber().startsWith(datePrefix))
                .findFirst()
                .map(CustomerPayment::getReceiptNumber)
                .orElse(null);

        int sequence = 1;
        if (lastReceiptNumber != null && lastReceiptNumber.length() >= 17) {
            try {
                sequence = Integer.parseInt(lastReceiptNumber.substring(13)) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return String.format("%s-%04d", datePrefix, sequence);
    }

    /**
     * Get customer's total pending amount across all bills
     */
    @Transactional
    public BigDecimal getCustomerPendingAmount(Long customerId) {
        List<Bill> pendingBills = getCustomerPendingBills(customerId);
        return pendingBills.stream()
                .map(Bill::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get list of pending bills for a customer
     */
    @Transactional
    public List<Bill> getCustomerPendingBills(Long customerId) {
        return billRepository.findByCustomerIdOrderByBillDateDesc(customerId).stream()
                .filter(bill -> bill.getPendingAmount() != null &&
                               bill.getPendingAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    /**
     * Record a customer payment receipt
     */
    @Transactional
    public CustomerPayment recordPayment(
            Customer customer,
            BankAccount bankAccount,
            BigDecimal paymentAmount,
            CustomerPayment.PaymentMode paymentMode,
            String transactionReference,
            String notes,
            java.time.LocalDate paymentDate) {

        LOG.info("Recording customer payment: Customer={}, Amount={}, Mode={}, Date={}",
                customer.getCustomerFullName(), paymentAmount, paymentMode, paymentDate);

        // Convert LocalDate to LocalDateTime (using current time)
        LocalDateTime paymentDateTime = paymentDate.atTime(java.time.LocalTime.now());

        // Get pending amounts
        BigDecimal previousPending = getCustomerPendingAmount(customer.getId());
        BigDecimal remainingPending = previousPending.subtract(paymentAmount);

        // Create bank transaction (CREDIT - money coming in)
        BankTransaction bankTransaction = BankTransaction.builder()
                .bankAccount(bankAccount)
                .transactionDate(paymentDateTime)
                .transactionType(BankTransaction.TransactionType.CREDIT)
                .source(BankTransaction.TransactionSource.BILL_PAYMENT)
                .amount(paymentAmount)
                .description("Payment received from customer: " + customer.getCustomerFullName())
                .transactionReference(transactionReference)
                .party(customer.getCustomerFullName())
                .balanceAfterTransaction(bankAccount.getCurrentBalance().add(paymentAmount))
                .build();

        bankTransaction = bankTransactionRepository.save(bankTransaction);

        // Update bank account balance (increase)
        bankAccount.setCurrentBalance(bankAccount.getCurrentBalance().add(paymentAmount));
        bankAccountRepository.save(bankAccount);

        // Create payment record
        CustomerPayment payment = CustomerPayment.builder()
                .receiptNumber(generateReceiptNumber())
                .customer(customer)
                .paymentDate(paymentDateTime)
                .paymentAmount(paymentAmount)
                .bankAccount(bankAccount)
                .bankTransaction(bankTransaction)
                .paymentMode(paymentMode)
                .transactionReference(transactionReference)
                .notes(notes)
                .previousPendingAmount(previousPending)
                .remainingPendingAmount(remainingPending.max(BigDecimal.ZERO))
                .build();

        payment = customerPaymentRepository.save(payment);

        // Allocate payment to pending bills (FIFO)
        allocatePaymentToBills(customer.getId(), paymentAmount);

        LOG.info("Payment recorded successfully: Receipt={}, Amount={}",
                payment.getReceiptNumber(), payment.getPaymentAmount());

        return payment;
    }

    /**
     * Allocate payment to pending bills (oldest first - FIFO)
     */
    private void allocatePaymentToBills(Long customerId, BigDecimal paymentAmount) {
        List<Bill> pendingBills = billRepository.findByCustomerIdOrderByBillDateDesc(customerId).stream()
                .filter(bill -> bill.getPendingAmount() != null &&
                               bill.getPendingAmount().compareTo(BigDecimal.ZERO) > 0)
                .sorted((b1, b2) -> b1.getBillDate().compareTo(b2.getBillDate())) // Oldest first
                .toList();

        BigDecimal remainingPayment = paymentAmount;

        for (Bill bill : pendingBills) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal billPending = bill.getPendingAmount();
            BigDecimal paymentForThisBill;

            if (remainingPayment.compareTo(billPending) >= 0) {
                // Full payment for this bill
                paymentForThisBill = billPending;
                bill.setPendingAmount(BigDecimal.ZERO);
            } else {
                // Partial payment
                paymentForThisBill = remainingPayment;
                bill.setPendingAmount(billPending.subtract(paymentForThisBill));
            }

            // Update paid amount
            BigDecimal currentPaid = bill.getPaidAmount() != null ?
                    bill.getPaidAmount() : BigDecimal.ZERO;
            bill.setPaidAmount(currentPaid.add(paymentForThisBill));

            billRepository.save(bill);

            remainingPayment = remainingPayment.subtract(paymentForThisBill);

            LOG.info("Allocated {} to bill {}, Remaining pending: {}",
                    paymentForThisBill, bill.getBillNumber(), bill.getPendingAmount());
        }
    }

    /**
     * Get all customer payments
     */
    @Transactional
    public List<CustomerPayment> getAllPayments() {
        return customerPaymentRepository.findAllByOrderByPaymentDateDesc();
    }

    /**
     * Get payments by date range
     */
    @Transactional
    public List<CustomerPayment> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return customerPaymentRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Get payments by customer and date range
     */
    @Transactional
    public List<CustomerPayment> getPaymentsByCustomerAndDateRange(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        return customerPaymentRepository.findAll().stream()
                .filter(payment -> payment.getCustomer().getId().equals(customerId))
                .filter(payment -> !payment.getPaymentDate().isBefore(startDate) &&
                                  !payment.getPaymentDate().isAfter(endDate))
                .sorted((p1, p2) -> p2.getPaymentDate().compareTo(p1.getPaymentDate()))
                .toList();
    }

    /**
     * Get payment by receipt number
     */
    @Transactional
    public Optional<CustomerPayment> getPaymentByReceiptNumber(String receiptNumber) {
        return customerPaymentRepository.findByReceiptNumber(receiptNumber);
    }

    /**
     * Get payment by ID
     */
    @Transactional
    public Optional<CustomerPayment> getPaymentById(Long id) {
        return customerPaymentRepository.findById(id);
    }

    /**
     * Get total payments received from a customer
     */
    @Transactional
    public BigDecimal getTotalPaymentsByCustomer(Long customerId) {
        return customerPaymentRepository.getTotalPaymentsByCustomer(customerId);
    }
}
