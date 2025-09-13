package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.ExchangeMetalStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeMetalStockRepository extends JpaRepository<ExchangeMetalStock, Long> {
    
    Optional<ExchangeMetalStock> findByMetalTypeAndPurity(String metalType, BigDecimal purity);
    
    List<ExchangeMetalStock> findByMetalType(String metalType);
    
    @Query("SELECT ems FROM ExchangeMetalStock ems WHERE ems.availableWeight > 0 ORDER BY ems.metalType, ems.purity DESC")
    List<ExchangeMetalStock> findAllWithAvailableStock();
    
    @Query("SELECT ems FROM ExchangeMetalStock ems ORDER BY ems.metalType, ems.purity DESC")
    List<ExchangeMetalStock> findAllOrderByMetalTypeAndPurity();
    
    @Query("SELECT COALESCE(SUM(ems.totalWeight), 0) FROM ExchangeMetalStock ems WHERE ems.metalType = :metalType")
    BigDecimal getTotalWeightByMetalType(@Param("metalType") String metalType);
    
    @Query("SELECT COALESCE(SUM(ems.availableWeight), 0) FROM ExchangeMetalStock ems WHERE ems.metalType = :metalType")
    BigDecimal getAvailableWeightByMetalType(@Param("metalType") String metalType);
    
    @Query("SELECT COALESCE(SUM(ems.soldWeight), 0) FROM ExchangeMetalStock ems WHERE ems.metalType = :metalType")
    BigDecimal getSoldWeightByMetalType(@Param("metalType") String metalType);
    
    @Query("SELECT COALESCE(SUM(ems.totalWeight), 0) FROM ExchangeMetalStock ems")
    BigDecimal getTotalWeight();
    
    @Query("SELECT COALESCE(SUM(ems.availableWeight), 0) FROM ExchangeMetalStock ems")
    BigDecimal getTotalAvailableWeight();
    
    @Query("SELECT COALESCE(SUM(ems.soldWeight), 0) FROM ExchangeMetalStock ems")
    BigDecimal getTotalSoldWeight();
    
    @Query("SELECT ems.metalType, COUNT(ems), SUM(ems.totalWeight), SUM(ems.availableWeight), SUM(ems.soldWeight) " +
           "FROM ExchangeMetalStock ems GROUP BY ems.metalType ORDER BY ems.metalType")
    List<Object[]> getStockSummaryByMetal();
    
    @Query("SELECT ems FROM ExchangeMetalStock ems WHERE ems.availableWeight >= :minWeight ORDER BY ems.metalType, ems.purity DESC")
    List<ExchangeMetalStock> findByMinimumAvailableWeight(@Param("minWeight") BigDecimal minWeight);
    
    @Query("SELECT CASE WHEN COUNT(ems) > 0 THEN true ELSE false END FROM ExchangeMetalStock ems " +
           "WHERE ems.metalType = :metalType AND ems.purity = :purity AND ems.availableWeight >= :requiredWeight")
    boolean hasAvailableStock(@Param("metalType") String metalType, 
                             @Param("purity") BigDecimal purity, 
                             @Param("requiredWeight") BigDecimal requiredWeight);
}