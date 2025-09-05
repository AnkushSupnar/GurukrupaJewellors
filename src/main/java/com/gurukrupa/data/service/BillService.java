package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.entities.ExchangeTransaction;
import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.repository.BillRepository;
import com.gurukrupa.data.repository.BillTransactionRepository;
import com.gurukrupa.data.repository.ExchangeTransactionRepository;
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
    private ExchangeTransactionRepository exchangeTransactionRepository;
    
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
                                        List<BillTransaction> billTransactions, 
                                        List<ExchangeTransaction> exchangeTransactions,
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
                .paidAmount(BigDecimal.ZERO)
                .pendingAmount(BigDecimal.ZERO)
                .billTransactions(new ArrayList<>())
                .exchangeTransactions(new ArrayList<>())
                .build();
        
        // Add all bill transactions with proper bill reference
        billTransactions.forEach(transaction -> {
            transaction.setBill(bill);
            bill.getBillTransactions().add(transaction);
        });
        
        // Add all exchange transactions with proper bill reference
        exchangeTransactions.forEach(transaction -> {
            transaction.setBill(bill);
            bill.getExchangeTransactions().add(transaction);
        });
        
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
    
    public List<ExchangeTransaction> getExchangeTransactions(Long billId) {
        return exchangeTransactionRepository.findByBillId(billId);
    }
    
    public List<Bill> findByCustomerId(Long customerId) {
        return billRepository.findByCustomerIdOrderByBillDateDesc(customerId);
    }
    
    public List<Bill> findByCustomerIdAndDateRange(Long customerId, LocalDateTime fromDate, LocalDateTime toDate) {
        return billRepository.findByCustomerIdAndBillDateBetween(customerId, fromDate, toDate);
    }
    
    public BigDecimal getTotalPendingAmountForCustomer(Long customerId) {
        return billRepository.getTotalPendingAmountByCustomerId(customerId);
    }
    
    public Double getTodaysCollectedAmount() {
        return billRepository.getTodaysCollectedAmount();
    }
    
    public Double getCollectedAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return billRepository.getCollectedAmountByDateRange(startDate, endDate);
    }
}