package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.ExchangeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExchangeTransactionRepository extends JpaRepository<ExchangeTransaction, Long> {
    
    List<ExchangeTransaction> findByBillId(Long billId);
    
    @Query("SELECT et FROM ExchangeTransaction et WHERE et.bill.id = :billId ORDER BY et.createdDate")
    List<ExchangeTransaction> findByBillIdOrderByCreatedDate(@Param("billId") Long billId);
    
    @Query("SELECT COUNT(et) FROM ExchangeTransaction et WHERE et.bill.id = :billId")
    Long countByBillId(@Param("billId") Long billId);
    
    @Query("SELECT SUM(et.totalAmount) FROM ExchangeTransaction et WHERE et.bill.id = :billId")
    Double getTotalExchangeAmountByBillId(@Param("billId") Long billId);
}