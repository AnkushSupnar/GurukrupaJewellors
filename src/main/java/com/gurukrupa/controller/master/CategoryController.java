package com.gurukrupa.controller.master;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.entities.Category;
import com.gurukrupa.data.service.CategoryService;
import com.gurukrupa.data.service.JewelryItemService;
import com.gurukrupa.view.StageManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CategoryController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private JewelryItemService jewelryItemService;
    
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Text txtFormTitle;
    
    @FXML
    private TextField txtCategoryName;
    
    @FXML
    private TextField txtDescription;
    
    @FXML
    private Button btnSave;
    
    @FXML
    private Button btnUpdate;
    
    @FXML
    private Button btnClear;
    
    @FXML
    private Text txtTotalCategories;
    
    @FXML
    private Text txtTotalItems;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private TextField txtSearch;
    
    @FXML
    private TableView<CategoryData> tableCategories;
    
    @FXML
    private TableColumn<CategoryData, Long> colId;
    
    @FXML
    private TableColumn<CategoryData, String> colCategoryName;
    
    @FXML
    private TableColumn<CategoryData, String> colDescription;
    
    @FXML
    private TableColumn<CategoryData, Integer> colItemCount;
    
    @FXML
    private TableColumn<CategoryData, Void> colActions;
    
    private ObservableList<CategoryData> categoryList;
    private FilteredList<CategoryData> filteredData;
    private Category selectedCategory;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Category Controller");
        
        // First, migrate existing categories from JewelryItems if needed
        migrateExistingCategories();
        
        initializeTable();
        setupEventHandlers();
        loadCategories();
        updateStatistics();
        
        logger.info("Category Controller initialized successfully");
    }
    
    private void migrateExistingCategories() {
        try {
            // Check if we need to migrate (if categories table is empty)
            if (categoryService.getTotalCategoryCount() == 0) {
                logger.info("No categories found, migrating from existing jewelry items...");
                categoryService.migrateExistingCategories();
                logger.info("Category migration completed");
            }
        } catch (Exception e) {
            logger.error("Error during category migration: {}", e.getMessage());
        }
    }
    
    private void initializeTable() {
        categoryList = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(categoryList, p -> true);
        
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colItemCount.setCellValueFactory(new PropertyValueFactory<>("itemCount"));
        
        setupActionsColumn();
        
        tableCategories.setItems(filteredData);
        
        // Setup search functionality
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(category -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (category.getCategoryName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (category.getDescription() != null && 
                          category.getDescription().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        
        // Add row click listener
        tableCategories.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadCategoryToForm(newSelection);
                }
            }
        );
    }
    
    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            
            {
                // Style edit button
                FontAwesomeIcon editIcon = new FontAwesomeIcon();
                editIcon.setGlyphName("EDIT");
                editIcon.setFill(javafx.scene.paint.Color.WHITE);
                editIcon.setSize("1.2em");
                editButton.setGraphic(editIcon);
                editButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; " +
                    "-fx-background-radius: 15; -fx-padding: 5 10 5 10; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(25,118,210,0.3), 2, 0, 0, 1);");
                
                // Style delete button
                FontAwesomeIcon deleteIcon = new FontAwesomeIcon();
                deleteIcon.setGlyphName("TRASH");
                deleteIcon.setFill(javafx.scene.paint.Color.WHITE);
                deleteIcon.setSize("1.2em");
                deleteButton.setGraphic(deleteIcon);
                deleteButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; " +
                    "-fx-background-radius: 15; -fx-padding: 5 10 5 10; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(211,47,47,0.3), 2, 0, 0, 1);");
                
                // Add hover effects
                editButton.setOnMouseEntered(e -> 
                    editButton.setStyle(editButton.getStyle() + "; -fx-scale-x: 1.1; -fx-scale-y: 1.1;"));
                editButton.setOnMouseExited(e -> 
                    editButton.setStyle(editButton.getStyle().replace("; -fx-scale-x: 1.1; -fx-scale-y: 1.1;", "")));
                
                deleteButton.setOnMouseEntered(e -> 
                    deleteButton.setStyle(deleteButton.getStyle() + "; -fx-scale-x: 1.1; -fx-scale-y: 1.1;"));
                deleteButton.setOnMouseExited(e -> 
                    deleteButton.setStyle(deleteButton.getStyle().replace("; -fx-scale-x: 1.1; -fx-scale-y: 1.1;", "")));
                
                editButton.setOnAction(event -> {
                    CategoryData category = getTableView().getItems().get(getIndex());
                    loadCategoryToForm(category);
                });
                
                deleteButton.setOnAction(event -> {
                    CategoryData category = getTableView().getItems().get(getIndex());
                    handleDelete(category);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.setAlignment(Pos.CENTER);
                    buttons.getChildren().addAll(editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void setupEventHandlers() {
        btnBack.setOnAction(e -> handleBack());
        btnSave.setOnAction(e -> handleSave());
        btnUpdate.setOnAction(e -> handleUpdate());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> {
            loadCategories();
            updateStatistics();
        });
    }
    
    private void handleBack() {
        try {
            // Get the dashboard's center panel
            BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
            
            // Load the Master Menu FXML
            Parent masterMenu = springFXMLLoader.load("/fxml/master/MasterMenu.fxml");
            
            // Set the master menu in the center of the dashboard
            dashboard.setCenter(masterMenu);
            
            logger.info("Navigated back to Master Menu");
            
        } catch (Exception e) {
            logger.error("Error navigating back to Master Menu: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Error navigating back: " + e.getMessage());
        }
    }
    
    private void handleSave() {
        if (!validateForm()) {
            return;
        }
        
        String categoryName = txtCategoryName.getText().trim();
        String description = txtDescription.getText().trim();
        
        try {
            // Check if category already exists
            if (!categoryService.isCategoryNameUnique(categoryName)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Category '" + categoryName + "' already exists!");
                return;
            }
            
            // Save to database
            Category newCategory = categoryService.createCategory(categoryName, description);
            
            // Reload categories to refresh the table
            loadCategories();
            clearForm();
            updateStatistics();
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Category added successfully!");
            logger.info("Category '{}' added successfully", categoryName);
            
        } catch (Exception e) {
            logger.error("Error saving category: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save category: " + e.getMessage());
        }
    }
    
    private void handleUpdate() {
        if (!validateForm() || selectedCategory == null) {
            return;
        }
        
        String categoryName = txtCategoryName.getText().trim();
        String description = txtDescription.getText().trim();
        
        try {
            // Check if new name conflicts with existing category
            if (!categoryService.isCategoryNameUnique(categoryName, selectedCategory.getId())) {
                showAlert(Alert.AlertType.ERROR, "Error", "Category '" + categoryName + "' already exists!");
                return;
            }
            
            // Update in database
            categoryService.updateCategory(selectedCategory.getId(), categoryName, description);
            
            // Reload categories to refresh the table
            loadCategories();
            clearForm();
            updateStatistics();
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Category updated successfully!");
            logger.info("Category updated successfully");
            
        } catch (Exception e) {
            logger.error("Error updating category: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update category: " + e.getMessage());
        }
    }
    
    private void handleDelete(CategoryData category) {
        try {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Confirmation");
            confirmDialog.setHeaderText("Delete Category");
            confirmDialog.setContentText("Are you sure you want to delete category '" + 
                category.getCategoryName() + "'?");
            
            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Delete from database
                categoryService.deleteCategory(category.getId());
                
                // Clear form if deleted category was selected
                if (selectedCategory != null && selectedCategory.getId().equals(category.getId())) {
                    clearForm();
                }
                
                // Reload categories
                loadCategories();
                updateStatistics();
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Category deleted successfully!");
                logger.info("Category '{}' deleted successfully", category.getCategoryName());
            }
        } catch (Exception e) {
            logger.error("Error deleting category: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }
    
    private void loadCategoryToForm(CategoryData categoryData) {
        try {
            // Load the actual Category entity from database
            Optional<Category> categoryOpt = categoryService.getCategoryById(categoryData.getId());
            if (categoryOpt.isPresent()) {
                selectedCategory = categoryOpt.get();
                txtCategoryName.setText(selectedCategory.getCategoryName());
                txtDescription.setText(selectedCategory.getDescription() != null ? selectedCategory.getDescription() : "");
                
                // Update form title and buttons
                txtFormTitle.setText("Edit Category");
                btnSave.setVisible(false);
                btnSave.setManaged(false);
                btnUpdate.setVisible(true);
                btnUpdate.setManaged(true);
            }
        } catch (Exception e) {
            logger.error("Error loading category to form: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load category: " + e.getMessage());
        }
    }
    
    private void clearForm() {
        selectedCategory = null;
        txtCategoryName.clear();
        txtDescription.clear();
        
        // Reset form title and buttons
        txtFormTitle.setText("Add New Category");
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnUpdate.setVisible(false);
        btnUpdate.setManaged(false);
        
        // Clear table selection
        tableCategories.getSelectionModel().clearSelection();
    }
    
    private boolean validateForm() {
        if (txtCategoryName.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Category name is required!");
            txtCategoryName.requestFocus();
            return false;
        }
        return true;
    }
    
    
    private void loadCategories() {
        try {
            categoryList.clear();
            
            // Get all categories from database
            List<Category> categories = categoryService.getAllCategories();
            Map<String, Long> categoryItemCount = categoryService.getCategoryItemCountMap();
            
            // Create CategoryData objects for display
            for (Category category : categories) {
                Long itemCount = categoryItemCount.getOrDefault(category.getCategoryName(), 0L);
                categoryList.add(new CategoryData(
                    category.getId(),
                    category.getCategoryName(),
                    category.getDescription(),
                    itemCount.intValue()
                ));
            }
            
            logger.info("Loaded {} categories", categoryList.size());
        } catch (Exception e) {
            logger.error("Error loading categories: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load categories: " + e.getMessage());
        }
    }
    
    private void updateStatistics() {
        try {
            // Get statistics from service
            long totalCategories = categoryService.getTotalCategoryCount();
            txtTotalCategories.setText(String.valueOf(totalCategories));
            
            // Calculate total items across all categories
            Map<String, Long> itemCountMap = categoryService.getCategoryItemCountMap();
            long totalItems = itemCountMap.values().stream().mapToLong(Long::longValue).sum();
            txtTotalItems.setText(String.valueOf(totalItems));
        } catch (Exception e) {
            logger.error("Error updating statistics: {}", e.getMessage());
            txtTotalCategories.setText("0");
            txtTotalItems.setText("0");
        }
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Inner class for category data
    public static class CategoryData {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty categoryName;
        private final SimpleStringProperty description;
        private final SimpleIntegerProperty itemCount;
        
        public CategoryData(long id, String categoryName, String description, int itemCount) {
            this.id = new SimpleIntegerProperty((int) id);
            this.categoryName = new SimpleStringProperty(categoryName);
            this.description = new SimpleStringProperty(description);
            this.itemCount = new SimpleIntegerProperty(itemCount);
        }
        
        public long getId() {
            return id.get();
        }
        
        public String getCategoryName() {
            return categoryName.get();
        }
        
        public void setCategoryName(String name) {
            this.categoryName.set(name);
        }
        
        public String getDescription() {
            return description.get();
        }
        
        public void setDescription(String desc) {
            this.description.set(desc);
        }
        
        public int getItemCount() {
            return itemCount.get();
        }
    }
}