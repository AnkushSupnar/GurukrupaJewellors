package com.gurukrupa.controller.settings;

import com.gurukrupa.controller.master.MetalFormController;
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
public class SettingsMenuController implements Initializable {
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @Autowired
    private AlertNotification alert;
    
    @FXML
    private Button btnAddMetal;
    
    @FXML
    private Button btnMetalRate;
    
    @FXML
    private Button btnAppSettings;
    
    @FXML
    private Button btnConfigureTax;
    
    @FXML
    private Button btnBackup;
    
    @FXML
    private Button btnManageUsers;
    
    @FXML
    private Button btnAddBank;
    
    @FXML
    private Button btnViewBanks;
    
    @FXML
    private Button btnEditShopInfo;
    
    @FXML
    private Button btnAddUPI;
    
    @FXML
    private Button btnViewUPI;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up button actions
        btnAddMetal.setOnAction(event -> openMetalManagement());
        btnMetalRate.setOnAction(event -> openMetalRateManagement());
        btnAppSettings.setOnAction(event -> openAppSettings());
        btnConfigureTax.setOnAction(event -> openTaxConfiguration());
        btnBackup.setOnAction(event -> performBackup());
        btnManageUsers.setOnAction(event -> openUserManagement());
        
        // Business Information actions
        btnAddBank.setOnAction(event -> openBankAccountForm());
        btnViewBanks.setOnAction(event -> viewAllBankAccounts());
        btnEditShopInfo.setOnAction(event -> editShopInformation());
        
        // UPI Payment Methods actions
        btnAddUPI.setOnAction(event -> openAddUPIPayment());
        btnViewUPI.setOnAction(event -> viewAllUPIPayments());
    }
    
    private void openMetalManagement() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, MetalFormController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.METAL_FORM.getFxmlFile(), MetalFormController.class);
            
            Parent root = entry.getKey();
            MetalFormController controller = entry.getValue();
            
            // Set up the dialog stage in controller
            controller.setDialogStage(dialog);
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Metal Management");
            dialog.setResizable(false);
            
            // Show the dialog and wait for it to close
            dialog.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            alert.showError("Failed to open Metal Management: " + e.getMessage());
        }
    }
    
    private void openAppSettings() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, AppSettingsController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.APP_SETTINGS.getFxmlFile(), AppSettingsController.class);
            
            Parent root = entry.getKey();
            AppSettingsController controller = entry.getValue();
            
            // Set up the dialog stage in controller
            controller.setDialogStage(dialog);
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Application Settings");
            dialog.setResizable(false);
            
            // Show the dialog and wait for it to close
            dialog.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            alert.showError("Failed to open Application Settings: " + e.getMessage());
        }
    }
    
    private void openTaxConfiguration() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the Tax Configuration FXML
            Map.Entry<Parent, TaxConfigurationController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController("/fxml/settings/TaxConfiguration.fxml", TaxConfigurationController.class);
            
            Parent root = entry.getKey();
            TaxConfigurationController controller = entry.getValue();
            
            // Set up the dialog stage in controller
            controller.setDialogStage(dialog);
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Tax Configuration");
            dialog.setResizable(false);
            
            // Show the dialog and wait for it to close
            dialog.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            alert.showError("Failed to open Tax Configuration: " + e.getMessage());
        }
    }
    
    private void performBackup() {
        // TODO: Implement backup functionality
        alert.showSuccess("Backup feature will be available soon!");
    }
    
    private void openUserManagement() {
        // TODO: Implement user management
        alert.showSuccess("User Management feature will be available soon!");
    }
    
    private void openMetalRateManagement() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, MetalRateFormController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.METAL_RATE_FORM.getFxmlFile(), MetalRateFormController.class);
            
            Parent root = entry.getKey();
            MetalRateFormController controller = entry.getValue();
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Metal Rate Management");
            dialog.setResizable(true);
            dialog.setWidth(1200);
            dialog.setHeight(700);
            
            // Show the dialog and wait for it to close
            dialog.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            alert.showError("Failed to open Metal Rate Management: " + e.getMessage());
        }
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
    
    private void openAddUPIPayment() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the Add UPI Payment FXML
            Map.Entry<Parent, UPIPaymentController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.ADD_UPI_PAYMENT.getFxmlFile(), UPIPaymentController.class);
            
            Parent root = entry.getKey();
            UPIPaymentController controller = entry.getValue();
            
            // Set up the dialog stage in controller
            controller.setDialogStage(dialog);
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Add UPI Payment Method");
            dialog.setResizable(false);
            
            // Show the dialog and wait for it to close
            dialog.showAndWait();
            
            if (controller.isSaved()) {
                alert.showSuccess("UPI Payment Method added successfully!");
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            alert.showError("Error opening add UPI payment form: " + e.getMessage());
        }
    }
    
    private void viewAllUPIPayments() {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the UPI Payment List FXML
            Map.Entry<Parent, UPIPaymentListController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.UPI_PAYMENT_LIST.getFxmlFile(), UPIPaymentListController.class);
            
            Parent root = entry.getKey();
            UPIPaymentListController controller = entry.getValue();
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("UPI Payment Methods");
            dialog.setWidth(1000);
            dialog.setHeight(700);
            dialog.setResizable(true);
            
            // Show the dialog
            dialog.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            alert.showError("Error opening UPI payments list: " + e.getMessage());
        }
    }
}