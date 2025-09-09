package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.Exchange;
import com.gurukrupa.data.entities.Exchange.ExchangeStatus;
import com.gurukrupa.data.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
    
    Optional<Exchange> findByExchangeNumber(String exchangeNumber);
    
    List<Exchange> findByCustomer(Customer customer);
    
    List<Exchange> findByStatus(ExchangeStatus status);
    
    List<Exchange> findByCustomerAndStatus(Customer customer, ExchangeStatus status);
    
    @Query("SELECT e FROM Exchange e WHERE e.exchangeDate BETWEEN :startDate AND :endDate")
    List<Exchange> findByExchangeDateBetween(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT e FROM Exchange e WHERE e.customer = :customer AND e.exchangeDate BETWEEN :startDate AND :endDate")
    List<Exchange> findByCustomerAndExchangeDateBetween(@Param("customer") Customer customer,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(e) FROM Exchange e WHERE e.exchangeNumber LIKE :prefix%")
    long countByExchangeNumberPrefix(@Param("prefix") String prefix);
    
    @Query("SELECT e FROM Exchange e WHERE e.customer.id = :customerId AND e.status = 'ACTIVE' ORDER BY e.exchangeDate DESC")
    List<Exchange> findActiveExchangesByCustomerId(@Param("customerId") Long customerId);
    
    List<Exchange> findByBillId(Long billId);
    
    @Query("SELECT e FROM Exchange e WHERE e.bill.id = :billId")
    Optional<Exchange> findByBillIdOptional(@Param("billId") Long billId);
}