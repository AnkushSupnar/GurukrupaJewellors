package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.StockTransaction;
import com.gurukrupa.data.entities.StockTransaction.TransactionType;
import com.gurukrupa.data.entities.StockTransaction.TransactionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    
    // Find by jewelry item
    List<StockTransaction> findByJewelryItemIdOrderByTransactionDateDesc(Long jewelryItemId);
    
    // Find by reference
    List<StockTransaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    
    Optional<StockTransaction> findByReferenceTypeAndReferenceNumber(String referenceType, String referenceNumber);
    
    // Find by transaction type
    List<StockTransaction> findByTransactionType(TransactionType transactionType);
    
    // Find by transaction source
    List<StockTransaction> findByTransactionSource(TransactionSource transactionSource);
    
    // Find by date range
    List<StockTransaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find by jewelry item and date range
    List<StockTransaction> findByJewelryItemIdAndTransactionDateBetween(
        Long jewelryItemId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Get latest transaction for an item
    @Query("SELECT st FROM StockTransaction st WHERE st.jewelryItem.id = :itemId ORDER BY st.transactionDate DESC, st.id DESC")
    List<StockTransaction> findLatestByJewelryItemId(@Param("itemId") Long itemId);
    
    // Calculate total IN quantity for an item
    @Query("SELECT COALESCE(SUM(st.quantity), 0) FROM StockTransaction st WHERE st.jewelryItem.id = :itemId AND st.transactionType = 'IN'")
    Integer getTotalInQuantityByItemId(@Param("itemId") Long itemId);
    
    // Calculate total OUT quantity for an item
    @Query("SELECT COALESCE(SUM(st.quantity), 0) FROM StockTransaction st WHERE st.jewelryItem.id = :itemId AND st.transactionType = 'OUT'")
    Integer getTotalOutQuantityByItemId(@Param("itemId") Long itemId);
    
    // Get stock transactions by item code
    @Query("SELECT st FROM StockTransaction st WHERE st.jewelryItem.itemCode = :itemCode ORDER BY st.transactionDate DESC")
    List<StockTransaction> findByItemCode(@Param("itemCode") String itemCode);
    
    // Get all sale transactions for date range
    @Query("SELECT st FROM StockTransaction st WHERE st.transactionSource = 'SALE' AND st.transactionDate BETWEEN :startDate AND :endDate ORDER BY st.transactionDate DESC")
    List<StockTransaction> findSaleTransactionsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Check if transaction exists for a bill
    @Query("SELECT CASE WHEN COUNT(st) > 0 THEN true ELSE false END FROM StockTransaction st WHERE st.referenceType = 'BILL' AND st.referenceId = :billId")
    boolean existsByBillId(@Param("billId") Long billId);
    
    // Get all transactions for multiple items
    @Query("SELECT st FROM StockTransaction st WHERE st.jewelryItem.id IN :itemIds ORDER BY st.transactionDate DESC")
    List<StockTransaction> findByJewelryItemIds(@Param("itemIds") List<Long> itemIds);
    
    // Get stock movement summary for an item
    @Query("SELECT st.transactionType, st.transactionSource, COUNT(st), SUM(st.quantity) FROM StockTransaction st WHERE st.jewelryItem.id = :itemId GROUP BY st.transactionType, st.transactionSource")
    List<Object[]> getStockMovementSummaryByItemId(@Param("itemId") Long itemId);
    
    // Get today's transactions
    @Query("SELECT st FROM StockTransaction st WHERE DATE(st.transactionDate) = DATE(CURRENT_DATE) ORDER BY st.transactionDate DESC")
    List<StockTransaction> findTodaysTransactions();
    
    // Count transactions by source for date range
    @Query("SELECT st.transactionSource, COUNT(st), SUM(st.quantity) FROM StockTransaction st WHERE st.transactionDate BETWEEN :startDate AND :endDate GROUP BY st.transactionSource")
    List<Object[]> getTransactionSummaryBySource(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}