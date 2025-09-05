package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.BillTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillTransactionRepository extends JpaRepository<BillTransaction, Long> {
    
    List<BillTransaction> findByBillId(Long billId);
    
    List<BillTransaction> findByTransactionType(BillTransaction.TransactionType transactionType);
    
    List<BillTransaction> findByItemCodeContainingIgnoreCase(String itemCode);
    
    List<BillTransaction> findByItemNameContainingIgnoreCase(String itemName);
    
    List<BillTransaction> findByMetalType(String metalType);
    
    @Query("SELECT bt FROM BillTransaction bt WHERE bt.bill.id = :billId AND bt.transactionType = :transactionType")
    List<BillTransaction> findByBillIdAndTransactionType(@Param("billId") Long billId, 
                                                          @Param("transactionType") BillTransaction.TransactionType transactionType);
    
    @Query("SELECT bt FROM BillTransaction bt WHERE bt.createdDate BETWEEN :startDate AND :endDate")
    List<BillTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COALESCE(SUM(bt.quantity), 0) FROM BillTransaction bt WHERE bt.itemCode = :itemCode AND bt.transactionType = 'SALE'")
    Integer getTotalQuantitySoldByItemCode(@Param("itemCode") String itemCode);
    
    @Query("SELECT COALESCE(SUM(bt.totalWeight), 0) FROM BillTransaction bt WHERE bt.metalType = :metalType AND bt.transactionType = 'SALE'")
    Double getTotalWeightSoldByMetal(@Param("metalType") String metalType);
    
    @Query("SELECT COALESCE(SUM(bt.totalWeight), 0) FROM BillTransaction bt WHERE bt.metalType = :metalType AND bt.transactionType = 'EXCHANGE'")
    Double getTotalWeightExchangedByMetal(@Param("metalType") String metalType);
    
    @Query("SELECT bt.itemName, COALESCE(SUM(bt.quantity), 0) as totalQuantity FROM BillTransaction bt WHERE bt.transactionType = 'SALE' GROUP BY bt.itemName ORDER BY totalQuantity DESC")
    List<Object[]> getTopSellingItems();
    
    @Query("SELECT bt.metalType, COALESCE(SUM(bt.totalWeight), 0) as totalWeight FROM BillTransaction bt WHERE bt.transactionType = 'SALE' GROUP BY bt.metalType ORDER BY totalWeight DESC")
    List<Object[]> getSalesByMetal();
}