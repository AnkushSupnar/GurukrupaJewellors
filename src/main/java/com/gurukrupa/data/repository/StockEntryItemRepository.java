package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.StockEntryItem;
import com.gurukrupa.data.entities.StockEntryMaster;
import com.gurukrupa.data.entities.JewelryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockEntryItemRepository extends JpaRepository<StockEntryItem, Long> {

    /**
     * Find all items for a specific stock entry
     */
    List<StockEntryItem> findByStockEntry(StockEntryMaster stockEntry);

    /**
     * Find all items by stock entry ID
     */
    List<StockEntryItem> findByStockEntryId(Long stockEntryId);

    /**
     * Find all entries containing a specific jewelry item
     */
    List<StockEntryItem> findByJewelryItem(JewelryItem jewelryItem);

    /**
     * Count items in a stock entry
     */
    long countByStockEntry(StockEntryMaster stockEntry);

    /**
     * Delete all items for a stock entry
     */
    void deleteByStockEntry(StockEntryMaster stockEntry);

    /**
     * Get total quantity of a specific jewelry item across all stock entries
     */
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM StockEntryItem s WHERE s.jewelryItem.id = :jewelryItemId")
    Integer getTotalQuantityByJewelryItem(@Param("jewelryItemId") Long jewelryItemId);

    /**
     * Find items by metal type
     */
    @Query("SELECT s FROM StockEntryItem s WHERE s.jewelryItem.metalType = :metalType")
    List<StockEntryItem> findByMetalType(@Param("metalType") String metalType);

    /**
     * Find items by category
     */
    @Query("SELECT s FROM StockEntryItem s WHERE s.jewelryItem.category = :category")
    List<StockEntryItem> findByCategory(@Param("category") String category);
}
