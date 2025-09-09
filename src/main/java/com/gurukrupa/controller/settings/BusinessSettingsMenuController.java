package com.gurukrupa.controller.settings;

import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class BusinessSettingsMenuController implements Initializable {
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @Autowired
    private AlertNotification alert;
    
    @FXML
    private Button btnAddBank, btnViewBanks, btnEditShopInfo, btnViewShopInfo,
                   btnTaxSettings, btnViewTaxSettings, btnBackupSettings, btnSecuritySettings;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bank Management buttons
        btnAddBank.setOnAction(e -> openBankAccountForm());
        btnViewBanks.setOnAction(e -> viewAllBankAccounts());
        
        // Shop Information buttons
        btnEditShopInfo.setOnAction(e -> editShopInformation());
        btnViewShopInfo.setOnAction(e -> viewShopInformation());
        
        // Tax Settings buttons
        btnTaxSettings.setOnAction(e -> configureTaxSettings());
        btnViewTaxSettings.setOnAction(e -> viewTaxSettings());
        
        // Backup & Security buttons
        btnBackupSettings.setOnAction(e -> configureBackupSettings());
        btnSecuritySettings.setOnAction(e -> configureSecuritySettings());
    }
    
    private void openBankAccountForm() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the Bank Account Form FXML
            Map.Entry<Parent, BankAccountFormController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController("/fxml/settings/BankAccountForm.fxml", BankAccountFormController.class);
            
            Parent root = entry.getKey();
            BankAccountFormController controller = entry.getValue();
            
            // Set up the dialog stage in controller
            controller.setDialogStage(dialog);
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Add New Bank Account");
            dialog.setResizable(false);
            
            // Show the dialog and wait for it to close
            dialog.showAndWait();
            
            // Handle the result after dialog closes
            if (controller.isSaved()) {
                System.out.println("Bank account saved successfully!");
                alert.showSuccess("Bank account added successfully!");
            } else {
                System.out.println("Bank account addition was cancelled");
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            alert.showError("Error opening bank account form: " + e.getMessage());
        }
    }
    
    private void viewAllBankAccounts() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the Bank Account List FXML
            Map.Entry<Parent, BankAccountListController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController("/fxml/settings/BankAccountList.fxml", BankAccountListController.class);
            
            Parent root = entry.getKey();
            BankAccountListController controller = entry.getValue();
            
            // Set the dialog stage in controller
            controller.setDialogStage(dialog);
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Bank Accounts Management");
            dialog.setWidth(1200);
            dialog.setHeight(700);
            dialog.setResizable(true);
            
            // Show the dialog
            dialog.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            alert.showError("Error opening bank accounts list: " + e.getMessage());
        }
    }
    
    private void editShopInformation() {
        // TODO: Implement edit shop information functionality
        alert.showSuccess("Edit Shop Information feature will be implemented soon!");
    }
    
    private void viewShopInformation() {
        // TODO: Implement view shop information functionality
        alert.showSuccess("View Shop Information feature will be implemented soon!");
    }
    
    private void configureTaxSettings() {
        // TODO: Implement tax settings configuration
        alert.showSuccess("Tax Settings Configuration feature will be implemented soon!");
    }
    
    private void viewTaxSettings() {
        // TODO: Implement view tax settings functionality
        alert.showSuccess("View Tax Settings feature will be implemented soon!");
    }
    
    private void configureBackupSettings() {
        // TODO: Implement backup settings configuration
        alert.showSuccess("Backup Settings Configuration feature will be implemented soon!");
    }
    
    private void configureSecuritySettings() {
        // TODO: Implement security settings configuration
        alert.showSuccess("Security Settings Configuration feature will be implemented soon!");
    }
}