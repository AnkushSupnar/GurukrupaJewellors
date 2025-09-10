package com.gurukrupa.controller.settings;

import com.gurukrupa.data.service.AppSettingsService;
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
public class TaxConfigurationController implements Initializable {
    
    @Autowired
    private AppSettingsService appSettingsService;
    
    @Autowired
    private AlertNotification alert;
    
    @FXML
    private TextField txtGstRate, txtCgstRate, txtSgstRate;
    
    @FXML
    private Label lblCurrentGstRate, lblCurrentCgstRate, lblCurrentSgstRate;
    
    @FXML
    private Button btnSaveGstRate, btnSaveCgstRate, btnSaveSgstRate, btnClose;
    
    private Stage dialogStage;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize individual button actions
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
    
    private void refreshSettings() {
        try {
            // Initialize default settings if they don't exist
            appSettingsService.initializeDefaultSettings();
            
            // Load current settings
            Double currentGstRate = appSettingsService.getDefaultGstRate();
            Double currentCgstRate = appSettingsService.getDefaultCgstRate();
            Double currentSgstRate = appSettingsService.getDefaultSgstRate();
            
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
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            Stage stage = (Stage) btnClose.getScene().getWindow();
            stage.close();
        }
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
}