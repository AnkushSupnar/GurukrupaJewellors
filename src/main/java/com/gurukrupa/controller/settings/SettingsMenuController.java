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
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class SettingsMenuController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(SettingsMenuController.class);
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @Autowired
    private AlertNotification alert;
    
    @FXML
    private Button btnAddMetal;
    
    
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
            logger.info("Opening Application Settings");
            
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnAppSettings.getScene().getRoot();
            
            // Load the FXML
            Parent appSettings = stageManager.getSpringFXMLLoader().load("/fxml/settings/AppSettings.fxml");
            
            // Set the form in the center of the dashboard
            dashboard.setCenter(appSettings);
            
            logger.info("Application Settings loaded in dashboard successfully");
            
        } catch (Exception e) {
            logger.error("Error opening Application Settings: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error opening Application Settings: " + e.getMessage());
        }
    }
    
    private void openTaxConfiguration() {
        try {
            logger.info("Opening Tax Configuration");
            
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnConfigureTax.getScene().getRoot();
            
            // Load the FXML
            Parent taxConfiguration = stageManager.getSpringFXMLLoader().load("/fxml/settings/TaxConfiguration.fxml");
            
            // Set the form in the center of the dashboard
            dashboard.setCenter(taxConfiguration);
            
            logger.info("Tax Configuration loaded in dashboard successfully");
            
        } catch (Exception e) {
            logger.error("Error opening Tax Configuration: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error opening Tax Configuration: " + e.getMessage());
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
    
    
    private void openBankAccountForm() {
        try {
            logger.info("Opening Bank Account Form");
            
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnAddBank.getScene().getRoot();
            
            // Load the FXML
            Parent bankAccountForm = stageManager.getSpringFXMLLoader().load("/fxml/settings/BankAccountForm.fxml");
            
            // Set the bank account form in the center of the dashboard
            dashboard.setCenter(bankAccountForm);
            
            logger.info("Bank Account form loaded in dashboard successfully");
            
        } catch (Exception e) {
            logger.error("Error opening bank account form: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error opening bank account form: " + e.getMessage());
        }
    }
    
    private void viewAllBankAccounts() {
        try {
            logger.info("Opening Bank Accounts List");
            
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnViewBanks.getScene().getRoot();
            
            // Load the FXML
            Parent bankAccountList = stageManager.getSpringFXMLLoader().load("/fxml/settings/BankAccountList.fxml");
            
            // Set the bank account list in the center of the dashboard
            dashboard.setCenter(bankAccountList);
            
            logger.info("Bank Accounts list loaded in dashboard successfully");
            
        } catch (Exception e) {
            logger.error("Error opening bank accounts list: {}", e.getMessage());
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
            logger.info("Opening Add UPI Payment");
            
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnAddUPI.getScene().getRoot();
            
            // Load the FXML
            Parent addUPIPayment = stageManager.getSpringFXMLLoader().load("/fxml/settings/AddUPIPayment.fxml");
            
            // Set the form in the center of the dashboard
            dashboard.setCenter(addUPIPayment);
            
            logger.info("Add UPI Payment loaded in dashboard successfully");
            
        } catch (Exception e) {
            logger.error("Error opening Add UPI Payment: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error opening Add UPI Payment: " + e.getMessage());
        }
    }
    
    private void viewAllUPIPayments() {
        try {
            logger.info("Opening UPI Payment List");
            
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnViewUPI.getScene().getRoot();
            
            // Load the FXML
            Parent upiPaymentList = stageManager.getSpringFXMLLoader().load("/fxml/settings/UPIPaymentList.fxml");
            
            // Set the form in the center of the dashboard
            dashboard.setCenter(upiPaymentList);
            
            logger.info("UPI Payment List loaded in dashboard successfully");
            
        } catch (Exception e) {
            logger.error("Error opening UPI Payment List: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error opening UPI Payment List: " + e.getMessage());
        }
    }
}