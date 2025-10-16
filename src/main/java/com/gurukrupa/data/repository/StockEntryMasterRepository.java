package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.StockEntryMaster;
import com.gurukrupa.data.entities.PurchaseInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockEntryMasterRepository extends JpaRepository<StockEntryMaster, Long> {

    /**
     * Find stock entry by entry number
     */
    Optional<StockEntryMaster> findByEntryNumber(String entryNumber);

    /**
     * Find all stock entries for a purchase invoice
     * IMPORTANT: Multiple entries can exist for the same invoice (partial consumption)
     */
    List<StockEntryMaster> findByPurchaseInvoiceAndStatus(PurchaseInvoice purchaseInvoice, StockEntryMaster.EntryStatus status);

    /**
     * Find all stock entries for a purchase invoice by ID
     */
    @Query("SELECT s FROM StockEntryMaster s WHERE s.purchaseInvoice.id = :invoiceId AND s.status = 'ACTIVE'")
    List<StockEntryMaster> findByPurchaseInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Find all stock entries by status
     */
    List<StockEntryMaster> findByStatus(StockEntryMaster.EntryStatus status);

    /**
     * Find all stock entries by date range
     */
    @Query("SELECT s FROM StockEntryMaster s WHERE s.entryDate BETWEEN :startDate AND :endDate ORDER BY s.entryDate DESC")
    List<StockEntryMaster> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find all active stock entries ordered by entry date descending
     */
    List<StockEntryMaster> findByStatusOrderByEntryDateDesc(StockEntryMaster.EntryStatus status);

    /**
     * Find stock entries by purchase invoice supplier
     */
    @Query("SELECT s FROM StockEntryMaster s WHERE s.purchaseInvoice.supplier.id = :supplierId ORDER BY s.entryDate DESC")
    List<StockEntryMaster> findBySupplier(@Param("supplierId") Long supplierId);

    /**
     * Get latest entry number for generating next entry number
     */
    @Query("SELECT s.entryNumber FROM StockEntryMaster s ORDER BY s.createdDate DESC")
    List<String> findLatestEntryNumber();

    /**
     * Count total stock entries
     */
    long countByStatus(StockEntryMaster.EntryStatus status);

    /**
     * Find entries by search term (entry number or purchase invoice number)
     */
    @Query("SELECT s FROM StockEntryMaster s WHERE " +
           "LOWER(s.entryNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.purchaseInvoice.invoiceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY s.entryDate DESC")
    List<StockEntryMaster> searchEntries(@Param("searchTerm") String searchTerm);
}
