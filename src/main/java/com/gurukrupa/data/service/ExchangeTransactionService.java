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
    
    public List<ExchangeTransaction> findByExchangeId(Long exchangeId) {
        return exchangeTransactionRepository.findByExchangeId(exchangeId);
    }
    
    public void deleteExchangeTransaction(Long id) {
        exchangeTransactionRepository.deleteById(id);
    }
    
    public BigDecimal getTotalExchangeAmountByExchangeId(Long exchangeId) {
        Double total = exchangeTransactionRepository.getTotalExchangeAmountByExchangeId(exchangeId);
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }
    
    /**
     * Get all distinct exchange item names for autocomplete suggestions
     * This supports the self-learning feature by returning all previously entered item names
     */
    public List<String> getAllDistinctItemNames() {
        return exchangeTransactionRepository.findDistinctItemNames();
    }
    
    /**
     * Get distinct exchange item names that match the search term
     * Used for filtered autocomplete suggestions
     */
    public List<String> getDistinctItemNamesBySearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllDistinctItemNames();
        }
        return exchangeTransactionRepository.findDistinctItemNamesBySearchTerm(searchTerm.trim());
    }
}