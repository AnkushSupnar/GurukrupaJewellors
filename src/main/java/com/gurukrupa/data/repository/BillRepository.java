package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    
    Optional<Bill> findByBillNumber(String billNumber);
    
    List<Bill> findByCustomerId(Long customerId);
    
    @Query("SELECT b FROM Bill b WHERE LOWER(CONCAT(b.customer.firstName, ' ', COALESCE(b.customer.middleName, ''), ' ', COALESCE(b.customer.lastName, ''))) LIKE LOWER(CONCAT('%', :customerName, '%'))")
    List<Bill> findByCustomerNameContainingIgnoreCase(@Param("customerName") String customerName);
    
    @Query("SELECT b FROM Bill b WHERE b.customer.mobile = :mobile")
    List<Bill> findByCustomerMobile(@Param("mobile") String mobile);
    
    List<Bill> findByStatus(Bill.BillStatus status);
    
    List<Bill> findByPaymentMethod(Bill.PaymentMethod paymentMethod);
    
    List<Bill> findByBillDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT b FROM Bill b WHERE b.createdDate BETWEEN :startDate AND :endDate ORDER BY b.createdDate DESC")
    List<Bill> findBillsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Bill b WHERE DATE(b.billDate) = DATE(:date) ORDER BY b.billDate DESC")
    List<Bill> findBillsByDate(@Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(b) FROM Bill b WHERE DATE(b.billDate) = DATE(CURRENT_DATE)")
    Long countTodaysBills();
    
    @Query("SELECT COALESCE(SUM(b.grandTotal), 0) FROM Bill b WHERE DATE(b.billDate) = DATE(CURRENT_DATE) AND b.status IN ('PAID', 'CONFIRMED')")
    Double getTodaysTotalSales();
    
    @Query("SELECT COALESCE(SUM(b.grandTotal), 0) FROM Bill b WHERE b.billDate BETWEEN :startDate AND :endDate AND b.status IN ('PAID', 'CONFIRMED')")
    Double getSalesByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Bill b ORDER BY b.createdDate DESC")
    List<Bill> findAllOrderByCreatedDateDesc();
    
    List<Bill> findByCustomerIdOrderByBillDateDesc(Long customerId);
    
    List<Bill> findByCustomerIdAndBillDateBetween(Long customerId, LocalDateTime fromDate, LocalDateTime toDate);
    
    @Query("SELECT COALESCE(SUM(b.pendingAmount), 0) FROM Bill b WHERE b.customer.id = :customerId AND b.status != 'CANCELLED'")
    BigDecimal getTotalPendingAmountByCustomerId(@Param("customerId") Long customerId);
    
    // Get today's actual collected amount (paid amounts)
    @Query("SELECT COALESCE(SUM(b.paidAmount), 0) FROM Bill b WHERE DATE(b.billDate) = DATE(CURRENT_DATE) AND b.status IN ('PAID', 'CONFIRMED')")
    Double getTodaysCollectedAmount();
    
    // Get collected amount for date range
    @Query("SELECT COALESCE(SUM(b.paidAmount), 0) FROM Bill b WHERE b.billDate BETWEEN :startDate AND :endDate AND b.status IN ('PAID', 'CONFIRMED')")
    Double getCollectedAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}