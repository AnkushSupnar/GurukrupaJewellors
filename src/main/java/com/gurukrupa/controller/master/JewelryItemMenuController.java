package com.gurukrupa.controller.master;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.service.JewelryItemService;
import com.gurukrupa.view.FxmlView;
import com.gurukrupa.view.StageManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class JewelryItemMenuController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(JewelryItemMenuController.class);
    
    @Autowired
    @Lazy
    private StageManager stageManager;
    
    @Autowired
    private JewelryItemService jewelryItemService;
    
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    @FXML
    private FlowPane mainFlowPane;
    
    // Jewelry Items
    @FXML
    private Button btnAddItem;
    @FXML
    private Button btnViewItems;
    
    // Customer Management
    @FXML
    private Button btnAddCustomer;
    @FXML
    private Button btnViewCustomers;
    
    // Categories Management
    @FXML
    private Button btnAddCategory;
    @FXML
    private Button btnViewCategories;
    
    // Metal Rates Management
    @FXML
    private Button btnUpdateRates;
    @FXML
    private Button btnViewRates;
    
    // Suppliers Management
    @FXML
    private Button btnAddSupplier;
    @FXML
    private Button btnViewSuppliers;
    
    // Settings & Configuration
    @FXML
    private Button btnBusinessSettings;
    @FXML
    private Button btnSystemConfig;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Jewelry Item Menu Controller with FlowPane layout");
        
        // Set up button actions
        setupButtonActions();
        
        // Add hover effects
        setupHoverEffects();
        
        logger.info("FlowPane initialized with {} children", mainFlowPane.getChildren().size());
    }
    
    private void openAddItemDialog() {
        logger.info("Opening Add Jewelry Item form");
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the FXML
            Parent root = springFXMLLoader.load(FxmlView.JEWELRY_ITEM_FORM.getFxmlFile());
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Jewelry Item Management");
            dialog.setResizable(true);
            dialog.setMaximized(true); // Open maximized for better form visibility
            
            // Show the dialog
            dialog.show();
            
            logger.info("Jewelry Item form opened successfully");
            
        } catch (Exception e) {
            logger.error("Error opening jewelry item form: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openViewItemsDialog() {
        logger.info("Opening View Jewelry Items form");
        try {
            // For now, open the same form which includes both add and view functionality
            openAddItemDialog();
        } catch (Exception e) {
            logger.error("Error opening view items form: {}", e.getMessage());
        }
    }
    
    private void setupButtonActions() {
        // Jewelry Items
        btnAddItem.setOnAction(event -> openAddItemDialog());
        btnViewItems.setOnAction(event -> openViewItemsDialog());
        
        // Customer Management
        btnAddCustomer.setOnAction(event -> openAddCustomerDialog());
        btnViewCustomers.setOnAction(event -> openViewCustomersDialog());
        
        // Categories Management
        btnAddCategory.setOnAction(event -> showNotImplemented("Add Category"));
        btnViewCategories.setOnAction(event -> showNotImplemented("Manage Categories"));
        
        // Metal Rates Management
        btnUpdateRates.setOnAction(event -> showNotImplemented("Update Metal Rates"));
        btnViewRates.setOnAction(event -> showNotImplemented("View Rates History"));
        
        // Suppliers Management
        btnAddSupplier.setOnAction(event -> showNotImplemented("Add Supplier"));
        btnViewSuppliers.setOnAction(event -> showNotImplemented("View Suppliers"));
        
        // Settings & Configuration
        btnBusinessSettings.setOnAction(event -> openBusinessSettingsDialog());
        btnSystemConfig.setOnAction(event -> showNotImplemented("System Configuration"));
    }
    
    private void setupHoverEffects() {
        // Add subtle hover effects for primary buttons
        setupPrimaryButtonHover(btnAddItem);
        setupPrimaryButtonHover(btnAddCustomer);
        setupPrimaryButtonHover(btnAddCategory);
        setupPrimaryButtonHover(btnUpdateRates);
        setupPrimaryButtonHover(btnAddSupplier);
        setupPrimaryButtonHover(btnBusinessSettings);
        
        // Add hover effects for secondary buttons
        setupSecondaryButtonHover(btnViewItems, "#1976D2");
        setupSecondaryButtonHover(btnViewCustomers, "#2E7D32");
        setupSecondaryButtonHover(btnViewCategories, "#F57C00");
        setupSecondaryButtonHover(btnViewRates, "#7B1FA2");
        setupSecondaryButtonHover(btnViewSuppliers, "#D32F2F");
        setupSecondaryButtonHover(btnSystemConfig, "#616161");
    }
    
    private void setupPrimaryButtonHover(Button button) {
        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "; -fx-scale-x: 1.05; -fx-scale-y: 1.05;");
        });
        button.setOnMouseExited(e -> {
            String style = button.getStyle().replace("; -fx-scale-x: 1.05; -fx-scale-y: 1.05;", "");
            button.setStyle(style);
        });
    }
    
    private void setupSecondaryButtonHover(Button button, String color) {
        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "; -fx-scale-x: 1.03; -fx-scale-y: 1.03; -fx-background-color: " + color + "22;");
        });
        button.setOnMouseExited(e -> {
            String style = button.getStyle()
                .replace("; -fx-scale-x: 1.03; -fx-scale-y: 1.03;", "")
                .replaceAll("; -fx-background-color: " + color + "22;", "");
            button.setStyle(style);
        });
    }
    
    private void openAddCustomerDialog() {
        logger.info("Opening Add Customer form");
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the customer form FXML
            Parent root = springFXMLLoader.load(FxmlView.CUSTOMER_FORM.getFxmlFile());
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Customer Management");
            dialog.setResizable(true);
            
            // Show the dialog
            dialog.show();
            
            logger.info("Customer form opened successfully");
            
        } catch (Exception e) {
            logger.error("Error opening customer form: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openViewCustomersDialog() {
        logger.info("Opening View Customers form");
        try {
            // For now, open the same customer form which includes both add and view functionality
            openAddCustomerDialog();
        } catch (Exception e) {
            logger.error("Error opening view customers form: {}", e.getMessage());
        }
    }
    
    private void openBusinessSettingsDialog() {
        logger.info("Opening Business Settings in center pane");
        try {
            // Find the main BorderPane from the Dashboard
            javafx.scene.Node node = mainFlowPane.getScene().getRoot();
            if (node instanceof javafx.scene.layout.BorderPane) {
                javafx.scene.layout.BorderPane mainPane = (javafx.scene.layout.BorderPane) node;
                javafx.scene.layout.Pane businessSettingsPane = springFXMLLoader.getPage("/fxml/settings/BusinessSettingsMenu.fxml");
                mainPane.setCenter(businessSettingsPane);
                logger.info("Business Settings loaded successfully in center pane");
            } else {
                logger.error("Could not find main BorderPane");
            }
        } catch (Exception e) {
            logger.error("Error opening business settings: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showNotImplemented(String featureName) {
        logger.info("Feature '{}' is not yet implemented", featureName);
        // TODO: Show a proper alert dialog
        System.out.println(featureName + " feature is not yet implemented");
    }
    
    public FlowPane getMainFlowPane() {
        return mainFlowPane;
    }
}