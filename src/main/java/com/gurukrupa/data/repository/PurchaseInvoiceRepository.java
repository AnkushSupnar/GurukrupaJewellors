package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.PurchaseInvoice;
import com.gurukrupa.data.entities.PurchaseInvoice.InvoiceStatus;
import com.gurukrupa.data.entities.PurchaseInvoice.PurchaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {
    
    Optional<PurchaseInvoice> findByInvoiceNumber(String invoiceNumber);
    
    List<PurchaseInvoice> findBySupplierId(Long supplierId);
    
    List<PurchaseInvoice> findBySupplierIdOrderByInvoiceDateDesc(Long supplierId);
    
    List<PurchaseInvoice> findByStatus(InvoiceStatus status);
    
    List<PurchaseInvoice> findByPurchaseType(PurchaseType purchaseType);
    
    List<PurchaseInvoice> findByInvoiceDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT pi FROM PurchaseInvoice pi WHERE pi.invoiceDate BETWEEN :startDate AND :endDate ORDER BY pi.invoiceDate DESC")
    List<PurchaseInvoice> findInvoicesByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pi FROM PurchaseInvoice pi WHERE DATE(pi.invoiceDate) = DATE(:date) ORDER BY pi.invoiceDate DESC")
    List<PurchaseInvoice> findInvoicesByDate(@Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(pi) FROM PurchaseInvoice pi WHERE DATE(pi.invoiceDate) = DATE(CURRENT_DATE)")
    Long countTodaysInvoices();
    
    @Query("SELECT COALESCE(SUM(pi.grandTotal), 0) FROM PurchaseInvoice pi WHERE DATE(pi.invoiceDate) = DATE(CURRENT_DATE) AND pi.status IN ('PAID', 'CONFIRMED')")
    Double getTodaysTotalPurchases();
    
    @Query("SELECT COALESCE(SUM(pi.grandTotal), 0) FROM PurchaseInvoice pi WHERE pi.invoiceDate BETWEEN :startDate AND :endDate AND pi.status IN ('PAID', 'CONFIRMED')")
    Double getPurchasesByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pi FROM PurchaseInvoice pi ORDER BY pi.createdDate DESC")
    List<PurchaseInvoice> findAllOrderByCreatedDateDesc();
    
    @Query("SELECT pi FROM PurchaseInvoice pi ORDER BY pi.invoiceDate DESC")
    List<PurchaseInvoice> findAllOrderByInvoiceDateDesc();
    
    @Query("SELECT pi FROM PurchaseInvoice pi WHERE LOWER(pi.supplier.supplierName) LIKE LOWER(CONCAT('%', :supplierName, '%'))")
    List<PurchaseInvoice> findBySupplierNameContainingIgnoreCase(@Param("supplierName") String supplierName);
    
    @Query("SELECT pi FROM PurchaseInvoice pi WHERE pi.supplier.mobile = :mobile")
    List<PurchaseInvoice> findBySupplierMobile(@Param("mobile") String mobile);
    
    List<PurchaseInvoice> findBySupplierIdAndInvoiceDateBetween(Long supplierId, LocalDateTime fromDate, LocalDateTime toDate);
    
    @Query("SELECT COALESCE(SUM(pi.pendingAmount), 0) FROM PurchaseInvoice pi WHERE pi.supplier.id = :supplierId AND pi.status != 'CANCELLED'")
    BigDecimal getTotalPendingAmountBySupplierId(@Param("supplierId") Long supplierId);
    
    // Get today's actual paid amount
    @Query("SELECT COALESCE(SUM(pi.paidAmount), 0) FROM PurchaseInvoice pi WHERE DATE(pi.invoiceDate) = DATE(CURRENT_DATE) AND pi.status IN ('PAID', 'CONFIRMED')")
    Double getTodaysPaidAmount();
    
    // Get paid amount for date range
    @Query("SELECT COALESCE(SUM(pi.paidAmount), 0) FROM PurchaseInvoice pi WHERE pi.invoiceDate BETWEEN :startDate AND :endDate AND pi.status IN ('PAID', 'CONFIRMED')")
    Double getPaidAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find invoices with exchange items
    @Query("SELECT pi FROM PurchaseInvoice pi WHERE pi.purchaseType = 'EXCHANGE_ITEMS' ORDER BY pi.invoiceDate DESC")
    List<PurchaseInvoice> findExchangePurchaseInvoices();
    
    // Check if supplier invoice number already exists
    @Query("SELECT CASE WHEN COUNT(pi) > 0 THEN true ELSE false END FROM PurchaseInvoice pi WHERE pi.supplierInvoiceNumber = :supplierInvoiceNumber AND pi.supplier.id = :supplierId")
    boolean existsBySupplierInvoiceNumberAndSupplierId(@Param("supplierInvoiceNumber") String supplierInvoiceNumber, @Param("supplierId") Long supplierId);
}