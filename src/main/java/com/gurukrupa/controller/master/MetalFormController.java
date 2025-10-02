package com.gurukrupa.controller.master;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.entities.Metal;
import com.gurukrupa.data.service.MetalService;
import com.gurukrupa.view.AlertNotification;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class MetalFormController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MetalFormController.class);
    
    @Autowired
    private MetalService metalService;
    
    @Autowired
    private AlertNotification alert;
    
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    // Form controls
    @FXML private TextField txtMetalName;
    @FXML private ComboBox<String> cmbMetalType;
    @FXML private TextField txtPurity;
    @FXML private TextArea txtDescription;
    @FXML private Text txtFormTitle;
    
    // Action buttons
    @FXML private Button btnBack;
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;
    @FXML private Button btnRefresh;
    @FXML private Button btnClearFilters;
    
    // Table controls
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterType;
    @FXML private TableView<Metal> tableMetals;
    @FXML private TableColumn<Metal, Long> colId;
    @FXML private TableColumn<Metal, String> colMetalName;
    @FXML private TableColumn<Metal, String> colMetalType;
    @FXML private TableColumn<Metal, String> colPurity;
    @FXML private TableColumn<Metal, String> colDescription;
    @FXML private TableColumn<Metal, String> colStatus;
    @FXML private TableColumn<Metal, Void> colActions;
    @FXML private Text txtTotalMetals;
    @FXML private Text txtActiveMetals;
    
    // Data
    private ObservableList<Metal> metalList = FXCollections.observableArrayList();
    private FilteredList<Metal> filteredMetals;
    private Metal currentEditingMetal = null;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize metal types
        List<String> metalTypes = Arrays.asList("Gold", "Silver", "Platinum", "Palladium", "Other");
        cmbMetalType.setItems(FXCollections.observableArrayList(metalTypes));
        
        // Initialize filter types
        List<String> filterTypes = new ArrayList<>();
        filterTypes.add("All Metal Types");
        filterTypes.addAll(metalTypes);
        cmbFilterType.setItems(FXCollections.observableArrayList(filterTypes));
        cmbFilterType.getSelectionModel().selectFirst();
        
        // Set up table columns
        setupTableColumns();
        
        // Set up event handlers
        setupEventHandlers();
        
        // Set up filters
        setupFilters();
        
        // Initialize default metals if needed
        metalService.initializeDefaultMetals();
        
        // Load metals
        loadMetals();
        
        // Set up filtered list
        filteredMetals = new FilteredList<>(metalList, p -> true);
        tableMetals.setItems(filteredMetals);
        
        // Update summary
        updateSummary();
        
        logger.info("Metal Form Controller initialized successfully");
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMetalName.setCellValueFactory(new PropertyValueFactory<>("metalName"));
        colMetalType.setCellValueFactory(new PropertyValueFactory<>("metalType"));
        colPurity.setCellValueFactory(new PropertyValueFactory<>("purity"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getIsActive() ? "Active" : "Inactive"));
        
        // Actions column with Edit and Delete/Deactivate buttons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnToggleStatus = new Button();
            
            {
                btnEdit.setGraphic(new FontAwesomeIcon());
                btnEdit.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
                ((FontAwesomeIcon) btnEdit.getGraphic()).setGlyphName("EDIT");
                ((FontAwesomeIcon) btnEdit.getGraphic()).setSize("1.0em");
                ((FontAwesomeIcon) btnEdit.getGraphic()).setFill(javafx.scene.paint.Color.WHITE);
                
                btnToggleStatus.setStyle("-fx-cursor: hand; -fx-padding: 4 8 4 8;");
                
                btnEdit.setOnAction(event -> {
                    Metal metal = getTableView().getItems().get(getIndex());
                    loadMetalForEditing(metal);
                });
                
                btnToggleStatus.setOnAction(event -> {
                    Metal metal = getTableView().getItems().get(getIndex());
                    toggleMetalStatus(metal);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Metal metal = getTableView().getItems().get(getIndex());
                    
                    // Update toggle button based on status
                    if (metal.getIsActive()) {
                        btnToggleStatus.setGraphic(new FontAwesomeIcon());
                        ((FontAwesomeIcon) btnToggleStatus.getGraphic()).setGlyphName("BAN");
                        ((FontAwesomeIcon) btnToggleStatus.getGraphic()).setSize("1.0em");
                        ((FontAwesomeIcon) btnToggleStatus.getGraphic()).setFill(javafx.scene.paint.Color.web("#D32F2F"));
                        btnToggleStatus.setStyle("-fx-background-color: #FFEBEE; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
                    } else {
                        btnToggleStatus.setGraphic(new FontAwesomeIcon());
                        ((FontAwesomeIcon) btnToggleStatus.getGraphic()).setGlyphName("CHECK");
                        ((FontAwesomeIcon) btnToggleStatus.getGraphic()).setSize("1.0em");
                        ((FontAwesomeIcon) btnToggleStatus.getGraphic()).setFill(javafx.scene.paint.Color.web("#388E3C"));
                        btnToggleStatus.setStyle("-fx-background-color: #E8F5E9; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
                    }
                    
                    HBox buttons = new HBox(5);
                    buttons.setAlignment(Pos.CENTER);
                    buttons.getChildren().addAll(btnEdit, btnToggleStatus);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void setupEventHandlers() {
        btnBack.setOnAction(e -> navigateBackToMasterMenu());
        btnSave.setOnAction(e -> saveMetal());
        btnUpdate.setOnAction(e -> updateMetal());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> refreshTable());
        btnClearFilters.setOnAction(e -> clearFilters());
        
        // Search functionality
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Table row selection
        tableMetals.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadMetalForEditing(newSelection);
                }
            });
    }
    
    private void setupFilters() {
        cmbFilterType.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }
    
    private void applyFilters() {
        filteredMetals.setPredicate(metal -> {
            boolean matchesSearch = true;
            boolean matchesType = true;
            
            // Search filter
            String searchText = txtSearch.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                matchesSearch = metal.getMetalName().toLowerCase().contains(lowerCaseFilter) ||
                               metal.getMetalType().toLowerCase().contains(lowerCaseFilter) ||
                               metal.getPurity().toLowerCase().contains(lowerCaseFilter) ||
                               (metal.getDescription() != null && metal.getDescription().toLowerCase().contains(lowerCaseFilter));
            }
            
            // Type filter
            String selectedType = cmbFilterType.getValue();
            if (selectedType != null && !selectedType.equals("All Metal Types")) {
                matchesType = selectedType.equals(metal.getMetalType());
            }
            
            return matchesSearch && matchesType;
        });
        updateSummary();
    }
    
    private void clearFilters() {
        txtSearch.clear();
        cmbFilterType.getSelectionModel().selectFirst();
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
    
    private void refreshTable() {
        loadMetals();
        clearFilters();
        clearForm();
        alert.showSuccess("Metal list refreshed successfully");
    }
    
    private void updateSummary() {
        long totalMetals = metalList.size();
        long activeMetals = metalList.stream().filter(Metal::getIsActive).count();
        
        txtTotalMetals.setText(String.valueOf(totalMetals));
        txtActiveMetals.setText(String.valueOf(activeMetals));
    }
    
    private void saveMetal() {
        if (!validateForm()) {
            return;
        }
        
        try {
            String metalName = txtMetalName.getText().trim();
            
            // Check if metal name already exists
            if (!metalService.isMetalNameUnique(metalName)) {
                alert.showError("Metal name already exists!");
                return;
            }
            
            Metal metal = Metal.builder()
                .metalName(metalName)
                .metalType(cmbMetalType.getValue())
                .purity(txtPurity.getText().trim())
                .description(txtDescription.getText().trim())
                .build();
            
            metalService.saveMetal(metal);
            alert.showSuccess("Metal added successfully!");
            clearForm();
            loadMetals();
            
        } catch (Exception e) {
            alert.showError("Error saving metal: " + e.getMessage());
        }
    }
    
    private void updateMetal() {
        if (!validateForm() || currentEditingMetal == null) {
            return;
        }
        
        try {
            String metalName = txtMetalName.getText().trim();
            
            // Check if metal name already exists (excluding current metal)
            if (!metalService.isMetalNameUnique(metalName, currentEditingMetal.getId())) {
                alert.showError("Metal name already exists!");
                return;
            }
            
            currentEditingMetal.setMetalName(metalName);
            currentEditingMetal.setMetalType(cmbMetalType.getValue());
            currentEditingMetal.setPurity(txtPurity.getText().trim());
            currentEditingMetal.setDescription(txtDescription.getText().trim());
            
            metalService.updateMetal(currentEditingMetal.getId(), currentEditingMetal);
            alert.showSuccess("Metal updated successfully!");
            clearForm();
            loadMetals();
            
        } catch (Exception e) {
            alert.showError("Error updating metal: " + e.getMessage());
        }
    }
    
    private void toggleMetalStatus(Metal metal) {
        try {
            if (metal.getIsActive()) {
                metalService.deactivateMetal(metal.getId());
                alert.showSuccess("Metal deactivated successfully!");
            } else {
                metalService.activateMetal(metal.getId());
                alert.showSuccess("Metal activated successfully!");
            }
            loadMetals();
        } catch (Exception e) {
            alert.showError("Error changing metal status: " + e.getMessage());
        }
    }
    
    private void loadMetalForEditing(Metal metal) {
        currentEditingMetal = metal;
        isEditMode = true;
        
        txtMetalName.setText(metal.getMetalName());
        cmbMetalType.setValue(metal.getMetalType());
        txtPurity.setText(metal.getPurity());
        txtDescription.setText(metal.getDescription() != null ? metal.getDescription() : "");
        
        // Update UI for edit mode
        txtFormTitle.setText("Edit Metal");
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }
    
    private void loadMetals() {
        metalList.clear();
        metalList.addAll(metalService.getAllMetals());
        updateSummary();
        logger.info("Loaded {} metals", metalList.size());
    }
    
    private boolean validateForm() {
        if (txtMetalName.getText().trim().isEmpty()) {
            alert.showError("Please enter metal name!");
            txtMetalName.requestFocus();
            return false;
        }
        
        if (cmbMetalType.getValue() == null) {
            alert.showError("Please select metal type!");
            cmbMetalType.requestFocus();
            return false;
        }
        
        if (txtPurity.getText().trim().isEmpty()) {
            alert.showError("Please enter purity!");
            txtPurity.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void clearForm() {
        txtMetalName.clear();
        cmbMetalType.setValue(null);
        txtPurity.clear();
        txtDescription.clear();
        
        // Reset to add mode
        currentEditingMetal = null;
        isEditMode = false;
        txtFormTitle.setText("Add New Metal");
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        tableMetals.getSelectionModel().clearSelection();
    }
    
    // Support for dialog mode (when opened from Settings menu)
    private Stage dialogStage;
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // Hide back button when in dialog mode
        if (btnBack != null) {
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        }
    }
}