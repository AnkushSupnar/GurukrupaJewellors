package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Customer;
import com.gurukrupa.data.entities.Exchange;
import com.gurukrupa.data.entities.Exchange.ExchangeStatus;
import com.gurukrupa.data.entities.ExchangeTransaction;
import com.gurukrupa.data.repository.ExchangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ExchangeService {
    
    @Autowired
    private ExchangeRepository exchangeRepository;
    
    @Autowired
    private ExchangeTransactionService exchangeTransactionService;
    
    @Transactional
    public Exchange createExchange(Customer customer, List<ExchangeTransaction> exchangeTransactions, String notes) {
        return createExchange(customer, exchangeTransactions, notes, null);
    }
    
    @Transactional
    public Exchange createExchange(Customer customer, List<ExchangeTransaction> exchangeTransactions, String notes, Long billId) {
        // Generate exchange number
        String exchangeNumber = generateExchangeNumber();
        
        // Create new exchange
        Exchange exchange = Exchange.builder()
            .exchangeNumber(exchangeNumber)
            .customer(customer)
            .notes(notes)
            .status(ExchangeStatus.ACTIVE)
            .exchangeDate(LocalDateTime.now())
            .totalExchangeAmount(BigDecimal.ZERO) // Initialize with zero
            .build();
        
        // Save exchange first
        exchange = exchangeRepository.save(exchange);
        
        // Associate transactions with exchange
        BigDecimal totalAmount = BigDecimal.ZERO;
        System.out.println("ExchangeService: Starting to process " + exchangeTransactions.size() + " transactions");
        
        for (ExchangeTransaction transaction : exchangeTransactions) {
            transaction.setExchange(exchange);
            // Ensure transaction has calculated its total before saving
            transaction.calculateNetWeightAndAmount();
            ExchangeTransaction savedTransaction = exchangeTransactionService.saveExchangeTransaction(transaction);
            BigDecimal transAmt = savedTransaction.getTotalAmount() != null ? savedTransaction.getTotalAmount() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(transAmt);
            System.out.println("ExchangeService: Added transaction amount: " + transAmt + ", Running total: " + totalAmount);
        }
        
        // Update total amount
        System.out.println("ExchangeService: Setting exchange total amount to: " + totalAmount);
        exchange.setTotalExchangeAmount(totalAmount);
        exchange = exchangeRepository.save(exchange);
        System.out.println("ExchangeService: Saved exchange with total: " + exchange.getTotalExchangeAmount());
        
        return exchange;
    }
    
    @Transactional(readOnly = true)
    public Optional<Exchange> findById(Long id) {
        return exchangeRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<Exchange> findByExchangeNumber(String exchangeNumber) {
        return exchangeRepository.findByExchangeNumber(exchangeNumber);
    }
    
    @Transactional(readOnly = true)
    public List<Exchange> findByCustomer(Customer customer) {
        return exchangeRepository.findByCustomer(customer);
    }
    
    @Transactional(readOnly = true)
    public List<Exchange> findActiveExchangesByCustomer(Customer customer) {
        return exchangeRepository.findByCustomerAndStatus(customer, ExchangeStatus.ACTIVE);
    }
    
    @Transactional(readOnly = true)
    public List<Exchange> findActiveExchangesByCustomerId(Long customerId) {
        return exchangeRepository.findActiveExchangesByCustomerId(customerId);
    }
    
    @Transactional(readOnly = true)
    public List<Exchange> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return exchangeRepository.findByExchangeDateBetween(startDate, endDate);
    }
    
    @Transactional
    public Exchange saveExchange(Exchange exchange) {
        // Don't recalculate if total is already set
        if (exchange.getTotalExchangeAmount() == null) {
            exchange.calculateTotalAmount();
        }
        System.out.println("ExchangeService.saveExchange: Saving exchange with total: " + exchange.getTotalExchangeAmount());
        return exchangeRepository.save(exchange);
    }
    
    @Transactional
    public void markExchangeAsUsed(Long exchangeId) {
        exchangeRepository.findById(exchangeId).ifPresent(exchange -> {
            exchange.setStatus(ExchangeStatus.USED_IN_BILL);
            exchangeRepository.save(exchange);
        });
    }
    
    @Transactional
    public void cancelExchange(Long exchangeId) {
        exchangeRepository.findById(exchangeId).ifPresent(exchange -> {
            exchange.setStatus(ExchangeStatus.CANCELLED);
            exchangeRepository.save(exchange);
        });
    }
    
    @Transactional(readOnly = true)
    public List<Exchange> getAllExchanges() {
        return exchangeRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Exchange> getExchangesByStatus(ExchangeStatus status) {
        return exchangeRepository.findByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public Optional<Exchange> findByBillId(Long billId) {
        return exchangeRepository.findByBillIdOptional(billId);
    }
    
    @Transactional
    public void linkExchangeToBill(Long exchangeId, Long billId) {
        exchangeRepository.findById(exchangeId).ifPresent(exchange -> {
            // Note: We need to get the Bill entity from BillService to set the reference
            // This will be handled in the BillService when creating the bill
            exchange.setStatus(ExchangeStatus.USED_IN_BILL);
            exchangeRepository.save(exchange);
        });
    }
    
    private String generateExchangeNumber() {
        // Format: EX/YYYY/MM/XXXX where XXXX is a sequential number
        LocalDate now = LocalDate.now();
        String prefix = String.format("EX/%d/%02d/", now.getYear(), now.getMonthValue());
        
        // Get count of exchanges with this prefix
        long count = exchangeRepository.countByExchangeNumberPrefix(prefix);
        
        // Generate new exchange number
        String exchangeNumber = String.format("%s%04d", prefix, count + 1);
        
        // Check if this number already exists (to handle race conditions)
        while (exchangeRepository.findByExchangeNumber(exchangeNumber).isPresent()) {
            count++;
            exchangeNumber = String.format("%s%04d", prefix, count + 1);
        }
        
        return exchangeNumber;
    }
}