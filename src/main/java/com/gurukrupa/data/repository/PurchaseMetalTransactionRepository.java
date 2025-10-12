package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.PurchaseMetalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseMetalTransactionRepository extends JpaRepository<PurchaseMetalTransaction, Long> {

    /**
     * Find all transactions for a specific purchase invoice
     */
    List<PurchaseMetalTransaction> findByPurchaseInvoiceId(Long purchaseInvoiceId);

    /**
     * Find all transactions for a specific metal type
     */
    List<PurchaseMetalTransaction> findByMetalType(String metalType);

    /**
     * Find all transactions for a specific metal type and purity
     */
    List<PurchaseMetalTransaction> findByMetalTypeAndPurity(String metalType, BigDecimal purity);

    /**
     * Get total gross weight purchased for a metal type
     */
    @Query("SELECT SUM(t.grossWeight) FROM PurchaseMetalTransaction t WHERE t.metalType = :metalType")
    BigDecimal getTotalGrossWeightByMetalType(@Param("metalType") String metalType);

    /**
     * Get total net weight charged for a metal type
     */
    @Query("SELECT SUM(t.netWeightCharged) FROM PurchaseMetalTransaction t WHERE t.metalType = :metalType")
    BigDecimal getTotalNetWeightByMetalType(@Param("metalType") String metalType);

    /**
     * Get total amount spent on a metal type
     */
    @Query("SELECT SUM(t.totalAmount) FROM PurchaseMetalTransaction t WHERE t.metalType = :metalType")
    BigDecimal getTotalAmountByMetalType(@Param("metalType") String metalType);

    /**
     * Find transactions by date range
     */
    @Query("SELECT t FROM PurchaseMetalTransaction t WHERE t.createdDate BETWEEN :startDate AND :endDate")
    List<PurchaseMetalTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Get total purchases for today
     */
    @Query("SELECT SUM(t.totalAmount) FROM PurchaseMetalTransaction t WHERE DATE(t.createdDate) = CURRENT_DATE")
    BigDecimal getTodaysTotalPurchases();

    /**
     * Delete all transactions for a specific invoice
     */
    void deleteByPurchaseInvoiceId(Long purchaseInvoiceId);
}
