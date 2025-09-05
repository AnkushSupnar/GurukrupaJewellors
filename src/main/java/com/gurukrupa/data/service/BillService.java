package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.repository.BillRepository;
import com.gurukrupa.data.repository.BillTransactionRepository;
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
public class BillService {
    
    @Autowired
    private BillRepository billRepository;
    
    @Autowired
    private BillTransactionRepository billTransactionRepository;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private AppSettingsService appSettingsService;
    
    public Bill saveBill(Bill bill) {
        // Generate bill number if not set
        if (bill.getBillNumber() == null || bill.getBillNumber().isEmpty()) {
            bill.setBillNumber(generateBillNumber());
        }
        
        // Set bill reference in all transactions before saving
        for (BillTransaction transaction : bill.getBillTransactions()) {
            transaction.setBill(bill);
        }
        
        // Calculate totals before saving
        bill.calculateTotals();
        
        // Save the bill (this will cascade save the transactions)
        return billRepository.save(bill);
    }
    
    public Bill createBillFromTransaction(Customer customer,
                                        List<BillTransaction> saleTransactions, 
                                        List<BillTransaction> exchangeTransactions,
                                        BigDecimal discount, BigDecimal gstRate, 
                                        Bill.PaymentMethod paymentMethod) {
        
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        
        // Create new bill
        Bill bill = Bill.builder()
                .customer(customer)
                .discount(discount != null ? discount : BigDecimal.ZERO)
                .gstRate(gstRate != null ? gstRate : new BigDecimal("3.00"))
                .exchangeAmount(BigDecimal.ZERO)
                .paymentMethod(paymentMethod != null ? paymentMethod : Bill.PaymentMethod.CASH)
                .status(Bill.BillStatus.DRAFT)
                .billDate(LocalDateTime.now())
                .billTransactions(new ArrayList<>())
                .build();
        
        // Add all transactions
        saleTransactions.forEach(transaction -> {
            transaction.setTransactionType(BillTransaction.TransactionType.SALE);
            bill.getBillTransactions().add(transaction);
        });
        
        exchangeTransactions.forEach(transaction -> {
            transaction.setTransactionType(BillTransaction.TransactionType.EXCHANGE);
            bill.getBillTransactions().add(transaction);
        });
        
        // Calculate exchange amount
        BigDecimal exchangeAmount = exchangeTransactions.stream()
                .map(BillTransaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        bill.setExchangeAmount(exchangeAmount);
        
        return saveBill(bill);
    }
    
    public String generateBillNumber() {
        // Initialize settings if not exists
        appSettingsService.initializeDefaultSettings();
        
        // Use settings-based bill number generation
        return appSettingsService.generateBillNumber();
    }
    
    public Optional<Bill> findById(Long id) {
        return billRepository.findById(id);
    }
    
    public Optional<Bill> findByBillNumber(String billNumber) {
        return billRepository.findByBillNumber(billNumber);
    }
    
    public List<Bill> findAllBills() {
        return billRepository.findAllOrderByCreatedDateDesc();
    }
    
    public List<Bill> findBillsByCustomer(Long customerId) {
        return billRepository.findByCustomerId(customerId);
    }
    
    public List<Bill> findBillsByCustomerName(String customerName) {
        return billRepository.findByCustomerNameContainingIgnoreCase(customerName);
    }
    
    public List<Bill> findBillsByCustomerMobile(String mobile) {
        return billRepository.findByCustomerMobile(mobile);
    }
    
    public List<Bill> findBillsByStatus(Bill.BillStatus status) {
        return billRepository.findByStatus(status);
    }
    
    public List<Bill> findBillsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return billRepository.findBillsByDateRange(startDate, endDate);
    }
    
    public List<Bill> findTodaysBills() {
        return billRepository.findBillsByDate(LocalDateTime.now());
    }
    
    public Double getTodaysTotalSales() {
        return billRepository.getTodaysTotalSales();
    }
    
    public Double getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return billRepository.getSalesByDateRange(startDate, endDate);
    }
    
    public Bill updateBillStatus(Long billId, Bill.BillStatus status) {
        Optional<Bill> billOpt = billRepository.findById(billId);
        if (billOpt.isPresent()) {
            Bill bill = billOpt.get();
            bill.setStatus(status);
            return billRepository.save(bill);
        }
        return null;
    }
    
    public Bill markBillAsPaid(Long billId) {
        return updateBillStatus(billId, Bill.BillStatus.PAID);
    }
    
    public Bill cancelBill(Long billId) {
        return updateBillStatus(billId, Bill.BillStatus.CANCELLED);
    }
    
    public void deleteBill(Long billId) {
        billRepository.deleteById(billId);
    }
    
    // Analytics methods
    public Long getTodaysBillCount() {
        return billRepository.countTodaysBills();
    }
    
    public List<BillTransaction> getBillTransactions(Long billId) {
        return billTransactionRepository.findByBillId(billId);
    }
    
    public List<BillTransaction> getSaleTransactions(Long billId) {
        return billTransactionRepository.findByBillIdAndTransactionType(billId, BillTransaction.TransactionType.SALE);
    }
    
    public List<BillTransaction> getExchangeTransactions(Long billId) {
        return billTransactionRepository.findByBillIdAndTransactionType(billId, BillTransaction.TransactionType.EXCHANGE);
    }
}