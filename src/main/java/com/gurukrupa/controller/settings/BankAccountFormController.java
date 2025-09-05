package com.gurukrupa.controller.settings;

import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.service.BankAccountService;
import com.gurukrupa.view.AlertNotification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

@Component
public class BankAccountFormController implements Initializable {
    
    @Autowired
    private BankAccountService bankAccountService;
    
    @Autowired
    private AlertNotification alert;
    
    @FXML
    private TextField txtBankName, txtAccountNumber, txtIFSCCode, txtAccountHolderName,
                      txtBranchName, txtBranchAddress, txtOpeningBalance;
    
    @FXML
    private ComboBox<BankAccount.AccountType> cmbAccountType;
    
    @FXML
    private ComboBox<BankAccount.BalanceType> cmbBalanceType;
    
    @FXML
    private Button btnSave, btnCancel, btnClear;
    
    private Stage dialogStage;
    private boolean saved = false;
    private BankAccount bankAccount; // For editing existing accounts
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize button actions
        btnSave.setOnAction(e -> saveBankAccount());
        btnCancel.setOnAction(e -> cancelForm());
        btnClear.setOnAction(e -> clearForm());
        
        // Initialize ComboBoxes
        setupComboBoxes();
        
        // Add input validation
        setupValidation();
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
            
            if (bankAccount == null) {
                // Creating new bank account
                bankAccount = bankAccountService.createBankAccount(
                    bankName, accountNumber, ifscCode, accountHolderName,
                    accountType, branchName, branchAddress, openingBalance, balanceType
                );
                alert.showSuccess("Bank account created successfully!");
            } else {
                // Updating existing bank account
                bankAccount.setBankName(bankName);
                bankAccount.setAccountNumber(accountNumber);
                bankAccount.setIfscCode(ifscCode);
                bankAccount.setAccountHolderName(accountHolderName);
                bankAccount.setAccountType(accountType);
                bankAccount.setBranchName(branchName);
                bankAccount.setBranchAddress(branchAddress);
                bankAccount.setOpeningBalance(openingBalance);
                bankAccount.setBalanceType(balanceType);
                
                bankAccount = bankAccountService.updateBankAccount(bankAccount);
                alert.showSuccess("Bank account updated successfully!");
            }
            
            saved = true;
            dialogStage.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error saving bank account: " + e.getMessage());
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
    }
    
    private void cancelForm() {
        saved = false;
        dialogStage.close();
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public boolean isSaved() {
        return saved;
    }
    
    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
        if (bankAccount != null) {
            populateForm();
        }
    }
    
    private void populateForm() {
        if (bankAccount != null) {
            txtBankName.setText(bankAccount.getBankName());
            txtAccountNumber.setText(bankAccount.getAccountNumber());
            txtIFSCCode.setText(bankAccount.getIfscCode());
            txtAccountHolderName.setText(bankAccount.getAccountHolderName());
            txtBranchName.setText(bankAccount.getBranchName());
            txtBranchAddress.setText(bankAccount.getBranchAddress());
            txtOpeningBalance.setText(bankAccount.getOpeningBalance().toString());
            cmbAccountType.setValue(bankAccount.getAccountType());
            cmbBalanceType.setValue(bankAccount.getBalanceType());
        }
    }
}