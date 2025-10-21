package com.gurukrupa.controller.purchase;


import com.gurukrupa.data.service.PurchaseInvoiceService;
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
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for Purchase Menu - handles all purchase-related operations
 */
@Component
public class PurchaseMenuController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseMenuController.class);
    @Autowired
    @Lazy
    private StageManager stageManager;

    @Autowired
    private PurchaseInvoiceService purchaseInvoiceService;

    @Autowired
    private com.gurukrupa.config.SpringFXMLLoader springFXMLLoader;

    // Button controls
    @FXML
    private Button btnPurchaseInvoice;

    @FXML
    private Button btnViewPurchases;

    @FXML
    private Button btnSupplierPayment;

    @FXML
    private Button btnViewPayments;

    @FXML
    private Button btnRefresh;

    // Statistics labels
    @FXML
    private Label lblTodayPurchases;

    @FXML
    private Label lblTodayPaid;

    @FXML
    private Label lblTodayInvoices;

    @FXML
    private Label lblMonthPurchases;

    @FXML
    private Label lblMonthPaid;

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up button actions
        btnPurchaseInvoice.setOnAction(e -> openPurchaseInvoiceDialog());
        btnViewPurchases.setOnAction(e -> openViewPurchasesDialog());
        btnSupplierPayment.setOnAction(e -> openSupplierPaymentDialog());
        btnViewPayments.setOnAction(e -> openViewPaymentsDialog());
        btnRefresh.setOnAction(e -> loadStatistics());

        // Load statistics on initialization
        Platform.runLater(this::loadStatistics);
    }

    /**
     * Load purchase statistics
     */
    private void loadStatistics() {
        // Add rotation animation to refresh button
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(500), btnRefresh);
        rotateTransition.setByAngle(360);
        rotateTransition.play();

        try {
            // Get today's purchases
            Double todayPurchases = purchaseInvoiceService.getTodaysTotalPurchases();
            if (todayPurchases != null) {
                lblTodayPurchases.setText(currencyFormatter.format(todayPurchases));
                lblTodayPaid.setText(currencyFormatter.format(todayPurchases)); // For now, same as total
            } else {
                lblTodayPurchases.setText(currencyFormatter.format(0));
                lblTodayPaid.setText(currencyFormatter.format(0));
            }

            // Get today's invoice count
            Long todayInvoiceCount = (long) purchaseInvoiceService.findTodaysInvoices().size();
            lblTodayInvoices.setText(todayInvoiceCount.toString());

            // Get this month's purchases
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59);

            Double monthPurchases = purchaseInvoiceService.getPurchasesByDateRange(startOfMonth, endOfMonth);
            if (monthPurchases != null) {
                lblMonthPurchases.setText(currencyFormatter.format(monthPurchases));
                lblMonthPaid.setText(currencyFormatter.format(monthPurchases)); // For now, same as total
            } else {
                lblMonthPurchases.setText(currencyFormatter.format(0));
                lblMonthPaid.setText(currencyFormatter.format(0));
            }

            LOG.info("Purchase statistics loaded - Today: {}, Invoices: {}, Month: {}",
                    todayPurchases, todayInvoiceCount, monthPurchases);

        } catch (Exception e) {
            LOG.error("Error loading purchase statistics: ", e);
            // Set default values on error
            lblTodayPurchases.setText(currencyFormatter.format(0));
            lblTodayPaid.setText(currencyFormatter.format(0));
            lblTodayInvoices.setText("0");
            lblMonthPurchases.setText(currencyFormatter.format(0));
            lblMonthPaid.setText(currencyFormatter.format(0));
        }
    }

    /**
     * Open Purchase Invoice dialog (Metal-based purchase)
     */
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
            dialog.setTitle("Metal Purchase Invoice - Gurukrupa Jewelry");
            dialog.setResizable(true);
            dialog.setMaximized(true);

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

    /**
     * Open View Purchases dialog
     */
    private void openViewPurchasesDialog() {
        LOG.info("Opening View Purchases dialog");
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());

            // Load the FXML
            Parent root = stageManager.getSpringFXMLLoader()
                    .load(FxmlView.VIEW_PURCHASES.getFxmlFile());

            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("View Purchases - Gurukrupa Jewelry");
            dialog.setResizable(true);
            dialog.setMaximized(true);

            // Show the dialog
            dialog.show();

            LOG.info("View Purchases dialog opened successfully");

        } catch (Exception e) {
            LOG.error("Error opening view purchases dialog: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Open Supplier Payment screen in the center of the home screen
     */
    private void openSupplierPaymentDialog() {
        LOG.info("Opening Supplier Payment screen");
        try {
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnSupplierPayment.getScene().getRoot();

            // Load the Supplier Payment FXML
            Parent supplierPayment = stageManager.getSpringFXMLLoader()
                    .load(FxmlView.SUPPLIER_PAYMENT.getFxmlFile());

            // Set the supplier payment screen in the center of the dashboard
            dashboard.setCenter(supplierPayment);

            LOG.info("Supplier Payment screen loaded in dashboard successfully");

        } catch (Exception e) {
            LOG.error("Error opening Supplier Payment screen: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Open View Payments screen in the center of the home screen
     */
    private void openViewPaymentsDialog() {
        LOG.info("Opening View Payments screen");
        try {
            // Get the dashboard's center panel through the parent hierarchy
            BorderPane dashboard = (BorderPane) btnViewPayments.getScene().getRoot();

            // Load the View Payments FXML
            Parent viewPayments = stageManager.getSpringFXMLLoader()
                    .load(FxmlView.VIEW_SUPPLIER_PAYMENTS.getFxmlFile());

            // Set the view payments screen in the center of the dashboard
            dashboard.setCenter(viewPayments);

            LOG.info("View Payments screen loaded in dashboard successfully");

        } catch (Exception e) {
            LOG.error("Error opening View Payments screen: {}", e.getMessage());
            e.printStackTrace();
        }
    }

}
