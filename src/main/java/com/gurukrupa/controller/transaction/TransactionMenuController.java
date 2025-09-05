package com.gurukrupa.controller.transaction;

import com.gurukrupa.controller.DashboardController;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class TransactionMenuController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    @Autowired
    @Lazy
    StageManager stageManager;
    @FXML
    private Button btnPurchaseInvoice;

    @FXML
    private Button btnBilling;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnPurchaseInvoice.setOnAction(e->stageManager.switchScene(FxmlView.PURCHASEINVOICE));
        btnBilling.setOnAction(e->openBillingDialog());
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
            
            LOG.info("Billing dialog opened successfully");
            
        } catch (Exception e) {
            LOG.error("Error opening billing dialog: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}