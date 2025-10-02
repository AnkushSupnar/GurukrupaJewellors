package com.gurukrupa.controller.settings;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.service.AppSettingsService;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.view.AlertNotification;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class AppSettingsController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(AppSettingsController.class);
    
    @Lazy
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    @Autowired
    private AppSettingsService appSettingsService;
    
    @Autowired
    private BillService billService;
    
    @Autowired
    private AlertNotification alert;
    
    @FXML
    private TextField txtBillPrefix;
    
    @FXML
    private Label lblCurrentPrefix;
    
    @FXML
    private Button btnSaveBillPrefix, btnClose, btnBack;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize individual button actions
        btnSaveBillPrefix.setOnAction(event -> saveBillPrefix());
        btnClose.setOnAction(event -> closeWindow());
        
        // Initialize back button if it exists
        if (btnBack != null) {
            btnBack.setOnAction(e -> navigateBackToSettingsMenu());
        }
        
        // Load current settings
        refreshSettings();
    }
    
    private void saveBillPrefix() {
        try {
            String newPrefix = txtBillPrefix.getText().trim().toUpperCase();
            
            if (newPrefix.isEmpty()) {
                alert.showError("Please enter a valid bill prefix");
                return;
            }
            
            if (newPrefix.length() > 10) {
                alert.showError("Bill prefix cannot be more than 10 characters");
                return;
            }
            
            if (!newPrefix.matches("^[A-Z0-9]+$")) {
                alert.showError("Bill prefix can only contain letters and numbers");
                return;
            }
            
            // Save the setting
            appSettingsService.setBillNumberPrefix(newPrefix);
            
            // Update display
            lblCurrentPrefix.setText(newPrefix);
            
            alert.showSuccess("Bill prefix saved successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error saving bill prefix: " + e.getMessage());
        }
    }
    
    
    private void resetToDefault() {
        try {
            // Reset to default values
            appSettingsService.setBillNumberPrefix("INV");
            
            // Update UI
            txtBillPrefix.setText("INV");
            refreshSettings();
            
            alert.showSuccess("Settings reset to default successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error resetting settings: " + e.getMessage());
        }
    }
    
    private void refreshSettings() {
        try {
            // Initialize default settings if they don't exist
            appSettingsService.initializeDefaultSettings();
            
            // Load current settings
            String currentPrefix = appSettingsService.getBillNumberPrefix();
            
            // Update Bill Number UI
            txtBillPrefix.setText(currentPrefix);
            lblCurrentPrefix.setText(currentPrefix);
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error loading settings: " + e.getMessage());
        }
    }
    
    
    
    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
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
    
    // Method to open this settings window from other controllers
    public void setDialogStage(Stage dialogStage) {
        // Hide back button when in dialog mode
        if (btnBack != null) {
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        }
    }
}