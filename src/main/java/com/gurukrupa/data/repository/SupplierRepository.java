package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    
    Optional<Supplier> findByMobile(String mobile);
    
    Optional<Supplier> findByGstNumber(String gstNumber);
    
    List<Supplier> findByIsActiveTrue();
    
    List<Supplier> findByIsActiveTrueOrderBySupplierNameAsc();
    
    @Query("SELECT s FROM Supplier s WHERE s.isActive = true AND " +
           "(LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.companyName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "s.mobile LIKE CONCAT('%', :searchTerm, '%') OR " +
           "s.gstNumber LIKE CONCAT('%', :searchTerm, '%'))")
    List<Supplier> searchActiveSuppliers(@Param("searchTerm") String searchTerm);
    
    List<Supplier> findBySupplierTypeAndIsActiveTrue(String supplierType);
    
    @Query("SELECT s FROM Supplier s WHERE s.isActive = true AND s.currentBalance > 0 ORDER BY s.currentBalance DESC")
    List<Supplier> findSuppliersWithOutstandingBalance();
    
    @Query("SELECT COUNT(s) FROM Supplier s WHERE s.isActive = true")
    Long countActiveSuppliers();
    
    @Query("SELECT COALESCE(SUM(s.currentBalance), 0) FROM Supplier s WHERE s.isActive = true")
    Double getTotalOutstandingAmount();
    
    @Query("SELECT s.supplierName FROM Supplier s WHERE s.isActive = true ORDER BY s.supplierName")
    List<String> findAllSupplierNames();
    
    @Query("SELECT CONCAT(s.supplierName, ' - ', s.mobile) FROM Supplier s WHERE s.isActive = true ORDER BY s.supplierName")
    List<String> findAllSupplierNamesWithMobile();
}