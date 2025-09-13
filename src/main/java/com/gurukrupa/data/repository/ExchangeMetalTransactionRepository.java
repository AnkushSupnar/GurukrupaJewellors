package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.ExchangeMetalTransaction;
import com.gurukrupa.data.entities.ExchangeMetalTransaction.TransactionType;
import com.gurukrupa.data.entities.ExchangeMetalTransaction.TransactionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExchangeMetalTransactionRepository extends JpaRepository<ExchangeMetalTransaction, Long> {
    
    List<ExchangeMetalTransaction> findByMetalStockIdOrderByTransactionDateDesc(Long metalStockId);
    
    List<ExchangeMetalTransaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    
    List<ExchangeMetalTransaction> findByReferenceNumber(String referenceNumber);
    
    List<ExchangeMetalTransaction> findByTransactionType(TransactionType transactionType);
    
    List<ExchangeMetalTransaction> findByTransactionSource(TransactionSource transactionSource);
    
    List<ExchangeMetalTransaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT emt FROM ExchangeMetalTransaction emt WHERE emt.metalStock.metalType = :metalType " +
           "ORDER BY emt.transactionDate DESC")
    List<ExchangeMetalTransaction> findByMetalType(@Param("metalType") String metalType);
    
    @Query("SELECT emt FROM ExchangeMetalTransaction emt WHERE emt.metalStock.metalType = :metalType " +
           "AND emt.metalStock.purity = :purity ORDER BY emt.transactionDate DESC")
    List<ExchangeMetalTransaction> findByMetalTypeAndPurity(@Param("metalType") String metalType, 
                                                           @Param("purity") BigDecimal purity);
    
    @Query("SELECT emt FROM ExchangeMetalTransaction emt WHERE DATE(emt.transactionDate) = DATE(CURRENT_DATE) " +
           "ORDER BY emt.transactionDate DESC")
    List<ExchangeMetalTransaction> findTodaysTransactions();
    
    @Query("SELECT emt.transactionSource, COUNT(emt), SUM(emt.weight) FROM ExchangeMetalTransaction emt " +
           "WHERE emt.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY emt.transactionSource")
    List<Object[]> getTransactionSummaryBySource(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT emt FROM ExchangeMetalTransaction emt WHERE emt.party LIKE %:partyName% " +
           "ORDER BY emt.transactionDate DESC")
    List<ExchangeMetalTransaction> findByPartyName(@Param("partyName") String partyName);
    
    @Query("SELECT CASE WHEN COUNT(emt) > 0 THEN true ELSE false END FROM ExchangeMetalTransaction emt " +
           "WHERE emt.referenceType = 'EXCHANGE' AND emt.referenceId = :exchangeId")
    boolean existsByExchangeId(@Param("exchangeId") Long exchangeId);
    
    @Query("SELECT CASE WHEN COUNT(emt) > 0 THEN true ELSE false END FROM ExchangeMetalTransaction emt " +
           "WHERE emt.referenceType = 'PURCHASE_INVOICE' AND emt.referenceId = :invoiceId")
    boolean existsByPurchaseInvoiceId(@Param("invoiceId") Long invoiceId);
}