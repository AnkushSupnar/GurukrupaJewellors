package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.PurchaseTransaction;
import com.gurukrupa.data.entities.PurchaseTransaction.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, Long> {
    
    List<PurchaseTransaction> findByPurchaseInvoiceId(Long purchaseInvoiceId);
    
    List<PurchaseTransaction> findByItemType(ItemType itemType);
    
    List<PurchaseTransaction> findByItemCodeContainingIgnoreCase(String itemCode);
    
    List<PurchaseTransaction> findByItemNameContainingIgnoreCase(String itemName);
    
    List<PurchaseTransaction> findByMetalType(String metalType);
    
    @Query("SELECT pt FROM PurchaseTransaction pt WHERE pt.createdDate BETWEEN :startDate AND :endDate ORDER BY pt.createdDate DESC")
    List<PurchaseTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Get total quantity purchased by item code
    @Query("SELECT COALESCE(SUM(pt.quantity), 0) FROM PurchaseTransaction pt WHERE pt.itemCode = :itemCode")
    Integer getTotalQuantityPurchasedByItemCode(@Param("itemCode") String itemCode);
    
    // Get total weight purchased by metal type
    @Query("SELECT COALESCE(SUM(pt.netWeight), 0) FROM PurchaseTransaction pt WHERE pt.metalType = :metalType")
    Double getTotalWeightPurchasedByMetal(@Param("metalType") String metalType);
    
    // Get frequently purchased items
    @Query("SELECT pt.itemCode, pt.itemName, COUNT(pt), SUM(pt.quantity) FROM PurchaseTransaction pt GROUP BY pt.itemCode, pt.itemName ORDER BY COUNT(pt) DESC")
    List<Object[]> getFrequentlyPurchasedItems();
    
    // Get purchases by metal type
    @Query("SELECT pt.metalType, COUNT(pt), SUM(pt.totalAmount) FROM PurchaseTransaction pt GROUP BY pt.metalType")
    List<Object[]> getPurchasesByMetal();
    
    // Find exchange item purchases
    @Query("SELECT pt FROM PurchaseTransaction pt WHERE pt.itemType = 'EXCHANGE_ITEM' ORDER BY pt.createdDate DESC")
    List<PurchaseTransaction> findExchangeItemPurchases();
    
    // Find purchase transactions by exchange item ID
    List<PurchaseTransaction> findByExchangeItemId(Long exchangeItemId);
    
    // Get total amount for exchange items
    @Query("SELECT COALESCE(SUM(pt.totalAmount), 0) FROM PurchaseTransaction pt WHERE pt.itemType = 'EXCHANGE_ITEM'")
    Double getTotalExchangeItemsPurchaseAmount();
}