package com.gurukrupa.controller.master;

import com.gurukrupa.data.entities.Category;
import com.gurukrupa.data.entities.JewelryItem;
import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.service.CategoryService;
import com.gurukrupa.data.service.JewelryItemService;
import com.gurukrupa.data.service.MetalService;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.config.SpringFXMLLoader;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

@Component
public class JewelryItemFormController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(JewelryItemFormController.class);
    
    @Autowired
    private JewelryItemService jewelryItemService;
    
    @Autowired
    private MetalService metalService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private AlertNotification alert;
    
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    // Form Header
    @FXML private Text txtFormTitle;
    @FXML private Button btnBack;
    
    // Basic Information
    @FXML private TextField txtItemCode;
    @FXML private TextField txtItemName;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private ComboBox<String> cmbMetalType;
    @FXML private TextField txtPurity;
    @FXML private TextField txtQuantity;
    
    // Weight Information
    @FXML private TextField txtGrossWeight;
    @FXML private TextField txtStoneWeight;
    @FXML private TextField txtNetWeight;
    
    // Pricing Information
    @FXML private TextField txtGoldRate;
    @FXML private TextField txtLabourCharges;
    @FXML private TextField txtStoneCharges;
    @FXML private TextField txtOtherCharges;
    @FXML private TextField txtTotalAmount;
    
    // Additional Information
    @FXML private TextArea txtDescription;
    
    // Action Buttons
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;
    
    // Right Panel - Item List
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterCategory;
    @FXML private ComboBox<String> cmbFilterMetalType;
    @FXML private Button btnClearFilters;
    @FXML private Button btnRefresh;
    @FXML private TableView<JewelryItem> tableJewelryItems;
    @FXML private TableColumn<JewelryItem, String> colItemCode;
    @FXML private TableColumn<JewelryItem, String> colItemName;
    @FXML private TableColumn<JewelryItem, String> colCategory;
    @FXML private TableColumn<JewelryItem, String> colMetalType;
    @FXML private TableColumn<JewelryItem, String> colPurity;
    @FXML private TableColumn<JewelryItem, String> colNetWeight;
    @FXML private TableColumn<JewelryItem, String> colLabourCharges;
    @FXML private TableColumn<JewelryItem, String> colTotalAmount;
    @FXML private TableColumn<JewelryItem, String> colQuantity;
    
    // Data
    private ObservableList<JewelryItem> jewelryItems = FXCollections.observableArrayList();
    private FilteredList<JewelryItem> filteredItems;
    private JewelryItem currentEditingItem = null;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Jewelry Item Form Controller");
        
        setupFormFields();
        setupTableColumns();
        setupEventHandlers();
        setupAutoCalculations();
        loadInitialData();
        
        logger.info("Jewelry Item Form Controller initialized successfully");
    }
    
    private void setupFormFields() {
        // Setup category options from database
        loadCategories();
        
        // Setup metal type options from database
        loadMetalTypes();
        
        // Make calculated fields non-editable with material colors
        txtNetWeight.setEditable(false);
        txtNetWeight.setStyle("-fx-background-color: #e8f5e8; -fx-background-radius: 8; -fx-padding: 12; -fx-font-size: 14px; -fx-border-color: transparent;");
        txtTotalAmount.setEditable(false);
        txtTotalAmount.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8; -fx-padding: 12; -fx-font-size: 14px; -fx-border-color: transparent; -fx-font-weight: bold;");
    }
    
    private void setupTableColumns() {
        colItemCode.setCellValueFactory(new PropertyValueFactory<>("itemCode"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colMetalType.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getMetalType()));
        colPurity.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPurity() + "K"));
        colNetWeight.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f g", cellData.getValue().getNetWeight())));
        colLabourCharges.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getLabourCharges() + "%"));
        colTotalAmount.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("₹%.2f", cellData.getValue().getTotalAmount())));
        colQuantity.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getQuantity().toString()));
        
        // Load data and setup filtered list
        loadJewelryItems();
        filteredItems = new FilteredList<>(jewelryItems, p -> true);
        tableJewelryItems.setItems(filteredItems);
    }
    
    private void setupEventHandlers() {
        // Back button
        btnBack.setOnAction(e -> navigateBackToMasterMenu());
        
        // Action buttons
        btnSave.setOnAction(e -> saveJewelryItem());
        btnUpdate.setOnAction(e -> updateJewelryItem());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> refreshTable());
        btnClearFilters.setOnAction(e -> clearFilters());
        
        // Table row selection
        tableJewelryItems.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadItemForEditing(newSelection);
                }
            });
        
        // Search functionality
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        cmbFilterCategory.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        cmbFilterMetalType.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        
        // Auto-generate item code when category and metal type change
        cmbCategory.valueProperty().addListener((obs, oldValue, newValue) -> generateItemCode());
        cmbMetalType.valueProperty().addListener((obs, oldValue, newValue) -> {
            generateItemCode();
            loadPurityForSelectedMetal();
        });
    }
    
    private void setupAutoCalculations() {
        // Auto-calculate net weight when gross weight or stone weight changes
        txtGrossWeight.textProperty().addListener((obs, oldValue, newValue) -> calculateNetWeight());
        txtStoneWeight.textProperty().addListener((obs, oldValue, newValue) -> calculateNetWeight());
        
        // Auto-calculate total amount when any pricing field changes
        txtNetWeight.textProperty().addListener((obs, oldValue, newValue) -> calculateTotalAmount());
        txtGoldRate.textProperty().addListener((obs, oldValue, newValue) -> calculateTotalAmount());
        txtLabourCharges.textProperty().addListener((obs, oldValue, newValue) -> calculateTotalAmount());
        txtStoneCharges.textProperty().addListener((obs, oldValue, newValue) -> calculateTotalAmount());
        txtOtherCharges.textProperty().addListener((obs, oldValue, newValue) -> calculateTotalAmount());
    }
    
    private void loadInitialData() {
        // This method is kept for future use if needed
        // Categories are loaded via loadCategories() in setupFormFields()
        // Metal types are loaded via loadMetalTypes() in setupFormFields()
    }
    
    private void loadCategories() {
        try {
            // First ensure existing categories are migrated
            if (categoryService.getTotalCategoryCount() == 0) {
                categoryService.migrateExistingCategories();
            }
            
            // Load all active categories
            List<Category> categories = categoryService.getAllActiveCategories();
            ObservableList<String> categoryNames = FXCollections.observableArrayList();
            
            for (Category category : categories) {
                categoryNames.add(category.getCategoryName());
            }
            
            cmbCategory.setItems(categoryNames);
            cmbFilterCategory.setItems(categoryNames);
            
            // Also add existing categories from items that might not be in the category table yet
            List<String> existingCategories = jewelryItemService.getDistinctCategories();
            for (String cat : existingCategories) {
                if (cat != null && !cat.trim().isEmpty() && !categoryNames.contains(cat)) {
                    categoryNames.add(cat);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error loading categories: {}", e.getMessage());
            // Fallback to default categories if database fails
            List<String> defaultCategories = Arrays.asList(
                "Ring", "Necklace", "Earrings", "Bracelet", "Pendant", 
                "Chain", "Bangles", "Anklet", "Nose Pin", "Toe Ring"
            );
            cmbCategory.setItems(FXCollections.observableArrayList(defaultCategories));
            cmbFilterCategory.setItems(FXCollections.observableArrayList(defaultCategories));
        }
    }
    
    private void loadMetalTypes() {
        try {
            List<String> metalNames = metalService.getAllMetalNames();
            cmbMetalType.setItems(FXCollections.observableArrayList(metalNames));
            cmbFilterMetalType.setItems(FXCollections.observableArrayList(metalNames));
            logger.info("Loaded {} metal types from database", metalNames.size());
        } catch (Exception e) {
            logger.error("Error loading metal types: {}", e.getMessage());
            alert.showError("Failed to load metal types: " + e.getMessage());
        }
    }

    private void loadPurityForSelectedMetal() {
        // Only auto-populate purity when adding new item, not when editing
        if (isEditMode) {
            return;
        }

        String selectedMetalName = cmbMetalType.getValue();
        if (selectedMetalName == null || selectedMetalName.trim().isEmpty()) {
            return;
        }

        try {
            Optional<Metal> metalOpt = metalService.getMetalByName(selectedMetalName);
            if (metalOpt.isPresent()) {
                Metal metal = metalOpt.get();
                String purity = metal.getPurity();

                // Extract numeric value from purity (e.g., "24K" -> "24", "92.5" -> "92.5")
                String purityValue = purity.replaceAll("[^0-9.]", "");
                txtPurity.setText(purityValue);

                logger.debug("Auto-populated purity: {} for metal: {}", purityValue, selectedMetalName);
            }
        } catch (Exception e) {
            logger.error("Error loading purity for metal {}: {}", selectedMetalName, e.getMessage());
        }
    }
    
    private void generateItemCode() {
        if (!isEditMode && cmbCategory.getValue() != null && cmbMetalType.getValue() != null) {
            try {
                String generatedCode = jewelryItemService.generateItemCode(
                    cmbCategory.getValue(), cmbMetalType.getValue());
                txtItemCode.setText(generatedCode);
            } catch (Exception e) {
                logger.error("Error generating item code: {}", e.getMessage());
            }
        }
    }
    
    private void calculateNetWeight() {
        try {
            BigDecimal grossWeight = parseDecimal(txtGrossWeight.getText());
            BigDecimal stoneWeight = parseDecimal(txtStoneWeight.getText());
            
            if (grossWeight != null) {
                BigDecimal netWeight = grossWeight.subtract(stoneWeight != null ? stoneWeight : BigDecimal.ZERO);
                txtNetWeight.setText(String.format("%.3f", netWeight));
            }
        } catch (Exception e) {
            logger.debug("Error calculating net weight: {}", e.getMessage());
        }
    }
    
    private void calculateTotalAmount() {
        try {
            BigDecimal netWeight = parseDecimal(txtNetWeight.getText());
            BigDecimal goldRate = parseDecimal(txtGoldRate.getText());
            BigDecimal labourChargesPercent = parseDecimal(txtLabourCharges.getText());
            BigDecimal stoneCharges = parseDecimal(txtStoneCharges.getText());
            BigDecimal otherCharges = parseDecimal(txtOtherCharges.getText());
            
            if (netWeight != null && goldRate != null && labourChargesPercent != null) {
                // Calculate gold value: (netWeight * goldRate) / 10
                BigDecimal goldValue = netWeight.multiply(goldRate).divide(BigDecimal.valueOf(10));
                
                // Calculate labour charges as percentage of gold value
                BigDecimal labourChargesAmount = goldValue.multiply(labourChargesPercent).divide(BigDecimal.valueOf(100));
                
                // Add all charges
                BigDecimal total = goldValue.add(labourChargesAmount);
                if (stoneCharges != null) total = total.add(stoneCharges);
                if (otherCharges != null) total = total.add(otherCharges);
                
                txtTotalAmount.setText(String.format("%.2f", total));
            }
        } catch (Exception e) {
            logger.debug("Error calculating total amount: {}", e.getMessage());
        }
    }
    
    private BigDecimal parseDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private void saveJewelryItem() {
        try {
            if (!validateForm()) {
                return;
            }
            
            JewelryItem item = createJewelryItemFromForm();
            JewelryItem savedItem = jewelryItemService.saveJewelryItem(item);
            
            jewelryItems.add(savedItem);
            alert.showSuccess("Jewelry item saved successfully!");
            clearForm();
            
            logger.info("Jewelry item saved: {}", savedItem.getItemCode());
            
        } catch (Exception e) {
            logger.error("Error saving jewelry item: {}", e.getMessage());
            alert.showError("Error saving jewelry item: " + e.getMessage());
        }
    }
    
    private void updateJewelryItem() {
        try {
            if (currentEditingItem == null) {
                alert.showError("No item selected for update");
                return;
            }
            
            if (!validateForm()) {
                return;
            }
            
            updateJewelryItemFromForm(currentEditingItem);
            JewelryItem updatedItem = jewelryItemService.saveJewelryItem(currentEditingItem);
            
            // Update the item in the table
            int index = jewelryItems.indexOf(currentEditingItem);
            if (index >= 0) {
                jewelryItems.set(index, updatedItem);
            }
            
            alert.showSuccess("Jewelry item updated successfully!");
            clearForm();
            
            logger.info("Jewelry item updated: {}", updatedItem.getItemCode());
            
        } catch (Exception e) {
            logger.error("Error updating jewelry item: {}", e.getMessage());
            alert.showError("Error updating jewelry item: " + e.getMessage());
        }
    }
    
    private boolean validateForm() {
        if (txtItemCode.getText().trim().isEmpty()) {
            alert.showError("Item code is required");
            txtItemCode.requestFocus();
            return false;
        }
        
        if (txtItemName.getText().trim().isEmpty()) {
            alert.showError("Item name is required");
            txtItemName.requestFocus();
            return false;
        }
        
        if (cmbCategory.getValue() == null || cmbCategory.getValue().trim().isEmpty()) {
            alert.showError("Category is required");
            cmbCategory.requestFocus();
            return false;
        }
        
        if (cmbMetalType.getValue() == null) {
            alert.showError("Metal type is required");
            cmbMetalType.requestFocus();
            return false;
        }
        
        if (parseDecimal(txtPurity.getText()) == null) {
            alert.showError("Valid purity is required");
            txtPurity.requestFocus();
            return false;
        }
        
        if (parseDecimal(txtGrossWeight.getText()) == null) {
            alert.showError("Valid gross weight is required");
            txtGrossWeight.requestFocus();
            return false;
        }
        
        if (parseDecimal(txtGoldRate.getText()) == null) {
            alert.showError("Valid gold rate is required");
            txtGoldRate.requestFocus();
            return false;
        }
        
        if (parseDecimal(txtLabourCharges.getText()) == null) {
            alert.showError("Valid labour charges percentage is required (e.g., 10, 15)");
            txtLabourCharges.requestFocus();
            return false;
        }
        
        // Quantity is now optional - validate only if provided
        if (!txtQuantity.getText().trim().isEmpty()) {
            try {
                Integer.parseInt(txtQuantity.getText().trim());
            } catch (NumberFormatException e) {
                alert.showError("Valid quantity is required");
                txtQuantity.requestFocus();
                return false;
            }
        }
        
        // Check if item code is unique (only for new items)
        if (!isEditMode && !jewelryItemService.isItemCodeUnique(txtItemCode.getText().trim())) {
            alert.showError("Item code already exists. Please use a different code.");
            txtItemCode.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private JewelryItem createJewelryItemFromForm() {
        // Look up Metal entity by name
        String metalName = cmbMetalType.getValue();
        Metal metal = metalService.getMetalByName(metalName).orElse(null);

        if (metal == null) {
            logger.warn("Metal not found for name: {}", metalName);
        }

        return JewelryItem.builder()
            .itemCode(txtItemCode.getText().trim())
            .itemName(txtItemName.getText().trim())
            .category(cmbCategory.getValue())
            .metal(metal) // Set Metal entity reference
            .metalType(metalName) // Set denormalized field (metal name)
            .purity(parseDecimal(txtPurity.getText()))
            .grossWeight(parseDecimal(txtGrossWeight.getText()))
            .stoneWeight(parseDecimal(txtStoneWeight.getText()))
            .netWeight(parseDecimal(txtNetWeight.getText()))
            .goldRate(parseDecimal(txtGoldRate.getText()))
            .labourCharges(parseDecimal(txtLabourCharges.getText()))
            .stoneCharges(parseDecimal(txtStoneCharges.getText()))
            .otherCharges(parseDecimal(txtOtherCharges.getText()))
            .totalAmount(parseDecimal(txtTotalAmount.getText()))
            .quantity(txtQuantity.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtQuantity.getText().trim()))
            .description(txtDescription.getText().trim())
            .isActive(true)
            .build();
    }
    
    private void updateJewelryItemFromForm(JewelryItem item) {
        // Look up Metal entity by name
        String metalName = cmbMetalType.getValue();
        Metal metal = metalService.getMetalByName(metalName).orElse(null);

        if (metal == null) {
            logger.warn("Metal not found for name: {}", metalName);
        }

        item.setItemCode(txtItemCode.getText().trim());
        item.setItemName(txtItemName.getText().trim());
        item.setCategory(cmbCategory.getValue());
        item.setMetal(metal); // Set Metal entity reference
        item.setMetalType(metalName); // Set denormalized field (metal name)
        item.setPurity(parseDecimal(txtPurity.getText()));
        item.setGrossWeight(parseDecimal(txtGrossWeight.getText()));
        item.setStoneWeight(parseDecimal(txtStoneWeight.getText()));
        item.setNetWeight(parseDecimal(txtNetWeight.getText()));
        item.setGoldRate(parseDecimal(txtGoldRate.getText()));
        item.setLabourCharges(parseDecimal(txtLabourCharges.getText()));
        item.setStoneCharges(parseDecimal(txtStoneCharges.getText()));
        item.setOtherCharges(parseDecimal(txtOtherCharges.getText()));
        item.setTotalAmount(parseDecimal(txtTotalAmount.getText()));
        item.setQuantity(Integer.parseInt(txtQuantity.getText().trim()));
        item.setDescription(txtDescription.getText().trim());
    }
    
    private void loadItemForEditing(JewelryItem item) {
        currentEditingItem = item;
        isEditMode = true;
        
        // Update form title
        txtFormTitle.setText("Edit Jewelry Item");
        
        // Load data into form
        txtItemCode.setText(item.getItemCode());
        txtItemName.setText(item.getItemName());
        cmbCategory.setValue(item.getCategory());
        cmbMetalType.setValue(item.getMetalType());
        txtPurity.setText(item.getPurity().toString());
        txtQuantity.setText(item.getQuantity().toString());
        txtGrossWeight.setText(item.getGrossWeight().toString());
        txtStoneWeight.setText(item.getStoneWeight() != null ? item.getStoneWeight().toString() : "");
        txtNetWeight.setText(item.getNetWeight().toString());
        txtGoldRate.setText(item.getGoldRate().toString());
        txtLabourCharges.setText(item.getLabourCharges().toString());
        txtStoneCharges.setText(item.getStoneCharges() != null ? item.getStoneCharges().toString() : "");
        txtOtherCharges.setText(item.getOtherCharges() != null ? item.getOtherCharges().toString() : "");
        txtTotalAmount.setText(item.getTotalAmount().toString());
        txtDescription.setText(item.getDescription() != null ? item.getDescription() : "");
        
        // Show update button, hide save button
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }
    
    private void clearForm() {
        currentEditingItem = null;
        isEditMode = false;
        
        // Update form title
        txtFormTitle.setText("Add New Jewelry Item");
        
        // Clear all form fields
        txtItemCode.clear();
        txtItemName.clear();
        cmbCategory.setValue(null);
        cmbMetalType.setValue(null);
        txtPurity.clear();
        txtQuantity.clear();
        txtGrossWeight.clear();
        txtStoneWeight.clear();
        txtNetWeight.clear();
        txtGoldRate.clear();
        txtLabourCharges.clear();
        txtStoneCharges.clear();
        txtOtherCharges.clear();
        txtTotalAmount.clear();
        txtDescription.clear();
        
        // Show save button, hide update button
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        // Clear table selection
        tableJewelryItems.getSelectionModel().clearSelection();
    }
    
    private void loadJewelryItems() {
        try {
            jewelryItems.clear();
            jewelryItems.addAll(jewelryItemService.getAllActiveItems());
            logger.info("Loaded {} jewelry items", jewelryItems.size());
        } catch (Exception e) {
            logger.error("Error loading jewelry items: {}", e.getMessage());
            alert.showError("Error loading jewelry items: " + e.getMessage());
        }
    }
    
    private void refreshTable() {
        loadCategories(); // Reload categories in case new ones were added
        loadJewelryItems();
        alert.showSuccess("Table refreshed successfully");
    }
    
    private void applyFilters() {
        Predicate<JewelryItem> searchFilter = item -> {
            String searchText = txtSearch.getText();
            if (searchText == null || searchText.trim().isEmpty()) {
                return true;
            }
            
            String searchLower = searchText.toLowerCase();
            return item.getItemCode().toLowerCase().contains(searchLower) ||
                   item.getItemName().toLowerCase().contains(searchLower) ||
                   item.getCategory().toLowerCase().contains(searchLower);
        };
        
        Predicate<JewelryItem> categoryFilter = item -> {
            String selectedCategory = cmbFilterCategory.getValue();
            return selectedCategory == null || selectedCategory.isEmpty() || 
                   item.getCategory().equals(selectedCategory);
        };
        
        Predicate<JewelryItem> metalTypeFilter = item -> {
            String selectedMetalType = cmbFilterMetalType.getValue();
            return selectedMetalType == null || selectedMetalType.isEmpty() || 
                   item.getMetalType().equals(selectedMetalType);
        };
        
        filteredItems.setPredicate(searchFilter.and(categoryFilter).and(metalTypeFilter));
    }
    
    private void clearFilters() {
        txtSearch.clear();
        cmbFilterCategory.setValue(null);
        cmbFilterMetalType.setValue(null);
        filteredItems.setPredicate(p -> true);
    }
    
    private void navigateBackToMasterMenu() {
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
            alert.showError("Error navigating back: " + e.getMessage());
        }
    }
}