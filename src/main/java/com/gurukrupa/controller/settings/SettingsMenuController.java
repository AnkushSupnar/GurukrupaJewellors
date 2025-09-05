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
    private Button btnAppSettings;
    
    @FXML
    private Button btnConfigureTax;
    
    @FXML
    private Button btnBackup;
    
    @FXML
    private Button btnManageUsers;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up button actions
        btnAddMetal.setOnAction(event -> openMetalManagement());
        btnAppSettings.setOnAction(event -> openAppSettings());
        btnConfigureTax.setOnAction(event -> openTaxConfiguration());
        btnBackup.setOnAction(event -> performBackup());
        btnManageUsers.setOnAction(event -> openUserManagement());
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
        // TODO: Implement tax configuration
        alert.showSuccess("Tax Configuration feature will be available soon!");
    }
    
    private void performBackup() {
        // TODO: Implement backup functionality
        alert.showSuccess("Backup feature will be available soon!");
    }
    
    private void openUserManagement() {
        // TODO: Implement user management
        alert.showSuccess("User Management feature will be available soon!");
    }
}