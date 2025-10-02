package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByCategoryName(String categoryName);
    
    List<Category> findByIsActiveTrue();
    
    @Query("SELECT c FROM Category c WHERE c.isActive = true ORDER BY c.categoryName")
    List<Category> findAllActiveCategories();
    
    @Query("SELECT c FROM Category c WHERE LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Category> searchCategories(@Param("searchTerm") String searchTerm);
    
    boolean existsByCategoryName(String categoryName);
    
    @Query("SELECT c FROM Category c WHERE LOWER(c.categoryName) = LOWER(:categoryName) AND c.id != :excludeId")
    Optional<Category> findByCategoryNameExcludingId(@Param("categoryName") String categoryName, @Param("excludeId") Long excludeId);
}