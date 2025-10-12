package com.gurukrupa.controller.report;

import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class ReportMenuController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportMenuController.class);
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @Autowired
    private AlertNotification alert;
    
    @FXML
    private Button btnCustomerReport;
    
    @FXML
    private Button btnSalesReport;
    
    @FXML
    private Button btnInventoryReport;
    
    @FXML
    private Button btnFinancialReport;
    
    @FXML
    private Button btnTaxReport;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing ReportMenuController");
        
        // Set up button actions
        btnCustomerReport.setOnAction(event -> handleCustomerReport());
        
        // Other reports can be implemented later
        if (btnSalesReport != null) {
            btnSalesReport.setOnAction(event -> alert.showSuccess("Sales Reports feature will be available soon!"));
        }
        if (btnInventoryReport != null) {
            btnInventoryReport.setOnAction(event -> alert.showSuccess("Inventory Reports feature will be available soon!"));
        }
        if (btnFinancialReport != null) {
            btnFinancialReport.setOnAction(event -> alert.showSuccess("Financial Reports feature will be available soon!"));
        }
        if (btnTaxReport != null) {
            btnTaxReport.setOnAction(event -> alert.showSuccess("Tax Reports feature will be available soon!"));
        }
    }
    
    @FXML
    private void handleCustomerReport() {
        try {
            logger.info("Opening Customer Report");
            
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnCustomerReport.getScene().getRoot();
            
            // Load the Customer Report FXML
            Parent customerReport = stageManager.getSpringFXMLLoader().load("/fxml/report/CustomerReport.fxml");
            
            // Set the customer report in the center of the dashboard
            dashboard.setCenter(customerReport);
            
            logger.info("Customer Report loaded in dashboard successfully");
            
        } catch (Exception e) {
            logger.error("Error opening Customer Report: {}", e.getMessage());
            e.printStackTrace();
            alert.showError("Error opening Customer Report: " + e.getMessage());
        }
    }
}