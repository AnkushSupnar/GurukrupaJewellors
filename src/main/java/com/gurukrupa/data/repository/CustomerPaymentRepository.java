package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, Long> {

    /**
     * Find payment by receipt number
     */
    Optional<CustomerPayment> findByReceiptNumber(String receiptNumber);

    /**
     * Find all payments for a customer
     */
    List<CustomerPayment> findByCustomerIdOrderByPaymentDateDesc(Long customerId);

    /**
     * Find all payments ordered by date (newest first)
     */
    List<CustomerPayment> findAllByOrderByPaymentDateDesc();

    /**
     * Find payments within date range
     */
    @Query("SELECT cp FROM CustomerPayment cp WHERE cp.paymentDate BETWEEN :startDate AND :endDate ORDER BY cp.paymentDate DESC")
    List<CustomerPayment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get total payments received from a customer
     */
    @Query("SELECT COALESCE(SUM(cp.paymentAmount), 0) FROM CustomerPayment cp WHERE cp.customer.id = :customerId")
    BigDecimal getTotalPaymentsByCustomer(@Param("customerId") Long customerId);

    /**
     * Get count of payments in a date range
     */
    @Query("SELECT COUNT(cp) FROM CustomerPayment cp WHERE cp.paymentDate BETWEEN :startDate AND :endDate")
    Long countPaymentsByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
}
