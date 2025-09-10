package com.gurukrupa.controller.settings;

import com.gurukrupa.data.entities.BankAccount;
import com.gurukrupa.data.entities.UPIPaymentMethod;
import com.gurukrupa.data.service.BankAccountService;
import com.gurukrupa.data.service.UPIPaymentMethodService;
import com.gurukrupa.view.AlertNotification;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class UPIPaymentController implements Initializable {
    
    @Autowired
    private UPIPaymentMethodService upiPaymentMethodService;
    
    @Autowired
    private BankAccountService bankAccountService;
    
    @Autowired
    private AlertNotification alert;
    
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
    private Button btnCancel;
    
    private Stage dialogStage;
    private boolean saved = false;
    private boolean isActive = true;
    private UPIPaymentMethod editingUpiMethod = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load bank accounts
        loadBankAccounts();
        
        // Configure bank account combo box
        configureBankAccountComboBox();
        
        // Set up button actions
        btnActiveStatus.setOnAction(e -> setActiveStatus(true));
        btnInactiveStatus.setOnAction(e -> setActiveStatus(false));
        btnSave.setOnAction(e -> saveUPIPaymentMethod());
        btnCancel.setOnAction(e -> handleCancel());
        
        // Set default status to active
        setActiveStatus(true);
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
            UPIPaymentMethod upiMethod;
            if (editingUpiMethod != null) {
                // Update existing
                upiMethod = editingUpiMethod;
            } else {
                // Create new
                upiMethod = new UPIPaymentMethod();
            }
            
            upiMethod.setAppName(txtUpiAppName.getText().trim());
            upiMethod.setUpiId(txtUpiId.getText().trim());
            upiMethod.setBankAccount(cmbBankAccount.getValue());
            upiMethod.setActive(isActive);
            
            // Save to database
            upiPaymentMethodService.saveUPIPaymentMethod(upiMethod);
            
            saved = true;
            alert.showSuccess("UPI Payment Method saved successfully!");
            
            if (dialogStage != null) {
                dialogStage.close();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error saving UPI Payment Method: " + e.getMessage());
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
    
    private void handleCancel() {
        saved = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    public boolean isSaved() {
        return saved;
    }
    
    public void setUPIPaymentMethod(UPIPaymentMethod upiMethod) {
        this.editingUpiMethod = upiMethod;
        if (upiMethod != null) {
            txtUpiAppName.setText(upiMethod.getAppName());
            txtUpiId.setText(upiMethod.getUpiId());
            cmbBankAccount.setValue(upiMethod.getBankAccount());
            setActiveStatus(upiMethod.isActive());
        }
    }
}