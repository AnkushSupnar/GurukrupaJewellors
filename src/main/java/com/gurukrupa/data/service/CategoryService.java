package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Category;
import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.repository.CategoryRepository;
import com.gurukrupa.data.repository.JewelryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final JewelryItemRepository jewelryItemRepository;
    
    @Autowired
    public CategoryService(CategoryRepository categoryRepository, JewelryItemRepository jewelryItemRepository) {
        this.categoryRepository = categoryRepository;
        this.jewelryItemRepository = jewelryItemRepository;
    }
    
    // Basic CRUD operations
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }
    
    public Category createCategory(String categoryName, String description) {
        Category category = Category.builder()
                .categoryName(categoryName.trim())
                .description(description != null ? description.trim() : null)
                .build();
        return categoryRepository.save(category);
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findAllActiveCategories();
    }
    
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
    
    public Optional<Category> getCategoryByName(String categoryName) {
        return categoryRepository.findByCategoryName(categoryName);
    }
    
    public Category updateCategory(Long id, String categoryName, String description) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        
        category.setCategoryName(categoryName.trim());
        category.setDescription(description != null ? description.trim() : null);
        
        return categoryRepository.save(category);
    }
    
    public void deleteCategory(Long id) {
        // Check if category is being used
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        
        long itemCount = getItemCountForCategory(category.getCategoryName());
        if (itemCount > 0) {
            throw new RuntimeException("Cannot delete category '" + category.getCategoryName() + 
                    "' because it has " + itemCount + " items associated with it.");
        }
        
        categoryRepository.deleteById(id);
    }
    
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        category.setIsActive(false);
        categoryRepository.save(category);
    }
    
    public void activateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        category.setIsActive(true);
        categoryRepository.save(category);
    }
    
    // Business logic methods
    public boolean isCategoryNameUnique(String categoryName) {
        return !categoryRepository.existsByCategoryName(categoryName.trim());
    }
    
    public boolean isCategoryNameUnique(String categoryName, Long excludeId) {
        Optional<Category> existingCategory = categoryRepository.findByCategoryNameExcludingId(categoryName.trim(), excludeId);
        return !existingCategory.isPresent();
    }
    
    public List<Category> searchCategories(String searchTerm) {
        return categoryRepository.searchCategories(searchTerm);
    }
    
    public long getItemCountForCategory(String categoryName) {
        return jewelryItemRepository.findByCategory(categoryName).stream()
                .filter(item -> item.getIsActive())
                .count();
    }
    
    public Map<String, Long> getCategoryItemCountMap() {
        List<JewelryItem> allItems = jewelryItemRepository.findByIsActiveTrue();
        return allItems.stream()
                .filter(item -> item.getCategory() != null && !item.getCategory().trim().isEmpty())
                .collect(Collectors.groupingBy(
                    JewelryItem::getCategory,
                    Collectors.counting()
                ));
    }
    
    // Migration method to create categories from existing jewelry items
    @Transactional
    public void migrateExistingCategories() {
        List<String> distinctCategories = jewelryItemRepository.findDistinctCategories();
        
        for (String categoryName : distinctCategories) {
            if (categoryName != null && !categoryName.trim().isEmpty() && isCategoryNameUnique(categoryName)) {
                createCategory(categoryName, "Migrated from existing items");
            }
        }
    }
    
    // Get total number of categories
    public long getTotalCategoryCount() {
        return categoryRepository.count();
    }
    
    // Get total number of active categories
    public long getActiveCategoryCount() {
        return categoryRepository.findByIsActiveTrue().size();
    }
}