package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.entities.StockTransaction;
import com.gurukrupa.data.repository.JewelryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JewelryItemService {

    private final JewelryItemRepository jewelryItemRepository;
    
    @Autowired
    private StockTransactionService stockTransactionService;

    @Autowired
    public JewelryItemService(JewelryItemRepository jewelryItemRepository) {
        this.jewelryItemRepository = jewelryItemRepository;
    }
    
    @Autowired
    public void setStockTransactionService(StockTransactionService stockTransactionService) {
        this.stockTransactionService = stockTransactionService;
    }

    // Basic CRUD operations
    public JewelryItem saveJewelryItem(JewelryItem jewelryItem) {
        return jewelryItemRepository.save(jewelryItem);
    }

    public List<JewelryItem> getAllJewelryItems() {
        return jewelryItemRepository.findAll();
    }

    public Optional<JewelryItem> getJewelryItemById(Long id) {
        return jewelryItemRepository.findById(id);
    }

    public void deleteJewelryItemById(Long id) {
        jewelryItemRepository.deleteById(id);
    }

    public boolean jewelryItemExists(Long id) {
        return jewelryItemRepository.existsById(id);
    }

    // Custom search methods
    public Optional<JewelryItem> findByItemCode(String itemCode) {
        return jewelryItemRepository.findByItemCode(itemCode);
    }

    public List<JewelryItem> searchByItemName(String itemName) {
        return jewelryItemRepository.findByItemNameContainingIgnoreCase(itemName);
    }

    public List<JewelryItem> findByCategory(String category) {
        return jewelryItemRepository.findByCategory(category);
    }

    public List<JewelryItem> findByMetalType(String metalType) {
        return jewelryItemRepository.findByMetalType(metalType);
    }

    public List<JewelryItem> findByPurity(BigDecimal purity) {
        return jewelryItemRepository.findByPurity(purity);
    }

    public List<JewelryItem> getAllActiveItems() {
        return jewelryItemRepository.findByIsActiveTrue();
    }

    public List<JewelryItem> findByCategoryAndMetalType(String category, String metalType) {
        return jewelryItemRepository.findByCategoryAndMetalType(category, metalType);
    }

    public List<JewelryItem> findByWeightRange(BigDecimal minWeight, BigDecimal maxWeight) {
        return jewelryItemRepository.findByWeightRange(minWeight, maxWeight);
    }

    public List<JewelryItem> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return jewelryItemRepository.findByPriceRange(minPrice, maxPrice);
    }

    public List<JewelryItem> searchItems(String itemName, String category, 
                                        String metalType, BigDecimal purity) {
        return jewelryItemRepository.searchItems(itemName, category, metalType, purity);
    }

    // Inventory management
    public List<JewelryItem> findLowStockItems(Integer threshold) {
        return jewelryItemRepository.findLowStockItems(threshold);
    }

    public List<JewelryItem> findAvailableItems() {
        return jewelryItemRepository.findAvailableItems();
    }

    // Utility methods
    public List<String> getDistinctCategories() {
        return jewelryItemRepository.findDistinctCategories();
    }

    public List<BigDecimal> getDistinctPurities() {
        return jewelryItemRepository.findDistinctPurities();
    }

    public List<String> getItemNamesForAutoComplete(String query) {
        return jewelryItemRepository.findItemNamesForAutoComplete(query);
    }

    public List<String> getItemCodesForAutoComplete(String query) {
        return jewelryItemRepository.findItemCodesForAutoComplete(query);
    }

    // Business logic methods
    public JewelryItem updateItemQuantity(Long itemId, Integer newQuantity) {
        Optional<JewelryItem> optionalItem = jewelryItemRepository.findById(itemId);
        if (optionalItem.isPresent()) {
            JewelryItem item = optionalItem.get();
            item.setQuantity(newQuantity);
            return jewelryItemRepository.save(item);
        }
        throw new RuntimeException("Jewelry item not found with id: " + itemId);
    }

    public JewelryItem updateGoldRate(Long itemId, BigDecimal newRate) {
        Optional<JewelryItem> optionalItem = jewelryItemRepository.findById(itemId);
        if (optionalItem.isPresent()) {
            JewelryItem item = optionalItem.get();
            item.setGoldRate(newRate);
            item.calculateTotalAmount(); // Recalculate total amount
            return jewelryItemRepository.save(item);
        }
        throw new RuntimeException("Jewelry item not found with id: " + itemId);
    }

    public void deactivateItem(Long itemId) {
        Optional<JewelryItem> optionalItem = jewelryItemRepository.findById(itemId);
        if (optionalItem.isPresent()) {
            JewelryItem item = optionalItem.get();
            item.setIsActive(false);
            jewelryItemRepository.save(item);
        } else {
            throw new RuntimeException("Jewelry item not found with id: " + itemId);
        }
    }

    public void activateItem(Long itemId) {
        Optional<JewelryItem> optionalItem = jewelryItemRepository.findById(itemId);
        if (optionalItem.isPresent()) {
            JewelryItem item = optionalItem.get();
            item.setIsActive(true);
            jewelryItemRepository.save(item);
        } else {
            throw new RuntimeException("Jewelry item not found with id: " + itemId);
        }
    }

    public boolean isItemCodeUnique(String itemCode) {
        return !jewelryItemRepository.findByItemCode(itemCode).isPresent();
    }

    public boolean isItemCodeUnique(String itemCode, Long excludeId) {
        Optional<JewelryItem> existingItem = jewelryItemRepository.findByItemCode(itemCode);
        return !existingItem.isPresent() || existingItem.get().getId().equals(excludeId);
    }

    // Generate unique item code
    public String generateItemCode(String category, String metalType) {
        String prefix = category.substring(0, Math.min(3, category.length())).toUpperCase();
        String metalPrefix = metalType.substring(0, Math.min(2, metalType.length())).toUpperCase();
        
        // Find the next available number
        Long count = jewelryItemRepository.count();
        String baseCode = prefix + metalPrefix;
        String itemCode;
        int counter = 1;
        
        do {
            itemCode = baseCode + String.format("%04d", count + counter);
            counter++;
        } while (!isItemCodeUnique(itemCode));
        
        return itemCode;
    }
    
    // Stock management methods with transaction tracking
    
    /**
     * Reduce stock for a sale (with stock transaction recording)
     */
    public JewelryItem reduceStockForSale(Long itemId, String itemCode, Integer quantity,
                                         Long billId, String billNumber, String customerName) {
        // Use stock transaction service which handles everything
        stockTransactionService.recordBillSale(itemId, itemCode, quantity, billId, billNumber, customerName);
        
        // Return the updated item - try by itemCode first since itemId might be null
        if (itemCode != null) {
            return findByItemCode(itemCode)
                    .orElseThrow(() -> new RuntimeException("Item not found with code: " + itemCode));
        } else if (itemId != null) {
            return getJewelryItemById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found with id: " + itemId));
        } else {
            throw new RuntimeException("Both itemId and itemCode are null");
        }
    }
    
    /**
     * Add stock (for purchases, returns, etc.)
     */
    public JewelryItem addStock(Long itemId, Integer quantity, StockTransaction.TransactionSource source,
                               String referenceType, Long referenceId, String referenceNumber,
                               String description, String createdBy) {
        JewelryItem item = getJewelryItemById(itemId)
                .orElseThrow(() -> new RuntimeException("Jewelry item not found with id: " + itemId));
        
        stockTransactionService.recordStockIn(item, quantity, source, referenceType,
                                            referenceId, referenceNumber, description, createdBy);
        
        return getJewelryItemById(itemId).orElse(item);
    }
    
    /**
     * Reduce stock (general method for non-sale reductions)
     */
    public JewelryItem reduceStock(Long itemId, Integer quantity, StockTransaction.TransactionSource source,
                                  String referenceType, Long referenceId, String referenceNumber,
                                  String description, String createdBy) {
        JewelryItem item = getJewelryItemById(itemId)
                .orElseThrow(() -> new RuntimeException("Jewelry item not found with id: " + itemId));
        
        stockTransactionService.recordStockOut(item, quantity, source, referenceType,
                                             referenceId, referenceNumber, description, createdBy);
        
        return getJewelryItemById(itemId).orElse(item);
    }
    
    /**
     * Adjust stock (for corrections)
     */
    public JewelryItem adjustStock(Long itemId, Integer newQuantity, String reason, String createdBy) {
        JewelryItem item = getJewelryItemById(itemId)
                .orElseThrow(() -> new RuntimeException("Jewelry item not found with id: " + itemId));
        
        stockTransactionService.recordStockAdjustment(item, newQuantity, reason, createdBy);
        
        return getJewelryItemById(itemId).orElse(item);
    }
    
    /**
     * Get stock transaction history for an item
     */
    public List<StockTransaction> getStockHistory(Long itemId) {
        return stockTransactionService.getItemTransactionHistory(itemId);
    }
    
    /**
     * Check if item has sufficient stock for sale
     */
    public boolean hasStock(Long itemId, Integer requiredQuantity) {
        Optional<JewelryItem> item = getJewelryItemById(itemId);
        return item.map(jewelryItem -> jewelryItem.getQuantity() >= requiredQuantity)
                   .orElse(false);
    }
    
    /**
     * Check stock by item code
     */
    public boolean hasStockByCode(String itemCode, Integer requiredQuantity) {
        Optional<JewelryItem> item = findByItemCode(itemCode);
        return item.map(jewelryItem -> jewelryItem.getQuantity() >= requiredQuantity)
                   .orElse(false);
    }
    
    /**
     * Get available stock for item
     */
    public Integer getAvailableStock(Long itemId) {
        return getJewelryItemById(itemId)
                .map(JewelryItem::getQuantity)
                .orElse(0);
    }
    
    /**
     * Get available stock by item code
     */
    public Integer getAvailableStockByCode(String itemCode) {
        return findByItemCode(itemCode)
                .map(JewelryItem::getQuantity)
                .orElse(0);
    }
}