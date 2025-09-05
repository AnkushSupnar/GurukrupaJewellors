package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.BillTransaction;
import com.gurukrupa.data.repository.BillTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BillTransactionService {
    
    @Autowired
    private BillTransactionRepository billTransactionRepository;
    
    public BillTransaction saveBillTransaction(BillTransaction transaction) {
        return billTransactionRepository.save(transaction);
    }
    
    public List<BillTransaction> saveAllBillTransactions(List<BillTransaction> transactions) {
        return billTransactionRepository.saveAll(transactions);
    }
    
    public Optional<BillTransaction> findById(Long id) {
        return billTransactionRepository.findById(id);
    }
    
    public List<BillTransaction> findByBillId(Long billId) {
        return billTransactionRepository.findByBillId(billId);
    }
    
    
    public List<BillTransaction> findByItemCode(String itemCode) {
        return billTransactionRepository.findByItemCodeContainingIgnoreCase(itemCode);
    }
    
    public List<BillTransaction> findByItemName(String itemName) {
        return billTransactionRepository.findByItemNameContainingIgnoreCase(itemName);
    }
    
    public List<BillTransaction> findByMetalType(String metalType) {
        return billTransactionRepository.findByMetalType(metalType);
    }
    
    public List<BillTransaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return billTransactionRepository.findByDateRange(startDate, endDate);
    }
    
    public BillTransaction createBillTransaction(String itemCode, String itemName, String metalType,
                                               BigDecimal weight, BigDecimal ratePerTenGrams, 
                                               BigDecimal labourCharges) {
        
        BillTransaction transaction = BillTransaction.builder()
                .itemCode(itemCode)
                .itemName(itemName)
                .metalType(metalType)
                .weight(weight)
                .ratePerTenGrams(ratePerTenGrams)
                .labourCharges(labourCharges != null ? labourCharges : BigDecimal.ZERO)
                .build();
        
        // Calculate total amount
        transaction.calculateTotalAmount();
        
        return transaction;
    }
    
    public void deleteBillTransaction(Long id) {
        billTransactionRepository.deleteById(id);
    }
    
    // Analytics methods
    public Integer getTotalQuantitySoldByItemCode(String itemCode) {
        return billTransactionRepository.getTotalQuantitySoldByItemCode(itemCode);
    }
    
    public Double getTotalWeightSoldByMetal(String metalType) {
        return billTransactionRepository.getTotalWeightSoldByMetal(metalType);
    }
    
    public List<Object[]> getTopSellingItems() {
        return billTransactionRepository.getTopSellingItems();
    }
    
    public List<Object[]> getSalesByMetal() {
        return billTransactionRepository.getSalesByMetal();
    }
}