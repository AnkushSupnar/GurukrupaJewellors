package com.gurukrupa.controller.settings;

import com.gurukrupa.data.service.AppSettingsService;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.view.AlertNotification;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class AppSettingsController implements Initializable {
    
    @Autowired
    private AppSettingsService appSettingsService;
    
    @Autowired
    private BillService billService;
    
    @Autowired
    private AlertNotification alert;
    
    @FXML
    private TextField txtBillPrefix, txtGstRate, txtCgstRate, txtSgstRate;
    
    @FXML
    private Label lblCurrentPrefix, lblCurrentGstRate, lblCurrentCgstRate, lblCurrentSgstRate;
    
    @FXML
    private Button btnSaveBillPrefix, btnSaveGstRate, btnSaveCgstRate, btnSaveSgstRate, btnClose;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize individual button actions
        btnSaveBillPrefix.setOnAction(event -> saveBillPrefix());
        btnSaveGstRate.setOnAction(event -> saveGstRate());
        btnSaveCgstRate.setOnAction(event -> saveCgstRate());
        btnSaveSgstRate.setOnAction(event -> saveSgstRate());
        btnClose.setOnAction(event -> closeWindow());
        
        // Add GST rate change listener for auto-calculation
        txtGstRate.textProperty().addListener((obs, oldVal, newVal) -> updateGstRates(newVal));
        
        // Add numeric validation
        makeNumericOnly(txtGstRate, 5);
        makeNumericOnly(txtCgstRate, 5);
        makeNumericOnly(txtSgstRate, 5);
        
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
    
    private void saveGstRate() {
        try {
            String gstRateText = txtGstRate.getText().trim();
            if (gstRateText.isEmpty()) {
                alert.showError("Please enter a valid GST rate");
                return;
            }
            
            Double gstRate = Double.parseDouble(gstRateText);
            if (gstRate < 0 || gstRate > 100) {
                alert.showError("GST rate must be between 0 and 100");
                return;
            }
            
            // Save the setting
            appSettingsService.setDefaultGstRate(gstRate);
            
            // Update display
            lblCurrentGstRate.setText(String.format("%.2f%%", gstRate));
            
            alert.showSuccess("GST rate saved successfully!");
            
        } catch (NumberFormatException e) {
            alert.showError("Please enter a valid numeric GST rate");
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error saving GST rate: " + e.getMessage());
        }
    }
    
    private void saveCgstRate() {
        try {
            String cgstRateText = txtCgstRate.getText().trim();
            if (cgstRateText.isEmpty()) {
                alert.showError("Please enter a valid CGST rate");
                return;
            }
            
            Double cgstRate = Double.parseDouble(cgstRateText);
            if (cgstRate < 0 || cgstRate > 100) {
                alert.showError("CGST rate must be between 0 and 100");
                return;
            }
            
            // Save the setting
            appSettingsService.setDefaultCgstRate(cgstRate);
            
            // Update display
            lblCurrentCgstRate.setText(String.format("%.2f%%", cgstRate));
            
            alert.showSuccess("CGST rate saved successfully!");
            
        } catch (NumberFormatException e) {
            alert.showError("Please enter a valid numeric CGST rate");
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error saving CGST rate: " + e.getMessage());
        }
    }
    
    private void saveSgstRate() {
        try {
            String sgstRateText = txtSgstRate.getText().trim();
            if (sgstRateText.isEmpty()) {
                alert.showError("Please enter a valid SGST rate");
                return;
            }
            
            Double sgstRate = Double.parseDouble(sgstRateText);
            if (sgstRate < 0 || sgstRate > 100) {
                alert.showError("SGST rate must be between 0 and 100");
                return;
            }
            
            // Save the setting
            appSettingsService.setDefaultSgstRate(sgstRate);
            
            // Update display
            lblCurrentSgstRate.setText(String.format("%.2f%%", sgstRate));
            
            alert.showSuccess("SGST rate saved successfully!");
            
        } catch (NumberFormatException e) {
            alert.showError("Please enter a valid numeric SGST rate");
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error saving SGST rate: " + e.getMessage());
        }
    }
    
    private void resetToDefault() {
        try {
            // Reset to default values
            appSettingsService.setBillNumberPrefix("INV");
            appSettingsService.setDefaultGstRate(3.00);
            
            // Update UI
            txtBillPrefix.setText("INV");
            txtGstRate.setText("3.00");
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
            Double currentGstRate = appSettingsService.getDefaultGstRate();
            Double currentCgstRate = appSettingsService.getDefaultCgstRate();
            Double currentSgstRate = appSettingsService.getDefaultSgstRate();
            
            // Update Bill Number UI
            txtBillPrefix.setText(currentPrefix);
            lblCurrentPrefix.setText(currentPrefix);
            
            // Update GST Rate UI
            txtGstRate.setText(String.format("%.2f", currentGstRate));
            txtCgstRate.setText(String.format("%.2f", currentCgstRate));
            txtSgstRate.setText(String.format("%.2f", currentSgstRate));
            
            // Update current GST rate labels
            lblCurrentGstRate.setText(String.format("%.2f%%", currentGstRate));
            lblCurrentCgstRate.setText(String.format("%.2f%%", currentCgstRate));
            lblCurrentSgstRate.setText(String.format("%.2f%%", currentSgstRate));
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error loading settings: " + e.getMessage());
        }
    }
    
    
    private void updateGstRates(String gstRateText) {
        try {
            if (gstRateText == null || gstRateText.trim().isEmpty()) {
                txtCgstRate.setText("0.00");
                txtSgstRate.setText("0.00");
                return;
            }
            
            Double gstRate = Double.parseDouble(gstRateText.trim());
            Double halfRate = gstRate / 2;
            
            txtCgstRate.setText(String.format("%.2f", halfRate));
            txtSgstRate.setText(String.format("%.2f", halfRate));
            
        } catch (NumberFormatException e) {
            txtCgstRate.setText("0.00");
            txtSgstRate.setText("0.00");
        }
    }
    
    private void makeNumericOnly(TextField textField, int maxLength) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?$")) {
                textField.setText(oldValue);
            } else if (newValue.length() > maxLength) {
                textField.setText(oldValue);
            }
        });
    }
    
    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
    
    // Method to open this settings window from other controllers
    public void setDialogStage(Stage dialogStage) {
        // This method can be used if needed to set dialog stage reference
    }
}