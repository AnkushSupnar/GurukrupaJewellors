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
    
    List<BillTransaction> findByItemCodeContainingIgnoreCase(String itemCode);
    
    List<BillTransaction> findByItemNameContainingIgnoreCase(String itemName);
    
    List<BillTransaction> findByMetalType(String metalType);
    
    @Query("SELECT bt FROM BillTransaction bt WHERE bt.createdDate BETWEEN :startDate AND :endDate")
    List<BillTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(bt) FROM BillTransaction bt WHERE bt.itemCode = :itemCode")
    Integer getTotalQuantitySoldByItemCode(@Param("itemCode") String itemCode);
    
    @Query("SELECT COALESCE(SUM(bt.weight), 0) FROM BillTransaction bt WHERE bt.metalType = :metalType")
    Double getTotalWeightSoldByMetal(@Param("metalType") String metalType);
    
    @Query("SELECT bt.itemName, COUNT(bt) as totalCount FROM BillTransaction bt GROUP BY bt.itemName ORDER BY totalCount DESC")
    List<Object[]> getTopSellingItems();
    
    @Query("SELECT bt.metalType, COALESCE(SUM(bt.weight), 0) as totalWeight FROM BillTransaction bt GROUP BY bt.metalType ORDER BY totalWeight DESC")
    List<Object[]> getSalesByMetal();
}