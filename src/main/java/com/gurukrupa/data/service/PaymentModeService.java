package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.PaymentMode;
import com.gurukrupa.data.entities.PaymentMode.PaymentType;
import com.gurukrupa.data.entities.PaymentMode.PaymentStatus;
import com.gurukrupa.data.repository.PaymentModeRepository;
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
    
    @Autowired
    private PaymentModeRepository paymentModeRepository;
    
    @Autowired
    private BankTransactionService bankTransactionService;
    
    public PaymentMode createCashPayment(Bill bill, BigDecimal amount) {
        PaymentMode payment = PaymentMode.builder()
                .bill(bill)
                .paymentType(PaymentType.CASH)
                .amount(amount)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();
        
        return paymentModeRepository.save(payment);
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
}