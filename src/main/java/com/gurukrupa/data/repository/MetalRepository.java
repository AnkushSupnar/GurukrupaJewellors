package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.Metal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetalRepository extends JpaRepository<Metal, Long> {
    
    // Find by metal name
    Optional<Metal> findByMetalName(String metalName);
    
    // Find by metal type
    List<Metal> findByMetalType(String metalType);
    
    // Find active metals only
    List<Metal> findByIsActiveTrue();
    
    // Find by metal type and active status
    List<Metal> findByMetalTypeAndIsActiveTrue(String metalType);
    
    // Check if metal name exists
    boolean existsByMetalName(String metalName);
    
    // Check if metal name exists excluding a specific id (for updates)
    boolean existsByMetalNameAndIdNot(String metalName, Long id);
    
    // Get distinct metal types
    @Query("SELECT DISTINCT m.metalType FROM Metal m WHERE m.isActive = true ORDER BY m.metalType")
    List<String> findDistinctMetalTypes();
    
    // Search metals by name or type
    @Query("SELECT m FROM Metal m WHERE " +
           "(LOWER(m.metalName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.metalType) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "m.isActive = true")
    List<Metal> searchMetals(@Param("query") String query);
    
    // Get metal names for dropdown
    @Query("SELECT m.metalName FROM Metal m WHERE m.isActive = true ORDER BY m.metalType, m.purity DESC")
    List<String> findAllMetalNames();
}