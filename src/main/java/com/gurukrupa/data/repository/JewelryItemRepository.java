package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.JewelryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface JewelryItemRepository extends JpaRepository<JewelryItem, Long> {
    
    // Find by item code (unique identifier)
    Optional<JewelryItem> findByItemCode(String itemCode);
    
    // Find by item name (partial match)
    List<JewelryItem> findByItemNameContainingIgnoreCase(String itemName);
    
    // Find by category
    List<JewelryItem> findByCategory(String category);
    
    // Find by metal type
    List<JewelryItem> findByMetalType(String metalType);
    
    // Find by purity
    List<JewelryItem> findByPurity(BigDecimal purity);
    
    // Find active items only
    List<JewelryItem> findByIsActiveTrue();
    
    // Find by category and metal type
    List<JewelryItem> findByCategoryAndMetalType(String category, String metalType);
    
    // Find by weight range
    @Query("SELECT j FROM JewelryItem j WHERE j.grossWeight BETWEEN :minWeight AND :maxWeight AND j.isActive = true")
    List<JewelryItem> findByWeightRange(@Param("minWeight") BigDecimal minWeight, @Param("maxWeight") BigDecimal maxWeight);
    
    // Find by price range
    @Query("SELECT j FROM JewelryItem j WHERE j.totalAmount BETWEEN :minPrice AND :maxPrice AND j.isActive = true")
    List<JewelryItem> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    // Search items by multiple criteria
    @Query("SELECT j FROM JewelryItem j WHERE " +
           "(:itemName IS NULL OR LOWER(j.itemName) LIKE LOWER(CONCAT('%', :itemName, '%'))) AND " +
           "(:category IS NULL OR j.category = :category) AND " +
           "(:metalType IS NULL OR j.metalType = :metalType) AND " +
           "(:purity IS NULL OR j.purity = :purity) AND " +
           "j.isActive = true")
    List<JewelryItem> searchItems(@Param("itemName") String itemName,
                                 @Param("category") String category,
                                 @Param("metalType") String metalType,
                                 @Param("purity") BigDecimal purity);
    
    // Get all distinct categories
    @Query("SELECT DISTINCT j.category FROM JewelryItem j WHERE j.isActive = true ORDER BY j.category")
    List<String> findDistinctCategories();
    
    // Get all distinct purities
    @Query("SELECT DISTINCT j.purity FROM JewelryItem j WHERE j.isActive = true ORDER BY j.purity DESC")
    List<BigDecimal> findDistinctPurities();

    // Get all distinct metal types
    @Query("SELECT DISTINCT j.metalType FROM JewelryItem j WHERE j.isActive = true ORDER BY j.metalType")
    List<String> findDistinctMetalTypes();

    // Get items with low stock (quantity <= threshold)
    @Query("SELECT j FROM JewelryItem j WHERE j.quantity <= :threshold AND j.isActive = true")
    List<JewelryItem> findLowStockItems(@Param("threshold") Integer threshold);
    
    // Get items by availability (in stock)
    @Query("SELECT j FROM JewelryItem j WHERE j.quantity > 0 AND j.isActive = true")
    List<JewelryItem> findAvailableItems();
    
    // Custom query for autocomplete item names
    @Query("SELECT j.itemName FROM JewelryItem j WHERE j.isActive = true AND LOWER(j.itemName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findItemNamesForAutoComplete(@Param("query") String query);
    
    // Get items by item code for autocomplete
    @Query("SELECT j.itemCode FROM JewelryItem j WHERE j.isActive = true AND LOWER(j.itemCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findItemCodesForAutoComplete(@Param("query") String query);
}