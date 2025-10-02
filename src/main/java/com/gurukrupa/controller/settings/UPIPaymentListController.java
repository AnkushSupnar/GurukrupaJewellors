package com.gurukrupa.controller.settings;

import com.gurukrupa.config.SpringFXMLLoader;
import com.gurukrupa.data.entities.UPIPaymentMethod;
import com.gurukrupa.data.service.UPIPaymentMethodService;
import com.gurukrupa.view.AlertNotification;
import com.gurukrupa.view.FxmlView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;

@Component
public class UPIPaymentListController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(UPIPaymentListController.class);
    
    @Autowired
    private UPIPaymentMethodService upiPaymentMethodService;
    
    @Autowired
    private AlertNotification alert;
    
    @Lazy
    @Autowired
    private SpringFXMLLoader springFXMLLoader;
    
    @FXML
    private TextField txtSearch;
    
    @FXML
    private Button btnAddNew;
    
    @FXML
    private Button btnRefresh;
    
    @FXML
    private Button btnBack;
    
    @FXML
    private Label lblTotalRecords;
    
    @FXML
    private FlowPane upiCardsContainer;
    
    @FXML
    private Text txtActiveCount;
    
    @FXML
    private Text txtInactiveCount;
    
    @FXML
    private Text txtLinkedBanks;
    
    private ObservableList<UPIPaymentMethod> upiPaymentList = FXCollections.observableArrayList();
    private FilteredList<UPIPaymentMethod> filteredData;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSearchFilter();
        loadUPIPaymentMethods();
        
        // Set button actions
        btnAddNew.setOnAction(e -> openAddUPIPaymentInDashboard());
        btnRefresh.setOnAction(e -> loadUPIPaymentMethods());
        
        // Initialize back button if it exists
        if (btnBack != null) {
            btnBack.setOnAction(e -> navigateBackToSettingsMenu());
        }
    }
    
    private VBox createUPICard(UPIPaymentMethod upiMethod) {
        VBox card = new VBox(15);
        card.setPrefWidth(300);
        card.setMinHeight(220);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-padding: 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 3); " +
                      "-fx-border-color: #E0E0E0; -fx-border-radius: 16; -fx-border-width: 1;");
        
        // Header with app icon and name
        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // App icon background with initials
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(50, 50);
        iconContainer.setMinSize(50, 50);
        iconContainer.setMaxSize(50, 50);
        iconContainer.setAlignment(javafx.geometry.Pos.CENTER);
        iconContainer.setStyle(getAppIconStyle(upiMethod.getAppName()) + 
                             "; -fx-background-radius: 25;");
        
        // App initials
        Label appInitials = new Label(getAppInitials(upiMethod.getAppName()));
        appInitials.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; " +
                           "-fx-text-fill: white; -fx-font-family: 'Segoe UI';");
        iconContainer.getChildren().add(appInitials);
        
        // App name and status
        VBox appInfo = new VBox(5);
        Label appName = new Label(upiMethod.getAppName());
        appName.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #212121;");
        
        // Status badge
        Label statusBadge = new Label(upiMethod.isActive() ? "Active" : "Inactive");
        if (upiMethod.isActive()) {
            statusBadge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; " +
                               "-fx-padding: 4 12 4 12; -fx-background-radius: 12; " +
                               "-fx-font-weight: 600; -fx-font-size: 11px;");
        } else {
            statusBadge.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; " +
                               "-fx-padding: 4 12 4 12; -fx-background-radius: 12; " +
                               "-fx-font-weight: 600; -fx-font-size: 11px;");
        }
        
        appInfo.getChildren().addAll(appName, statusBadge);
        header.getChildren().addAll(iconContainer, appInfo);
        
        // UPI ID section
        VBox upiIdSection = new VBox(5);
        Label upiIdLabel = new Label("UPI ID");
        upiIdLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575; -fx-font-weight: 600;");
        Label upiIdValue = new Label(upiMethod.getUpiId() != null ? upiMethod.getUpiId() : "Not provided");
        upiIdValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #424242;");
        upiIdSection.getChildren().addAll(upiIdLabel, upiIdValue);
        
        // Bank account section
        VBox bankSection = new VBox(5);
        Label bankLabel = new Label("Linked Bank Account");
        bankLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575; -fx-font-weight: 600;");
        
        if (upiMethod.getBankAccount() != null) {
            Label bankName = new Label(upiMethod.getBankAccount().getBankName());
            bankName.setStyle("-fx-font-size: 14px; -fx-text-fill: #424242; -fx-font-weight: bold;");
            Label accountNumber = new Label("A/C: " + upiMethod.getBankAccount().getAccountNumber());
            accountNumber.setStyle("-fx-font-size: 13px; -fx-text-fill: #616161;");
            bankSection.getChildren().addAll(bankLabel, bankName, accountNumber);
        } else {
            Label noBankLabel = new Label("No bank linked");
            noBankLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #9E9E9E; -fx-font-style: italic;");
            bankSection.getChildren().addAll(bankLabel, noBankLabel);
        }
        
        // Created date
        Label createdDate = new Label("Added on " + 
            (upiMethod.getCreatedAt() != null ? upiMethod.getCreatedAt().format(DATE_FORMATTER) : "-"));
        createdDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #9E9E9E;");
        
        // Separator
        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
        separator.setStyle("-fx-background-color: #E0E0E0;");
        
        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button editBtn = new Button("Edit");
        editBtn.setPrefWidth(80);
        editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-padding: 8 15 8 15; -fx-background-radius: 20; " +
                        "-fx-font-size: 12px; -fx-font-weight: 600;");
        editBtn.setOnAction(e -> editUPIPayment(upiMethod));
        
        Button toggleBtn = new Button(upiMethod.isActive() ? "Deactivate" : "Activate");
        toggleBtn.setPrefWidth(90);
        toggleBtn.setStyle("-fx-background-color: " + (upiMethod.isActive() ? "#FF9800" : "#4CAF50") + "; " +
                          "-fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 15 8 15; " +
                          "-fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: 600;");
        toggleBtn.setOnAction(e -> toggleUPIStatus(upiMethod));
        
        actions.getChildren().addAll(editBtn, toggleBtn);
        
        // Add hover effect
        card.setOnMouseEntered(e -> 
            card.setStyle(card.getStyle() + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);"));
        card.setOnMouseExited(e -> 
            card.setStyle(card.getStyle().replace("; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);", "")));
        
        // Add all components to card
        card.getChildren().addAll(header, upiIdSection, bankSection, createdDate, separator, actions);
        
        return card;
    }
    
    private String getAppIconStyle(String appName) {
        switch (appName.toLowerCase()) {
            case "google pay":
            case "gpay":
                return "-fx-background-color: #4285F4;";
            case "phonepe":
                return "-fx-background-color: #5E1F88;";
            case "paytm":
                return "-fx-background-color: #00B9F1;";
            case "amazon pay":
                return "-fx-background-color: #FF9900;";
            case "bhim":
                return "-fx-background-color: #FB8C00;";
            default:
                return "-fx-background-color: #3949AB;";
        }
    }
    
    private String getAppInitials(String appName) {
        if (appName == null || appName.trim().isEmpty()) {
            return "UP";
        }
        
        String[] words = appName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty() && Character.isLetter(word.charAt(0))) {
                initials.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        
        // If no initials found, use first two characters
        if (initials.length() == 0) {
            return appName.length() >= 2 ? 
                   appName.substring(0, 2).toUpperCase() : 
                   appName.toUpperCase();
        }
        
        // Return up to 2 characters
        return initials.length() > 2 ? 
               initials.substring(0, 2) : 
               initials.toString();
    }
    
    private void updateUPICards() {
        upiCardsContainer.getChildren().clear();
        
        // Sort by active status first (active methods come first)
        List<UPIPaymentMethod> sortedList = new ArrayList<>(filteredData);
        sortedList.sort((a, b) -> {
            // Active methods come first
            if (a.isActive() && !b.isActive()) return -1;
            if (!a.isActive() && b.isActive()) return 1;
            // If same status, sort by app name
            return a.getAppName().compareToIgnoreCase(b.getAppName());
        });
        
        for (UPIPaymentMethod upiMethod : sortedList) {
            VBox card = createUPICard(upiMethod);
            upiCardsContainer.getChildren().add(card);
        }
        
        updateSummaryCards();
        lblTotalRecords.setText("Total: " + filteredData.size());
    }
    
    private void updateSummaryCards() {
        int activeCount = 0;
        int inactiveCount = 0;
        Set<Long> uniqueBankIds = new HashSet<>();
        
        for (UPIPaymentMethod upiMethod : filteredData) {
            if (upiMethod.isActive()) {
                activeCount++;
            } else {
                inactiveCount++;
            }
            
            if (upiMethod.getBankAccount() != null) {
                uniqueBankIds.add(upiMethod.getBankAccount().getId());
            }
        }
        
        txtActiveCount.setText(String.valueOf(activeCount));
        txtInactiveCount.setText(String.valueOf(inactiveCount));
        txtLinkedBanks.setText(String.valueOf(uniqueBankIds.size()));
    }
    
    private void toggleUPIStatus(UPIPaymentMethod upiMethod) {
        try {
            upiMethod.setActive(!upiMethod.isActive());
            upiPaymentMethodService.saveUPIPaymentMethod(upiMethod);
            loadUPIPaymentMethods();
            alert.showSuccess("UPI Payment Method " + (upiMethod.isActive() ? "activated" : "deactivated") + " successfully!");
        } catch (Exception e) {
            logger.error("Error toggling UPI status: {}", e.getMessage());
            alert.showError("Error toggling UPI status: " + e.getMessage());
        }
    }
    
    private void setupSearchFilter() {
        filteredData = new FilteredList<>(upiPaymentList, p -> true);
        
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(upiMethod -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (upiMethod.getAppName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (upiMethod.getUpiId() != null && upiMethod.getUpiId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (upiMethod.getBankAccount() != null) {
                    String bankInfo = upiMethod.getBankAccount().getBankName() + " " + upiMethod.getBankAccount().getAccountNumber();
                    return bankInfo.toLowerCase().contains(lowerCaseFilter);
                }
                return false;
            });
            updateTotalRecords();
            updateUPICards();
        });
    }
    
    private void loadUPIPaymentMethods() {
        try {
            upiPaymentList.clear();
            upiPaymentList.addAll(upiPaymentMethodService.getAllUPIPaymentMethods());
            updateTotalRecords();
            updateUPICards();
        } catch (Exception e) {
            logger.error("Error loading UPI payment methods: {}", e.getMessage());
            alert.showError("Error loading UPI payment methods: " + e.getMessage());
        }
    }
    
    private void updateTotalRecords() {
        int total = filteredData.size();
        lblTotalRecords.setText("Total Records: " + total);
    }
    
    private void openAddUPIPaymentInDashboard() {
        try {
            // Check if we're in dashboard mode
            if (btnBack != null && btnBack.getScene() != null) {
                BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
                Parent addUPIPayment = springFXMLLoader.load(FxmlView.ADD_UPI_PAYMENT.getFxmlFile());
                dashboard.setCenter(addUPIPayment);
                logger.info("Opened Add UPI Payment form in dashboard");
            } else {
                // Fallback to dialog mode
                openAddUPIPaymentDialog();
            }
        } catch (Exception e) {
            logger.error("Error opening add UPI payment form: {}", e.getMessage());
            alert.showError("Error opening add UPI payment form: " + e.getMessage());
        }
    }
    
    private void openAddUPIPaymentDialog() {
        try {
            Stage stage = new Stage();
            stage.setTitle("Add UPI Payment Method");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Load FXML with controller
            Map.Entry<Parent, UPIPaymentController> entry = springFXMLLoader
                    .loadWithController(FxmlView.ADD_UPI_PAYMENT.getFxmlFile(), UPIPaymentController.class);
            
            Parent root = entry.getKey();
            UPIPaymentController controller = entry.getValue();
            
            stage.setScene(new Scene(root));
            controller.setDialogStage(stage);
            
            stage.showAndWait();
            
            if (controller.isSaved()) {
                loadUPIPaymentMethods();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error opening add UPI payment dialog: " + e.getMessage());
        }
    }
    
    private void editUPIPayment(UPIPaymentMethod upiMethod) {
        try {
            // Check if we're in dashboard mode
            if (btnBack != null && btnBack.getScene() != null) {
                BorderPane dashboard = (BorderPane) btnBack.getScene().getRoot();
                Map.Entry<Parent, UPIPaymentController> entry = springFXMLLoader
                        .loadWithController(FxmlView.ADD_UPI_PAYMENT.getFxmlFile(), UPIPaymentController.class);
                
                Parent root = entry.getKey();
                UPIPaymentController controller = entry.getValue();
                controller.setUPIPaymentMethod(upiMethod);
                
                dashboard.setCenter(root);
                logger.info("Opened Edit UPI Payment form in dashboard");
            } else {
                // Fallback to dialog mode
                Stage stage = new Stage();
                stage.setTitle("Edit UPI Payment Method");
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setResizable(false);
                
                // Load FXML with controller
                Map.Entry<Parent, UPIPaymentController> entry = springFXMLLoader
                        .loadWithController(FxmlView.ADD_UPI_PAYMENT.getFxmlFile(), UPIPaymentController.class);
                
                Parent root = entry.getKey();
                UPIPaymentController controller = entry.getValue();
                
                stage.setScene(new Scene(root));
                controller.setDialogStage(stage);
                controller.setUPIPaymentMethod(upiMethod);
                
                stage.showAndWait();
                
                if (controller.isSaved()) {
                    loadUPIPaymentMethods();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error opening edit UPI payment dialog: " + e.getMessage());
        }
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
    
    public void setDialogStage(Stage dialogStage) {
        // Hide back button when in dialog mode
        if (btnBack != null) {
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        }
    }
}