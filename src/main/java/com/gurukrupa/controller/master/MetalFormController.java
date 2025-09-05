package com.gurukrupa.controller.master;

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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class MetalFormController implements Initializable {
    
    @Autowired
    private MetalService metalService;
    
    @Autowired
    private AlertNotification alert;
    
    // Form controls
    @FXML private TextField txtMetalName;
    @FXML private ComboBox<String> cmbMetalType;
    @FXML private TextField txtPurity;
    @FXML private TextArea txtDescription;
    
    // Action buttons
    @FXML private Button btnSave;
    @FXML private Button btnUpdate;
    @FXML private Button btnClear;
    @FXML private Button btnClose;
    @FXML private Button btnRefresh;
    
    // Table controls
    @FXML private TextField txtSearch;
    @FXML private TableView<Metal> tableMetals;
    @FXML private TableColumn<Metal, String> colMetalName;
    @FXML private TableColumn<Metal, String> colMetalType;
    @FXML private TableColumn<Metal, String> colPurity;
    @FXML private TableColumn<Metal, String> colStatus;
    @FXML private TableColumn<Metal, Void> colActions;
    
    // Data
    private ObservableList<Metal> metalList = FXCollections.observableArrayList();
    private FilteredList<Metal> filteredMetals;
    private Metal currentEditingMetal = null;
    private boolean isEditMode = false;
    private Stage dialogStage;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize metal types
        List<String> metalTypes = Arrays.asList("Gold", "Silver", "Platinum", "Palladium", "Other");
        cmbMetalType.setItems(FXCollections.observableArrayList(metalTypes));
        
        // Set up table columns
        setupTableColumns();
        
        // Set up event handlers
        setupEventHandlers();
        
        // Initialize default metals if needed
        metalService.initializeDefaultMetals();
        
        // Load metals
        loadMetals();
        
        // Set up filtered list
        filteredMetals = new FilteredList<>(metalList, p -> true);
        tableMetals.setItems(filteredMetals);
    }
    
    private void setupTableColumns() {
        colMetalName.setCellValueFactory(new PropertyValueFactory<>("metalName"));
        colMetalType.setCellValueFactory(new PropertyValueFactory<>("metalType"));
        colPurity.setCellValueFactory(new PropertyValueFactory<>("purity"));
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
        btnSave.setOnAction(e -> saveMetal());
        btnUpdate.setOnAction(e -> updateMetal());
        btnClear.setOnAction(e -> clearForm());
        btnClose.setOnAction(e -> closeDialog());
        btnRefresh.setOnAction(e -> loadMetals());
        
        // Search functionality
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredMetals.setPredicate(metal -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return metal.getMetalName().toLowerCase().contains(lowerCaseFilter) ||
                       metal.getMetalType().toLowerCase().contains(lowerCaseFilter) ||
                       metal.getPurity().toLowerCase().contains(lowerCaseFilter);
            });
        });
        
        // Table row selection
        tableMetals.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadMetalForEditing(newSelection);
                }
            });
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
        txtDescription.setText(metal.getDescription());
        
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }
    
    private void loadMetals() {
        metalList.clear();
        metalList.addAll(metalService.getAllMetals());
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
        
        currentEditingMetal = null;
        isEditMode = false;
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        tableMetals.getSelectionModel().clearSelection();
    }
    
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
}