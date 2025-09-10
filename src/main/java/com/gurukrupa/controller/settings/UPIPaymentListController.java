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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class UPIPaymentListController implements Initializable {
    
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
    private Button btnClose;
    
    @FXML
    private Label lblTotalRecords;
    
    @FXML
    private TableView<UPIPaymentMethod> tableView;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colAppName;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colUpiId;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colBankAccount;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colStatus;
    
    @FXML
    private TableColumn<UPIPaymentMethod, String> colCreatedDate;
    
    @FXML
    private TableColumn<UPIPaymentMethod, Void> colActions;
    
    private ObservableList<UPIPaymentMethod> upiPaymentList = FXCollections.observableArrayList();
    private FilteredList<UPIPaymentMethod> filteredData;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTableColumns();
        setupSearchFilter();
        loadUPIPaymentMethods();
        
        // Set button actions
        btnAddNew.setOnAction(e -> openAddUPIPaymentDialog());
        btnRefresh.setOnAction(e -> loadUPIPaymentMethods());
        btnClose.setOnAction(e -> closeWindow());
    }
    
    private void initializeTableColumns() {
        colAppName.setCellValueFactory(new PropertyValueFactory<>("appName"));
        colUpiId.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getUpiId() != null ? 
                cellData.getValue().getUpiId() : "-"));
        
        colBankAccount.setCellValueFactory(cellData -> {
            var bankAccount = cellData.getValue().getBankAccount();
            if (bankAccount != null) {
                return new SimpleStringProperty(bankAccount.getBankName() + " - " + bankAccount.getAccountNumber());
            }
            return new SimpleStringProperty("-");
        });
        
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActive() ? "Active" : "Inactive"));
        
        colStatus.setCellFactory(column -> new TableCell<UPIPaymentMethod, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label label = new Label(status);
                    if ("Active".equals(status)) {
                        label.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-padding: 4 12 4 12; -fx-background-radius: 12; -fx-font-weight: 600; -fx-font-size: 12px;");
                    } else {
                        label.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 4 12 4 12; -fx-background-radius: 12; -fx-font-weight: 600; -fx-font-size: 12px;");
                    }
                    setGraphic(label);
                }
            }
        });
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        colCreatedDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCreatedAt() != null ? 
                cellData.getValue().getCreatedAt().format(formatter) : "-"));
        
        setupActionColumn();
    }
    
    private void setupActionColumn() {
        colActions.setCellFactory(column -> new TableCell<UPIPaymentMethod, Void>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            
            {
                // Setup edit button
                editButton.setText("Edit");
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-font-size: 12px; -fx-font-weight: 600;");
                editButton.setTooltip(new Tooltip("Edit"));
                
                // Setup delete button  
                deleteButton.setText("Delete");
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-font-size: 12px; -fx-font-weight: 600;");
                deleteButton.setTooltip(new Tooltip("Delete"));
                
                editButton.setOnAction(e -> {
                    UPIPaymentMethod upi = getTableView().getItems().get(getIndex());
                    editUPIPayment(upi);
                });
                
                deleteButton.setOnAction(e -> {
                    UPIPaymentMethod upi = getTableView().getItems().get(getIndex());
                    deleteUPIPayment(upi);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5);
                    buttons.setAlignment(javafx.geometry.Pos.CENTER);
                    buttons.getChildren().addAll(editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
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
        });
        
        SortedList<UPIPaymentMethod> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);
    }
    
    private void loadUPIPaymentMethods() {
        try {
            upiPaymentList.clear();
            upiPaymentList.addAll(upiPaymentMethodService.getAllUPIPaymentMethods());
            updateTotalRecords();
        } catch (Exception e) {
            System.err.println("Error loading UPI payment methods: " + e.getMessage());
            alert.showError("Error loading UPI payment methods: " + e.getMessage());
        }
    }
    
    private void updateTotalRecords() {
        int total = filteredData.size();
        lblTotalRecords.setText("Total Records: " + total);
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
            
        } catch (Exception e) {
            e.printStackTrace();
            alert.showError("Error opening edit UPI payment dialog: " + e.getMessage());
        }
    }
    
    private void deleteUPIPayment(UPIPaymentMethod upiMethod) {
        boolean result = alert.showConfirmation(
            "Delete UPI Payment Method",
            "Are you sure you want to delete '" + upiMethod.getAppName() + "'? This action cannot be undone."
        );
        
        if (result) {
            try {
                upiPaymentMethodService.deleteUPIPaymentMethod(upiMethod.getId());
                loadUPIPaymentMethods();
                alert.showSuccess("UPI Payment Method deleted successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                alert.showError("Error deleting UPI payment method: " + e.getMessage());
            }
        }
    }
    
    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}