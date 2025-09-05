package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.entities.MetalRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetalRateRepository extends JpaRepository<MetalRate, Long> {
    
    Optional<MetalRate> findByMetalAndRateDate(Metal metal, LocalDate rateDate);
    
    @Query("SELECT mr FROM MetalRate mr WHERE mr.metal.id = :metalId AND mr.rateDate = :date")
    Optional<MetalRate> findByMetalIdAndDate(@Param("metalId") Long metalId, @Param("date") LocalDate date);
    
    List<MetalRate> findByRateDate(LocalDate rateDate);
    
    @Query("SELECT mr FROM MetalRate mr WHERE mr.rateDate BETWEEN :startDate AND :endDate ORDER BY mr.rateDate DESC")
    List<MetalRate> findByDateRange(@Param("startDate") LocalDate startDate, 
                                    @Param("endDate") LocalDate endDate);
    
    @Query("SELECT mr FROM MetalRate mr WHERE mr.metal.id = :metalId ORDER BY mr.rateDate DESC")
    List<MetalRate> findByMetalIdOrderByDateDesc(@Param("metalId") Long metalId);
    
    @Query("SELECT mr FROM MetalRate mr WHERE mr.metal.id = :metalId AND mr.rateDate <= :date ORDER BY mr.rateDate DESC")
    List<MetalRate> findLatestRateForMetal(@Param("metalId") Long metalId, @Param("date") LocalDate date);
    
    @Query("SELECT mr FROM MetalRate mr JOIN FETCH mr.metal WHERE mr.rateDate = :date")
    List<MetalRate> findByRateDateWithMetal(@Param("date") LocalDate date);
    
    @Query("SELECT mr FROM MetalRate mr WHERE mr.metal.metalType = :metalType AND mr.rateDate = :date")
    List<MetalRate> findByMetalTypeAndDate(@Param("metalType") String metalType, @Param("date") LocalDate date);
}