package com.gurukrupa.controller.settings;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.UPIPaymentMethod;
import com.gurukrupa.data.service.BankAccountService;
import com.gurukrupa.data.service.UPIPaymentMethodService;
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
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class UPIPaymentController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(UPIPaymentController.class);
    
    @Lazy
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    @Autowired
    private UPIPaymentMethodService upiPaymentMethodService;
    
    @Autowired
    private BankAccountService bankAccountService;
    
    @Autowired
    private AlertNotification alert;
    
    // Form controls
    @FXML
    private TextField txtUpiAppName;
    
    @FXML
    private TextField txtUpiId;
    
    @FXML
    private ComboBox<BankAccount> cmbBankAccount;
    
    @FXML
    private Button btnActiveStatus;
    
    @FXML
    private Button btnInactiveStatus;
    
    @FXML
    private Button btnSave;
    
    @FXML
    private Button btnUpdate;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Button btnClear;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Text txtFormTitle;
    
    // Table controls
    @FXML
    private TableView<UPIPaymentMethod> tableUpiPayments;
    
    @FXML
    private TableColumn<UPIPaymentMethod, Long> colId;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colAppName;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colUpiId;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colBankAccount;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colStatus;
    
    @FXML
    private TableColumn<UPIPaymentMethod, Void> colActions;
    
    // Search and filter controls
    @FXML
    private TextField txtSearch;
    
    @FXML
    private ComboBox<String> cmbFilterStatus;
    
    // Summary fields
    @FXML
    private Text txtTotalUpi;
    
    @FXML
    private Text txtActiveUpi;
    
    // Data management
    private ObservableList<UPIPaymentMethod> upiPaymentList = FXCollections.observableArrayList();
    private FilteredList<UPIPaymentMethod> filteredUpiPayments;
    private UPIPaymentMethod currentEditingUpi = null;
    private boolean isEditMode = false;
    private boolean isActive = true;
    
    private Stage dialogStage;
    private boolean saved = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load bank accounts
        loadBankAccounts();
        
        // Configure bank account combo box
        configureBankAccountComboBox();
        
        // Set up table columns
        setupTableColumns();
        
        // Set up event handlers
        setupEventHandlers();
        
        // Set up filters
        setupFilters();
        
        // Load initial data
        loadUpiPayments();
        
        // Set up filtered list
        filteredUpiPayments = new FilteredList<>(upiPaymentList, p -> true);
        tableUpiPayments.setItems(filteredUpiPayments);
        
        // Update summary
        updateSummary();
        
        // Set default status to active
        setActiveStatus(true);
        
        logger.info("UPI Payment Controller initialized successfully");
    }
    
    private void loadBankAccounts() {
        try {
            List<BankAccount> bankAccounts = bankAccountService.getAllActiveBankAccounts();
            cmbBankAccount.getItems().clear();
            cmbBankAccount.getItems().addAll(bankAccounts);
        } catch (Exception e) {
            System.err.println("Error loading bank accounts: " + e.getMessage());
            alert.showError("Error loading bank accounts: " + e.getMessage());
        }
    }
    
    private void configureBankAccountComboBox() {
        cmbBankAccount.setConverter(new StringConverter<BankAccount>() {
            @Override
            public String toString(BankAccount bankAccount) {
                if (bankAccount == null) return "";
                return bankAccount.getBankName() + " - " + bankAccount.getAccountNumber();
            }
            
            @Override
            public BankAccount fromString(String string) {
                return null; // Not needed for non-editable combo box
            }
        });
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAppName.setCellValueFactory(new PropertyValueFactory<>("appName"));
        colUpiId.setCellValueFactory(new PropertyValueFactory<>("upiId"));
        colBankAccount.setCellValueFactory(cellData -> {
            BankAccount bankAccount = cellData.getValue().getBankAccount();
            String accountInfo = bankAccount != null ? 
                bankAccount.getBankName() + " - " + bankAccount.getAccountNumber() : "";
            return new SimpleStringProperty(accountInfo);
        });
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActive() ? "Active" : "Inactive"));
        
        // Actions column with Edit and Toggle Status buttons
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
                    UPIPaymentMethod upi = getTableView().getItems().get(getIndex());
                    loadUpiForEditing(upi);
                });
                
                btnToggleStatus.setOnAction(event -> {
                    UPIPaymentMethod upi = getTableView().getItems().get(getIndex());
                    toggleUpiStatus(upi);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UPIPaymentMethod upi = getTableView().getItems().get(getIndex());
                    
                    // Update toggle button based on status
                    if (upi.isActive()) {
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
        // Button actions
        btnActiveStatus.setOnAction(e -> setActiveStatus(true));
        btnInactiveStatus.setOnAction(e -> setActiveStatus(false));
        btnSave.setOnAction(e -> saveUPIPaymentMethod());
        btnUpdate.setOnAction(e -> updateUPIPaymentMethod());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> refreshTable());
        
        // Initialize back button if it exists
        if (btnBack != null) {
            btnBack.setOnAction(e -> navigateBackToSettingsMenu());
        }
        
        // Search functionality
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Table row selection
        tableUpiPayments.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadUpiForEditing(newSelection);
                }
            });
    }
    
    private void setupFilters() {
        // Setup status filter
        cmbFilterStatus.setItems(FXCollections.observableArrayList(
            "All Status", "Active", "Inactive"
        ));
        cmbFilterStatus.getSelectionModel().selectFirst();
        
        cmbFilterStatus.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }
    
    private void loadUpiPayments() {
        try {
            upiPaymentList.clear();
            upiPaymentList.addAll(upiPaymentMethodService.getAllUPIPaymentMethods());
            updateSummary();
            logger.info("Loaded {} UPI payment methods", upiPaymentList.size());
        } catch (Exception e) {
            logger.error("Error loading UPI payment methods: {}", e.getMessage());
            alert.showError("Error loading UPI payment methods: " + e.getMessage());
        }
    }
    
    private void updateSummary() {
        long totalUpi = upiPaymentList.size();
        long activeUpi = upiPaymentList.stream()
            .filter(UPIPaymentMethod::isActive)
            .count();
        
        if (txtTotalUpi != null) txtTotalUpi.setText(String.valueOf(totalUpi));
        if (txtActiveUpi != null) txtActiveUpi.setText(String.valueOf(activeUpi));
    }
    
    private void loadUpiForEditing(UPIPaymentMethod upi) {
        currentEditingUpi = upi;
        isEditMode = true;
        
        // Populate form with UPI details
        txtUpiAppName.setText(upi.getAppName());
        txtUpiId.setText(upi.getUpiId() != null ? upi.getUpiId() : "");
        cmbBankAccount.setValue(upi.getBankAccount());
        setActiveStatus(upi.isActive());
        
        // Update UI for edit mode
        if (txtFormTitle != null) txtFormTitle.setText("Edit UPI Payment Method");
        if (btnSave != null) btnSave.setVisible(false);
        if (btnUpdate != null) btnUpdate.setVisible(true);
    }
    
    private void clearForm() {
        txtUpiAppName.clear();
        txtUpiId.clear();
        cmbBankAccount.setValue(null);
        setActiveStatus(true);
        
        // Reset to add mode
        currentEditingUpi = null;
        isEditMode = false;
        if (txtFormTitle != null) txtFormTitle.setText("Add UPI Payment Method");
        if (btnSave != null) btnSave.setVisible(true);
        if (btnUpdate != null) btnUpdate.setVisible(false);
        
        // Clear table selection
        tableUpiPayments.getSelectionModel().clearSelection();
    }
    
    private void refreshTable() {
        loadUpiPayments();
        clearForm();
        alert.showSuccess("UPI payment list refreshed successfully");
    }
    
    private void applyFilters() {
        filteredUpiPayments.setPredicate(upi -> {
            boolean matchesSearch = true;
            boolean matchesStatus = true;
            
            // Search filter
            String searchText = txtSearch.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                matchesSearch = upi.getAppName().toLowerCase().contains(lowerCaseFilter) ||
                               (upi.getUpiId() != null && upi.getUpiId().toLowerCase().contains(lowerCaseFilter)) ||
                               (upi.getBankAccount() != null && 
                                (upi.getBankAccount().getBankName().toLowerCase().contains(lowerCaseFilter) ||
                                 upi.getBankAccount().getAccountNumber().toLowerCase().contains(lowerCaseFilter)));
            }
            
            // Status filter
            String selectedStatus = cmbFilterStatus.getValue();
            if (selectedStatus != null && !selectedStatus.equals("All Status")) {
                matchesStatus = (selectedStatus.equals("Active") && upi.isActive()) ||
                               (selectedStatus.equals("Inactive") && !upi.isActive());
            }
            
            return matchesSearch && matchesStatus;
        });
        updateSummary();
    }
    
    private void toggleUpiStatus(UPIPaymentMethod upi) {
        try {
            upi.setActive(!upi.isActive());
            upiPaymentMethodService.updateUPIPaymentMethod(upi);
            
            if (upi.isActive()) {
                alert.showSuccess("UPI payment method activated successfully!");
            } else {
                alert.showSuccess("UPI payment method deactivated successfully!");
            }
            
            loadUpiPayments();
        } catch (Exception e) {
            alert.showError("Error changing UPI status: " + e.getMessage());
        }
    }
    
    private void setActiveStatus(boolean active) {
        isActive = active;
        if (active) {
            btnActiveStatus.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
            btnInactiveStatus.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #757575; -fx-font-weight: 600; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
        } else {
            btnActiveStatus.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #757575; -fx-font-weight: 600; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
            btnInactiveStatus.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: 600; -fx-background-radius: 20; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
        }
    }
    
    private void saveUPIPaymentMethod() {
        if (!validateForm()) {
            return;
        }
        
        try {
            if (!isEditMode) {
                // Create new UPI payment method
                UPIPaymentMethod upiMethod = new UPIPaymentMethod();
                upiMethod.setAppName(txtUpiAppName.getText().trim());
                upiMethod.setUpiId(txtUpiId.getText().trim().isEmpty() ? null : txtUpiId.getText().trim());
                upiMethod.setBankAccount(cmbBankAccount.getValue());
                upiMethod.setActive(isActive);
                
                // Save to database
                upiPaymentMethodService.saveUPIPaymentMethod(upiMethod);
                
                saved = true;
                alert.showSuccess("UPI Payment Method created successfully!");
                
                loadUpiPayments();
                clearForm();
                
                if (dialogStage != null) {
                    dialogStage.close();
                }
            } else {
                // Should not reach here in new implementation
                alert.showError("Please use the Update button to update a UPI payment method");
                return;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error saving UPI Payment Method: " + e.getMessage());
        }
    }
    
    private void updateUPIPaymentMethod() {
        try {
            if (currentEditingUpi == null) {
                alert.showError("No UPI payment method selected for update");
                return;
            }
            
            if (!validateForm()) {
                return;
            }
            
            // Update UPI payment method fields
            currentEditingUpi.setAppName(txtUpiAppName.getText().trim());
            currentEditingUpi.setUpiId(txtUpiId.getText().trim().isEmpty() ? null : txtUpiId.getText().trim());
            currentEditingUpi.setBankAccount(cmbBankAccount.getValue());
            currentEditingUpi.setActive(isActive);
            
            UPIPaymentMethod updatedUpi = upiPaymentMethodService.updateUPIPaymentMethod(currentEditingUpi);
            alert.showSuccess("UPI Payment Method updated successfully!");
            
            // Refresh table
            loadUpiPayments();
            clearForm();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error updating UPI Payment Method: " + e.getMessage());
        }
    }
    
    private boolean validateForm() {
        String errorMessage = "";
        
        if (txtUpiAppName.getText() == null || txtUpiAppName.getText().trim().isEmpty()) {
            errorMessage += "UPI App Name is required.\n";
        }
        
        if (cmbBankAccount.getValue() == null) {
            errorMessage += "Associated Bank Account is required.\n";
        }
        
        if (!errorMessage.isEmpty()) {
            alert.showError(errorMessage);
            return false;
        }
        
        return true;
    }
    
    private void navigateBackToSettingsMenu() {
        try {
            BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
            Parent settingsMenu = springFXMLLoader.load("/fxml/settings/SettingsMenu.fxml");
            dashboard.setCenter(settingsMenu);
            logger.info("Navigated back to Settings Menu");
        } catch (Exception e) {
            logger.error("Error navigating back: {}", e.getMessage());
            alert.showError("Error navigating back: " + e.getMessage());
        }
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        // Hide back button when in dialog mode
        if (btnBack != null) {
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        }
    }
    
    public boolean isSaved() {
        return saved;
    }
    
    public void setUPIPaymentMethod(UPIPaymentMethod upiMethod) {
        if (upiMethod != null) {
            loadUpiForEditing(upiMethod);
        }
    }
    
    public void setEditMode(UPIPaymentMethod upi) {
        if (upi != null) {
            loadUpiForEditing(upi);
        }
    }
}