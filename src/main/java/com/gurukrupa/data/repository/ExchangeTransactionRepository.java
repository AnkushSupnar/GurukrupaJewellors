package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.ExchangeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExchangeTransactionRepository extends JpaRepository<ExchangeTransaction, Long> {
    
    List<ExchangeTransaction> findByExchangeId(Long exchangeId);
    
    @Query("SELECT et FROM ExchangeTransaction et WHERE et.exchange.id = :exchangeId ORDER BY et.createdDate")
    List<ExchangeTransaction> findByExchangeIdOrderByCreatedDate(@Param("exchangeId") Long exchangeId);
    
    @Query("SELECT COUNT(et) FROM ExchangeTransaction et WHERE et.exchange.id = :exchangeId")
    Long countByExchangeId(@Param("exchangeId") Long exchangeId);
    
    @Query("SELECT SUM(et.totalAmount) FROM ExchangeTransaction et WHERE et.exchange.id = :exchangeId")
    Double getTotalExchangeAmountByExchangeId(@Param("exchangeId") Long exchangeId);
    
    @Query("SELECT DISTINCT et.itemName FROM ExchangeTransaction et WHERE et.itemName IS NOT NULL ORDER BY et.itemName")
    List<String> findDistinctItemNames();
    
    @Query("SELECT DISTINCT et.itemName FROM ExchangeTransaction et WHERE et.itemName IS NOT NULL AND LOWER(et.itemName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY et.itemName")
    List<String> findDistinctItemNamesBySearchTerm(@Param("searchTerm") String searchTerm);
}