package com.gurukrupa.controller.transaction;

import com.gurukrupa.controller.DashboardController;
import com.gurukrupa.data.service.BillService;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class TransactionMenuController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionMenuController.class);
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @Autowired
    private BillService billService;
    
    @FXML
    private Button btnPurchaseInvoice;
    
    @FXML
    private Button btnBilling;
    
    @FXML
    private Button btnViewBills;
    
    @FXML
    private Button btnViewPurchases;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Label lblTodaySales;
    
    @FXML
    private Label lblTodayBills;
    
    @FXML
    private Label lblMonthSales;
    
    @FXML
    private Label lblTodayCollected;
    
    @FXML
    private Label lblMonthCollected;
    
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnPurchaseInvoice.setOnAction(e -> openPurchaseInvoiceDialog());
        btnBilling.setOnAction(e -> openBillingDialog());
        btnViewBills.setOnAction(e -> openViewBillsDialog());
        btnViewPurchases.setOnAction(e -> {
            LOG.info("View Purchases button clicked - functionality to be implemented");
            // TODO: Implement view purchases functionality
        });
        btnRefresh.setOnAction(e -> loadStatistics());
        
        // Load statistics on initialization
        Platform.runLater(this::loadStatistics);
    }
    
    private void loadStatistics() {
        // Add rotation animation to refresh button
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(500), btnRefresh);
        rotateTransition.setByAngle(360);
        rotateTransition.play();
        
        try {
            // Get today's sales
            Double todaySales = billService.getTodaysTotalSales();
            if (todaySales != null) {
                lblTodaySales.setText(currencyFormatter.format(todaySales));
            } else {
                lblTodaySales.setText(currencyFormatter.format(0));
            }
            
            // Get today's collected amount
            Double todayCollected = billService.getTodaysCollectedAmount();
            if (todayCollected != null) {
                lblTodayCollected.setText(currencyFormatter.format(todayCollected));
            } else {
                lblTodayCollected.setText(currencyFormatter.format(0));
            }
            
            // Get today's bill count
            Long todayBillCount = billService.getTodaysBillCount();
            lblTodayBills.setText(todayBillCount != null ? todayBillCount.toString() : "0");
            
            // Get this month's sales
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
            
            Double monthSales = billService.getSalesByDateRange(startOfMonth, endOfMonth);
            if (monthSales != null) {
                lblMonthSales.setText(currencyFormatter.format(monthSales));
            } else {
                lblMonthSales.setText(currencyFormatter.format(0));
            }
            
            // Get this month's collected amount
            Double monthCollected = billService.getCollectedAmountByDateRange(startOfMonth, endOfMonth);
            if (monthCollected != null) {
                lblMonthCollected.setText(currencyFormatter.format(monthCollected));
            } else {
                lblMonthCollected.setText(currencyFormatter.format(0));
            }
            
            LOG.info("Statistics loaded successfully - Today's Sales: {}, Collected: {}, Bills: {}, Month Sales: {}, Month Collected: {}", 
                     todaySales, todayCollected, todayBillCount, monthSales, monthCollected);
            
        } catch (Exception e) {
            LOG.error("Error loading statistics: ", e);
            // Set default values on error
            lblTodaySales.setText(currencyFormatter.format(0));
            lblTodayCollected.setText(currencyFormatter.format(0));
            lblTodayBills.setText("0");
            lblMonthSales.setText(currencyFormatter.format(0));
            lblMonthCollected.setText(currencyFormatter.format(0));
        }
    }
    
    private void openBillingDialog() {
        LOG.info("Opening Billing dialog");
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, BillingController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.BILLING.getFxmlFile(), BillingController.class);
            
            Parent root = entry.getKey();
            BillingController controller = entry.getValue();
            
            // Set up the dialog stage in controller
            controller.setDialogStage(dialog);
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Daily Billing System - Gurukrupa Jewelry");
            dialog.setResizable(true);
            dialog.setMaximized(true); // Open maximized for better visibility
            
            // Show the dialog
            dialog.show();
            
            // Refresh statistics when dialog is closed
            dialog.setOnHidden(event -> loadStatistics());
            
            LOG.info("Billing dialog opened successfully");
            
        } catch (Exception e) {
            LOG.error("Error opening billing dialog: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openViewBillsDialog() {
        LOG.info("Opening View Bills dialog");
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, ViewBillsController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.VIEW_BILLS.getFxmlFile(), ViewBillsController.class);
            
            Parent root = entry.getKey();
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("View Bills - Gurukrupa Jewelry");
            dialog.setResizable(true);
            dialog.setMaximized(true);
            
            // Show the dialog
            dialog.show();
            
            LOG.info("View Bills dialog opened successfully");
            
        } catch (Exception e) {
            LOG.error("Error opening view bills dialog: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openPurchaseInvoiceDialog() {
        LOG.info("Opening Purchase Invoice dialog");
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML and get both the root and controller
            Map.Entry<Parent, PurchaseInvoiceController> entry = stageManager.getSpringFXMLLoader()
                    .loadWithController(FxmlView.PURCHASE_INVOICE.getFxmlFile(), PurchaseInvoiceController.class);
            
            Parent root = entry.getKey();
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Purchase Invoice - Gurukrupa Jewelry");
            dialog.setResizable(true);
            dialog.setMaximized(true); // Open maximized for better visibility
            
            // Show the dialog
            dialog.show();
            
            // Refresh statistics when dialog is closed
            dialog.setOnHidden(event -> loadStatistics());
            
            LOG.info("Purchase Invoice dialog opened successfully");
            
        } catch (Exception e) {
            LOG.error("Error opening purchase invoice dialog: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}