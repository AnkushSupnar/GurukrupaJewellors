package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.BankTransaction;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.entities.PaymentMode;
import com.gurukrupa.data.entities.PaymentMode.PaymentType;
import com.gurukrupa.data.entities.PaymentMode.PaymentStatus;
import com.gurukrupa.data.entities.PurchaseInvoice;
import com.gurukrupa.data.repository.PaymentModeRepository;
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
public class PaymentModeService {
    
    private static final Logger LOG = LoggerFactory.getLogger(PaymentModeService.class);
    
    @Autowired
    private PaymentModeRepository paymentModeRepository;
    
    @Autowired
    private BankTransactionService bankTransactionService;
    
    @Autowired
    private BankAccountService bankAccountService;
    
    public PaymentMode createCashPayment(Bill bill, BigDecimal amount) {
        PaymentMode payment = PaymentMode.builder()
                .bill(bill)
                .paymentType(PaymentType.CASH)
                .amount(amount)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();
        
        PaymentMode savedPayment = paymentModeRepository.save(payment);
        
        // Find Cash bank account and record transaction
        Optional<BankAccount> cashAccount = bankAccountService.findByBankName("Cash").stream()
                .filter(acc -> acc.getIsActive())
                .findFirst();
        
        if (cashAccount.isEmpty()) {
            // Try to find by account name containing "Cash"
            cashAccount = bankAccountService.getAllActiveBankAccounts().stream()
                    .filter(acc -> acc.getBankName().equalsIgnoreCase("Cash") || 
                                  acc.getAccountHolderName().equalsIgnoreCase("Cash"))
                    .findFirst();
        }
        
        if (cashAccount.isPresent()) {
            // Record bank transaction for cash payment
            String customerName = bill.getCustomer() != null ? bill.getCustomer().getCustomerFullName() : "Customer";
            bankTransactionService.recordBillPayment(
                cashAccount.get(),
                amount,
                bill.getId(),
                bill.getBillNumber(),
                "CASH-" + bill.getBillNumber(),
                customerName
            );
        }
        
        return savedPayment;
    }
    
    public PaymentMode createBankPayment(Bill bill, BigDecimal amount, BankAccount bankAccount, String referenceNumber) {
        PaymentMode payment = PaymentMode.builder()
                .bill(bill)
                .paymentType(PaymentType.BANK)
                .amount(amount)
                .bankAccount(bankAccount)
                .referenceNumber(referenceNumber)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();
        
        PaymentMode savedPayment = paymentModeRepository.save(payment);
        
        // Record bank transaction for the payment received
        String customerName = bill.getCustomer() != null ? bill.getCustomer().getCustomerFullName() : "Customer";
        bankTransactionService.recordBillPayment(
            bankAccount,
            amount,
            bill.getId(),
            bill.getBillNumber(),
            referenceNumber,
            customerName
        );
        
        return savedPayment;
    }
    
    public PaymentMode createUPIPayment(Bill bill, BigDecimal amount, String referenceNumber, String upiId) {
        PaymentMode payment = PaymentMode.builder()
                .bill(bill)
                .paymentType(PaymentType.UPI)
                .amount(amount)
                .referenceNumber(referenceNumber)
                .upiId(upiId)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();
        
        return paymentModeRepository.save(payment);
    }
    
    public PaymentMode createUPIPaymentWithBankAccount(Bill bill, BigDecimal amount, String referenceNumber, 
                                                      String upiId, BankAccount bankAccount) {
        PaymentMode payment = PaymentMode.builder()
                .bill(bill)
                .paymentType(PaymentType.UPI)
                .amount(amount)
                .referenceNumber(referenceNumber)
                .upiId(upiId)
                .bankAccount(bankAccount)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();
        
        PaymentMode savedPayment = paymentModeRepository.save(payment);
        
        // Record bank transaction for UPI payment
        String customerName = bill.getCustomer() != null ? bill.getCustomer().getCustomerFullName() : "Customer";
        bankTransactionService.recordBillPayment(
            bankAccount,
            amount,
            bill.getId(),
            bill.getBillNumber(),
            referenceNumber,
            customerName
        );
        
        return savedPayment;
    }
    
