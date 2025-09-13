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
    
    // Metal Types
    @FXML
    private Button btnAddMetal;
    @FXML
    private Button btnViewMetals;
    
    // Metal Rates Management
    @FXML
    private Button btnUpdateRates;
    @FXML
    private Button btnViewRates;
    
    // Categories Management
    @FXML
    private Button btnAddCategory;
    @FXML
    private Button btnViewCategories;
    
    // Suppliers Management
    @FXML
    private Button btnAddSupplier;
    @FXML
    private Button btnViewSuppliers;
    
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Master Menu Controller");
        
        // Set up button actions
        setupButtonActions();
        
        // Add hover effects
        setupHoverEffects();
        
        logger.info("Master Menu initialized successfully");
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
        
        // Metal Types
        btnAddMetal.setOnAction(event -> openMetalForm());
        btnViewMetals.setOnAction(event -> openMetalForm());
        
        // Metal Rates Management
        btnUpdateRates.setOnAction(event -> showNotImplemented("Update Metal Rates"));
        btnViewRates.setOnAction(event -> showNotImplemented("View Rates History"));
        
        // Categories Management
        btnAddCategory.setOnAction(event -> showNotImplemented("Add Category"));
        btnViewCategories.setOnAction(event -> showNotImplemented("Manage Categories"));
        
        // Suppliers Management
        btnAddSupplier.setOnAction(event -> openAddSupplierDialog());
        btnViewSuppliers.setOnAction(event -> openAddSupplierDialog()); // For now, use same form for view
    }
    
    private void setupHoverEffects() {
        // Add subtle hover effects for primary buttons
        setupPrimaryButtonHover(btnAddItem);
        setupPrimaryButtonHover(btnAddCustomer);
        setupPrimaryButtonHover(btnAddMetal);
        setupPrimaryButtonHover(btnUpdateRates);
        setupPrimaryButtonHover(btnAddCategory);
        setupPrimaryButtonHover(btnAddSupplier);
        
        // Add hover effects for secondary buttons
        setupSecondaryButtonHover(btnViewItems, "#1976D2");
        setupSecondaryButtonHover(btnViewCustomers, "#388E3C");
        setupSecondaryButtonHover(btnViewMetals, "#F57C00");
        setupSecondaryButtonHover(btnViewRates, "#7B1FA2");
        setupSecondaryButtonHover(btnViewCategories, "#00796B");
        setupSecondaryButtonHover(btnViewSuppliers, "#C62828");
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
    
    private void openMetalForm() {
        logger.info("Opening Metal Form");
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the metal form FXML
            Parent root = springFXMLLoader.load(FxmlView.METAL_FORM.getFxmlFile());
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Metal Management");
            dialog.setResizable(true);
            
            // Show the dialog
            dialog.show();
            
            logger.info("Metal form opened successfully");
            
        } catch (Exception e) {
            logger.error("Error opening metal form: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openAddSupplierDialog() {
        logger.info("Opening Add Supplier form");
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stageManager.getPrimaryStage());
            
            // Load the supplier form FXML
            Parent root = springFXMLLoader.load(FxmlView.ADD_SUPPLIER.getFxmlFile());
            
            // Set up the dialog
            dialog.setScene(new Scene(root));
            dialog.setTitle("Supplier Management");
            dialog.setResizable(true);
            
            // Show the dialog
            dialog.show();
            
            logger.info("Supplier form opened successfully");
            
        } catch (Exception e) {
            logger.error("Error opening supplier form: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showNotImplemented(String featureName) {
        logger.info("Feature '{}' is not yet implemented", featureName);
        // TODO: Show a proper alert dialog
        System.out.println(featureName + " feature is not yet implemented");
    }
    
}