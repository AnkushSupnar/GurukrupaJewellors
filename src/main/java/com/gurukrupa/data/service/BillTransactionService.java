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
    
    public List<BillTransaction> findByTransactionType(BillTransaction.TransactionType transactionType) {
        return billTransactionRepository.findByTransactionType(transactionType);
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
    
    public BillTransaction createSaleTransaction(Long jewelryItemId, String itemCode, String itemName, String metalType,
                                               Integer quantity, BigDecimal weightPerUnit, 
                                               BigDecimal ratePerTenGrams, BigDecimal labourCharges,
                                               BigDecimal stoneCharges, BigDecimal otherCharges) {
        
        BillTransaction transaction = BillTransaction.builder()
                .jewelryItemId(jewelryItemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .metalType(metalType)
                .quantity(quantity)
                .weightPerUnit(weightPerUnit)
                .ratePerTenGrams(ratePerTenGrams)
                .labourCharges(labourCharges != null ? labourCharges : BigDecimal.ZERO)
                .stoneCharges(stoneCharges != null ? stoneCharges : BigDecimal.ZERO)
                .otherCharges(otherCharges != null ? otherCharges : BigDecimal.ZERO)
                .transactionType(BillTransaction.TransactionType.SALE)
                .build();
        
        // Calculate amounts
        transaction.calculateAmounts();
        
        return transaction;
    }
    
    public BillTransaction createExchangeTransaction(String itemName, String metalType,
                                                   BigDecimal totalWeight, BigDecimal deductionWeight,
                                                   BigDecimal ratePerTenGrams) {
        
        BillTransaction transaction = BillTransaction.builder()
                .itemCode("EXCHANGE-" + System.currentTimeMillis()) // Generate temporary code for exchange
                .itemName(itemName)
                .metalType(metalType)
                .quantity(1) // Exchange items are typically counted as 1
                .weightPerUnit(totalWeight)
                .totalWeight(totalWeight)
                .deductionWeight(deductionWeight != null ? deductionWeight : BigDecimal.ZERO)
                .ratePerTenGrams(ratePerTenGrams)
                .labourCharges(BigDecimal.ZERO) // Exchange items typically don't have labour charges
                .stoneCharges(BigDecimal.ZERO)
                .otherCharges(BigDecimal.ZERO)
                .transactionType(BillTransaction.TransactionType.EXCHANGE)
                .build();
        
        // Calculate amounts
        transaction.calculateAmounts();
        
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
    
    public Double getTotalWeightExchangedByMetal(String metalType) {
        return billTransactionRepository.getTotalWeightExchangedByMetal(metalType);
    }
    
    public List<Object[]> getTopSellingItems() {
        return billTransactionRepository.getTopSellingItems();
    }
    
    public List<Object[]> getSalesByMetal() {
        return billTransactionRepository.getSalesByMetal();
    }
}