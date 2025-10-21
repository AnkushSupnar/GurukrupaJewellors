package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    /**
     * Find payment by receipt number
     */
    Optional<SupplierPayment> findByReceiptNumber(String receiptNumber);

    /**
     * Find all payments for a supplier
     */
    List<SupplierPayment> findBySupplierIdOrderByPaymentDateDesc(Long supplierId);

    /**
     * Find payments by supplier and date range
     */
    @Query("SELECT sp FROM SupplierPayment sp WHERE sp.supplier.id = :supplierId " +
           "AND sp.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sp.paymentDate DESC")
    List<SupplierPayment> findBySupplierAndDateRange(@Param("supplierId") Long supplierId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find all payments ordered by date
     */
    List<SupplierPayment> findAllByOrderByPaymentDateDesc();

    /**
     * Find payments by date range
     */
    @Query("SELECT sp FROM SupplierPayment sp WHERE sp.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sp.paymentDate DESC")
    List<SupplierPayment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Get total payments made to a supplier
     */
    @Query("SELECT COALESCE(SUM(sp.paymentAmount), 0) FROM SupplierPayment sp " +
           "WHERE sp.supplier.id = :supplierId")
    BigDecimal getTotalPaymentsBySupplier(@Param("supplierId") Long supplierId);

    /**
     * Get total payments in a date range
     */
    @Query("SELECT COALESCE(SUM(sp.paymentAmount), 0) FROM SupplierPayment sp " +
           "WHERE sp.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPaymentsByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Count payments for today
     */
    @Query("SELECT COUNT(sp) FROM SupplierPayment sp WHERE DATE(sp.paymentDate) = CURRENT_DATE")
    long countTodayPayments();

    /**
     * Get latest receipt number for generating next number
     */
    @Query("SELECT sp.receiptNumber FROM SupplierPayment sp ORDER BY sp.createdDate DESC LIMIT 1")
    Optional<String> findLatestReceiptNumber();
}
