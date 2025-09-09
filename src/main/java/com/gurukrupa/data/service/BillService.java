package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Bill;
import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.entities.ExchangeTransaction;
import com.gurukrupa.data.entities.Exchange;
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
    private ExchangeService exchangeService;
    
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
                                        Exchange exchange,
                                        BigDecimal discount, BigDecimal gstRate, 
                                        Bill.PaymentMethod paymentMethod) {
        
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }
        
        // Create new bill
        BigDecimal exchangeAmt = exchange != null ? exchange.getTotalExchangeAmount() : BigDecimal.ZERO;
        System.out.println("Creating bill with exchange amount: " + exchangeAmt);
        
        Bill bill = Bill.builder()
                .customer(customer)
                .discount(discount != null ? discount : BigDecimal.ZERO)
                .gstRate(gstRate != null ? gstRate : new BigDecimal("3.00"))
                .exchangeAmount(exchangeAmt)
                .paymentMethod(paymentMethod != null ? paymentMethod : Bill.PaymentMethod.CASH)
                .status(Bill.BillStatus.DRAFT)
                .billDate(LocalDateTime.now())
                .paidAmount(BigDecimal.ZERO)
                .pendingAmount(BigDecimal.ZERO)
                .billTransactions(new ArrayList<>())
                .build();
        
        // Add all bill transactions with proper bill reference
        billTransactions.forEach(transaction -> {
            transaction.setBill(bill);
            bill.getBillTransactions().add(transaction);
        });
        
        // Save the bill first
        Bill savedBill = saveBill(bill);
        
        // Update exchange with bill reference if present
        if (exchange != null) {
            exchange.setBill(savedBill);
            exchange.setStatus(Exchange.ExchangeStatus.USED_IN_BILL);
            Exchange savedExchange = exchangeService.saveExchange(exchange);
            
            // Set exchange on bill for transient use (not persisted)
            savedBill.setExchange(savedExchange);
            
            // Exchange amount is already set, just ensure totals are calculated
            savedBill.setExchangeAmount(savedExchange.getTotalExchangeAmount());
            
            // Recalculate totals with exchange
            savedBill.calculateTotals();
            
            // Save the updated bill (without exchange reference in DB)
            savedBill = billRepository.save(savedBill);
            
            System.out.println("BillService: Bill updated with exchange amount: " + savedBill.getExchangeAmount());
        }
        
        return savedBill;
    }
    
    public String generateBillNumber() {
        // Initialize settings if not exists
        appSettingsService.initializeDefaultSettings();
        
        // Use settings-based bill number generation
        return appSettingsService.generateBillNumber();
    }
    
    public Optional<Bill> findById(Long id) {
        Optional<Bill> billOpt = billRepository.findById(id);
        // Load associated exchange if exists
        if (billOpt.isPresent()) {
            Bill bill = billOpt.get();
            Exchange exchange = exchangeService.findByBillId(id).orElse(null);
            bill.setExchange(exchange);
        }
        return billOpt;
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
        Optional<Bill> billOpt = billRepository.findById(billId);
        if (billOpt.isPresent() && billOpt.get().getExchange() != null) {
            return exchangeService.findById(billOpt.get().getExchange().getId())
                    .map(Exchange::getExchangeTransactions)
                    .orElse(new ArrayList<>());
        }
        return new ArrayList<>();
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