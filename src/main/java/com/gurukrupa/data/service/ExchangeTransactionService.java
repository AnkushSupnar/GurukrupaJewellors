package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.ExchangeTransaction;
import com.gurukrupa.data.repository.ExchangeTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExchangeTransactionService {
    
    @Autowired
    private ExchangeTransactionRepository exchangeTransactionRepository;
    
    public ExchangeTransaction createExchangeTransaction(String itemName, String metalType,
                                                        BigDecimal grossWeight, BigDecimal deduction,
                                                        BigDecimal ratePerTenGrams) {
        
        ExchangeTransaction transaction = ExchangeTransaction.builder()
                .itemName(itemName)
                .metalType(metalType)
                .grossWeight(grossWeight)
                .deduction(deduction != null ? deduction : BigDecimal.ZERO)
                .ratePerTenGrams(ratePerTenGrams)
                .build();
        
        // Calculate net weight and amount
        transaction.calculateNetWeightAndAmount();
        
        return transaction;
    }
    
    public ExchangeTransaction saveExchangeTransaction(ExchangeTransaction transaction) {
        return exchangeTransactionRepository.save(transaction);
    }
    
    public Optional<ExchangeTransaction> findById(Long id) {
        return exchangeTransactionRepository.findById(id);
    }
    
    public List<ExchangeTransaction> findByBillId(Long billId) {
        return exchangeTransactionRepository.findByBillId(billId);
    }
    
    public void deleteExchangeTransaction(Long id) {
        exchangeTransactionRepository.deleteById(id);
    }
    
    public BigDecimal getTotalExchangeAmountByBillId(Long billId) {
        Double total = exchangeTransactionRepository.getTotalExchangeAmountByBillId(billId);
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }
}