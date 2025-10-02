package com.gurukrupa.controller.settings;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.service.BankAccountService;
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

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class BankAccountFormController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(BankAccountFormController.class);
    
    @Autowired
    private BankAccountService bankAccountService;
    
    @Autowired
    private AlertNotification alert;
    
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    // Form controls
    @FXML private TextField txtBankName, txtAccountNumber, txtIFSCCode, txtAccountHolderName,
                      txtBranchName, txtBranchAddress, txtOpeningBalance;
    @FXML private ComboBox<BankAccount.AccountType> cmbAccountType;
    @FXML private ComboBox<BankAccount.BalanceType> cmbBalanceType;
    @FXML private Text txtFormTitle;
    
    // Buttons
    @FXML private Button btnBack, btnSave, btnUpdate, btnClear, btnRefresh, btnClearFilters;
    
    // Table controls
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterType;
    @FXML private TableView<BankAccount> tableAccounts;
    @FXML private TableColumn<BankAccount, Long> colId;
    @FXML private TableColumn<BankAccount, String> colBankName;
    @FXML private TableColumn<BankAccount, String> colAccountNumber;
    @FXML private TableColumn<BankAccount, String> colAccountType;
    @FXML private TableColumn<BankAccount, String> colHolderName;
    @FXML private TableColumn<BankAccount, String> colBalance;
    @FXML private TableColumn<BankAccount, String> colStatus;
    @FXML private TableColumn<BankAccount, Void> colActions;
    @FXML private Text txtTotalAccounts, txtTotalBalance;
    
    // Data
    private ObservableList<BankAccount> accountList = FXCollections.observableArrayList();
    private FilteredList<BankAccount> filteredAccounts;
    private BankAccount currentEditingAccount = null;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize ComboBoxes
        setupComboBoxes();
        
        // Set up table columns
        setupTableColumns();
        
        // Set up event handlers
        setupEventHandlers();
        
        // Add input validation
        setupValidation();
        
        // Set up filters
        setupFilters();
        
        // Load initial data
        loadAccounts();
        
        // Set up filtered list
        filteredAccounts = new FilteredList<>(accountList, p -> true);
        tableAccounts.setItems(filteredAccounts);
        
        // Update summary
        updateSummary();
        
        logger.info("Bank Account Form Controller initialized successfully");
    }
    
    public void setEditMode(BankAccount account) {
        if (account != null) {
            loadAccountForEditing(account);
        }
    }
    
    private void setupComboBoxes() {
        // Setup Account Type ComboBox
        cmbAccountType.setItems(FXCollections.observableArrayList(BankAccount.AccountType.values()));
        cmbAccountType.setCellFactory(param -> new javafx.scene.control.ListCell<BankAccount.AccountType>() {
            @Override
            protected void updateItem(BankAccount.AccountType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        cmbAccountType.setButtonCell(new javafx.scene.control.ListCell<BankAccount.AccountType>() {
            @Override
            protected void updateItem(BankAccount.AccountType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        
        // Setup Balance Type ComboBox
        cmbBalanceType.setItems(FXCollections.observableArrayList(BankAccount.BalanceType.values()));
        cmbBalanceType.setCellFactory(param -> new javafx.scene.control.ListCell<BankAccount.BalanceType>() {
            @Override
            protected void updateItem(BankAccount.BalanceType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        cmbBalanceType.setButtonCell(new javafx.scene.control.ListCell<BankAccount.BalanceType>() {
            @Override
            protected void updateItem(BankAccount.BalanceType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        
        // Set default values
        cmbAccountType.setValue(BankAccount.AccountType.SAVINGS);
        cmbBalanceType.setValue(BankAccount.BalanceType.CREDIT);
        
        // Setup filter types
        List<String> filterTypes = new ArrayList<>();
        filterTypes.add("All Account Types");
        filterTypes.add("Savings Account");
        filterTypes.add("Current Account");
        cmbFilterType.setItems(FXCollections.observableArrayList(filterTypes));
        cmbFilterType.getSelectionModel().selectFirst();
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colBankName.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        colAccountNumber.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));
        colAccountType.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAccountType().getDisplayName()));
        colHolderName.setCellValueFactory(new PropertyValueFactory<>("accountHolderName"));
        colBalance.setCellValueFactory(cellData -> {
            BankAccount account = cellData.getValue();
            String balance = String.format("₹ %.2f", account.getCurrentBalance());
            return new SimpleStringProperty(balance);
        });
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getIsActive() ? "Active" : "Inactive"));
        
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
                    BankAccount account = getTableView().getItems().get(getIndex());
                    loadAccountForEditing(account);
                });
                
                btnToggleStatus.setOnAction(event -> {
                    BankAccount account = getTableView().getItems().get(getIndex());
                    toggleAccountStatus(account);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BankAccount account = getTableView().getItems().get(getIndex());
                    
                    // Update toggle button based on status
                    if (account.getIsActive()) {
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
        btnBack.setOnAction(e -> navigateBackToSettingsMenu());
        btnSave.setOnAction(e -> saveBankAccount());
        btnUpdate.setOnAction(e -> updateBankAccount());
        btnClear.setOnAction(e -> clearForm());
        btnRefresh.setOnAction(e -> refreshTable());
        btnClearFilters.setOnAction(e -> clearFilters());
        
        // Search functionality
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Table row selection
        tableAccounts.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadAccountForEditing(newSelection);
                }
            });
    }
    
    private void setupFilters() {
        cmbFilterType.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }
    
    private void applyFilters() {
        filteredAccounts.setPredicate(account -> {
            boolean matchesSearch = true;
            boolean matchesType = true;
            
            // Search filter
            String searchText = txtSearch.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                matchesSearch = account.getBankName().toLowerCase().contains(lowerCaseFilter) ||
                               account.getAccountNumber().toLowerCase().contains(lowerCaseFilter) ||
                               account.getAccountHolderName().toLowerCase().contains(lowerCaseFilter) ||
                               (account.getIfscCode() != null && account.getIfscCode().toLowerCase().contains(lowerCaseFilter));
            }
            
            // Type filter
            String selectedType = cmbFilterType.getValue();
            if (selectedType != null && !selectedType.equals("All Account Types")) {
                matchesType = selectedType.equals(account.getAccountType().getDisplayName());
            }
            
            return matchesSearch && matchesType;
        });
        updateSummary();
    }
    
    private void clearFilters() {
        txtSearch.clear();
        cmbFilterType.getSelectionModel().selectFirst();
    }
    
    private void navigateBackToSettingsMenu() {
        try {
            // Get the dashboard's center panel
            BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
            
            // Load the Settings Menu FXML
            Parent settingsMenu = springFXMLLoader.load("/fxml/settings/SettingsMenu.fxml");
            
            // Set the settings menu in the center of the dashboard
            dashboard.setCenter(settingsMenu);
            
            logger.info("Navigated back to Settings Menu");
            
        } catch (Exception e) {
            logger.error("Error navigating back to Settings Menu: {}", e.getMessage());
            alert.showError("Error navigating back: " + e.getMessage());
        }
    }
    
    private void loadAccounts() {
        try {
            accountList.clear();
            accountList.addAll(bankAccountService.getAllActiveBankAccounts());
            updateSummary();
            logger.info("Loaded {} bank accounts", accountList.size());
        } catch (Exception e) {
            logger.error("Error loading bank accounts: {}", e.getMessage());
            alert.showError("Error loading bank accounts: " + e.getMessage());
        }
    }
    
    private void refreshTable() {
        loadAccounts();
        clearFilters();
        clearForm();
        alert.showSuccess("Bank account list refreshed successfully");
    }
    
    private void updateSummary() {
        long totalAccounts = accountList.size();
        BigDecimal totalBalance = accountList.stream()
            .filter(BankAccount::getIsActive)
            .map(BankAccount::getCurrentBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        txtTotalAccounts.setText(String.valueOf(totalAccounts));
        txtTotalBalance.setText(String.format("₹ %.2f", totalBalance));
    }
    
    private void loadAccountForEditing(BankAccount account) {
        currentEditingAccount = account;
        isEditMode = true;
        
        // Populate form with account details
        txtBankName.setText(account.getBankName());
        txtAccountNumber.setText(account.getAccountNumber());
        txtIFSCCode.setText(account.getIfscCode());
        txtAccountHolderName.setText(account.getAccountHolderName());
        txtBranchName.setText(account.getBranchName() != null ? account.getBranchName() : "");
        txtBranchAddress.setText(account.getBranchAddress() != null ? account.getBranchAddress() : "");
        txtOpeningBalance.setText(account.getOpeningBalance().toString());
        cmbAccountType.setValue(account.getAccountType());
        cmbBalanceType.setValue(account.getBalanceType());
        
        // Disable editing of account number and opening balance in edit mode
        txtAccountNumber.setEditable(false);
        txtOpeningBalance.setEditable(false);
        
        // Update UI for edit mode
        txtFormTitle.setText("Edit Bank Account");
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }
    
    private void toggleAccountStatus(BankAccount account) {
        try {
            if (account.getIsActive()) {
                bankAccountService.deactivateBankAccount(account.getId());
                alert.showSuccess("Bank account deactivated successfully!");
            } else {
                bankAccountService.activateBankAccount(account.getId());
                alert.showSuccess("Bank account activated successfully!");
            }
            loadAccounts();
        } catch (Exception e) {
            alert.showError("Error changing account status: " + e.getMessage());
        }
    }
    
    private void setupValidation() {
        // Make numeric fields accept only numbers and decimal points
        makeNumericOnly(txtOpeningBalance);
        
        // Add uppercase transformation for IFSC code
        txtIFSCCode.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtIFSCCode.setText(newVal.toUpperCase());
            }
        });
        
        // Limit account number to numbers only
        txtAccountNumber.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                txtAccountNumber.setText(oldVal);
            }
        });
    }
    
    private void makeNumericOnly(TextField textField) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*(\\.\\d*)?")) {
                textField.setText(oldVal);
            }
        });
    }
    
    private void saveBankAccount() {
        try {
            // Validate required fields
            if (!validateFields()) {
                return;
            }
            
            // Get form data
            String bankName = txtBankName.getText().trim();
            String accountNumber = txtAccountNumber.getText().trim();
            String ifscCode = txtIFSCCode.getText().trim();
            String accountHolderName = txtAccountHolderName.getText().trim();
            BankAccount.AccountType accountType = cmbAccountType.getValue();
            String branchName = txtBranchName.getText().trim();
            String branchAddress = txtBranchAddress.getText().trim();
            BigDecimal openingBalance = new BigDecimal(txtOpeningBalance.getText().trim());
            BankAccount.BalanceType balanceType = cmbBalanceType.getValue();
            
            if (!isEditMode) {
                // Creating new bank account
                BankAccount newAccount = bankAccountService.createBankAccount(
                    bankName, accountNumber, ifscCode, accountHolderName,
                    accountType, branchName, branchAddress, openingBalance, balanceType
                );
                alert.showSuccess("Bank account created successfully!");
            } else {
                // Should not reach here in new implementation
                alert.showError("Please use the Update button to update an account");
                return;
            }
            
            loadAccounts();
            clearForm();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error saving bank account: " + e.getMessage());
        }
    }
    
    private void updateBankAccount() {
        try {
            if (currentEditingAccount == null) {
                alert.showError("No account selected for update");
                return;
            }
            
            // Validate required fields
            if (!validateFields()) {
                return;
            }
            
            // Update account fields (except account number and opening balance)
            currentEditingAccount.setBankName(txtBankName.getText().trim());
            currentEditingAccount.setIfscCode(txtIFSCCode.getText().trim());
            currentEditingAccount.setAccountHolderName(txtAccountHolderName.getText().trim());
            currentEditingAccount.setAccountType(cmbAccountType.getValue());
            currentEditingAccount.setBranchName(txtBranchName.getText().trim().isEmpty() ? null : txtBranchName.getText().trim());
            currentEditingAccount.setBranchAddress(txtBranchAddress.getText().trim().isEmpty() ? null : txtBranchAddress.getText().trim());
            currentEditingAccount.setBalanceType(cmbBalanceType.getValue());
            
            BankAccount updatedAccount = bankAccountService.updateBankAccount(currentEditingAccount);
            alert.showSuccess("Bank account updated successfully!");
            
            // Refresh table
            loadAccounts();
            clearForm();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error updating bank account: " + e.getMessage());
        }
    }
    
    private boolean validateFields() {
        if (txtBankName.getText().trim().isEmpty()) {
            alert.showError("Please enter bank name");
            txtBankName.requestFocus();
            return false;
        }
        
        if (txtAccountNumber.getText().trim().isEmpty()) {
            alert.showError("Please enter account number");
            txtAccountNumber.requestFocus();
            return false;
        }
        
        if (txtIFSCCode.getText().trim().isEmpty()) {
            alert.showError("Please enter IFSC code");
            txtIFSCCode.requestFocus();
            return false;
        }
        
        if (txtAccountHolderName.getText().trim().isEmpty()) {
            alert.showError("Please enter account holder name");
            txtAccountHolderName.requestFocus();
            return false;
        }
        
        if (cmbAccountType.getValue() == null) {
            alert.showError("Please select account type");
            cmbAccountType.requestFocus();
            return false;
        }
        
        if (txtOpeningBalance.getText().trim().isEmpty()) {
            alert.showError("Please enter opening balance");
            txtOpeningBalance.requestFocus();
            return false;
        }
        
        if (cmbBalanceType.getValue() == null) {
            alert.showError("Please select balance type");
            cmbBalanceType.requestFocus();
            return false;
        }
        
        try {
            new BigDecimal(txtOpeningBalance.getText().trim());
        } catch (NumberFormatException e) {
            alert.showError("Please enter valid opening balance");
            txtOpeningBalance.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void clearForm() {
        txtBankName.clear();
        txtAccountNumber.clear();
        txtIFSCCode.clear();
        txtAccountHolderName.clear();
        txtBranchName.clear();
        txtBranchAddress.clear();
        txtOpeningBalance.setText("0.00");
        cmbAccountType.setValue(BankAccount.AccountType.SAVINGS);
        cmbBalanceType.setValue(BankAccount.BalanceType.CREDIT);
        
        // Enable fields back
        txtAccountNumber.setEditable(true);
        txtOpeningBalance.setEditable(true);
        
        // Reset to add mode
        currentEditingAccount = null;
        isEditMode = false;
        txtFormTitle.setText("Add Bank Account");
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
        
        // Clear table selection
        tableAccounts.getSelectionModel().clearSelection();
    }
}