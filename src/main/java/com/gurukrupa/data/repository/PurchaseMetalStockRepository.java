package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.entities.PurchaseMetalStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseMetalStockRepository extends JpaRepository<PurchaseMetalStock, Long> {

    /**
     * Find metal stock by Metal entity reference
     */
    Optional<PurchaseMetalStock> findByMetal(Metal metal);

    /**
     * Find metal stock by metal ID
     */
    Optional<PurchaseMetalStock> findByMetal_Id(Long metalId);

    /**
     * Find metal stock by metal type and purity (backward compatibility)
     */
    Optional<PurchaseMetalStock> findByMetalTypeAndPurity(String metalType, BigDecimal purity);

    /**
     * Find all stock for a specific metal type
     */
    List<PurchaseMetalStock> findByMetalType(String metalType);

    /**
     * Find all stock ordered by metal type and purity
     */
    List<PurchaseMetalStock> findAllByOrderByMetalTypeAscPurityDesc();

    /**
     * Get total available weight for a metal type
     */
    @Query("SELECT SUM(p.availableWeight) FROM PurchaseMetalStock p WHERE p.metalType = :metalType")
    BigDecimal getTotalAvailableWeightByMetalType(@Param("metalType") String metalType);

    /**
     * Get total available weight for a metal type and purity
     */
    @Query("SELECT p.availableWeight FROM PurchaseMetalStock p WHERE p.metalType = :metalType AND p.purity = :purity")
    BigDecimal getAvailableWeight(@Param("metalType") String metalType, @Param("purity") BigDecimal purity);

    /**
     * Find all stock with available weight greater than zero
     */
    @Query("SELECT p FROM PurchaseMetalStock p WHERE p.availableWeight > 0 ORDER BY p.metalType, p.purity DESC")
    List<PurchaseMetalStock> findAllWithAvailableStock();

    /**
     * Check if stock exists for metal type and purity
     */
    boolean existsByMetalTypeAndPurity(String metalType, BigDecimal purity);
}