    public PaymentMode createCardPayment(Bill bill, BigDecimal amount, String referenceNumber, 
                                       String cardLastFour, String cardType, String cardNetwork) {
        PaymentMode payment = PaymentMode.builder()
                .bill(bill)
                .paymentType(PaymentType.CARD)
                .amount(amount)
                .referenceNumber(referenceNumber)
                .cardLastFour(cardLastFour)
                .cardType(cardType)
                .cardNetwork(cardNetwork)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();
        
        return paymentModeRepository.save(payment);
    }
    
    public PaymentMode savePaymentMode(PaymentMode paymentMode) {
        return paymentModeRepository.save(paymentMode);
    }
    
    @Transactional(readOnly = true)
    public Optional<PaymentMode> findById(Long id) {
        return paymentModeRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<PaymentMode> findByBillId(Long billId) {
        return paymentModeRepository.findByBillIdOrderByCreatedDate(billId);
    }
    
    @Transactional(readOnly = true)
    public List<PaymentMode> findByPaymentType(PaymentType paymentType) {
        return paymentModeRepository.findByPaymentType(paymentType);
    }
    
    @Transactional(readOnly = true)
    public List<PaymentMode> findByReferenceNumber(String referenceNumber) {
        return paymentModeRepository.findByReferenceNumber(referenceNumber);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidAmountForBill(Long billId) {
        BigDecimal total = paymentModeRepository.getTotalPaidAmountByBillId(billId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    @Transactional(readOnly = true)
    public List<PaymentMode> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentModeRepository.findByPaymentDateBetween(startDate, endDate);
    }
    
    public void deletePaymentMode(Long id) {
        paymentModeRepository.deleteById(id);
    }
    
    public PaymentMode updatePaymentStatus(Long id, PaymentStatus status) {
        Optional<PaymentMode> paymentOpt = paymentModeRepository.findById(id);
        if (paymentOpt.isPresent()) {
            PaymentMode payment = paymentOpt.get();
            payment.setStatus(status);
            return paymentModeRepository.save(payment);
        }
        return null;
    }
    
    public void createPurchasePayment(PurchaseInvoice invoice, String paymentType, BigDecimal amount, String reference) {
        try {
            LOG.info("Processing purchase payment for invoice {} with amount {}", 
                     invoice.getInvoiceNumber(), amount);
            
            // Handle cash payments through Cash bank account
            if ("CASH".equalsIgnoreCase(paymentType)) {
                Optional<BankAccount> cashAccount = bankAccountService.findByBankName("Cash").stream()
                        .filter(acc -> acc.getIsActive())
                        .findFirst();
                
                if (cashAccount.isPresent()) {
                    // Record debit transaction for cash payment (money going out)
                    bankTransactionService.recordDebit(
                        cashAccount.get(),
                        amount,
                        BankTransaction.TransactionSource.PURCHASE_PAYMENT,
                        "PURCHASE_INVOICE",
                        invoice.getId(),
                        invoice.getInvoiceNumber(),
                        reference,  // transaction reference
                        invoice.getSupplier().getSupplierFullName(),
                        "Purchase payment for invoice " + invoice.getInvoiceNumber()
                    );
                    LOG.info("Created bank debit transaction for cash purchase payment");
                }
            } else if ("UPI".equalsIgnoreCase(paymentType)) {
                // Handle UPI payment - need to select bank account
                // For now, we'll log this requirement
                LOG.info("UPI payment for purchase invoice - bank account selection needed");
            } else {
                // Other payment types
                LOG.info("Processing {} payment for purchase invoice {}", paymentType, invoice.getInvoiceNumber());
            }
            
        } catch (Exception e) {
            LOG.error("Error processing purchase payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process purchase payment: " + e.getMessage());
        }
    }
}